package com.garbo.repository;

import com.garbo.core.entity.*;
import com.garbo.core.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class RepositoriesIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        r.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired RouteAssignmentRepository routeAssignmentRepository;
    @Autowired RouteVehicleRouteRepository vehicleRouteRepository;
    @Autowired RouteBinStopRepository binStopRepository;
    @Autowired VehicleRepository vehicleRepository;
    @Autowired BinCollectorRepository collectorRepository;
    @Autowired EntityManager entityManager;

    @Test
    void routeAssignmentRepository_findBySessionId_roundtrip() {
        Vehicle v = new Vehicle(); v.setLicensePlate("ABC-123");
        vehicleRepository.save(v);

        BinCollector driver = new BinCollector(); driver.setEmpName("Joe"); driver.setEmail("joe@example.com");
        collectorRepository.save(driver);

        RouteAssignment a = new RouteAssignment();
        UUID sessionId = UUID.randomUUID();
        a.setSessionId(sessionId);
        a.setVehicle(v);
        a.setDriver(driver);
        routeAssignmentRepository.save(a);

        Optional<RouteAssignment> found = routeAssignmentRepository.findBySessionId(sessionId);
        assertTrue(found.isPresent());
        assertEquals(v.getLicensePlate(), found.get().getVehicle().getLicensePlate());
    }

    @Test
    void vehicleRoute_and_binStop_persistence_and_query() {
        UUID sessionId = UUID.randomUUID();
        RouteVehicleRoute vr = new RouteVehicleRoute();
        vr.setSessionId(sessionId);
        vr.setVehicleKey("0");
        vr.setCapacity(10);
        vr.setTotalBins(2);
        vr.setEstimatedDurationSeconds(100.0);

        RouteBinStop s1 = new RouteBinStop(); s1.setStopOrder(1); s1.setBinId(100L); s1.setLat(1.0); s1.setLng(2.0);
        RouteBinStop s2 = new RouteBinStop(); s2.setStopOrder(2); s2.setBinId(101L); s2.setLat(1.1); s2.setLng(2.1);
        s1.setVehicleRoute(vr); s2.setVehicleRoute(vr);
        vr.getBinStops().add(s1); vr.getBinStops().add(s2);

        vehicleRouteRepository.save(vr);

        List<RouteVehicleRoute> routes = vehicleRouteRepository.findBySessionIdWithStops(sessionId);
        assertFalse(routes.isEmpty());
        RouteVehicleRoute persisted = routes.get(0);
        assertEquals(2, persisted.getBinStops().size());

        RouteBinStop persistedStop = persisted.getBinStops().get(0);
        int updated = binStopRepository.markCollected(persistedStop.getId(), LocalDateTime.now());
        assertEquals(1, updated);

        // The bulk @Modifying update above bypasses Hibernate's persistence
        // context, so the entity loaded earlier (persistedStop / anything
        // cached under the same id) is now stale. Clear the context so the
        // next findById() actually re-queries the database instead of
        // returning the cached in-memory object.
        entityManager.clear();

        var reloaded = binStopRepository.findById(persistedStop.getId()).orElseThrow();
        assertEquals("COLLECTED", reloaded.getStatus());
        assertNotNull(reloaded.getCollectedAt());

        // second mark should not update (status no longer PENDING)
        int updated2 = binStopRepository.markCollected(persistedStop.getId(), LocalDateTime.now());
        assertEquals(0, updated2);
    }
}