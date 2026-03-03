package com.autisheimer.fetchMyOfferMicroService.controller;

import com.autisheimer.fetchMyOfferMicroService.service.JobEvaluatorService;
import com.autisheimer.fetchMyOfferMicroService.service.TelegramNotificationService; // 🛑 NEW IMPORT
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final JobEvaluatorService evaluatorService;
    private final TelegramNotificationService telegramService; // 🛑 NEW

    @Autowired
    public WebhookController(JobEvaluatorService evaluatorService,
                             TelegramNotificationService telegramService) { // 🛑 INJECTED HERE
        this.evaluatorService = evaluatorService;
        this.telegramService = telegramService;
    }

    public record JobPayload(String jobId, String status, List<Map<String, String>> data) {}

    @PostMapping("/scrape-results")
    public ResponseEntity<String> receiveScrapeResults(@RequestBody JobPayload payload) {

        System.out.println("=====================================================");
        System.out.println("[✓] WEBHOOK RECEIVED: " + payload.data().size() + " jobs.");

        int matchCount = 0;

        // Evaluate the first 3 jobs to test the LLM
        int limit = Math.min(3, payload.data().size());
        for (int i = 0; i < limit; i++) {
            Map<String, String> job = payload.data().get(i);
            try {
                JobEvaluatorService.EvaluationResult result = evaluatorService.evaluateJob(job);

                System.out.println("\nJob: " + job.get("title") + " at " + job.get("company"));
                System.out.println("Match: " + (result.isMatch() ? "✅ YES" : "❌ NO"));
                System.out.println("Reason: " + result.reason());

                // 🛑 NEW: IF IT'S A MATCH, SEND IT TO YOUR PHONE!
                if(result.isMatch()) {
                    matchCount++;
                    telegramService.sendJobMatchNotification(
                            job.get("title"),
                            job.get("company"),
                            job.get("url"), // The URL grabbed by Python Playwright
                            result.reason()
                    );
                }

            } catch (Exception e) {
                System.err.println("Failed to evaluate job: " + e.getMessage());
            }
        }

        System.out.println("\n=====================================================");
        System.out.println("Evaluated " + limit + " jobs. Found " + matchCount + " matches.");
        System.out.println("=====================================================");

        return ResponseEntity.ok("Webhook successfully processed");
    }
}