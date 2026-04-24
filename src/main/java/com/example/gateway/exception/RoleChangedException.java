package com.example.gateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class RoleChangedException extends ResponseStatusException {
    public RoleChangedException() {
        super(HttpStatus.UNAUTHORIZED, "Role changed. Please re-login.");
    }
}
