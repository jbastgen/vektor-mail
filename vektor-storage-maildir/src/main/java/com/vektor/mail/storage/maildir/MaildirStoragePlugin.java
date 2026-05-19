package com.vektor.mail.storage.maildir;

import com.vektor.mail.core.model.Mailbox;
import com.vektor.mail.core.model.Message;
import com.vektor.mail.core.plugin.*;
import org.pf4j.Extension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Maildir storage plugin.
 * Layout: ${vektor.storage.maildir.base}/{accountId}/Maildir/{folder}/{new|cur|tmp}/{filename}
 */
@Extension
@Component
public class MaildirStoragePlugin implements StoragePlugin {

    @Value("${vektor.storage.maildir.base:./data/maildir}")
    private String baseDir;

    @Override
    public String getPluginId() {
        return "vektor-storage-maildir";
    }

    @Override
    public void storeMessage(StorageContext context, Message message, byte[] rawContent) throws StorageException {
        try {
            Path newDir = resolveNewDir(message);
            Files.createDirectories(newDir);

            String filename = message.getId() + "." + System.currentTimeMillis() + ".vektor";
            Path file = newDir.resolve(filename);
            Files.write(file, rawContent, StandardOpenOption.CREATE_NEW);

            message.setStorageRef("maildir:" + file.toAbsolutePath());
        } catch (IOException e) {
            throw new StorageException("Maildir write failed", e);
        }
    }

    @Override
    public byte[] loadMessageContent(Message message) throws StorageException {
        Path file = extractPath(message.getStorageRef());
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new StorageException("Maildir read failed: " + file, e);
        }
    }

    @Override
    public void deleteMessage(String storageRef) throws StorageException {
        Path file = extractPath(storageRef);
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new StorageException("Maildir delete failed: " + file, e);
        }
    }

    @Override
    public List<Message> listMessages(Mailbox mailbox, MessageSearchCriteria criteria) {
        // Metadata listing delegates to the DB; this plugin only manages content
        return List.of();
    }

    @Override
    public long calculateUsedBytes(UUID accountId) {
        Path accountPath = Path.of(baseDir, accountId.toString());
        if (!Files.exists(accountPath)) return 0L;
        try (Stream<Path> walk = Files.walk(accountPath)) {
            return walk.filter(Files::isRegularFile)
                       .mapToLong(p -> p.toFile().length())
                       .sum();
        } catch (IOException e) {
            return 0L;
        }
    }

    private Path resolveNewDir(Message message) {
        String accountId = message.getMailbox().getAccount().getId().toString();
        String folder = sanitize(message.getMailbox().getPath());
        return Path.of(baseDir, accountId, "Maildir", folder, "new");
    }

    private Path extractPath(String ref) throws StorageException {
        if (ref == null || !ref.startsWith("maildir:")) {
            throw new StorageException("Invalid Maildir storage ref: " + ref);
        }
        return Path.of(ref.substring(8));
    }

    private String sanitize(String path) {
        return path.replace('/', '_').replace('\\', '_');
    }
}
