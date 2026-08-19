package com.autisheimer.fetchMyOfferMicroService.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobEvaluatorServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @InjectMocks
    private JobEvaluatorService jobEvaluatorService;

    @BeforeEach
    void setUp() {
        // JobEvaluatorService is instantiated via @InjectMocks
    }

    @Test
    void evaluateAllJobs_success() {
        // Arrange
        String mockJsonResponse = "[{\"url\": \"http://job1.com\", \"isMatch\": true, \"reason\": \"Good match.\"}," +
                "{\"url\": \"http://job2.com\", \"isMatch\": false, \"reason\": \"No match.\"}]";

        // Deep stubbing handles chatClient.prompt().system().user().call().content()
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn(mockJsonResponse);

        List<Map<String, String>> jobs = List.of(
                Map.of("url", "http://job1.com", "title", "Developer"),
                Map.of("url", "http://job2.com", "title", "Manager")
        );

        String profile = "{\"skills\": [\"Java\"]}";

        // Act
        List<JobEvaluatorService.BatchEvaluationResponse> results = jobEvaluatorService.evaluateAllJobs(jobs, profile);

        // Assert
        assertEquals(2, results.size());
        
        JobEvaluatorService.BatchEvaluationResponse res1 = results.get(0);
        assertEquals("http://job1.com", res1.url());
        assertTrue(res1.isMatch());
        assertEquals("Good match.", res1.reason());

        JobEvaluatorService.BatchEvaluationResponse res2 = results.get(1);
        assertEquals("http://job2.com", res2.url());
        assertTrue(!res2.isMatch());
        assertEquals("No match.", res2.reason());
    }

    @Test
    void evaluateAllJobs_returnsEmptyListOnException() {
        // Arrange
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenThrow(new RuntimeException("Simulated API failure"));

        List<Map<String, String>> jobs = List.of(
                Map.of("url", "http://job1.com", "title", "Developer")
        );
        String profile = "{\"skills\": [\"Java\"]}";

        // Act
        List<JobEvaluatorService.BatchEvaluationResponse> results = jobEvaluatorService.evaluateAllJobs(jobs, profile);

        // Assert
        assertTrue(results.isEmpty(), "Should return empty list on exception to not crash the loop");
    }

    @Test
    void evaluateAllJobs_handlesMarkdownCodeBlocks() {
        // Arrange
        String mockJsonResponse = "```json\n" +
                "[{\"url\": \"http://job1.com\", \"isMatch\": true, \"reason\": \"Good match.\"}]\n" +
                "```";

        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn(mockJsonResponse);

        List<Map<String, String>> jobs = List.of(
                Map.of("url", "http://job1.com", "title", "Developer")
        );
        String profile = "{\"skills\": [\"Java\"]}";

        // Act
        List<JobEvaluatorService.BatchEvaluationResponse> results = jobEvaluatorService.evaluateAllJobs(jobs, profile);

        // Assert
        assertEquals(1, results.size());
        assertTrue(results.get(0).isMatch());
    }
}
