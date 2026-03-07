package com.autisheimer.fetchMyOfferMicroService.dto;

public record BatchEvaluationResponse(
        String url,
        boolean isMatch,
        String reason
) {}