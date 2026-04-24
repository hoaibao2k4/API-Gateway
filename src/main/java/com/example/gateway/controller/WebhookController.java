package com.example.gateway.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.gateway.services.TokenBlacklistService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Controller nhận Keycloak Webhook Event (P2-inc plugin).
 * Chỉ nhận request và delegate toàn bộ logic cho Service.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class WebhookController {

    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/webhook")
    public Mono<ResponseEntity<Map<String, String>>> handleKeycloakEvent(@RequestBody Map<String, Object> event) {
        return tokenBlacklistService.handleWebhookEvent(event);
    }
}
