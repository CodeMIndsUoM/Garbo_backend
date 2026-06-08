package com.garbo.core.service;

import com.garbo.core.entity.Citizen;
import com.garbo.core.repository.CitizenRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class AdminCitizenService {

    private final CitizenRepository citizenRepository;
    private final CouncilAccessService councilAccessService;

    public AdminCitizenService(CitizenRepository citizenRepository, CouncilAccessService councilAccessService) {
        this.citizenRepository = citizenRepository;
        this.councilAccessService = councilAccessService;
    }

    public List<Citizen> listCitizens(String requesterEmail, String councilFilter) {
        if (councilAccessService.isSuperAdmin(requesterEmail)) {
            if (councilFilter != null && !councilFilter.isBlank()) {
                return sortByName(citizenRepository.findByCouncilIgnoreCase(councilFilter.trim()));
            }
            return sortByName(citizenRepository.findAll());
        }

        String adminCouncil = councilAccessService.resolveCouncilForEmail(requesterEmail)
                .orElseThrow(() -> new AccessDeniedException("Council admin profile is required"));
        return sortByName(citizenRepository.findByCouncilIgnoreCase(adminCouncil));
    }

    private List<Citizen> sortByName(List<Citizen> citizens) {
        return citizens.stream()
                .sorted(Comparator.comparing(
                        c -> c.getEmpName() == null ? "" : c.getEmpName(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
