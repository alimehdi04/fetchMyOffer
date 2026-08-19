package com.autisheimer.fetchMyOfferMicroService.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobQueryGeneratorServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private JobQueryGeneratorService jobQueryGeneratorService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void generateSearchQueries_withProfile() {
        // Arrange
        Document doc1 = new Document("Java Developer with 5 years of experience.");
        Document doc2 = new Document("Worked on Spring Boot and React.");
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(doc1, doc2));

        String mockJsonResponse = "{\"queries\": [\"Java Backend Intern\", \"Spring Boot Developer\", \"Software Engineer\"]}";
        
        when(chatClient.prompt().system(any(Consumer.class)).call().content())
                .thenReturn(mockJsonResponse);

        // Act
        JobQueryGeneratorService.SearchQueries result = jobQueryGeneratorService.generateSearchQueries();

        // Assert
        assertEquals(3, result.queries().size());
        assertEquals("Java Backend Intern", result.queries().get(0));
        assertEquals("Spring Boot Developer", result.queries().get(1));
        assertEquals("Software Engineer", result.queries().get(2));
        
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void generateSearchQueries_withBlankProfileReturnsFallback() {
        // Arrange
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(Collections.emptyList());

        // Act
        JobQueryGeneratorService.SearchQueries result = jobQueryGeneratorService.generateSearchQueries();

        // Assert
        assertEquals(1, result.queries().size());
        assertEquals("Software Engineering Intern", result.queries().get(0));
        
        // ChatClient shouldn't be called if profile is blank
        verify(chatClient, never()).prompt();
    }
}
