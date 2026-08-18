package com.garbo.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garbo.core.entity.Complaint;
import com.garbo.core.service.ComplaintService;
import com.garbo.infrastructure.config.security.CustomUserDetailsService;
import com.garbo.infrastructure.config.security.JwtUtil;
import com.garbo.infrastructure.storage.CloudinaryUploadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ComplaintController.class)
public class ComplaintControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private ComplaintService complaintService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CloudinaryUploadService cloudinaryUploadService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(username = "staff@example.com")
    void getAssignedComplaints_returns200() throws Exception {
        Complaint complaint = new Complaint();
        complaint.setId(15L);
        complaint.setTitle("Bad smell from waste");
        complaint.setStatus("IN_PROGRESS");

        when(complaintService.getAssignedComplaints("staff@example.com")).thenReturn(List.of(complaint));

        mvc.perform(get("/api/complaints/assigned-to-me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(15))
                .andExpect(jsonPath("$[0].title").value("Bad smell from waste"))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"));
    }

    @Test
    @WithMockUser(username = "staff@example.com")
    void confirmComplaint_returns200() throws Exception {
        Complaint confirmed = new Complaint();
        confirmed.setId(15L);
        confirmed.setStatus("RESOLVED");
        confirmed.setIsConfirmedTrue(true);
        confirmed.setFieldStaffNote("Checked and resolved");

        when(complaintService.confirmComplaint(eq(15L), eq(true), eq("Checked and resolved"), eq("https://image.url"), eq("staff@example.com")))
                .thenReturn(confirmed);

        Map<String, Object> body = Map.of(
                "isTrue", true,
                "note", "Checked and resolved",
                "photoUrl", "https://image.url"
        );

        mvc.perform(post("/api/complaints/15/confirm")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.isConfirmedTrue").value(true));
    }

    @Test
    @WithMockUser(username = "staff@example.com")
    void uploadComplaintImage_returns200WithUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "photo",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake image content".getBytes()
        );

        when(cloudinaryUploadService.uploadComplaintPhoto(any())).thenReturn("https://cloudinary.com/test.jpg");

        mvc.perform(multipart("/api/complaints/upload-image")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").value("https://cloudinary.com/test.jpg"))
                .andExpect(jsonPath("$.imageUrl").value("https://cloudinary.com/test.jpg"));
    }
}
