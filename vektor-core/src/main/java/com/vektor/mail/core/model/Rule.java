package com.vektor.mail.core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "rules")
@Getter
@Setter
public class Rule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false)
    private int priority = 100;

    @Column(length = 255)
    private String name;

    /** JSON: {"field":"from","op":"contains","value":"@example.com"} */
    @Column(columnDefinition = "TEXT")
    private String condition;

    /** JSON: {"type":"move","target":"Junk"} */
    @Column(columnDefinition = "TEXT")
    private String action;

    @Column(nullable = false)
    private boolean active = true;
}
