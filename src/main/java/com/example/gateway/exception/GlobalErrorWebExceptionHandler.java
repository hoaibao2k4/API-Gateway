package com.example.gateway.exception;

import java.net.ConnectException;
import java.time.LocalDateTime;

import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import com.example.gateway.dto.response.ErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GlobalErrorWebExceptionHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // Skip if response already committed (e.g. Spring Session post-processing)
        if (exchange.getResponse().isCommitted()) {
            return Mono.empty();
        }

        log.error("Gateway Error: {}", ex.getMessage());

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String error = "INTERNAL_SERVER_ERROR";
        String message = ex.getMessage();

        // classify error
        if (ex instanceof RoleChangedException) {
            status = HttpStatus.UNAUTHORIZED;
            error = "UNAUTHORIZED";
            message = "Role changed. Please re-login.";
            exchange.getResponse().getHeaders().add("X-Reason", "ROLE_CHANGED");
        } else if (ex instanceof ResponseStatusException rsEx) {
            status = (HttpStatus) rsEx.getStatusCode();
            error = status.name();
            message = rsEx.getReason();
        } else if (ex instanceof ConnectException
                || (ex.getMessage() != null && ex.getMessage().contains("Connection refused"))) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            error = "SERVICE_UNAVAILABLE";
            message = "The downstream service is currently unreachable.";
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(status.value())
                .timestamp(LocalDateTime.now())
                .path(exchange.getRequest().getURI().getPath())
                .error(error)
                .message(message)
                .build();

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error writing JSON response", e);
            return Mono.error(e);
        }
    }
}
