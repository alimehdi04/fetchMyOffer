package com.autisheimer.fetchMyOfferMicroService.service;

import com.autisheimer.fetchMyOfferMicroService.dto.CandidateMasterProfile;
import com.autisheimer.fetchMyOfferMicroService.entity.UserProfile;
import com.autisheimer.fetchMyOfferMicroService.repository.UserProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResumeProcessingService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final UserProfileRepository userProfileRepository;
    private final ObjectMapper objectMapper;

    public ResumeProcessingService(VectorStore vectorStore,
                                   @Qualifier("groqChatClient") ChatClient chatClient,
                                   UserProfileRepository userProfileRepository) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
        this.userProfileRepository = userProfileRepository;
        this.objectMapper = new ObjectMapper();
    }

    public void processAndStoreResume(MultipartFile file) throws IOException {
        // 1. Convert the uploaded file into a Spring Resource
        Resource pdfResource = new InputStreamResource(file.getInputStream());

        // 2. Extract raw text from the PDF using Apache Tika
        TikaDocumentReader documentReader = new TikaDocumentReader(pdfResource);
        List<Document> rawDocuments = documentReader.get();

        // Combine all pages into one massive string for the LLM
        String fullResumeText = rawDocuments.stream()
                .map(Document::getFormattedContent)
                .collect(Collectors.joining("\n"));

        // 3. ONE-SHOT LLM DISTILLATION
        System.out.println("🧠 Asking Groq to distill resume and generate search queries...");

        String systemPrompt = """
            You are an expert technical recruiter and data extractor.
            Read the provided resume text and generate a structured CandidateMasterProfile.
            
            Rules:
            1. Extract the core education, tech stack, and roles the candidate is seeking.
            2. Evaluate the candidate's 'experienceLevel' based on chronological history (e.g., "3rd-year student graduating May 2027 with 11 months of total internship experience").
            3. Determine exactly what they are 'eligibleFor' (e.g., ["Summer Internships", "Co-ops", "New-Grad 2027 Roles"]). Do NOT include immediate full-time jobs if they are still a student.
            4. Infer 3-4 strict 'rejectCriteria' focusing heavily on experience mismatches (e.g., "Requires 2+ years full-time experience", "Requires immediate full-time joining", "Senior/Lead roles").
            5. Generate exactly 3 highly optimized search queries for job boards (e.g. Internshala/Naukri) based on their skills. Keep queries under 4 words.
            """;

        // Spring AI automatically forces the LLM to output JSON matching our Record!
        CandidateMasterProfile masterProfile = chatClient.prompt()
                .system(systemPrompt)
                .user(fullResumeText)
                .call()
                .entity(CandidateMasterProfile.class);

        // 4. Save to PostgreSQL Database
        try {
            // Fetch User ID 1 (Sahil), or create it if it doesn't exist yet
            UserProfile user = userProfileRepository.findById(1L)
                    .orElse(new UserProfile("Sahil", "sahil@fetchmyoffer.com"));

            String distilledJson = objectMapper.writeValueAsString(masterProfile.distilledProfile());
            user.setDistilledProfileJson(distilledJson);
            user.setSearchQueries(masterProfile.searchQueries());

            userProfileRepository.save(user);
            System.out.println("✅ Profile distilled and queries saved to database successfully!");

        } catch (Exception e) {
            System.err.println("❌ Failed to save distilled profile: " + e.getMessage());
        }

        // 5. Chunk and Save to Vector Store (Legacy Memory - Optional but good to keep)
        System.out.println("💾 Saving vector embeddings to pgvector...");
        TokenTextSplitter splitter = new TokenTextSplitter(800, 400, 5, 10000, true);
        List<Document> chunkedDocuments = splitter.apply(rawDocuments);

        for (Document doc : chunkedDocuments) {
            doc.getMetadata().put("doc_type", "candidate_resume");
        }
        vectorStore.add(chunkedDocuments);
    }
}