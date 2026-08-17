package com.garbo.core.service;

import com.garbo.core.entity.Vehicle;
import com.garbo.core.repository.BinCollectorRepository;
import com.garbo.core.repository.RouteAssignmentRepository;
import com.garbo.core.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private BinCollectorRepository binCollectorRepository;

    @Mock
    private RouteAssignmentRepository routeAssignmentRepository;

    @Mock
    private com.garbo.core.service.security.SystemIncidentService systemIncidentService;

    @InjectMocks
    private VehicleService vehicleService;

    private Vehicle validVehicle;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(vehicleService, "systemIncidentService", systemIncidentService);
        validVehicle = new Vehicle();
        validVehicle.setId(1L);
        validVehicle.setLicensePlate("AB-1234");
        validVehicle.setAssignedCouncil("CouncilA");
        validVehicle.setStatus("available");
    }


    @Test
    void getAll_success() {
        when(vehicleRepository.findAll()).thenReturn(List.of(validVehicle));
        
        List<Vehicle> result = vehicleService.getAll();

        assertEquals(1, result.size());
        assertEquals("AB-1234", result.get(0).getLicensePlate());
        verify(vehicleRepository, times(1)).findAll();
    }

    @Test
    void create_success() {
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(validVehicle);

        Vehicle result = vehicleService.create(validVehicle);

        assertEquals("AB-1234", result.getLicensePlate());
        assertEquals("CouncilA", result.getAssignedCouncil());
        verify(vehicleRepository, times(1)).save(any(Vehicle.class));
    }

    @Test
    void create_missingLicensePlate_throwsException() {
        validVehicle.setLicensePlate(null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> vehicleService.create(validVehicle));
        assertEquals("License plate is required", ex.getMessage());
    }

    @Test
    void delete_success() {
        when(vehicleRepository.existsById(1L)).thenReturn(true);
        doNothing().when(routeAssignmentRepository).deleteByVehicleId(1L);
        doNothing().when(vehicleRepository).deleteById(1L);

        vehicleService.delete(1L);

        verify(routeAssignmentRepository, times(1)).deleteByVehicleId(1L);
        verify(vehicleRepository, times(1)).deleteById(1L);
    }

    @Test
    void getByCouncil_success() {
        when(vehicleRepository.findByAssignedCouncil("CouncilA")).thenReturn(List.of(validVehicle));
        
        List<Vehicle> result = vehicleService.getByCouncil("CouncilA");

        assertEquals(1, result.size());
        assertEquals("AB-1234", result.get(0).getLicensePlate());
        verify(vehicleRepository, times(1)).findByAssignedCouncil("CouncilA");
    }

    @Test
    void update_success() {
        Vehicle updatePayload = new Vehicle();
        updatePayload.setLicensePlate("XYZ-9876");
        updatePayload.setAssignedCouncil("CouncilB");

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(validVehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(validVehicle);

        Vehicle result = vehicleService.update(1L, updatePayload);

        assertEquals("XYZ-9876", result.getLicensePlate());
        assertEquals("CouncilB", result.getAssignedCouncil());
        verify(vehicleRepository, times(1)).save(validVehicle);
    }

    @Test
    void update_notFound() {
        when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

        Vehicle updatePayload = new Vehicle();
        updatePayload.setLicensePlate("XYZ-9876");

        java.util.NoSuchElementException ex = assertThrows(java.util.NoSuchElementException.class, () -> vehicleService.update(999L, updatePayload));
        assertEquals("Vehicle not found", ex.getMessage());
    }

    @Test
    void updateStatus_success() {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(validVehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(validVehicle);

        Vehicle result = vehicleService.updateStatus(1L, "in_maintenance");

        assertEquals("in_maintenance", result.getStatus());
        verify(vehicleRepository, times(1)).save(validVehicle);
    }

    @Test
    void updateStatus_notFound() {
        when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

        java.util.NoSuchElementException ex = assertThrows(java.util.NoSuchElementException.class, () -> vehicleService.updateStatus(999L, "in_maintenance"));
        assertEquals("Vehicle not found", ex.getMessage());
    }
}
