package com.garbo.core.service;

import com.garbo.api.dto.CitizenRegisterRequest;
import com.garbo.core.entity.Citizen;
import com.garbo.core.entity.Council;
import com.garbo.core.repository.CitizenRepository;
import com.garbo.core.repository.CouncilRepository;
import com.garbo.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class CitizenServiceTest {

    @Mock
    private CitizenRepository citizenRepository;

    @Mock
    private CouncilRepository councilRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CitizenService citizenService;

    private CitizenRegisterRequest validRequest;
    private Council validCouncil;

    @BeforeEach
    void setUp() {
        validRequest = new CitizenRegisterRequest();
        validRequest.setEmail("test@example.com");
        validRequest.setPassword("password123");
        validRequest.setCouncil("CouncilA");
        validRequest.setFullName("John Doe");

        validCouncil = new Council();
        validCouncil.setName("CouncilA");
        validCouncil.setActive(true);
    }

    @Test
    void registerCitizen_success() {
        when(userRepository.findFirstByEmailIgnoreCase("test@example.com")).thenReturn(Optional.empty());
        when(councilRepository.findByNameIgnoreCase("CouncilA")).thenReturn(Optional.of(validCouncil));
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pwd");

        Citizen mockCitizen = new Citizen();
        mockCitizen.setEmail("test@example.com");
        mockCitizen.setEmpName("John Doe");

        when(citizenRepository.save(any(Citizen.class))).thenReturn(mockCitizen);

        Citizen result = citizenService.registerCitizen(validRequest);

        assertEquals("test@example.com", result.getEmail());
        assertEquals("John Doe", result.getEmpName());
        verify(citizenRepository, times(1)).save(any(Citizen.class));
    }

    @Test
    void registerCitizen_emailMissing_throwsException() {
        validRequest.setEmail(null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> citizenService.registerCitizen(validRequest));
        assertEquals("Email is required", ex.getMessage());
    }

    @Test
    void registerCitizen_emailAlreadyExists_throwsException() {
        when(userRepository.findFirstByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(new Citizen()));
        
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> citizenService.registerCitizen(validRequest));
        assertEquals("An account with this email already exists", ex.getMessage());
    }

    @Test
    void registerCitizen_invalidCouncil_throwsException() {
        when(userRepository.findFirstByEmailIgnoreCase("test@example.com")).thenReturn(Optional.empty());
        when(councilRepository.findByNameIgnoreCase("CouncilA")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> citizenService.registerCitizen(validRequest));
        assertEquals("Invalid or inactive council", ex.getMessage());
    }
}
