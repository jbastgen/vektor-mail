package com.vektor.mail.storage.db.service;

import com.vektor.mail.core.model.Mailbox;
import com.vektor.mail.core.model.Message;
import com.vektor.mail.core.plugin.*;
import com.vektor.mail.storage.db.entity.MessageContent;
import com.vektor.mail.storage.db.repository.MessageContentRepository;
import com.vektor.mail.storage.db.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.pf4j.Extension;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Stores message content directly in PostgreSQL as encrypted BYTEA.
 */
@Extension
@Component
@RequiredArgsConstructor
public class DatabaseStoragePlugin implements StoragePlugin {

    private final MessageRepository messageRepository;
    private final MessageContentRepository contentRepository;

    @Override
    public String getPluginId() {
        return "vektor-storage-db";
    }

    @Override
    @Transactional
    public void storeMessage(StorageContext context, Message message, byte[] rawContent) throws StorageException {
        try {
            Message saved = messageRepository.save(message);

            MessageContent mc = new MessageContent();
            mc.setMessageId(saved.getId());
            mc.setContent(rawContent);
            contentRepository.save(mc);

            saved.setStorageRef("db:" + saved.getId());
            messageRepository.save(saved);
        } catch (Exception e) {
            throw new StorageException("Failed to store message in database", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] loadMessageContent(Message message) throws StorageException {
        UUID id = extractId(message.getStorageRef());
        return contentRepository.findById(id)
                .map(MessageContent::getContent)
                .orElseThrow(() -> new StorageException("Content not found for message " + message.getId()));
    }

    @Override
    @Transactional
    public void deleteMessage(String storageRef) throws StorageException {
        UUID id = extractId(storageRef);
        contentRepository.deleteById(id);
        messageRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> listMessages(Mailbox mailbox, MessageSearchCriteria criteria) {
        return messageRepository.findByMailboxId(
                mailbox.getId(),
                PageRequest.of(criteria.offset() / Math.max(criteria.limit(), 1), Math.max(criteria.limit(), 1))
        ).getContent();
    }

    @Override
    public long calculateUsedBytes(UUID accountId) {
        return messageRepository.sumSizeByAccountId(accountId);
    }

    private UUID extractId(String storageRef) throws StorageException {
        if (storageRef == null || !storageRef.startsWith("db:")) {
            throw new StorageException("Invalid DB storage ref: " + storageRef);
        }
        try {
            return UUID.fromString(storageRef.substring(3));
        } catch (IllegalArgumentException e) {
            throw new StorageException("Malformed DB storage ref: " + storageRef, e);
        }
    }
}
