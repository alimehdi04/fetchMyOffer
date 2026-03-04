package com.autisheimer.fetchMyOfferMicroService.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class JobQueryGeneratorService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public JobQueryGeneratorService(ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }

    // We use a Record to force Groq to return a clean JSON array
    public record SearchQueries(List<String> queries) {}

    public SearchQueries generateSearchQueries() {
        System.out.println(" Fetching candidate profile to generate search queries...");

        // 1. Fetch the broad context of the resume from the database
        List<Document> resumeChunks = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("skills experience education projects programming languages")
                        .topK(4)
                        .build()
        );

        String candidateProfile = resumeChunks.stream()
                .map(Document::getFormattedContent)
                .collect(Collectors.joining("\n... "));

        if (candidateProfile.isBlank()) {
            return new SearchQueries(List.of("Software Engineering Intern")); // Fallback
        }

        // 2. Set up the structured output converter
        var converter = new BeanOutputConverter<>(SearchQueries.class);

        String systemPrompt = """
            You are an expert technical career coach. 
            Analyze the following Candidate Profile and generate exactly 3 highly relevant, concise job search keywords or job titles that this candidate should search for on job boards like Internshala or LinkedIn.
            Keep them short (e.g., "Java Backend Intern", "Data Analyst", "Junior Spring Boot Developer").
            
            Candidate Profile:
            {profile}
            
            {formatInstructions}
            """;

        System.out.println(" Asking Groq to generate optimal job titles...");

        // 3. Ask Groq for the queries
        String jsonResponse = chatClient.prompt()
                .system(s -> s.text(systemPrompt)
                        .param("profile", candidateProfile)
                        .param("formatInstructions", converter.getFormat()))
                .call()
                .content();

        // 4. Convert to our Java Record
        return converter.convert(jsonResponse);
    }
}