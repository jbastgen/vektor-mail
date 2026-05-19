package com.vektor.mail.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;
import java.util.UUID;

public record CreateAccountRequest(
        @Email @NotBlank String email,
        @NotBlank String password,
        UUID domainId,
        Set<String> roles,
        long quotaBytes
) {}
