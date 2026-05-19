package com.vektor.mail.core.plugin;

import com.vektor.mail.core.event.MailEvent;
import com.vektor.mail.core.model.SyncPeer;
import org.pf4j.ExtensionPoint;

import java.time.Instant;
import java.util.List;

/**
 * Extension point for multi-instance synchronisation.
 */
public interface SyncPlugin extends ExtensionPoint {

    String getPluginId();

    void publishEvent(MailEvent event);

    void registerPeer(SyncPeer peer);

    void removePeer(SyncPeer peer);

    List<MailEvent> catchUp(SyncPeer peer, Instant since);
}
