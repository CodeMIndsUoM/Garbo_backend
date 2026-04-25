package com.garbo.core.service;

import com.garbo.core.entity.AdminNew;
import com.garbo.core.entity.Citizen;
import com.garbo.core.entity.User;
import com.garbo.core.repository.AdminNewRepository;
import com.garbo.core.repository.CitizenRepository;
import com.garbo.core.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouncilAccessServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CitizenRepository citizenRepository;

    @Mock
    private AdminNewRepository adminNewRepository;

    @InjectMocks
    private CouncilAccessService councilAccessService;

    @Test
    void resolveCouncilForEmail_prefersCitizenCouncil() {
        Citizen citizen = new Citizen();
        citizen.setCouncil("Colombo");

        when(citizenRepository.findFirstByEmailIgnoreCase("citizen@garbo.com"))
                .thenReturn(Optional.of(citizen));

        Optional<String> council = councilAccessService.resolveCouncilForEmail("citizen@garbo.com");
        assertTrue(council.isPresent());
        assertEquals("Colombo", council.get());
    }

    @Test
    void resolveCouncilForEmail_fallsBackToAdminCouncil() {
        AdminNew admin = new AdminNew();
        admin.setCouncil("Kandy");

        when(citizenRepository.findFirstByEmailIgnoreCase("admin@garbo.com"))
                .thenReturn(Optional.empty());
        when(adminNewRepository.findFirstByEmailIgnoreCase("admin@garbo.com"))
                .thenReturn(Optional.of(admin));

        Optional<String> council = councilAccessService.resolveCouncilForEmail("admin@garbo.com");
        assertTrue(council.isPresent());
        assertEquals("Kandy", council.get());
    }

    @Test
    void isSuperAdmin_returnsTrueForSuperadminRole() {
        User user = new User();
        user.setRole("SUPERADMIN");
        when(userRepository.findFirstByEmailIgnoreCase("sa@garbo.com"))
                .thenReturn(Optional.of(user));

        assertTrue(councilAccessService.isSuperAdmin("sa@garbo.com"));
    }
}
