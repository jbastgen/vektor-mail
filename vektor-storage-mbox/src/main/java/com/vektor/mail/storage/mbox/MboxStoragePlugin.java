package com.vektor.mail.storage.mbox;

import com.vektor.mail.core.model.Mailbox;
import com.vektor.mail.core.model.Message;
import com.vektor.mail.core.plugin.*;
import org.pf4j.Extension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Mbox storage plugin.
 * Each mailbox is a single mbox file: ${base}/{accountId}/{mailboxPath}.mbox
 * Appends messages with standard mbox "From " separator lines.
 * The storageRef encodes the file path and byte offset for retrieval.
 */
@Extension
@Component
public class MboxStoragePlugin implements StoragePlugin {

    @Value("${vektor.storage.mbox.base:./data/mbox}")
    private String baseDir;

    @Override
    public String getPluginId() {
        return "vektor-storage-mbox";
    }

    @Override
    public synchronized void storeMessage(StorageContext context, Message message, byte[] rawContent)
            throws StorageException {
        Path mboxFile = resolveMboxFile(message.getMailbox());
        try {
            Files.createDirectories(mboxFile.getParent());
            long offset = Files.exists(mboxFile) ? Files.size(mboxFile) : 0L;

            try (OutputStream out = Files.newOutputStream(mboxFile,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                String separator = "From vektor@localhost " + Instant.now() + "\n";
                out.write(separator.getBytes());
                out.write(rawContent);
                out.write("\n".getBytes());
            }
            message.setStorageRef("mbox:" + mboxFile.toAbsolutePath() + "@" + offset);
        } catch (IOException e) {
            throw new StorageException("Mbox write failed", e);
        }
    }

    @Override
    public byte[] loadMessageContent(Message message) throws StorageException {
        String ref = message.getStorageRef();
        if (ref == null || !ref.startsWith("mbox:")) {
            throw new StorageException("Invalid mbox ref: " + ref);
        }
        String[] parts = ref.substring(5).split("@");
        Path file = Path.of(parts[0]);
        long offset = Long.parseLong(parts[1]);
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            raf.seek(offset);
            // Skip the "From " separator line
            raf.readLine();
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] line;
            String l;
            while ((l = raf.readLine()) != null && !l.startsWith("From ")) {
                buf.write(l.getBytes());
                buf.write('\n');
            }
            return buf.toByteArray();
        } catch (IOException e) {
            throw new StorageException("Mbox read failed: " + file, e);
        }
    }

    @Override
    public void deleteMessage(String storageRef) {
        // Mbox does not support partial deletion without rewriting.
        // Mark as deleted; compaction must be triggered separately.
    }

    @Override
    public List<Message> listMessages(Mailbox mailbox, MessageSearchCriteria criteria) {
        return List.of();
    }

    @Override
    public long calculateUsedBytes(UUID accountId) {
        Path accountPath = Path.of(baseDir, accountId.toString());
        if (!Files.exists(accountPath)) return 0L;
        try {
            return Files.walk(accountPath)
                        .filter(Files::isRegularFile)
                        .mapToLong(p -> p.toFile().length())
                        .sum();
        } catch (IOException e) {
            return 0L;
        }
    }

    private Path resolveMboxFile(Mailbox mailbox) {
        String accountId = mailbox.getAccount().getId().toString();
        String name = mailbox.getPath().replace('/', '_').replace('\\', '_');
        return Path.of(baseDir, accountId, name + ".mbox");
    }
}
