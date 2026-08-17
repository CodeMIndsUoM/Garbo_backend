package com.garbo.core.service.security;

import com.garbo.core.entity.SystemIncident;
import com.garbo.core.repository.SystemIncidentRepository;
import com.garbo.core.service.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Service
public class SystemIncidentService {

    private final SystemIncidentRepository repository;

    public SystemIncidentService(SystemIncidentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void logIncident(String operationType, String targetId, String details) {
        String operator = CurrentUserService.getCurrentEmail().orElse("SYSTEM");
        if ("anonymousUser".equalsIgnoreCase(operator)) {
            operator = "SYSTEM";
        }
        String ipAddress = getClientIp();

        SystemIncident incident = new SystemIncident();
        incident.setOperator(operator);
        incident.setOperationType(operationType);
        incident.setTargetId(targetId);
        incident.setDetails(details);
        incident.setIpAddress(ipAddress);
        incident.setTimestamp(LocalDateTime.now());

        repository.save(incident);
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception ignored) {
        }
        return "127.0.0.1";
    }
}
