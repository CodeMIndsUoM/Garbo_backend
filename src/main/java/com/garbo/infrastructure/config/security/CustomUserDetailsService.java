package com.garbo.infrastructure.config.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.garbo.core.entity.ThirdPartyCollector;
import com.garbo.core.entity.User;
import com.garbo.core.enums.RegistrationStatus;
import com.garbo.core.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepository.findFirstByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Block third-party collectors whose registration is not yet approved
        if (user instanceof ThirdPartyCollector) {
            ThirdPartyCollector collector = (ThirdPartyCollector) user;
            RegistrationStatus status = collector.getRegistrationStatus();
            if (status == RegistrationStatus.PENDING) {
                throw new UsernameNotFoundException("Registration is pending admin approval");
            }
            if (status == RegistrationStatus.REJECTED) {
                throw new UsernameNotFoundException("Registration has been rejected");
            }
        }

        // Build Spring Security UserDetails object
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword()) // hashed password from DB
                .authorities("ROLE_" + user.getRole()) // e.g. ROLE_SUPERADMIN
                .build();
    }
}
