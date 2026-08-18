package com.garbo.core.service;

import com.garbo.api.dto.BinSuggestionCreateRequest;
import com.garbo.core.entity.Bin;
import com.garbo.core.entity.BinSuggestion;
import com.garbo.core.entity.FieldMentor;
import com.garbo.core.repository.BinSuggestionRepository;
import com.garbo.core.repository.UserRepository;
import com.garbo.core.service.field_staff.BinService;
import com.garbo.core.service.notification.NotificationPublisher;
import com.garbo.infrastructure.websocket.TaskAlertBroadcaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BinSuggestionServiceTest {

    @Mock
    private BinSuggestionRepository binSuggestionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CouncilAccessService councilAccessService;

    @Mock
    private BinService binService;

    @Mock
    private TaskAlertBroadcaster taskAlertBroadcaster;

    @Mock
    private NotificationPublisher notificationPublisher;

    @InjectMocks
    private BinSuggestionService binSuggestionService;

    private FieldMentor mentorUser;
    private BinSuggestion suggestion;

    @BeforeEach
    void setUp() {
        mentorUser = new FieldMentor();
        mentorUser.setEmpId(501L);
        mentorUser.setEmpName("Mentor Alex");
        mentorUser.setEmail("mentor@example.com");
        mentorUser.setAssignedCouncil("Colombo");
        mentorUser.setRole("FIELD_STAFF");

        suggestion = new BinSuggestion();
        suggestion.setId(100L);
        suggestion.setMentorId(501L);
        suggestion.setMentorName("Mentor Alex");
        suggestion.setCouncil("Colombo");
        suggestion.setLocation("6.9271,79.8612");
        suggestion.setLatitude(6.9271);
        suggestion.setLongitude(79.8612);
        suggestion.setCategory("Recyclables");
        suggestion.setStatus("PENDING");
    }

    @Test
    void createSuggestion_Success() {
        when(userRepository.findFirstByEmailIgnoreCase("mentor@example.com")).thenReturn(Optional.of(mentorUser));
        when(binSuggestionRepository.save(any(BinSuggestion.class))).thenAnswer(inv -> {
            BinSuggestion s = inv.getArgument(0);
            s.setId(200L);
            return s;
        });

        BinSuggestionCreateRequest request = new BinSuggestionCreateRequest();
        request.setLocation("6.9271,79.8612");
        request.setCategory("Recyclables");
        request.setNotes("High waste generation area");
        request.setImageUrl("https://photo.url");

        BinSuggestion created = binSuggestionService.createSuggestion(request, "mentor@example.com");

        assertNotNull(created);
        assertEquals("PENDING", created.getStatus());
        assertEquals("Colombo", created.getCouncil());
        assertEquals(501L, created.getMentorId());
        verify(notificationPublisher, times(1)).binSuggestionSubmitted(created);
    }

    @Test
    void getMySuggestions_ReturnsMentorSuggestions() {
        when(userRepository.findFirstByEmailIgnoreCase("mentor@example.com")).thenReturn(Optional.of(mentorUser));
        when(binSuggestionRepository.findByMentorIdOrderByCreatedAtDesc(501L)).thenReturn(List.of(suggestion));

        List<BinSuggestion> list = binSuggestionService.getMySuggestions("mentor@example.com");

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Recyclables", list.get(0).getCategory());
    }

    @Test
    void updateStatus_Approved_UpdatesStatus() {
        when(councilAccessService.isAdmin("admin@example.com")).thenReturn(true);
        when(councilAccessService.resolveCouncilForEmail("admin@example.com")).thenReturn(Optional.of("Colombo"));
        when(binSuggestionRepository.findByIdAndCouncilIgnoreCase(100L, "Colombo")).thenReturn(Optional.of(suggestion));
        when(binSuggestionRepository.save(any(BinSuggestion.class))).thenAnswer(inv -> inv.getArgument(0));

        Bin createdBin = new Bin();
        createdBin.setId(300L);
        when(binService.createBinFromSuggestion(suggestion)).thenReturn(createdBin);

        BinSuggestion updated = binSuggestionService.updateStatus(100L, "APPROVED", "Approved for placement", "admin@example.com");

        assertNotNull(updated);
        assertEquals("APPROVED", updated.getStatus());
        assertEquals("Approved for placement", updated.getResolutionNotes());
        assertEquals(300L, updated.getCreatedBinId());
        verify(notificationPublisher, times(1)).binSuggestionResolved(updated);
    }
}
