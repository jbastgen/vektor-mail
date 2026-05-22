package com.vektor.mail.core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "greylist_entries")
@Getter
@Setter
public class GreylistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sender_ip", length = 45)
    private String senderIp;

    @Column(name = "sender_domain", length = 255)
    private String senderDomain;

    @Column(name = "recipient_addr", length = 255)
    private String recipientAddr;

    @Column(name = "first_seen", nullable = false)
    private Instant firstSeen;

    /** Null until the retry passes the wait window. */
    @Column(name = "passed_at")
    private Instant passedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;
}
