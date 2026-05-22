package com.vektor.mail.core.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "mailboxes",
       uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "path"}))
@Getter
@Setter
public class Mailbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    /** Display name (e.g. "INBOX", "Sent", "Drafts"). */
    @NotBlank
    @Column(nullable = false, length = 255)
    private String name;

    /** Full IMAP hierarchical path, e.g. "INBOX.Sent". */
    @NotBlank
    @Column(nullable = false, length = 1024)
    private String path;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "mailbox_flags", joinColumns = @JoinColumn(name = "mailbox_id"))
    @Column(name = "flag")
    private Set<String> flags;

    @Column(name = "uid_validity", nullable = false)
    private long uidValidity;

    @Column(name = "uid_next", nullable = false)
    private long uidNext = 1L;
}
