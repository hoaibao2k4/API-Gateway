package com.example.gateway.services.implement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.gateway.services.AuthService;
import com.example.gateway.services.TokenBlacklistService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  @Value("${KEYCLOAK_URL:http://localhost:8033}")
  private String keycloakUrl;

  @Value("${KEYCLOAK_REALM:Ecommerce-Realm}")
  private String realm;

  private final TokenBlacklistService tokenBlacklistService;

    @Override
    public Mono<Map<String, Object>> getMyInfo(OidcUser principle) {
      if (principle == null)
        return Mono.just(Map.of("authenticated", false));

      List<Object> roles = principle.getAttribute("client_roles");

      Map<String, Object> claims = new HashMap<>();

      claims.put("username", principle.getPreferredUsername() != null ? principle.getPreferredUsername() : "");
      claims.put("email", principle.getEmail() != null ? principle.getEmail() : "");
      claims.put("role", roles != null ? roles.get(0) : "");

      return Mono.just(claims);
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
          .onErrorResume(e -> Mono.empty());
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