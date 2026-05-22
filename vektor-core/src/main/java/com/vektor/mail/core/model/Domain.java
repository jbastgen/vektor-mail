package com.vektor.mail.core.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.PrePersist;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "domains")
@Getter
@Setter
public class Domain {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(unique = true, nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    /** AES-256-GCM encrypted DKIM private key bytes. */
    @Column(name = "dkim_private_key")
    private byte[] dkimPrivateKey;

    @Column(name = "dkim_selector", length = 64)
    private String dkimSelector;

    @Column(name = "spf_policy", length = 255)
    private String spfPolicy;

    @Column(name = "dmarc_policy", length = 255)
    private String dmarcPolicy;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void initCreatedAt() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
