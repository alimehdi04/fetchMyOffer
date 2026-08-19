package com.autisheimer.fetchMyOfferMicroService.controller;

import com.autisheimer.fetchMyOfferMicroService.entity.EvaluatedJob;
import com.autisheimer.fetchMyOfferMicroService.entity.UserProfile;
import com.autisheimer.fetchMyOfferMicroService.repository.EvaluatedJobRepository;
import com.autisheimer.fetchMyOfferMicroService.repository.UserProfileRepository;
import com.autisheimer.fetchMyOfferMicroService.service.JobEvaluatorService;
import com.autisheimer.fetchMyOfferMicroService.service.TelegramNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebhookController.class)
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobEvaluatorService evaluatorService;

    @MockBean
    private TelegramNotificationService telegramService;

    @MockBean
    private EvaluatedJobRepository jobRepository;

    @MockBean
    private UserProfileRepository userProfileRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void receiveScrapeResults_success() throws Exception {
        // Arrange
        UserProfile userProfile = new UserProfile();
        userProfile.setDistilledProfileJson("{\"skills\": [\"Java\"]}");
        when(userProfileRepository.findById(1L)).thenReturn(Optional.of(userProfile));

        // Create 2 jobs, one new, one duplicate
        List<Map<String, String>> jobData = List.of(
                Map.of("url", "http://job1.com", "title", "Java Dev", "company", "Tech 1"),
                Map.of("url", "http://job2.com", "title", "React Dev", "company", "Tech 2")
        );
        WebhookController.JobPayload payload = new WebhookController.JobPayload("jobId123", "success", jobData);

        when(jobRepository.existsByJobUrl("http://job1.com")).thenReturn(false);
        when(jobRepository.existsByJobUrl("http://job2.com")).thenReturn(true); // Duplicate

        List<JobEvaluatorService.BatchEvaluationResponse> aiResults = List.of(
                new JobEvaluatorService.BatchEvaluationResponse("http://job1.com", true, "Good fit.")
        );
        when(evaluatorService.evaluateAllJobs(anyList(), anyString())).thenReturn(aiResults);

        // Act & Assert
        mockMvc.perform(post("/api/v1/webhooks/scrape-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook successfully processed"));

        verify(evaluatorService).evaluateAllJobs(argThat(list -> list.size() == 1), anyString()); // Only 1 non-duplicate sent
        verify(telegramService).sendJobMatchNotification("Java Dev", "Tech 1", "http://job1.com", "Good fit.");
        verify(jobRepository).save(any(EvaluatedJob.class)); // 1 save for the evaluated job
    }

    @Test
    void receiveScrapeResults_allDuplicates() throws Exception {
        UserProfile userProfile = new UserProfile();
        when(userProfileRepository.findById(1L)).thenReturn(Optional.of(userProfile));

        List<Map<String, String>> jobData = List.of(
                Map.of("url", "http://job1.com", "title", "Java Dev", "company", "Tech 1")
        );
        WebhookController.JobPayload payload = new WebhookController.JobPayload("jobId123", "success", jobData);

        when(jobRepository.existsByJobUrl("http://job1.com")).thenReturn(true);

        mockMvc.perform(post("/api/v1/webhooks/scrape-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(content().string("Processed. No new jobs."));

        verify(evaluatorService, never()).evaluateAllJobs(anyList(), anyString());
        verify(telegramService, never()).sendJobMatchNotification(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void receiveScrapeResults_capsAt10Jobs() throws Exception {
        UserProfile userProfile = new UserProfile();
        userProfile.setDistilledProfileJson("{\"skills\": [\"Java\"]}");
        when(userProfileRepository.findById(1L)).thenReturn(Optional.of(userProfile));

        List<Map<String, String>> jobData = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            jobData.add(Map.of("url", "http://job" + i + ".com", "title", "Dev " + i, "company", "Tech"));
        }
        WebhookController.JobPayload payload = new WebhookController.JobPayload("jobId123", "success", jobData);

        when(jobRepository.existsByJobUrl(anyString())).thenReturn(false);

        when(evaluatorService.evaluateAllJobs(anyList(), anyString())).thenReturn(new ArrayList<>());

        mockMvc.perform(post("/api/v1/webhooks/scrape-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        // Verifying it was capped at 10 jobs
        verify(evaluatorService).evaluateAllJobs(argThat(list -> list.size() == 10), anyString());
    }

    @Test
    void receiveScrapeResults_userNotFoundThrowsException() throws Exception {
        when(userProfileRepository.findById(1L)).thenReturn(Optional.empty());

        WebhookController.JobPayload payload = new WebhookController.JobPayload("jobId123", "success", List.of());

        org.junit.jupiter.api.Assertions.assertThrows(
                jakarta.servlet.ServletException.class,
                () -> mockMvc.perform(post("/api/v1/webhooks/scrape-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
        );
    }
}
