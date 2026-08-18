package com.garbo.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garbo.api.dto.BinSuggestionCreateRequest;
import com.garbo.core.entity.BinSuggestion;
import com.garbo.core.service.BinSuggestionService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BinSuggestionController.class)
public class BinSuggestionControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private BinSuggestionService binSuggestionService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CloudinaryUploadService cloudinaryUploadService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(username = "mentor@example.com")
    void getMySuggestions_returns200() throws Exception {
        BinSuggestion suggestion = new BinSuggestion();
        suggestion.setId(10L);
        suggestion.setCategory("Organic");
        suggestion.setStatus("PENDING");

        when(binSuggestionService.getMySuggestions("mentor@example.com")).thenReturn(List.of(suggestion));

        mvc.perform(get("/api/bin-suggestions/my")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].category").value("Organic"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "mentor@example.com")
    void createSuggestion_returns200() throws Exception {
        BinSuggestion created = new BinSuggestion();
        created.setId(11L);
        created.setCategory("Recyclables");
        created.setStatus("PENDING");

        when(binSuggestionService.createSuggestion(any(BinSuggestionCreateRequest.class), eq("mentor@example.com")))
                .thenReturn(created);

        BinSuggestionCreateRequest request = new BinSuggestionCreateRequest();
        request.setLocation("6.9271,79.8612");
        request.setCategory("Recyclables");
        request.setNotes("High demand area");

        mvc.perform(post("/api/bin-suggestions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.category").value("Recyclables"));
    }

    @Test
    @WithMockUser(username = "mentor@example.com")
    void uploadSuggestionImage_returns200WithUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "photo",
                "bin.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "photo binary data".getBytes()
        );

        when(cloudinaryUploadService.uploadBinSuggestionPhoto(any())).thenReturn("https://cloudinary.com/bin.jpg");

        mvc.perform(multipart("/api/bin-suggestions/upload-image")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").value("https://cloudinary.com/bin.jpg"))
                .andExpect(jsonPath("$.imageUrl").value("https://cloudinary.com/bin.jpg"));
    }
}
