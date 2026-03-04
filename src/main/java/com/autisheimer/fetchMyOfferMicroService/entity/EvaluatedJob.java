package com.autisheimer.fetchMyOfferMicroService.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "evaluated_jobs")
public class EvaluatedJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // We use the URL as the unique identifier to prevent duplicates
    @Column(unique = true, nullable = false, length = 1000)
    private String jobUrl;

    private String title;
    private String company;
    private LocalDateTime evaluatedAt;

    public EvaluatedJob() {
        // Default constructor required by JPA
    }

    public EvaluatedJob(String jobUrl, String title, String company) {
        this.jobUrl = jobUrl;
        this.title = title;
        this.company = company;
        this.evaluatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getJobUrl() { return jobUrl; }
    public String getTitle() { return title; }
    public String getCompany() { return company; }
    public LocalDateTime getEvaluatedAt() { return evaluatedAt; }
}