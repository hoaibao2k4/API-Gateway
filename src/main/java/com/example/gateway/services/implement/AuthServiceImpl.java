package com.example.gateway.services.implement;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import com.example.gateway.dto.response.MyInfoResponse;
import com.example.gateway.services.AuthService;
import com.example.gateway.services.TokenBlacklistService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  @Value("${KEYCLOAK_URL:http://localhost:8033}")
  private String keycloakUrl;

  @Value("${KEYCLOAK_REALM:Ecommerce-Realm}")
  private String realm;

  private final TokenBlacklistService tokenBlacklistService;

    @Override
    public Mono<MyInfoResponse> getMyInfo(OidcUser principal) {
      if (principal == null)
        return Mono.just(MyInfoResponse.builder().authenticated(false).build());

      List<Object> roles = principal.getAttribute("client_roles");

      return Mono.just(MyInfoResponse.builder()
          .username(principal.getPreferredUsername() != null ? principal.getPreferredUsername() : "")
          .email(principal.getEmail() != null ? principal.getEmail() : "")
          .role(roles != null && !roles.isEmpty() ? (String) roles.get(0) : "")
          .authenticated(true)
          .build());
    }

    @Override
    public Mono<Void> logoutAllDevices(
        OAuth2AuthorizedClient authorizedClient, OidcUser oidcUser) {

      String clientId = authorizedClient.getClientRegistration().getClientId();
      String clientSecret = authorizedClient.getClientRegistration().getClientSecret();
      String userId = oidcUser.getSubject(); // Keycloak user ID (sub claim)


      // use token to call Admin API to delete ALL user sessions
      return getServiceAccountToken(clientId, clientSecret)
          .flatMap(adminToken -> deleteAllUserSessions(adminToken, userId))
          .then(tokenBlacklistService.blacklistUser(userId))
          .doOnError(e -> log.error("Failed to logout all devices for user {}: {}", userId, e.getMessage()))
          .onErrorMap(e -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to logout all devices from Keycloak", e));
    }

  // get service account token by client credentials flow
  private Mono<String> getServiceAccountToken(String clientId, String clientSecret) {
    String tokenEndpoint = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";

    return WebClient.create().post()
        .uri(tokenEndpoint)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED) // form data
        .body(BodyInserters
            .fromFormData("grant_type", "client_credentials")
            .with("client_id", clientId)
            .with("client_secret", clientSecret))
        .retrieve()
        .bodyToMono(Map.class) // json to map
        .map(response -> (String) response.get("access_token"));
  }

  /**
   * call admin keycloak to delete all user sessions
   * POST /admin/realms/{realm}/users/{userId}/logout
   */
  private Mono<Void> deleteAllUserSessions(String adminToken, String userId) {
    String adminEndpoint = keycloakUrl + "/admin/realms/" + realm + "/users/" + userId + "/logout";

    return WebClient.create().post()
        .uri(adminEndpoint)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
        .retrieve()
        .toBodilessEntity() // Mono<ResponseEntity<Void>>
        .then(); // Mono<Void>
  }
}