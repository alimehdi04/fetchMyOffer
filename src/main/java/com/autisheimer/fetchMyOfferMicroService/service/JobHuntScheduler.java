package com.autisheimer.fetchMyOfferMicroService.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Service
public class JobHuntScheduler {

    private final JobQueryGeneratorService queryGeneratorService;
    private final RestClient restClient;

    @Value("${scraper.python.url:http://localhost:8000/api/v1/scrape}")
    private String pythonScraperUrl;

    @Value("${webhook.callback.url:http://localhost:8080/api/v1/webhooks/scrape-results}")
    private String webhookCallbackUrl;

    public JobHuntScheduler(JobQueryGeneratorService queryGeneratorService, RestClient.Builder restClientBuilder) {
        this.queryGeneratorService = queryGeneratorService;
        this.restClient = restClientBuilder.build();
    }

    // CRON JOB: Runs based on the property file, defaults to 9:00 AM every day
    @Scheduled(cron = "${job.hunt.cron:0 0 9 * * ?}")
    public void triggerAutonomousJobHunt() {
        System.out.println("\n [CRON TRIGGERED] Good Morning! Starting daily autonomous job hunt...");

        // 1. Ask the "Brain" what we should search for based on the resume
        JobQueryGeneratorService.SearchQueries searchData = queryGeneratorService.generateSearchQueries();

        if (searchData == null || searchData.queries() == null || searchData.queries().isEmpty()) {
            System.out.println(" No queries generated. Skipping today's hunt.");
            return;
        }

        System.out.println(" Today's Target Queries: " + searchData.queries());

        // 2. Send each query to the Python Scraper Worker
        for (String query : searchData.queries()) {
            System.out.println(" Dispatching Python Scraper for: " + query);

            Map<String, Object> payload = Map.of(
                    "query", query,
                    "location", "India",
                    "callback_url", webhookCallbackUrl,
                    "job_id", "job-" + UUID.randomUUID().toString().substring(0, 8)
            );

            try {
                restClient.post()
                        .uri(pythonScraperUrl)
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();

                System.out.println(" Successfully triggered scraper for: " + query);

                // Sleep for 45 seconds between requests to be polite to the target server
                Thread.sleep(45000);
            } catch (Exception e) {
                System.err.println("Failed to trigger scraper for '" + query + "': " + e.getMessage());
            }
        }

        System.out.println("Daily hunt dispatched. Scheduler hibernating until tomorrow.\n");
    }
}