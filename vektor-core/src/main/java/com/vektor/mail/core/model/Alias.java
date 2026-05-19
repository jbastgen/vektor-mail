package com.vektor.mail.core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "aliases")
@Getter
@Setter
public class Alias {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "source_address", nullable = false, length = 255)
    private String sourceAddress;

    @Column(name = "target_address", nullable = false, length = 255)
    private String targetAddress;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "domain_id", nullable = false)
    private Domain domain;

    @Column(nullable = false)
    private boolean active = true;
}
