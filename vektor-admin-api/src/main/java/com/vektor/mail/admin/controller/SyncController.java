package com.vektor.mail.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vektor.mail.core.event.MailEvent;
import com.vektor.mail.core.model.SyncPeer;
import com.vektor.mail.storage.db.repository.SyncPeerRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sync - Inter-node")
public class SyncController {

    private final SyncPeerRepository syncPeerRepository;
    private final ObjectMapper objectMapper;

    /** Receive a sync event from a peer node. */
    @PostMapping("/events")
    public ResponseEntity<Void> receiveEvent(@RequestBody String eventJson,
                                              @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            MailEvent event = objectMapper.readValue(eventJson, MailEvent.class);
            log.info("Received sync event: {}", event.getClass().getSimpleName());
            // Future: apply event to local state (idempotent replay)
        } catch (Exception e) {
            log.warn("Invalid sync event received: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }

    /** List all sync peers. */
    @GetMapping("/peers")
    public List<SyncPeer> listPeers() {
        return syncPeerRepository.findAll();
    }

    /** Add a sync peer. */
    @PostMapping("/peers")
    public ResponseEntity<SyncPeer> addPeer(@RequestBody SyncPeer peer) {
        return ResponseEntity.ok(syncPeerRepository.save(peer));
    }

    /** Remove a sync peer. */
    @DeleteMapping("/peers/{id}")
    public ResponseEntity<Void> removePeer(@PathVariable UUID id) {
        syncPeerRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
