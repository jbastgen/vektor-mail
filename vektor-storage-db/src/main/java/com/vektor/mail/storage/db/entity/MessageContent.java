package com.vektor.mail.storage.db.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Stores the raw (encrypted) message content separate from the metadata
 * so that metadata queries remain fast even with large content blobs.
 */
@Entity
@Table(name = "message_contents")
@Getter
@Setter
public class MessageContent {

    @Id
    @Column(name = "message_id")
    private UUID messageId;

    /** AES-256-GCM encrypted RFC 2822 bytes. */
    @Column(nullable = false, columnDefinition = "BYTEA")
    private byte[] content;
}
