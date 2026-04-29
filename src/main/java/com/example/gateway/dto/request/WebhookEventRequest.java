package com.example.gateway.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEventRequest {
    
    @NotBlank(message = "Event type is required")
    private String type;

    @NotBlank(message = "Resource type is required")
    private String resourceType;

    @NotBlank(message = "Resource path is required")
    private String resourcePath;
}
