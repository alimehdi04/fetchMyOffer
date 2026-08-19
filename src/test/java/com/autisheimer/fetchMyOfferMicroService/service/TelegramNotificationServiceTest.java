package com.autisheimer.fetchMyOfferMicroService.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramNotificationServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestClient restClient;

    @Mock
    private RestClient.Builder restClientBuilder;

    private TelegramNotificationService telegramNotificationService;

    private final String botToken = "test-bot-token";
    private final String chatId = "123456789";

    @BeforeEach
    void setUp() {
        when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
        when(restClientBuilder.build()).thenReturn(restClient);

        telegramNotificationService = new TelegramNotificationService(restClientBuilder, botToken, chatId);
    }

    @Test
    void sendJobMatchNotification_success() {
        // Arrange
        String jobTitle = "Java Developer";
        String companyName = "Tech Corp";
        String url = "https://job.url";
        String aiReasoning = "Great match for your skills.";

        // Act
        telegramNotificationService.sendJobMatchNotification(jobTitle, companyName, url, aiReasoning);

        // Assert
        verify(restClient).post();
        verify(restClient.post()).uri(eq("/bot{token}/sendMessage"), eq(botToken));
        
        // Verify payload mapping implicitly happens, but we just verify body is passed
        verify(restClient.post().uri(anyString(), anyString())).body(any(Map.class));
        verify(restClient.post().uri(anyString(), anyString()).body(any(Map.class)).retrieve()).toBodilessEntity();
    }

    @Test
    void sendJobMatchNotification_handlesException() {
        // Arrange
        String jobTitle = "Java Developer";
        String companyName = "Tech Corp";
        String url = "https://job.url";
        String aiReasoning = "Great match for your skills.";

        when(restClient.post().uri(anyString(), anyString()).body(any(Map.class)).retrieve().toBodilessEntity())
                .thenThrow(new RuntimeException("Simulated Telegram API failure"));

        // Act
        // Should catch the exception and log, not throw to the caller
        telegramNotificationService.sendJobMatchNotification(jobTitle, companyName, url, aiReasoning);

        // Assert
        verify(restClient, times(2)).post();
    }
}
