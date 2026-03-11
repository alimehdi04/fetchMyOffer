package com.autisheimer.fetchMyOfferMicroService.dto;

import java.util.List;

public record CandidateMasterProfile(
        DistilledProfile distilledProfile,
        List<String> searchQueries
) {
    public record DistilledProfile(
            String education,
            String experienceLevel,
            List<String> eligibleFor,
            List<String> coreSkills,
            List<String> seekingRoles,
            List<String> rejectCriteria
    ) {}
}