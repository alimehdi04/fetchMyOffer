package com.autisheimer.fetchMyOfferMicroService.controller;

import com.autisheimer.fetchMyOfferMicroService.entity.EvaluatedJob;
import com.autisheimer.fetchMyOfferMicroService.entity.UserProfile;
import com.autisheimer.fetchMyOfferMicroService.repository.EvaluatedJobRepository;
import com.autisheimer.fetchMyOfferMicroService.repository.UserProfileRepository;
import com.autisheimer.fetchMyOfferMicroService.service.JobEvaluatorService;
import com.autisheimer.fetchMyOfferMicroService.service.TelegramNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final JobEvaluatorService evaluatorService;
    private final TelegramNotificationService telegramService;
    private final EvaluatedJobRepository jobRepository;
    private final UserProfileRepository userProfileRepository; // 🛑 NEW: To fetch the distilled profile

    public WebhookController(JobEvaluatorService evaluatorService,
                             TelegramNotificationService telegramService,
                             EvaluatedJobRepository jobRepository,
                             UserProfileRepository userProfileRepository) {
        this.evaluatorService = evaluatorService;
        this.telegramService = telegramService;
        this.jobRepository = jobRepository;
        this.userProfileRepository = userProfileRepository;
    }

    public record JobPayload(String jobId, String status, List<Map<String, String>> data) {}

    @PostMapping("/scrape-results")
    public ResponseEntity<String> receiveScrapeResults(@RequestBody JobPayload payload) {

        System.out.println("=====================================================");
        System.out.println("[✓] WEBHOOK RECEIVED: " + payload.data().size() + " jobs from scraper.");

        // 1. Fetch User Profile
        // (Hardcoded to ID 1L for now since it's your personal tool. Later, this can be dynamic!)
        UserProfile myProfile = userProfileRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("User profile not found in database! Please upload a resume first."));

        // 2. Filter out duplicates FIRST
        List<Map<String, String>> newJobsToEvaluate = new ArrayList<>();
        int duplicateCount = 0;

        for (Map<String, String> job : payload.data()) {
            String jobUrl = job.get("url");
            if (jobUrl == null || jobRepository.existsByJobUrl(jobUrl)) {
                duplicateCount++;
            } else {
                newJobsToEvaluate.add(job);
            }
        }

        System.out.println("Skipped " + duplicateCount + " previously seen jobs.");

        // Optional: Cap the max jobs we evaluate per webhook to save tokens (e.g., max 20)
        // Since we process in batches of 5, 20 jobs = 4 API calls.
        if (newJobsToEvaluate.size() > 10) {                         // changed from 20 -> 5
            System.out.println("⚠️ Capping evaluation to 5 jobs to respect AI rate limits.");
            newJobsToEvaluate = newJobsToEvaluate.subList(0, 10);    // changed from 20 -> 5
        }

        if (newJobsToEvaluate.isEmpty()) {
            System.out.println("No new jobs to evaluate. Exiting.");
            System.out.println("=====================================================\n");
            return ResponseEntity.ok("Processed. No new jobs.");
        }

        // 3. Send the clean list to the Batch Evaluator
        List<JobEvaluatorService.BatchEvaluationResponse> results =
                evaluatorService.evaluateAllJobs(newJobsToEvaluate, myProfile.getDistilledProfileJson());

        // 4. Process the AI's decisions
        int matchCount = 0;
        for (JobEvaluatorService.BatchEvaluationResponse result : results) {

            // Find the original job data so we have the title and company for Telegram
            Map<String, String> originalJob = newJobsToEvaluate.stream()
                    .filter(j -> j.get("url").equals(result.url()))
                    .findFirst()
                    .orElse(null);

            if (originalJob != null) {
                System.out.println("\nJob: " + originalJob.get("title") + " at " + originalJob.get("company"));
                System.out.println("Match: " + (result.isMatch() ? "YES" : "NO"));
                System.out.println("Reason: " + result.reason());

                if (result.isMatch()) {
                    matchCount++;
                    telegramService.sendJobMatchNotification(
                            originalJob.get("title"),
                            originalJob.get("company"),
                            originalJob.get("url"),
                            result.reason()
                    );
                }

                // Save to Database so we never evaluate it again, regardless of whether it was a match or not
                jobRepository.save(new EvaluatedJob(originalJob.get("url"), originalJob.get("title"), originalJob.get("company")));
            }
        }

        System.out.println("\n=====================================================");
        System.out.println("Evaluated " + newJobsToEvaluate.size() + " new jobs. Found " + matchCount + " matches.");
        System.out.println("=====================================================\n");

        return ResponseEntity.ok("Webhook successfully processed");
    }
}