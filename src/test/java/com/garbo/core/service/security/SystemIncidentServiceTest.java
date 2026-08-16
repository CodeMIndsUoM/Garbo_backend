package com.garbo.core.service.security;

import com.garbo.core.entity.SystemIncident;
import com.garbo.core.repository.SystemIncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

class SystemIncidentServiceTest {

    private SystemIncidentRepository repository;
    private SystemIncidentService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(SystemIncidentRepository.class);
        service = new SystemIncidentService(repository);
    }

    @Test
    void logIncident_savesIncidentWithRequestIP() {
        // Mock request context
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.50");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        service.logIncident("BIN_ADDITION", "100", "Bin created");

        // Verify and capture
        ArgumentCaptor<SystemIncident> captor = ArgumentCaptor.forClass(SystemIncident.class);
        verify(repository).save(captor.capture());

        SystemIncident incident = captor.getValue();
        assertEquals("BIN_ADDITION", incident.getOperationType());
        assertEquals("100", incident.getTargetId());
        assertEquals("Bin created", incident.getDetails());
        assertEquals("192.168.1.50", incident.getIpAddress());
        assertNotNull(incident.getTimestamp());

        // Cleanup request context
        RequestContextHolder.resetRequestAttributes();
    }
}
