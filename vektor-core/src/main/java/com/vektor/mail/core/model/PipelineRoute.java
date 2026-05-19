package com.vektor.mail.core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pipeline_routes")
@Getter
@Setter
public class PipelineRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    /** YAML Camel route definition. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String definition;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
