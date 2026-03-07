package com.autisheimer.fetchMyOfferMicroService.service;

import com.autisheimer.fetchMyOfferMicroService.entity.UserProfile;
import com.autisheimer.fetchMyOfferMicroService.repository.UserProfileRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Service
public class JobHuntScheduler {

    private final UserProfileRepository userProfileRepository;
    private final RestClient restClient;

    @Value("${scraper.python.url:http://localhost:8000/api/v1/scrape}")
    private String pythonScraperUrl;

    @Value("${webhook.callback.url:http://localhost:8080/api/v1/webhooks/scrape-results}")
    private String webhookCallbackUrl;

    public record ScrapeRequest(
            String query,
            String location,
            String platform, // 🛑 Target platform (naukri or internshala)
            @JsonProperty("callback_url") String callbackUrl,
            @JsonProperty("job_id") String jobId
    ) {}

    public JobHuntScheduler(UserProfileRepository userProfileRepository, RestClient.Builder restClientBuilder) {
        this.userProfileRepository = userProfileRepository;

        // 🛑 Tell Spring Boot to wait up to 60 seconds for the Python app to wake up (Fixes Render 502 Cold Start)
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setReadTimeout(60000);
        requestFactory.setConnectTimeout(60000);

        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
    }

    // CRON JOB: Runs based on the property file, defaults to 9:00 AM every day
    @Scheduled(cron = "${job.hunt.cron:0 0 9 * * ?}")
    public void triggerAutonomousJobHunt() {
        System.out.println("\n⏰ [CRON TRIGGERED] Good Morning! Fetching AI-generated queries from Database...");

        // 1. Fetch the user profile (Hardcoded ID 1L for now)
        UserProfile myProfile = userProfileRepository.findById(1L).orElse(null);

        if (myProfile == null || myProfile.getSearchQueries() == null || myProfile.getSearchQueries().isEmpty()) {
            System.out.println("⚠️ No profile or queries found in the database. Please upload a resume first! Skipping today's hunt.");
            return;
        }

        List<String> targetQueries = myProfile.getSearchQueries();
        System.out.println("🎯 Today's Target Queries: " + targetQueries);

        // 🛑 Define our hunting grounds
        List<String> targetPlatforms = List.of("internshala", "naukri");

        // 2. Send each query to the Python Scraper Worker for EACH platform
        for (String query : targetQueries) {
            for (String platform : targetPlatforms) {
                System.out.println("🚀 Dispatching Python Scraper for: '" + query + "' on [" + platform.toUpperCase() + "]");

                String jobId = "job-" + UUID.randomUUID().toString().substring(0, 8);

                ScrapeRequest payload = new ScrapeRequest(
                        query,
                        "India",
                        platform,
                        webhookCallbackUrl,
                        jobId
                );

                try {
                    restClient.post()
                            .uri(pythonScraperUrl)
                            .body(payload)
                            .retrieve()
                            .toBodilessEntity();

                    System.out.println("✅ Successfully triggered scraper for: '" + query + "' on [" + platform.toUpperCase() + "]");

                    // Sleep for 30 seconds to let the Python container process the current browser session
                    // before hitting it with another request. This prevents Out Of Memory crashes on free tiers.
                    Thread.sleep(30000);
                } catch (Exception e) {
                    System.err.println("❌ Failed to trigger scraper for '" + query + "' on [" + platform + "]: " + e.getMessage());
                }
            }
        }

        System.out.println("💤 Daily hunt dispatched. Scheduler hibernating until tomorrow.\n");
    }
}