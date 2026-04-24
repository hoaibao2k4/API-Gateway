package com.example.gateway.services.implement;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.gateway.services.TokenBlacklistService;

import reactor.core.publisher.Mono;

@Service
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
    public Mono<ResponseEntity<Map<String, String>>> handleWebhookEvent(Map<String, Object> event) {
        String type = (String) event.getOrDefault("type", "");
        String resourceType = (String) event.getOrDefault("resourceType", "");
        String resourcePath = (String) event.getOrDefault("resourcePath", "");

        // admin change role mapping
        // P2-inc plugin send type "admin.CLIENT_ROLE_MAPPING-CREATE"
        if (type.startsWith("admin.") && isRoleMappingEvent(resourceType)) {
            String userId = extractUserIdFromResourcePath(resourcePath);
            if (userId != null) {
                return blacklistUser(userId)
                        .then(Mono.just(ResponseEntity.ok(Map.of(
                                "status", "processed",
                                "userId", userId,
                                "action", "blacklisted"))));
            }
        }

        // other events : ignored
        return Mono.just(ResponseEntity.ok(Map.of("status", "ignored")));
    }

    @Override
    public Mono<Void> blacklistUser(String keycloakUserId) {
        String key = blacklistPrefix + keycloakUserId;
        // save issuedAt and compare iat (token)
        String issuedAt = String.valueOf(Instant.now().toEpochMilli());
        return redisTemplate.opsForValue()
                .set(key, issuedAt, blacklistTtl)
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
