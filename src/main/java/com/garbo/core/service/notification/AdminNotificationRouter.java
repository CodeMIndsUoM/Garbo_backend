package com.garbo.core.service.notification;

import com.garbo.core.entity.AdminNew;
import com.garbo.core.entity.SuperAdmin;
import com.garbo.core.repository.AdminNewRepository;
import com.garbo.core.repository.SuperAdminRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AdminNotificationRouter {

    private final AdminNewRepository adminNewRepository;
    private final SuperAdminRepository superAdminRepository;

    public AdminNotificationRouter(
            AdminNewRepository adminNewRepository,
            SuperAdminRepository superAdminRepository
    ) {
        this.adminNewRepository = adminNewRepository;
        this.superAdminRepository = superAdminRepository;
    }

    public List<Long> resolveCouncilAdminUserIds(String council) {
        if (council == null || council.isBlank()) {
            return List.of();
        }
        return adminNewRepository.findByCouncilIgnoreCase(council.trim()).stream()
                .map(AdminNew::getEmpId)
                .toList();
    }

    public List<Long> resolveSuperAdminUserIds() {
        return superAdminRepository.findAll().stream()
                .map(SuperAdmin::getEmpId)
                .toList();
    }

    public List<Long> resolveCouncilAndSuperAdminUserIds(String council) {
        Set<Long> ids = new LinkedHashSet<>();
        ids.addAll(resolveCouncilAdminUserIds(council));
        ids.addAll(resolveSuperAdminUserIds());
        return new ArrayList<>(ids);
    }

    public List<Long> resolveSuperAdminOnly() {
        return resolveSuperAdminUserIds();
    }

    public boolean councilsMatch(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }

    public String normalizeCouncil(String council) {
        if (council == null) {
            return "";
        }
        return council.trim().toLowerCase(Locale.ROOT);
    }
}
