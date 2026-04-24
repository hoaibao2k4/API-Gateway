package com.example.gateway.services;

import java.util.Map;

import org.springframework.http.ResponseEntity;

import reactor.core.publisher.Mono;


public interface TokenBlacklistService {


    Mono<ResponseEntity<Map<String, String>>> handleWebhookEvent(Map<String, Object> event);

    
    Mono<Void> blacklistUser(String keycloakUserId);

    Mono<Long> getBlacklistTimestamp(String keycloakUserId);
    Mono<Void> removeFromBlacklist(String keycloakUserId);
}
