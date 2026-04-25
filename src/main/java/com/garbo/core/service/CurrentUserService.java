package com.garbo.core.service;

import com.garbo.core.entity.AdminNew;
import com.garbo.core.entity.BinCollector;
import com.garbo.core.entity.FieldMentor;
import com.garbo.core.entity.User;
import com.garbo.core.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<String> getCurrentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null)
            return Optional.empty();
        String name = auth.getName();
        return Optional.ofNullable(name);
    }

    public Optional<String> getCurrentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null)
            return Optional.empty();
        return auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "").trim().toLowerCase());
    }

    public Optional<User> getCurrentUser() {
        Optional<String> emailOpt = getCurrentEmail();
        if (emailOpt.isEmpty())
            return Optional.empty();
        return userRepository.findFirstByEmailIgnoreCase(emailOpt.get());
    }

    public Optional<String> getCurrentCouncil() {
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
        return Optional.empty();
    }
}
