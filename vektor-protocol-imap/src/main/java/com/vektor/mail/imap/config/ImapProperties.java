package com.vektor.mail.imap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vektor.imap")
public record ImapProperties(int port, int imapsPort) {
    public ImapProperties {
        if (port == 0) port = 143;
        if (imapsPort == 0) imapsPort = 993;
    }
}
