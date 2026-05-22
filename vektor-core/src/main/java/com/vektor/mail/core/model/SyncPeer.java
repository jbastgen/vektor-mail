package com.vektor.mail.core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sync_peers")
@Getter
@Setter
public class SyncPeer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 1024)
    private String url;

    /** Encrypted bearer token for authenticating to peer. */
    @Column(name = "auth_token", length = 512)
    private String authToken;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;
}
