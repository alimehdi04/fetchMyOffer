package com.autisheimer.fetchMyOfferMicroService.controller;

import com.autisheimer.fetchMyOfferMicroService.entity.EvaluatedJob;
import com.autisheimer.fetchMyOfferMicroService.repository.EvaluatedJobRepository;
import com.autisheimer.fetchMyOfferMicroService.service.JobEvaluatorService;
import com.autisheimer.fetchMyOfferMicroService.service.TelegramNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final JobEvaluatorService evaluatorService;
    private final TelegramNotificationService telegramService;
    private final EvaluatedJobRepository jobRepository; // The Memory Bank

    @Autowired
    public WebhookController(JobEvaluatorService evaluatorService,
                             TelegramNotificationService telegramService,
                             EvaluatedJobRepository jobRepository) {
        this.evaluatorService = evaluatorService;
        this.telegramService = telegramService;
        this.jobRepository = jobRepository;
    }

    public record JobPayload(String jobId, String status, List<Map<String, String>> data) {}

    @PostMapping("/scrape-results")
    public ResponseEntity<String> receiveScrapeResults(@RequestBody JobPayload payload) {

        System.out.println("=====================================================");
        System.out.println("[✓] WEBHOOK RECEIVED: " + payload.data().size() + " jobs.");

        int newJobsEvaluated = 0;
        int matchCount = 0;
        int duplicateCount = 0;

        for (Map<String, String> job : payload.data()) {
            //  1. Check our Smart Limit (Stop after 5 NEW evaluations)
            if (newJobsEvaluated >= 5) {
                break;
            }

            String jobUrl = job.get("url");

            //  2. Deduplication Check
            if (jobUrl == null || jobRepository.existsByJobUrl(jobUrl)) {
                duplicateCount++;
                continue; // Skip this iteration, we've seen this job before
            }

            //  3. Evaluate the New Job
            try {
                JobEvaluatorService.EvaluationResult result = evaluatorService.evaluateJob(job);

                System.out.println("\nJob: " + job.get("title") + " at " + job.get("company"));
                System.out.println("Match: " + (result.isMatch() ? "YES" : "NO"));
                System.out.println("Reason: " + result.reason());

                if (result.isMatch()) {
                    matchCount++;
                    telegramService.sendJobMatchNotification(
                            job.get("title"),
                            job.get("company"),
                            jobUrl,
                            result.reason()
                    );
                }

                // 4. Save to Database so we never evaluate it again
                jobRepository.save(new EvaluatedJob(jobUrl, job.get("title"), job.get("company")));
                newJobsEvaluated++;

            } catch (Exception e) {
                System.err.println("Failed to evaluate job: " + e.getMessage());
            }
        }

        System.out.println("\n=====================================================");
        System.out.println("Skipped " + duplicateCount + " previously seen jobs.");
        System.out.println("Evaluated " + newJobsEvaluated + " new jobs. Found " + matchCount + " matches.");
        System.out.println("=====================================================");

        return ResponseEntity.ok("Webhook successfully processed");
    }
}