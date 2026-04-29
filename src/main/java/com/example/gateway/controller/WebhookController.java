package com.example.gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.gateway.dto.request.WebhookEventRequest;
import com.example.gateway.dto.response.WebhookResponse;
import com.example.gateway.services.TokenBlacklistService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class WebhookController {

    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/webhook")
    public Mono<ResponseEntity<WebhookResponse>> handleKeycloakEvent(@Valid @RequestBody WebhookEventRequest event) {
        return tokenBlacklistService.handleWebhookEvent(event);
    }
}
