package com.autisheimer.fetchMyOfferMicroService.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    // We store the LLM-distilled JSON profile as plain text to save database complexity
    @Column(columnDefinition = "TEXT")
    private String distilledProfileJson;

    // This creates a secondary table automatically to store the List of strings
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_search_queries", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "query")
    private List<String> searchQueries;

    // Constructors
    public UserProfile() {}

    public UserProfile(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDistilledProfileJson() { return distilledProfileJson; }
    public void setDistilledProfileJson(String distilledProfileJson) { this.distilledProfileJson = distilledProfileJson; }
    public List<String> getSearchQueries() { return searchQueries; }
    public void setSearchQueries(List<String> searchQueries) { this.searchQueries = searchQueries; }
}