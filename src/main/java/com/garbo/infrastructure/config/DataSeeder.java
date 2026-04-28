package com.garbo.infrastructure.config;

import com.garbo.core.entity.BinCollector;
import com.garbo.core.entity.Citizen;
import com.garbo.core.entity.FieldMentor;
import com.garbo.core.entity.ThirdPartyCollector;
import com.garbo.core.repository.FieldMentorRepository;
import com.garbo.core.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final FieldMentorRepository fieldMentorRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    public DataSeeder(UserRepository userRepository,
            FieldMentorRepository fieldMentorRepository,
            PasswordEncoder passwordEncoder,
            EntityManager entityManager) {
        this.userRepository = userRepository;
        this.fieldMentorRepository = fieldMentorRepository;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedMobileTestUsers();
        seedFieldMentorDemoUser();
        assignBinsToSasindu();
    }

    private void seedMobileTestUsers() {
        seedCitizens();
        seedBinCollector();
        seedThirdPartyCollectors();
    }

    private void seedCitizens() {
        seedCitizen("Citizen Demo One", "citizen.one@garbo.com", "Citizen123", "0771000001",
                "123 Galle Road, Colombo 04", "Colombo");
        seedCitizen("Citizen Demo Two", "citizen.two@garbo.com", "Citizen123", "0771000002",
                "45 Peradeniya Road, Kandy", "Kandy");
        seedCitizen("Citizen Demo Three", "citizen.three@garbo.com", "Citizen123", "0771000003",
                "78 Wakwella Road, Galle", "Galle");
    }

    private void seedCitizen(String name, String email, String password, String phone, String address, String area) {
        if (userRepository.findFirstByEmailIgnoreCase(email).isPresent()) {
            System.out.println("Citizen already exists, skipping seed: " + email);
            return;
        }

        Citizen citizen = new Citizen();
        citizen.setEmpName(name);
        citizen.setEmail(email);
        citizen.setPassword(passwordEncoder.encode(password));
        citizen.setRole("CITIZEN");
        citizen.setPhone(phone);
        citizen.setAddress(address);
        citizen.setDefaultAddress(address);
        citizen.setArea(area);
        citizen.setReportCount(0);
        userRepository.save(citizen);
        System.out.println("Seeded citizen: " + email);
    }

    private void seedBinCollector() {
        if (userRepository.findFirstByEmailIgnoreCase("collector.test@garbo.com").isPresent()) {
            System.out.println("Bin collector test user already exists, skipping seed.");
            return;
        }

        BinCollector collector = new BinCollector();
        collector.setEmpName("Collection Team Test User");
        collector.setEmail("collector.test@garbo.com");
        collector.setPassword(passwordEncoder.encode("Collector123"));
        collector.setRole("BIN_COLLECTOR");
        collector.setPhone("0770000002");
        collector.setAssignedZone("Zone A");
        collector.setTeam("Collection Team A");
        collector.setWorkShift("Morning");
        collector.setOnDuty(true);
        collector.setCompletedCollections(0);
        collector.setMissedCollections(0);
        collector.setRewardPoints(0);
        userRepository.save(collector);
        System.out.println("Seeded bin collector test user: collector.test@garbo.com");
    }

    private void seedThirdPartyCollectors() {
        seedThirdPartyCollector("Third Party Collector One", "thirdparty.one@garbo.com", "ThirdParty123",
                "0772000001", "199012345678", "Garbo Partner Logistics", "TPC-001",
                LocalDate.of(2026, 4, 15), LocalDate.of(2027, 4, 15));
        seedThirdPartyCollector("Third Party Collector Two", "thirdparty.two@garbo.com", "ThirdParty123",
                "0772000002", "199012345679", "EcoCycle Lanka", "TPC-002",
                LocalDate.of(2026, 4, 15), LocalDate.of(2027, 4, 15));
        seedThirdPartyCollector("Third Party Collector Three", "thirdparty.three@garbo.com", "ThirdParty123",
                "0772000003", "199012345680", "GreenLoop Services", "TPC-003",
                LocalDate.of(2026, 4, 15), LocalDate.of(2027, 4, 15));
    }

    private void seedThirdPartyCollector(String name, String email, String password, String phone, String nic,
            String company, String contractId, LocalDate contractStart, LocalDate contractEnd) {
        if (userRepository.findFirstByEmailIgnoreCase(email).isPresent()) {
            System.out.println("Third-party collector already exists, skipping seed: " + email);
            return;
        }

        ThirdPartyCollector thirdPartyCollector = new ThirdPartyCollector();
        thirdPartyCollector.setEmpName(name);
        thirdPartyCollector.setEmail(email);
        thirdPartyCollector.setPassword(passwordEncoder.encode(password));
        thirdPartyCollector.setRole("THIRD_PARTY_COLLECTOR");
        thirdPartyCollector.setPhone(phone);
        thirdPartyCollector.setNIC(nic);
        thirdPartyCollector.setCompany(company);
        thirdPartyCollector.setContractId(contractId);
        thirdPartyCollector.setContractStart(contractStart);
        thirdPartyCollector.setContractEnd(contractEnd);
        thirdPartyCollector.setCompletedRequests(0);
        userRepository.save(thirdPartyCollector);
        System.out.println("Seeded third-party collector: " + email);
    }

    private void seedFieldMentorDemoUser() {
        // Only seed if this user doesn't already exist
        if (userRepository.findFirstByEmailIgnoreCase("sasindu@gmail.com").isEmpty()) {
            String dialect = String.valueOf(entityManager.getEntityManagerFactory()
                    .getProperties().getOrDefault("hibernate.dialect", ""));

            // The sequence reset query is PostgreSQL-specific and can mark the
            // transaction rollback-only on H2 even if we catch the exception.
            if (dialect.contains("PostgreSQL")) {
                entityManager.createNativeQuery(
                        "SELECT setval('users_emp_id_seq', (SELECT COALESCE(MAX(emp_id), 0) FROM users))")
                        .getSingleResult();
                System.out.println("Reset users_emp_id_seq to current max emp_id.");
            } else {
                System.out.println("Skipping PostgreSQL sequence reset for dialect: " + dialect);
            }

            FieldMentor fm = new FieldMentor();
            fm.setEmpName("Sasindu");
            fm.setEmail("sasindu@gmail.com");
            fm.setPassword(passwordEncoder.encode("Sj1234"));
            fm.setRole("FIELD_MENTOR");
            fm.setPhone("0771234567");
            fm.setAssignedZone("Zone A");
            fm.setWorkShift("Morning");
            fm.setOnDuty(true);
            fm.setRewardPoints(0);
            fieldMentorRepository.save(fm);
            System.out.println("Seeded field mentor: sasindu@gmail.com");
        } else {
            System.out.println("Field mentor sasindu@gmail.com already exists, skipping seed.");
        }
    }

    private void assignBinsToSasindu() {
        try {
            // Find Sasindu's emp_id
            Object result = entityManager.createNativeQuery(
                    "SELECT emp_id FROM users WHERE LOWER(email) = 'sasindu@gmail.com'").getSingleResult();

            Long sasinduEmpId = ((Number) result).longValue();

            // Demo data: realistic locations, categories, statuses, zones
            String[][] demoData = {
                    // { location, type, status, zone }
                    { "Galle Road, Colombo 03", "public", "notChecked", "Zone A" },
                    { "Kandy City Center, Kandy", "commercial", "notChecked", "Zone A" },
                    { "Viharamahadevi Park, Colombo 07", "park", "notChecked", "Zone A" },
                    { "Faculty of Science, Peradeniya", "education", "notChecked", "Zone B" },
                    { "Colombo South Hospital, Kalubowila", "medical", "notChecked", "Zone A" },
            };

            // First: update bins already assigned to Sasindu that have missing location
            // data
            @SuppressWarnings("unchecked")
            List<String> existingBinIds = entityManager.createNativeQuery(
                    "SELECT id FROM bins WHERE assigned_to = :empId AND location IS NULL")
                    .setParameter("empId", sasinduEmpId).getResultList();

            int updated = 0;
            for (int i = 0; i < existingBinIds.size(); i++) {
                String binId = existingBinIds.get(i);
                String[] data = demoData[i % demoData.length];

                entityManager.createNativeQuery(
                        "UPDATE bins SET location = :location, type = :type, status = :status, zone = :zone " +
                                "WHERE id = :binId")
                        .setParameter("location", data[0])
                        .setParameter("type", data[1])
                        .setParameter("status", data[2])
                        .setParameter("zone", data[3])
                        .setParameter("binId", binId)
                        .executeUpdate();
                updated++;
            }

            if (updated > 0) {
                System.out.println("Updated " + updated + " existing bins with demo data for Sasindu.");
            }

            // Second: if Sasindu has fewer than 5 bins total, assign more unassigned bins
            Object countResult = entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM bins WHERE assigned_to = :empId").setParameter("empId", sasinduEmpId)
                    .getSingleResult();

            long assignedCount = ((Number) countResult).longValue();

            if (assignedCount >= 5) {
                System.out.println("Sasindu has " + assignedCount + " bins assigned. Done.");
                return;
            }

            int needed = (int) (5 - assignedCount);

            @SuppressWarnings("unchecked")
            List<String> newBinIds = entityManager.createNativeQuery(
                    "SELECT id FROM bins WHERE assigned_to IS NULL LIMIT :lim").setParameter("lim", needed)
                    .getResultList();

            int assigned = 0;
            for (int i = 0; i < newBinIds.size(); i++) {
                String binId = newBinIds.get(i);
                String[] data = demoData[(int) (assignedCount + i) % demoData.length];

                entityManager.createNativeQuery(
                        "UPDATE bins SET assigned_to = :empId, " +
                                "location = :location, type = :type, status = :status, zone = :zone " +
                                "WHERE id = :binId")
                        .setParameter("empId", sasinduEmpId)
                        .setParameter("location", data[0])
                        .setParameter("type", data[1])
                        .setParameter("status", data[2])
                        .setParameter("zone", data[3])
                        .setParameter("binId", binId)
                        .executeUpdate();
                assigned++;
            }

            System.out.println(
                    "Assigned " + assigned + " new bins with demo data to Sasindu (emp_id=" + sasinduEmpId + ").");
        } catch (Exception e) {
            System.out.println("Could not assign bins to Sasindu: " + e.getMessage());
        }
    }
}
