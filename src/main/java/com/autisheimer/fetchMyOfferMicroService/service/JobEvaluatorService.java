package com.autisheimer.fetchMyOfferMicroService.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JobEvaluatorService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore; // Inject the memory vault!

    // Inject both the Groq Brain and the pgvector database
    public JobEvaluatorService(@Qualifier("groqChatClient") ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }

    public record EvaluationResult(boolean isMatch, String reason) {}

    public EvaluationResult evaluateJob(Map<String, String> jobData) {
        String jobTitle = jobData.get("title");
        String jobDescription = jobData.get("description");

        System.out.println("Performing Mathematical Similarity Search for: " + jobTitle);

        // 1. RAG RETRIEVAL
        List<Document> relevantResumeChunks = vectorStore.similaritySearch(
                SearchRequest.builder().query(jobDescription).topK(3).build()
        );

        String fetchedProfile = relevantResumeChunks.stream()
                .map(Document::getFormattedContent)
                .collect(Collectors.joining("\n... "));

        final String finalCandidateProfile = fetchedProfile.isBlank()
                ? "No resume data found in the database. Please upload a resume."
                : fetchedProfile;

        // Create a converter to safely map the LLM output to our Record
        var converter = new BeanOutputConverter<>(EvaluationResult.class);

        String systemPrompt = """
            You are an expert technical recruiter. 
            Evaluate if the provided Job Opportunity is a good match for the Candidate Profile.
            
            Rules for matching:
            1. If the job requires significantly more experience than the candidate has, it is NOT a match.
            2. If the job requires completely different core technologies, it is NOT a match.
            3. If the job aligns with the candidate's skills and experience level, it IS a match.
            
            Candidate Profile (Extracted from Resume):
            {userProfile}
            
            {formatInstructions}
            """;

        String userPrompt = """
            Job Title: {jobTitle}
            Job Description: {jobDescription}
            """;

        System.out.println("Asking Groq (Llama 3) to evaluate match...");

        // 2. Ask Groq for TEXT, not a strict entity, bypassing the extra_body bug!
        String jsonResponse = chatClient.prompt()
                .system(s -> s.text(systemPrompt)
                        .param("userProfile", finalCandidateProfile)
                        .param("formatInstructions", converter.getFormat())) // Inject JSON rules
                .user(u -> u.text(userPrompt)
                        .param("jobTitle", jobTitle)
                        .param("jobDescription", jobDescription))
                .call()
                .content(); // Ask for raw text!

        // 3. Convert the safe JSON text into our Java Record
        return converter.convert(jsonResponse);
    }
}