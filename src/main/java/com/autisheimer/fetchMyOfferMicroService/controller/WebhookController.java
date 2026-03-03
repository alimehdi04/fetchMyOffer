package com.autisheimer.fetchMyOfferMicroService.controller;

import com.autisheimer.fetchMyOfferMicroService.service.JobEvaluatorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final JobEvaluatorService evaluatorService;

    @Autowired
    public WebhookController(JobEvaluatorService evaluatorService) {
        this.evaluatorService = evaluatorService;
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
                // 🛑 NEW: We no longer pass a hardcoded profile!
                JobEvaluatorService.EvaluationResult result = evaluatorService.evaluateJob(job);

                System.out.println("\nJob: " + job.get("title") + " at " + job.get("company"));
                System.out.println("Match: " + (result.isMatch() ? "✅ YES" : "❌ NO"));
                System.out.println("Reason: " + result.reason());

                if(result.isMatch()) matchCount++;

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
//package com.autisheimer.fetchMyOfferMicroService.controller;
//
//import com.autisheimer.fetchMyOfferMicroService.service.JobEvaluatorService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import java.util.List;
//import java.util.Map;
//
//import org.springframework.beans.factory.annotation.Autowired;
//
//@RestController
//@RequestMapping("/api/v1/webhooks")
//public class WebhookController {
//
//    private final JobEvaluatorService evaluatorService;
//
//    @Autowired
//    public WebhookController(JobEvaluatorService evaluatorService) {
//        this.evaluatorService = evaluatorService;
//    }
//
//    public record JobPayload(String jobId, String status, List<Map<String, String>> data) {}
//
//    @PostMapping("/scrape-results")
//    public ResponseEntity<String> receiveScrapeResults(@RequestBody JobPayload payload) {
//
//        System.out.println("=====================================================");
//        System.out.println("[✓] WEBHOOK RECEIVED: " + payload.data().size() + " jobs.");
//
//        // Hardcoded profile for testing. We will make this dynamic later.
//        String myProfile = "I am a 3rd-year Computer Science engineering student at Jamia Millia Islamia. " +
//                "My core skills are Java, Spring Boot, Data Structures and Algorithms (DSA), " +
//                "socket programming, and I am currently learning about RAG systems and LLMs.";
//
//        int matchCount = 0;
//
//        // Evaluate the first 3 jobs to test the LLM (so we don't blow through API limits immediately)
//        int limit = Math.min(3, payload.data().size());
//        for (int i = 0; i < limit; i++) {
//            Map<String, String> job = payload.data().get(i);
//            try {
//                JobEvaluatorService.EvaluationResult result = evaluatorService.evaluateJob(myProfile, job);
//
//                System.out.println("\nJob: " + job.get("title") + " at " + job.get("company"));
//                System.out.println("Match: " + (result.isMatch() ? "✅ YES" : "❌ NO"));
//                System.out.println("Reason: " + result.reason());
//
//                if(result.isMatch()) matchCount++;
//
//            } catch (Exception e) {
//                System.err.println("Failed to evaluate job: " + e.getMessage());
//            }
//        }
//
//        System.out.println("\n=====================================================");
//        System.out.println("Evaluated " + limit + " jobs. Found " + matchCount + " matches.");
//        System.out.println("=====================================================");
//
//        return ResponseEntity.ok("Webhook successfully processed");
//    }
//}
