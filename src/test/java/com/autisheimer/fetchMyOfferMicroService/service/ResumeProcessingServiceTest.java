package com.autisheimer.fetchMyOfferMicroService.service;

import com.autisheimer.fetchMyOfferMicroService.dto.CandidateMasterProfile;
import com.autisheimer.fetchMyOfferMicroService.entity.UserProfile;
import com.autisheimer.fetchMyOfferMicroService.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeProcessingServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private ResumeProcessingService resumeProcessingService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void processAndStoreResume_success() throws Exception {
        // Arrange
        MockMultipartFile mockFile = new MockMultipartFile("file", "resume.pdf", "application/pdf", "dummy pdf content".getBytes());
        
        CandidateMasterProfile mockProfile = new CandidateMasterProfile(
                new CandidateMasterProfile.DistilledProfile(
                        "B.Tech",
                        "Mid-Level",
                        List.of("Junior Roles"),
                        List.of("Java", "Spring Boot"),
                        List.of("Software Engineer"),
                        List.of("Senior Roles")
                ),
                List.of("Java Developer", "Spring Boot Engineer", "Backend Developer")
        );

        when(chatClient.prompt().system(anyString()).user(anyString()).call().entity(CandidateMasterProfile.class))
                .thenReturn(mockProfile);

        UserProfile existingUser = new UserProfile("Sahil", "sahil@fetchmyoffer.com");
        when(userProfileRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        // Mock TikaDocumentReader construction to avoid actually reading the dummy PDF via Tika
        try (MockedConstruction<TikaDocumentReader> mockedTika = mockConstruction(TikaDocumentReader.class,
                (mock, context) -> {
                    when(mock.get()).thenReturn(List.of(new Document("Extracted resume text")));
                })) {

            // Act
            assertDoesNotThrow(() -> resumeProcessingService.processAndStoreResume(mockFile));

            // Assert
            verify(userProfileRepository).findById(1L);
            verify(userProfileRepository).save(existingUser);
            verify(vectorStore).add(anyList());
        }
    }

    @Test
    void processAndStoreResume_createsNewUserIfNotFound() throws Exception {
        // Arrange
        MockMultipartFile mockFile = new MockMultipartFile("file", "resume.pdf", "application/pdf", "dummy pdf content".getBytes());
        
        CandidateMasterProfile mockProfile = new CandidateMasterProfile(
                new CandidateMasterProfile.DistilledProfile(
                        "B.Tech",
                        "Mid-Level",
                        List.of("Junior Roles"),
                        List.of("Java", "Spring Boot"),
                        List.of("Software Engineer"),
                        List.of("Senior Roles")
                ),
                List.of("Java Developer", "Spring Boot Engineer", "Backend Developer")
        );

        when(chatClient.prompt().system(anyString()).user(anyString()).call().entity(CandidateMasterProfile.class))
                .thenReturn(mockProfile);

        when(userProfileRepository.findById(1L)).thenReturn(Optional.empty());

        try (MockedConstruction<TikaDocumentReader> mockedTika = mockConstruction(TikaDocumentReader.class,
                (mock, context) -> {
                    when(mock.get()).thenReturn(List.of(new Document("Extracted resume text")));
                })) {

            // Act
            assertDoesNotThrow(() -> resumeProcessingService.processAndStoreResume(mockFile));

            // Assert
            verify(userProfileRepository).findById(1L);
            verify(userProfileRepository).save(any(UserProfile.class)); // Verifies save was called with the newly created profile
        }
    }
}
