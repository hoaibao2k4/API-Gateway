package com.example.gateway.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;

import com.example.gateway.services.AuthService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;

  @GetMapping("/me")
  public Mono<Map<String, Object>> getUserClaims(@AuthenticationPrincipal OidcUser oidcUser) {
    return authService.getMyInfo(oidcUser);
  }

  @PostMapping("/logout-all")
  public Mono<ResponseEntity<Map<String, String>>> logoutAllDevices(
      @AuthenticationPrincipal OidcUser oidcUser,
      @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient authorizedClient,
      ServerWebExchange exchange) {

    return authService.logoutAllDevices(authorizedClient, oidcUser)
        .then(exchange.getSession().flatMap(WebSession::invalidate))
        .then(Mono.just(ResponseEntity.ok(Map.of("message", "Logged out from all devices"))));
  }

}
