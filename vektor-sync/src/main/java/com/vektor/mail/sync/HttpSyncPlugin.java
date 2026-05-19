package com.vektor.mail.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vektor.mail.core.event.MailEvent;
import com.vektor.mail.core.event.MailStoredSpringEvent;
import com.vektor.mail.core.model.SyncPeer;
import com.vektor.mail.core.plugin.SyncPlugin;
import com.vektor.mail.storage.db.repository.SyncPeerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;

/**
 * Pushes MailEvents to all active sync peers via HTTPS POST.
 * Listens to Spring MailStoredSpringEvent and converts it to a MailEvent.
 */
@Extension
@Component
@RequiredArgsConstructor
@Slf4j
public class HttpSyncPlugin implements SyncPlugin {

    private final SyncPeerRepository syncPeerRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getPluginId() { return "vektor-sync-http"; }

    @Async
    @EventListener
    public void onMailStored(MailStoredSpringEvent event) {
        MailEvent mailEvent = new MailEvent.MessageStoredEvent(
                event.getMessage().getId(),
                event.getMessage().getMailbox().getId(),
                event.getMessage().getMailbox().getAccount().getId(),
                Instant.now(),
                java.util.UUID.randomUUID().toString() // node ID — should come from config
        );
        publishEvent(mailEvent);
    }

    @Override
    public void publishEvent(MailEvent event) {
        List<SyncPeer> peers = syncPeerRepository.findByActive(true);
        for (SyncPeer peer : peers) {
            try {
                String json = objectMapper.writeValueAsString(event);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                if (peer.getAuthToken() != null) {
                    headers.setBearerAuth(peer.getAuthToken());
                }
                restTemplate.exchange(
                        peer.getUrl() + "/api/sync/events",
                        HttpMethod.POST,
                        new HttpEntity<>(json, headers),
                        Void.class
                );
                log.debug("Sync event sent to peer {}", peer.getName());
            } catch (Exception e) {
                log.warn("Failed to sync event to peer {}: {}", peer.getName(), e.getMessage());
            }
        }
    }

    @Override
    public void registerPeer(SyncPeer peer) {
        syncPeerRepository.save(peer);
    }

    @Override
    public void removePeer(SyncPeer peer) {
        syncPeerRepository.deleteById(peer.getId());
    }

    @Override
    public List<MailEvent> catchUp(SyncPeer peer, Instant since) {
        // Simplified: retrieve missed events from peer
        return List.of();
    }
}
