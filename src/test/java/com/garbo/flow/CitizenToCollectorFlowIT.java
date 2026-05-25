package com.garbo.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garbo.core.repository.CitizenRepository;
import com.garbo.core.repository.CollectionOfferRepository;
import com.garbo.core.repository.CollectionRequestRepository;
import com.garbo.core.repository.ThirdPartyCollectorRepository;
import com.garbo.core.enums.RequestStatus;
import com.garbo.infrastructure.config.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CitizenToCollectorFlowIT extends FlowTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CitizenRepository citizenRepository;

    @Autowired
    private ThirdPartyCollectorRepository collectorRepository;

    @Autowired
    private CollectionRequestRepository requestRepository;

    @Autowired
    private CollectionOfferRepository offerRepository;

    @BeforeEach
    void cleanDb() {
        offerRepository.deleteAll();
        requestRepository.deleteAll();
        collectorRepository.deleteAll();
        citizenRepository.deleteAll();
    }

    // Flow: citizen creates request -> collector offers -> citizen accepts.
    @Test
    void citizenCreatesRequest_collectorOffers_citizenAccepts() throws Exception {
        var citizen = createCitizen(citizenRepository, passwordEncoder, "citizen1@garbo.test", "Colombo");
        var collector = createCollector(collectorRepository, passwordEncoder, "collector1@garbo.test", "colombo");

        String citizenToken = tokenFor(jwtUtil, citizen.getEmail(), citizen.getRole());
        String collectorToken = tokenFor(jwtUtil, collector.getEmail(), collector.getRole());

        Long requestId = createRequest(citizenToken, citizen.getEmpId());
        Long offerId = createOffer(collectorToken, requestId);

        MvcResult acceptResult = mockMvc.perform(post("/api/offers/{id}/accept", offerId)
                .header("Authorization", citizenToken))
                .andExpect(status().isOk())
                .andReturn();

        String offerStatus = objectMapper.readTree(acceptResult.getResponse().getContentAsString())
                .path("data").path("status").asText();
        assertThat(offerStatus).isEqualTo("ACCEPTED");
    }

    // Flow: citizen rejects a pending offer; request stays OPEN.
    @Test
    void citizenRejectsOffer_requestRemainsOpen() throws Exception {
        var citizen = createCitizen(citizenRepository, passwordEncoder, "citizen2@garbo.test", "Colombo");
        var collector = createCollector(collectorRepository, passwordEncoder, "collector2@garbo.test", "colombo");

        String citizenToken = tokenFor(jwtUtil, citizen.getEmail(), citizen.getRole());
        String collectorToken = tokenFor(jwtUtil, collector.getEmail(), collector.getRole());

        Long requestId = createRequest(citizenToken, citizen.getEmpId());
        Long offerId = createOffer(collectorToken, requestId);

        MvcResult rejectResult = mockMvc.perform(post("/api/offers/{id}/reject", offerId)
                .header("Authorization", citizenToken))
                .andExpect(status().isOk())
                .andReturn();

        String offerStatus = objectMapper.readTree(rejectResult.getResponse().getContentAsString())
                .path("data").path("status").asText();
        assertThat(offerStatus).isEqualTo("REJECTED");
        assertThat(requestRepository.findById(requestId).orElseThrow().getStatus())
                .isEqualTo(RequestStatus.OPEN);
    }

    // Flow: citizen cancels request; pending offers are rejected.
    @Test
    void citizenCancelsRequest_rejectsPendingOffers() throws Exception {
        var citizen = createCitizen(citizenRepository, passwordEncoder, "citizen3@garbo.test", "Colombo");
        var collector = createCollector(collectorRepository, passwordEncoder, "collector3@garbo.test", "colombo");

        String citizenToken = tokenFor(jwtUtil, citizen.getEmail(), citizen.getRole());
        String collectorToken = tokenFor(jwtUtil, collector.getEmail(), collector.getRole());

        Long requestId = createRequest(citizenToken, citizen.getEmpId());
        Long offerId = createOffer(collectorToken, requestId);

        Map<String, Object> cancelPayload = new HashMap<>();
        cancelPayload.put("reason", "Changed my plan");

        MvcResult cancelResult = mockMvc.perform(post("/api/collection-requests/{id}/cancel", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", citizenToken)
                .content(objectMapper.writeValueAsString(cancelPayload)))
                .andExpect(status().isOk())
                .andReturn();

        String requestStatus = objectMapper.readTree(cancelResult.getResponse().getContentAsString())
                .path("data").path("status").asText();
        assertThat(requestStatus).isEqualTo("CANCELLED");
        assertThat(offerRepository.findById(offerId).orElseThrow().getStatus().name())
                .isEqualTo("REJECTED");
    }

    // Flow: collector withdraws a pending offer.
    @Test
    void collectorWithdrawsOffer_statusWithdrawn() throws Exception {
        var citizen = createCitizen(citizenRepository, passwordEncoder, "citizen4@garbo.test", "Colombo");
        var collector = createCollector(collectorRepository, passwordEncoder, "collector4@garbo.test", "colombo");

        String collectorToken = tokenFor(jwtUtil, collector.getEmail(), collector.getRole());
        String citizenToken = tokenFor(jwtUtil, citizen.getEmail(), citizen.getRole());

        Long requestId = createRequest(citizenToken, citizen.getEmpId());
        Long offerId = createOffer(collectorToken, requestId);

        MvcResult withdrawResult = mockMvc.perform(post("/api/offers/{id}/withdraw", offerId)
                .header("Authorization", collectorToken))
                .andExpect(status().isOk())
                .andReturn();

        String offerStatus = objectMapper.readTree(withdrawResult.getResponse().getContentAsString())
                .path("data").path("status").asText();
        assertThat(offerStatus).isEqualTo("WITHDRAWN");
    }

    private Long createRequest(String citizenToken, Long citizenId) throws Exception {
        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("wasteType", "PLASTIC");
        createRequest.put("quantityLabel", "2 bags");
        createRequest.put("quantityKgEstimate", 5.0);
        createRequest.put("addressLine", "123 Test Street");
        createRequest.put("latitude", 6.9271);
        createRequest.put("longitude", 79.8612);
        createRequest.put("preferredDate", LocalDate.now().plusDays(1).toString());
        createRequest.put("preferredSlot", "MORNING");
        createRequest.put("contactPhone", "0771234567");
        createRequest.put("notes", "Handle with care");
        createRequest.put("photoUrl", null);

        MvcResult createResult = mockMvc.perform(post("/api/citizens/{id}/collection-requests", citizenId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", citizenToken)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    private Long createOffer(String collectorToken, Long requestId) throws Exception {
        Map<String, Object> createOffer = new HashMap<>();
        createOffer.put("pricePerUnit", 100.0);
        createOffer.put("priceUnit", "FIXED");
        createOffer.put("exchangeItem", null);
        createOffer.put("proposedPickupAt", Instant.now().plusSeconds(3600).toString());
        createOffer.put("messageToCitizen", "Can pick up today");

        MvcResult offerResult = mockMvc.perform(post("/api/collection-requests/{id}/offers", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", collectorToken)
                .content(objectMapper.writeValueAsString(createOffer)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(offerResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }
}
