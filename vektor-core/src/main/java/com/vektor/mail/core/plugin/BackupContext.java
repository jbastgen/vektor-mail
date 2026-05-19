package com.vektor.mail.core.plugin;

import java.nio.file.Path;
import java.time.Instant;

public record BackupContext(
        Path sourceDirectory,
        String databaseUrl,
        byte[] encryptionKey,
        Instant scheduledAt,
        String targetPath
) {}
