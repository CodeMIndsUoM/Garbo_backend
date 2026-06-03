package com.garbo.flow;

import com.garbo.core.entity.Citizen;
import com.garbo.core.entity.FieldMentor;
import com.garbo.core.entity.ThirdPartyCollector;
import com.garbo.core.enums.RegistrationStatus;
import com.garbo.core.repository.CitizenRepository;
import com.garbo.core.repository.FieldMentorRepository;
import com.garbo.core.repository.ThirdPartyCollectorRepository;
import com.garbo.infrastructure.config.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

public abstract class FlowTestBase {
    protected Citizen createCitizen(
            CitizenRepository citizenRepository,
            PasswordEncoder passwordEncoder,
            String email,
            String council) {
        Citizen citizen = new Citizen();
        citizen.setEmpName("Test Citizen");
        citizen.setEmail(email);
        citizen.setPassword(passwordEncoder.encode("pass"));
        citizen.setRole("CITIZEN");
        citizen.setCouncil(council);
        citizen.setCreatedAt(LocalDateTime.now());
        return citizenRepository.save(citizen);
    }

    protected ThirdPartyCollector createCollector(
            ThirdPartyCollectorRepository collectorRepository,
            PasswordEncoder passwordEncoder,
            String email,
            String assignedCouncils) {
        ThirdPartyCollector collector = new ThirdPartyCollector();
        collector.setEmpName("Test Collector");
        collector.setEmail(email);
        collector.setPassword(passwordEncoder.encode("pass"));
        collector.setRole("THIRD_PARTY_COLLECTOR");
        collector.setAssignedCouncils(assignedCouncils);
        collector.setRegistrationStatus(RegistrationStatus.APPROVED);
        collector.setCreatedAt(LocalDateTime.now());
        return collectorRepository.save(collector);
    }

    protected FieldMentor createFieldMentor(
            FieldMentorRepository fieldMentorRepository,
            PasswordEncoder passwordEncoder,
            String email,
            String council) {
        FieldMentor mentor = new FieldMentor();
        mentor.setEmpName("Test Field Mentor");
        mentor.setEmail(email);
        mentor.setPassword(passwordEncoder.encode("pass"));
        mentor.setRole("FIELD_MENTOR");
        mentor.setAssignedCouncil(council);
        mentor.setCreatedAt(LocalDateTime.now());
        return fieldMentorRepository.save(mentor);
    }

    protected String tokenFor(JwtUtil jwtUtil, String email, String role) {
        return "Bearer " + jwtUtil.generateToken(email, role);
    }
}
