package com.garbo.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garbo.core.entity.Bin;
import com.garbo.core.repository.BinRepository;
import com.garbo.core.repository.FieldMentorRepository;
import com.garbo.infrastructure.config.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FieldStaffBinReportFlowIT extends FlowTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FieldMentorRepository fieldMentorRepository;

    @Autowired
    private BinRepository binRepository;

    @BeforeEach
    void cleanDb() {
        binRepository.deleteAll();
        fieldMentorRepository.deleteAll();
    }

    // Flow: field mentor reports bin status and then undoes the report.
    @Test
    void fieldMentorReportsAndUndoesBinStatus() throws Exception {
        var mentor = createFieldMentor(fieldMentorRepository, passwordEncoder, "mentor1@garbo.test", "Colombo");
        String mentorToken = tokenFor(jwtUtil, mentor.getEmail(), mentor.getRole());

        Bin bin = new Bin();
        bin.setCouncil("Colombo");
        bin.setStatus("notChecked");
        bin.setFillLevel(0);
        bin.setLatitude(6.9271);
        bin.setLongitude(79.8612);
        bin.setLastChecked(LocalDateTime.now());
        bin = binRepository.save(bin);

        MvcResult reportResult = mockMvc.perform(multipart("/api/bins/{id}/report", bin.getId())
                .param("status", "full")
                .param("fillLevel", "100")
                .param("latitude", "6.9271")
                .param("longitude", "79.8612")
                .param("notes", "Bin is full")
                .header("Authorization", mentorToken))
                .andExpect(status().isOk())
                .andReturn();

        String reportStatus = objectMapper.readTree(reportResult.getResponse().getContentAsString())
                .path("data").path("status").asText();
        int fillLevel = objectMapper.readTree(reportResult.getResponse().getContentAsString())
                .path("data").path("fillLevel").asInt();
        assertThat(reportStatus).isEqualTo("full");
        assertThat(fillLevel).isEqualTo(100);

        MvcResult undoResult = mockMvc.perform(post("/api/bins/{id}/undo", bin.getId())
                .header("Authorization", mentorToken))
                .andExpect(status().isOk())
                .andReturn();

        String undoStatus = objectMapper.readTree(undoResult.getResponse().getContentAsString())
                .path("data").path("status").asText();
        int undoFillLevel = objectMapper.readTree(undoResult.getResponse().getContentAsString())
                .path("data").path("fillLevel").asInt();
        assertThat(undoStatus).isEqualTo("notChecked");
        assertThat(undoFillLevel).isEqualTo(0);
    }

    // Flow: report endpoint requires JWT (expect 401 without token).
    @Test
    void reportBinStatus_requiresJwt() throws Exception {
        Bin bin = new Bin();
        bin.setCouncil("Colombo");
        bin.setStatus("notChecked");
        bin.setFillLevel(0);
        bin.setLatitude(6.9271);
        bin.setLongitude(79.8612);
        bin.setLastChecked(LocalDateTime.now());
        bin = binRepository.save(bin);

        mockMvc.perform(multipart("/api/bins/{id}/report", bin.getId())
                .param("status", "full")
                .param("fillLevel", "100")
                .param("latitude", "6.9271")
                .param("longitude", "79.8612"))
                .andExpect(status().isUnauthorized());
    }
}
