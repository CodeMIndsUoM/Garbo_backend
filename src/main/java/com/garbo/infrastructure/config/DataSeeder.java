package com.garbo.infrastructure.config;

import com.garbo.core.entity.FieldMentor;
import com.garbo.core.repository.FieldMentorRepository;
import com.garbo.core.repository.UserRepository;
import jakarta.persistence.EntityManager;
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
        // Only seed if this user doesn't already exist
        if (userRepository.findFirstByEmailIgnoreCase("sasindu@gmail.com").isEmpty()) {
            // Fix the PostgreSQL sequence to avoid duplicate key conflicts
            try {
                entityManager.createNativeQuery(
                        "SELECT setval('users_emp_id_seq', (SELECT COALESCE(MAX(emp_id), 0) FROM users))")
                        .getSingleResult();
                System.out.println("Reset users_emp_id_seq to current max emp_id.");
            } catch (Exception e) {
                System.out.println("Could not reset sequence (may not be needed): " + e.getMessage());
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

        // Assign 5 bins to Sasindu if not already assigned
        assignBinsToSasindu();
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
