package com.garbo.core.service;

import com.garbo.core.entity.AdminNew;
import com.garbo.core.entity.BinCollector;
import com.garbo.core.entity.Citizen;
import com.garbo.core.entity.FieldMentor;
import com.garbo.core.entity.User;
import com.garbo.core.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CurrentUserService {

    private static UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        CurrentUserService.userRepository = userRepository;
    }

    public static Optional<String> getCurrentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null)
            return Optional.empty();
        String name = auth.getName();
        return Optional.ofNullable(name);
    }

    public static Optional<String> getCurrentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null)
            return Optional.empty();
        return auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "").trim().toLowerCase());
    }

    public static Optional<User> getCurrentUser() {
        Optional<String> emailOpt = getCurrentEmail();
        if (emailOpt.isEmpty())
            return Optional.empty();
        return userRepository.findFirstByEmailIgnoreCase(emailOpt.get());
    }

    public static Optional<Long> getCurrentEmpId() {
        return getCurrentUser().map(User::getEmpId);
    }

    public static Optional<String> getCurrentCouncil() {
        Optional<User> uOpt = getCurrentUser();
        if (uOpt.isEmpty())
            return Optional.empty();
        User u = uOpt.get();
        if (u instanceof AdminNew) {
            return Optional.ofNullable(((AdminNew) u).getCouncil());
        }
        if (u instanceof BinCollector) {
            return Optional.ofNullable(((BinCollector) u).getAssignedCouncil());
        }
        if (u instanceof FieldMentor) {
            return Optional.ofNullable(((FieldMentor) u).getAssignedCouncil());
        }
        if (u instanceof Citizen citizen) {
            String council = UserLookup.resolveCitizenCouncil(citizen);
            return council == null || council.isBlank()
                    ? Optional.empty()
                    : Optional.of(council);
        }
        return Optional.empty();
    }

    public static boolean isCurrentUserOrAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        Optional<Long> currentEmpId = getCurrentEmpId();
        if (currentEmpId.isPresent() && currentEmpId.get().equals(userId)) {
            return true;
        }
        Optional<String> roleOpt = getCurrentRole();
        if (roleOpt.isPresent()) {
            String role = roleOpt.get();
            return role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("superadmin");
        }
        return false;
    }
}
