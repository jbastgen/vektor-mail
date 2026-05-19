package com.vektor.mail.core.plugin;

import java.nio.file.Path;

public record RestoreContext(
        String backupRef,
        Path targetDirectory,
        byte[] encryptionKey
) {}
