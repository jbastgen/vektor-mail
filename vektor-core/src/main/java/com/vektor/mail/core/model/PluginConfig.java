package com.vektor.mail.core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plugin_configs")
@Getter
@Setter
public class PluginConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "plugin_id", nullable = false, unique = true, length = 255)
    private String pluginId;

    /** JSON configuration blob. */
    @Column(columnDefinition = "TEXT")
    private String config;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
