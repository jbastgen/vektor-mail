package com.vektor.mail.core.plugin;

import com.vektor.mail.core.model.Message;
import org.pf4j.ExtensionPoint;

import java.util.List;
import java.util.UUID;

/**
 * Extension point for full-text search indexing.
 */
public interface IndexPlugin extends ExtensionPoint {

    String getPluginId();

    void index(Message message, String plainTextBody);

    List<UUID> search(String query, UUID accountId, int limit);

    void delete(UUID messageId);

    void rebuildIndex(UUID accountId);
}
