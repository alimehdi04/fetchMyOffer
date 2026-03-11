package com.autisheimer.fetchMyOfferMicroService.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class JobEvaluatorService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public JobEvaluatorService(@Qualifier("groqChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
        this.objectMapper = new ObjectMapper();
    }

    // Record to hold the AI's response for each job
    public record BatchEvaluationResponse(String url, boolean isMatch, String reason) {}

    /**
     * Master method to evaluate all jobs. It chunks them into batches of 5 to save tokens
     * and respects Groq's Rate Limits.
     */
    public List<BatchEvaluationResponse> evaluateAllJobs(List<Map<String, String>> allJobs, String distilledProfileJson) {
        List<BatchEvaluationResponse> allEvaluations = new ArrayList<>();
        int BATCH_SIZE = 5;

        System.out.println("🧠 Evaluating " + allJobs.size() + " jobs in batches of " + BATCH_SIZE + "...");

        for (int i = 0; i < allJobs.size(); i += BATCH_SIZE) {
            int end = Math.min(allJobs.size(), i + BATCH_SIZE);
            List<Map<String, String>> batch = allJobs.subList(i, end);

            // Evaluate this specific chunk of 5 jobs
            List<BatchEvaluationResponse> batchResult = evaluateJobBatch(batch, distilledProfileJson);
            allEvaluations.addAll(batchResult);

            // Throttle Groq to avoid 429 Rate Limit (Wait 8 seconds between batches)
            if (end < allJobs.size()) {
                try {
                    System.out.println("⏳ Throttling AI (8 seconds) to respect Groq TPM limits...");
                    Thread.sleep(8000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return allEvaluations;
    }

    /**
     * The actual AI call for a single batch of jobs.
     */
    private List<BatchEvaluationResponse> evaluateJobBatch(List<Map<String, String>> jobBatch, String distilledProfileJson) {
        try {
            // Convert the 5 jobs into a JSON string so the LLM can read them
            String jobsJson = objectMapper.writeValueAsString(jobBatch);

            String systemPrompt = String.format("""
                You are an expert technical recruiter. 
                Evaluate the following batch of job descriptions against the Candidate Profile.
                
                Rules for matching:
                1. If the job requires significantly more experience than the candidate has, it is NOT a match.
                2. If the job requires completely different core technologies, it is NOT a match.
                3. If the job aligns with the candidate's skills and experience level, it IS a match.
                
                Candidate Profile:
                %s
                
                OUTPUT FORMAT:
                Return a STRICTLY VALID JSON ARRAY of objects. Do not include markdown formatting like ```json.
                Each object must have exactly these keys:
                - "url": (the exact url of the job from the input)
                - "isMatch": (boolean, true if it fits)
                - "reason": (string, Write a concise, 2-sentence explanation of why this job is a match. Explicitly name the overlapping skills and confirm it aligns with the candidate's current experience level. Keep it punchy, direct, and avoid generic introductory filler. DO NOT write 1-liners.)
                """, distilledProfileJson);

            String userPrompt = String.format("""
                JOB BATCH TO EVALUATE:
                %s
                """, jobsJson);

            // Call Groq (Ask for raw JSON text)
            String jsonResponse = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

            // Clean the response (Groq sometimes wraps JSON in markdown tags)
            if (jsonResponse != null && jsonResponse.startsWith("```json")) {
                jsonResponse = jsonResponse.replace("```json", "").replace("```", "").trim();
            }

            // Convert the JSON array back into Java objects
            return objectMapper.readValue(jsonResponse, new TypeReference<List<BatchEvaluationResponse>>() {});

        } catch (Exception e) {
            System.err.println("❌ Failed to evaluate batch: " + e.getMessage());
            return new ArrayList<>(); // Return empty list on failure to prevent crashing the loop
        }
    }
}