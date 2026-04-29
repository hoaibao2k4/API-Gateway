package com.example.gateway.services;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.example.gateway.dto.response.MyInfoResponse;

import reactor.core.publisher.Mono;

public interface AuthService {
  Mono<MyInfoResponse> getMyInfo(OidcUser principal);
  Mono<Void> logoutAllDevices(OAuth2AuthorizedClient authorizedClient, OidcUser oidcUser);
}
