package com.autisheimer.fetchMyOfferMicroService.controller;

import com.autisheimer.fetchMyOfferMicroService.service.JobHuntScheduler;
import com.autisheimer.fetchMyOfferMicroService.service.JobQueryGeneratorService;
import com.autisheimer.fetchMyOfferMicroService.service.ResumeProcessingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final ResumeProcessingService resumeService;
    private final JobQueryGeneratorService queryGeneratorService;
    private final JobHuntScheduler jobHuntScheduler; //  Added Scheduler

    //  Injected all three services into the constructor
    public ProfileController(ResumeProcessingService resumeService,
                             JobQueryGeneratorService queryGeneratorService,
                             JobHuntScheduler jobHuntScheduler) {
        this.resumeService = resumeService;
        this.queryGeneratorService = queryGeneratorService;
        this.jobHuntScheduler = jobHuntScheduler;
    }

    @PostMapping("/upload-resume")
    public ResponseEntity<String> uploadResume(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || !file.getContentType().equals("application/pdf")) {
            return ResponseEntity.badRequest().body("Please upload a valid PDF file.");
        }

        try {
            resumeService.processAndStoreResume(file);
            return ResponseEntity.ok("Resume parsed, vectorized, and securely stored in memory.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to process resume: " + e.getMessage());
        }
    }

    @GetMapping("/generate-queries")
    public ResponseEntity<JobQueryGeneratorService.SearchQueries> getRecommendedQueries() {
        return ResponseEntity.ok(queryGeneratorService.generateSearchQueries());
    }

    //  NEW: Manual Trigger API
    @GetMapping("/force-hunt")
    public ResponseEntity<String> forceHunt() {
        System.out.println(" Manual hunt initiated via API!");

        // We run this in a new thread so the browser doesn't sit there spinning
        // while it waits for the whole scraping and evaluation process to finish.
        new Thread(() -> jobHuntScheduler.triggerAutonomousJobHunt()).start();

        return ResponseEntity.ok("Manual job hunt successfully triggered! Check Telegram for matches.");
    }
}