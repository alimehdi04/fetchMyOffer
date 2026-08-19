package com.autisheimer.fetchMyOfferMicroService.controller;

import com.autisheimer.fetchMyOfferMicroService.entity.UserProfile;
import com.autisheimer.fetchMyOfferMicroService.repository.UserProfileRepository;
import com.autisheimer.fetchMyOfferMicroService.service.JobHuntScheduler;
import com.autisheimer.fetchMyOfferMicroService.service.ResumeProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResumeProcessingService resumeService;

    @MockBean
    private JobHuntScheduler jobHuntScheduler;

    @MockBean
    private UserProfileRepository userProfileRepository;

    @Test
    void uploadResume_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "dummy pdf content".getBytes()
        );

        doNothing().when(resumeService).processAndStoreResume(any());

        mockMvc.perform(multipart("/api/v1/profile/upload-resume")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(content().string("✅ Resume parsed, Profile distilled, Search Queries generated, and data securely stored in PostgreSQL!"));
    }

    @Test
    void uploadResume_invalidFileReturnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.txt",
                "text/plain",
                "dummy text content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/profile/upload-resume")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Please upload a valid PDF file."));

        verify(resumeService, never()).processAndStoreResume(any());
    }

    @Test
    void uploadResume_serviceExceptionReturnsInternalServerError() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "dummy pdf content".getBytes()
        );

        doThrow(new RuntimeException("Tika failed to parse PDF"))
                .when(resumeService).processAndStoreResume(any());

        mockMvc.perform(multipart("/api/v1/profile/upload-resume")
                        .file(file))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Failed to process resume: Tika failed to parse PDF"));
    }

    @Test
    void getRecommendedQueries_success() throws Exception {
        UserProfile user = new UserProfile();
        user.setSearchQueries(List.of("Java Developer", "Spring Boot"));
        when(userProfileRepository.findById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/v1/profile/generate-queries"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("Java Developer"))
                .andExpect(jsonPath("$[1]").value("Spring Boot"));
    }

    @Test
    void getRecommendedQueries_userNotFoundThrowsException() throws Exception {
        when(userProfileRepository.findById(1L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
                jakarta.servlet.ServletException.class,
                () -> mockMvc.perform(get("/api/v1/profile/generate-queries"))
        );
    }

    @Test
    void forceHunt_success() throws Exception {
        doNothing().when(jobHuntScheduler).triggerAutonomousJobHunt();

        mockMvc.perform(get("/api/v1/profile/force-hunt"))
                .andExpect(status().isOk())
                .andExpect(content().string("Manual job hunt successfully triggered! Check Telegram for matches."));
        
        // Since it runs in a new thread, we might need to sleep slightly to let the thread execute
        // before verifying the mock interaction.
        Thread.sleep(100);
        verify(jobHuntScheduler, times(1)).triggerAutonomousJobHunt();
    }
}
