package com.example.gateway.dto.response;

import lombok.Builder;

@Builder
public record MessageResponse(
    String message
) {}
