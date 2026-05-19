package com.vektor.mail.pop3.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vektor.pop3")
public record Pop3Properties(
        int port,
        int pop3sPort,
        boolean enabled
) {}
