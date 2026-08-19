package com.autisheimer.fetchMyOfferMicroService.service;

import com.autisheimer.fetchMyOfferMicroService.entity.UserProfile;
import com.autisheimer.fetchMyOfferMicroService.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobHuntSchedulerTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestClient restClient;

    @Mock
    private RestClient.Builder restClientBuilder;

    private JobHuntScheduler jobHuntScheduler;

    @BeforeEach
    void setUp() {
        // Setup RestClient Builder Mock
        when(restClientBuilder.requestFactory(any(ClientHttpRequestFactory.class))).thenReturn(restClientBuilder);
        when(restClientBuilder.build()).thenReturn(restClient);

        jobHuntScheduler = new JobHuntScheduler(userProfileRepository, restClientBuilder);

        ReflectionTestUtils.setField(jobHuntScheduler, "pythonScraperUrl", "http://localhost:8000/api/v1/scrape");
        ReflectionTestUtils.setField(jobHuntScheduler, "webhookCallbackUrl", "http://localhost:8080/webhook");
    }

    @Test
    void triggerAutonomousJobHunt_skipsIfNoProfile() {
        // Arrange
        when(userProfileRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        jobHuntScheduler.triggerAutonomousJobHunt();

        // Assert
        verify(restClient, never()).post();
    }

    @Test
    void triggerAutonomousJobHunt_skipsIfNoQueries() {
        // Arrange
        UserProfile profile = new UserProfile();
        profile.setSearchQueries(Collections.emptyList());
        when(userProfileRepository.findById(1L)).thenReturn(Optional.of(profile));

        // Act
        jobHuntScheduler.triggerAutonomousJobHunt();

        // Assert
        verify(restClient, never()).post();
    }

    @Test
    void triggerAutonomousJobHunt_success() {
        // Arrange
        UserProfile profile = new UserProfile();
        profile.setSearchQueries(List.of("Java Developer"));
        when(userProfileRepository.findById(1L)).thenReturn(Optional.of(profile));

        // Throw an exception here to bypass the hardcoded Thread.sleep(30000) inside the loop,
        // otherwise this unit test will take 60 seconds to execute!
        when(restClient.post().uri(anyString()).body(any(JobHuntScheduler.ScrapeRequest.class)).retrieve().toBodilessEntity())
                .thenThrow(new RuntimeException("Skip Sleep Exception"));

        // Act
        jobHuntScheduler.triggerAutonomousJobHunt();

        // Assert
        // We have 1 query, and 2 target platforms (internshala, naukri) in the code
        verify(restClient, times(3)).post();
    }

    @Test
    void triggerAutonomousJobHunt_handlesRestClientException() {
        // Arrange
        UserProfile profile = new UserProfile();
        profile.setSearchQueries(List.of("Java Developer"));
        when(userProfileRepository.findById(1L)).thenReturn(Optional.of(profile));

        when(restClient.post().uri(anyString()).body(any(JobHuntScheduler.ScrapeRequest.class)).retrieve().toBodilessEntity())
                .thenThrow(new RuntimeException("Simulated RestClient failure"));

        // Act
        jobHuntScheduler.triggerAutonomousJobHunt();

        // Assert
        // Should not crash the application, loop continues
        verify(restClient, times(3)).post();
    }
}
