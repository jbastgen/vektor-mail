package com.vektor.mail.core.plugin;

import com.vektor.mail.core.model.Mailbox;
import com.vektor.mail.core.model.Message;
import org.pf4j.ExtensionPoint;

import java.util.List;
import java.util.Optional;

/**
 * Extension point for mail storage backends.
 * Exactly one StoragePlugin is active at any time (selected via config).
 */
public interface StoragePlugin extends ExtensionPoint {

    String getPluginId();

    /**
     * Persist a message. Implementations must populate {@code message.storageRef}.
     *
     * @param context additional context (e.g. encryption keys)
     * @param message the message entity (partially populated; storageRef will be set)
     * @param rawContent raw RFC 2822 message bytes (may be encrypted)
     */
    void storeMessage(StorageContext context, Message message, byte[] rawContent) throws StorageException;

    /**
     * Load the raw content bytes for the given message.
     */
    byte[] loadMessageContent(Message message) throws StorageException;

    /**
     * Permanently delete message content for the given storageRef.
     */
    void deleteMessage(String storageRef) throws StorageException;

    /**
     * Return summaries of messages in the given mailbox, filtered by criteria.
     */
    List<Message> listMessages(Mailbox mailbox, MessageSearchCriteria criteria);

    /**
     * Return the total content bytes used by the given account (for quota checks).
     */
    long calculateUsedBytes(java.util.UUID accountId);
}
