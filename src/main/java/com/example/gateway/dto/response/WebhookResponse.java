package com.example.gateway.dto.response;

import lombok.Builder;

@Builder
public record WebhookResponse(
    String status,
    String userId,
    String action
) {}
