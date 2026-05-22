package com.vektor.mail.smtp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vektor.smtp")
public record SmtpProperties(
        int port,
        int submissionPort,
        int smtpsPort,
        String hostname,
        boolean tlsEnabled,
        int maxMessageSizeBytes
) {
    public SmtpProperties {
        if (port == 0) port = 25;
        if (submissionPort == 0) submissionPort = 587;
        if (smtpsPort == 0) smtpsPort = 465;
        if (hostname == null || hostname.isBlank()) hostname = "localhost";
        if (maxMessageSizeBytes == 0) maxMessageSizeBytes = 25 * 1024 * 1024; // 25 MB
    }
}
