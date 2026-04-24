package com.example.gateway.security;

import java.time.Instant;


import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebSession;

import com.example.gateway.exception.RoleChangedException;
import com.example.gateway.services.TokenBlacklistService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@Order(1) // run after Spring Security filter chain
@RequiredArgsConstructor
public class TokenBlacklistFilter implements WebFilter {

    private final TokenBlacklistService tokenBlacklistService;

    @Override
                            // req + res
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return exchange.getPrincipal()
                .filter(OAuth2AuthenticationToken.class::isInstance)
                .cast(OAuth2AuthenticationToken.class)
                .flatMap(authToken -> {
                    Object principalObj = authToken.getPrincipal();
                    if (principalObj instanceof OidcUser oidcUser) {
                        String keycloakUserId = oidcUser.getSubject();
                        return checkBlacklist(exchange, chain, oidcUser, keycloakUserId);
                    }
                    return chain.filter(exchange);
                })
                .switchIfEmpty(chain.filter(exchange)); // anonymous => pass
    }

    private Mono<Void> checkBlacklist(ServerWebExchange exchange, WebFilterChain chain,
            OidcUser oidcUser, String keycloakUserId) {

        return tokenBlacklistService.getBlacklistTimestamp(keycloakUserId)
                .flatMap(blacklistTimestamp -> {
                    Instant tokenIssuedAt = oidcUser.getIssuedAt();
                    long tokenIatMillis = tokenIssuedAt != null ? tokenIssuedAt.toEpochMilli() : 0;

                    if (tokenIatMillis < blacklistTimestamp) {
                        // remove session
                        return exchange.getSession()
                                .flatMap(WebSession::invalidate)
                                .then(Mono.error(new RoleChangedException()));
                    } else {
                        return tokenBlacklistService.removeFromBlacklist(keycloakUserId)
                                .then(chain.filter(exchange));
                    }
                })
                .switchIfEmpty(chain.filter(exchange));
    }
}
