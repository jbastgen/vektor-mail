package com.vektor.mail.core.plugin;

/** Carries runtime context for storage operations (e.g. decrypted DEK). */
public record StorageContext(byte[] dataEncryptionKey) {}
