package com.vektor.mail.core.plugin;

import org.pf4j.ExtensionPoint;

/**
 * Extension point for backup strategies (local, S3, SFTP, …).
 */
public interface BackupPlugin extends ExtensionPoint {

    String getPluginId();

    void backup(BackupContext ctx) throws BackupException;

    void restore(RestoreContext ctx) throws BackupException;

    /** Human-readable status of the last backup. */
    String getStatus();
}
