package com.autisheimer.fetchMyOfferMicroService.controller;

import com.autisheimer.fetchMyOfferMicroService.entity.UserProfile;
import com.autisheimer.fetchMyOfferMicroService.repository.UserProfileRepository;
import com.autisheimer.fetchMyOfferMicroService.service.JobHuntScheduler;
import com.autisheimer.fetchMyOfferMicroService.service.ResumeProcessingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final ResumeProcessingService resumeService;
    private final JobHuntScheduler jobHuntScheduler;
    private final UserProfileRepository userProfileRepository;

    // Removed JobQueryGeneratorService because we fetch queries directly from the DB now!
    public ProfileController(ResumeProcessingService resumeService,
                             JobHuntScheduler jobHuntScheduler,
                             UserProfileRepository userProfileRepository) {
        this.resumeService = resumeService;
        this.jobHuntScheduler = jobHuntScheduler;
        this.userProfileRepository = userProfileRepository;
    }

    @PostMapping("/upload-resume")
    public ResponseEntity<String> uploadResume(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || !file.getContentType().equals("application/pdf")) {
            return ResponseEntity.badRequest().body("Please upload a valid PDF file.");
        }

        try {
            resumeService.processAndStoreResume(file);
            return ResponseEntity.ok("✅ Resume parsed, Profile distilled, Search Queries generated, and data securely stored in PostgreSQL!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to process resume: " + e.getMessage());
        }
    }

    @GetMapping("/generate-queries")
    public ResponseEntity<List<String>> getRecommendedQueries() {
        // Now, we just instantly fetch the pre-computed queries from the database!
        UserProfile user = userProfileRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("User not found. Upload resume first."));

        return ResponseEntity.ok(user.getSearchQueries());
    }

    @GetMapping("/force-hunt")
    public ResponseEntity<String> forceHunt() {
        System.out.println("🕹️ Manual hunt initiated via API!");

        new Thread(() -> jobHuntScheduler.triggerAutonomousJobHunt()).start();

        return ResponseEntity.ok("Manual job hunt successfully triggered! Check Telegram for matches.");
    }
}