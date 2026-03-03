package com.autisheimer.fetchMyOfferMicroService.controller;



import com.autisheimer.fetchMyOfferMicroService.service.ResumeProcessingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final ResumeProcessingService resumeService;

    public ProfileController(ResumeProcessingService resumeService) {
        this.resumeService = resumeService;
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
}