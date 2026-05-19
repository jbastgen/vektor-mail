package com.vektor.mail.core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Getter
@Setter
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** RFC 5322 Message-ID header value. */
    @Column(name = "message_id", length = 512)
    private String messageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mailbox_id", nullable = false)
    private Mailbox mailbox;

    @Column(length = 1024)
    private String subject;

    @Column(name = "from_address", length = 512)
    private String fromAddress;

    /** JSON array of recipient addresses. */
    @Column(name = "to_addresses", columnDefinition = "TEXT")
    private String toAddresses;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "size_bytes")
    private long sizeBytes;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "message_flags", joinColumns = @JoinColumn(name = "message_id_fk"))
    @Column(name = "flag")
    private Set<String> flags;

    /**
     * Backend-specific reference to the stored content.
     * For DB storage: the DB row id. For Maildir: the file path. For Mbox: byte offset.
     */
    @Column(name = "storage_ref", length = 1024)
    private String storageRef;

    /** AES-256-GCM encrypted Data Encryption Key, wrapped with master key. */
    @Column(name = "encrypted_dek")
    private byte[] encryptedDek;

    @Column(name = "spam_score", precision = 5, scale = 2)
    private BigDecimal spamScore;

    @Column(name = "dkim_result", length = 32)
    private String dkimResult;

    @Column(name = "spf_result", length = 32)
    private String spfResult;

    @Column(name = "uid")
    private long uid;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attachment> attachments;
}
