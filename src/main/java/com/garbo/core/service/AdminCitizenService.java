package com.garbo.core.service;

import com.garbo.core.entity.Citizen;
import com.garbo.core.repository.CitizenRepository;
import com.garbo.core.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class AdminCitizenService {

    private static final Logger log = LoggerFactory.getLogger(AdminCitizenService.class);

    private final CitizenRepository citizenRepository;
    private final UserRepository userRepository;
    private final CouncilAccessService councilAccessService;

    public AdminCitizenService(
            CitizenRepository citizenRepository,
            UserRepository userRepository,
            CouncilAccessService councilAccessService) {
        this.citizenRepository = citizenRepository;
        this.userRepository = userRepository;
        this.councilAccessService = councilAccessService;
    }

    public List<Citizen> listCitizens(String requesterEmail, String councilFilter) {
        List<Citizen> citizens;
        if (councilAccessService.isSuperAdmin(requesterEmail)) {
            if (councilFilter != null && !councilFilter.isBlank()) {
                citizens = citizenRepository.findByCouncilIgnoreCase(councilFilter.trim());
            } else {
                citizens = citizenRepository.findAll();
            }
        } else {
            String adminCouncil = councilAccessService.resolveCouncilForEmail(requesterEmail)
                    .orElseThrow(() -> new AccessDeniedException("Council admin profile is required"));
            citizens = citizenRepository.findByCouncilIgnoreCase(adminCouncil);
        }

        return sortByName(citizens.stream()
                .filter(c -> !Boolean.TRUE.equals(c.getAdminHidden()))
                .toList());
    }

    public String hideCitizen(String requesterEmail, Long empId) {
        Optional<Citizen> citizenOpt = citizenRepository.findById(empId);
        if (citizenOpt.isEmpty()) {
            return "NOT_FOUND";
        }

        Citizen citizen = citizenOpt.get();
        if (!canManageCitizen(requesterEmail, citizen)) {
            return "FORBIDDEN";
        }

        citizen.setAdminHidden(true);
        citizenRepository.save(citizen);
        return "HIDDEN";
    }

    public String deleteCitizen(String requesterEmail, Long empId) {
        Optional<Citizen> citizenOpt = citizenRepository.findById(empId);
        if (citizenOpt.isEmpty()) {
            return "NOT_FOUND";
        }

        Citizen citizen = citizenOpt.get();
        if (!canManageCitizen(requesterEmail, citizen)) {
            return "FORBIDDEN";
        }

        try {
            userRepository.deleteById(empId);
            return "DELETED";
        } catch (DataIntegrityViolationException ex) {
            log.error("Failed to delete citizen {} due to constraint", empId, ex);
            return "CONFLICT";
        } catch (Exception ex) {
            log.error("Unexpected error deleting citizen {}", empId, ex);
            return "ERROR";
        }
    }

    private boolean canManageCitizen(String requesterEmail, Citizen citizen) {
        if (councilAccessService.isSuperAdmin(requesterEmail)) {
            return true;
        }
        String adminCouncil = councilAccessService.resolveCouncilForEmail(requesterEmail).orElse(null);
        if (adminCouncil == null || citizen.getCouncil() == null) {
            return false;
        }
        return adminCouncil.equalsIgnoreCase(citizen.getCouncil().trim());
    }

    private List<Citizen> sortByName(List<Citizen> citizens) {
        return citizens.stream()
                .sorted(Comparator.comparing(
                        c -> c.getEmpName() == null ? "" : c.getEmpName(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
