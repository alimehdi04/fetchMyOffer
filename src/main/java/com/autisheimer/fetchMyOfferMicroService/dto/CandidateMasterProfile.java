package com.autisheimer.fetchMyOfferMicroService.dto;

import java.util.List;

public record CandidateMasterProfile(
        DistilledProfile distilledProfile,
        List<String> searchQueries
) {
    public record DistilledProfile(
            String education,
            List<String> coreSkills,
            List<String> seekingRoles,
            List<String> rejectCriteria
    ) {}
}