package com.vektor.mail.core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "attachments")
@Getter
@Setter
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(length = 512)
    private String filename;

    @Column(name = "mime_type", length = 256)
    private String mimeType;

    @Column(name = "size_bytes")
    private long sizeBytes;

    @Column(name = "storage_ref", length = 1024)
    private String storageRef;

    @Column(name = "encrypted_dek")
    private byte[] encryptedDek;
}
