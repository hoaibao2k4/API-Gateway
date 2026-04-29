package com.example.gateway.dto.response;

import lombok.Builder;

@Builder
public record MyInfoResponse(
    String username,
    String email,
    String role,
    boolean authenticated
) {}
