package com.example.gateway.services;

import org.springframework.http.ResponseEntity;

import com.example.gateway.dto.request.WebhookEventRequest;
import com.example.gateway.dto.response.WebhookResponse;

import reactor.core.publisher.Mono;

public interface TokenBlacklistService {

    Mono<ResponseEntity<WebhookResponse>> handleWebhookEvent(WebhookEventRequest event);

    Mono<Void> blacklistUser(String keycloakUserId);

    Mono<Long> getBlacklistTimestamp(String keycloakUserId);

    Mono<Void> removeFromBlacklist(String keycloakUserId);
}
