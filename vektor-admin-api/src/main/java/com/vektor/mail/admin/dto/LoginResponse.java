package com.vektor.mail.admin.dto;

import java.util.Set;

public record LoginResponse(String accessToken, String email, Set<String> roles) {}
