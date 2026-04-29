package com.example.gateway.services.implement;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.gateway.dto.request.WebhookEventRequest;
import com.example.gateway.dto.response.WebhookResponse;
import com.example.gateway.services.TokenBlacklistService;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final Duration blacklistTtl;

    @Value("${BLACKLIST_PREFIX:token:blacklist:}")
    private String blacklistPrefix;

    public TokenBlacklistServiceImpl(
            ReactiveStringRedisTemplate redisTemplate,
            @Value("${ACCESS_TOKEN_LIFESPAN:300}") long accessTokenLifespanSeconds) {
        this.redisTemplate = redisTemplate;
        this.blacklistTtl = Duration.ofSeconds(accessTokenLifespanSeconds);
    }

    @Override
    public Mono<ResponseEntity<WebhookResponse>> handleWebhookEvent(WebhookEventRequest event) {
        String type = event.getType() != null ? event.getType() : "";
        String resourceType = event.getResourceType() != null ? event.getResourceType() : "";
        String resourcePath = event.getResourcePath() != null ? event.getResourcePath() : "";

        // admin change role mapping
        if (type.startsWith("admin.") && isRoleMappingEvent(resourceType)) {
            String userId = extractUserIdFromResourcePath(resourcePath);
            if (userId != null) {
                return blacklistUser(userId)
                        .then(Mono.just(ResponseEntity.ok(WebhookResponse.builder()
                                .status("processed")
                                .userId(userId)
                                .action("blacklisted")
                                .build())));
            }
        }

        // other events : ignored
        return Mono.just(ResponseEntity.ok(WebhookResponse.builder().status("ignored").build()));
    }

    @Override
    public Mono<Void> blacklistUser(String keycloakUserId) {
        String key = blacklistPrefix + keycloakUserId;
        // save issuedAt and compare iat (token)
        String issuedAt = String.valueOf(Instant.now().toEpochMilli());
        return redisTemplate.opsForValue()
                .set(key, issuedAt, blacklistTtl)
                .doOnError(e -> log.error("Failed to blacklist user {} in Redis: {}", keycloakUserId, e.getMessage()))
                .onErrorMap(e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Redis operation failed", e))
                .then();
    }

    @Override
    public Mono<Long> getBlacklistTimestamp(String keycloakUserId) {
        String key = blacklistPrefix + keycloakUserId;
        return redisTemplate.opsForValue().get(key)
                .map(Long::parseLong);
    }

    @Override
    public Mono<Void> removeFromBlacklist(String keycloakUserId) {
        String key = blacklistPrefix + keycloakUserId;
        return redisTemplate.delete(key)
                .then();
    }

    private boolean isRoleMappingEvent(String resourceType) {
        return "CLIENT_ROLE_MAPPING".equals(resourceType)
                || "REALM_ROLE_MAPPING".equals(resourceType);
    }

    /**
     * extract userId from resourcePath.
     * "users/5dcc1d60-.../role-mappings/clients/..."
     */
    private String extractUserIdFromResourcePath(String resourcePath) {
        if (resourcePath == null || !resourcePath.startsWith("users/")) {
            return null;
        }
        String[] parts = resourcePath.split("/");
        return parts.length >= 2 ? parts[1] : null;
    }
}
