package com.example.gateway.services;

import java.util.Map;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import reactor.core.publisher.Mono;

public interface AuthService {
  Mono<Map<String, Object>> getMyInfo(OidcUser principle);
  Mono<Void> logoutAllDevices(OAuth2AuthorizedClient authorizedClient, OidcUser oidcUser);
}
