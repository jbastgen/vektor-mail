package com.vektor.mail.core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.PrePersist;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "blocklist_entries")
@Getter
@Setter
public class BlocklistEntry {

    public enum Type { IP, DOMAIN, CIDR }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Type type;

    @Column(nullable = false, length = 255)
    private String value;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by")
    private Account addedBy;

    @Column(name = "added_at", updatable = false)
    private Instant addedAt;

    @PrePersist
    void initAddedAt() {
        if (addedAt == null) addedAt = Instant.now();
    }

    /** Null = permanent. */
    @Column(name = "expires_at")
    private Instant expiresAt;
}
