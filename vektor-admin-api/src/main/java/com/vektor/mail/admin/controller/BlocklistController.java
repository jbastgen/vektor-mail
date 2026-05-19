package com.vektor.mail.admin.controller;

import com.vektor.mail.core.model.BlocklistEntry;
import com.vektor.mail.storage.db.repository.BlocklistRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/blocklist")
@PreAuthorize("hasRole('ADMIN') or hasRole('DOMAIN_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin - Blocklist")
public class BlocklistController {

    private final BlocklistRepository blocklistRepository;

    @GetMapping
    public List<BlocklistEntry> list() {
        return blocklistRepository.findAllActive(Instant.now());
    }

    @PostMapping
    public ResponseEntity<BlocklistEntry> create(@RequestBody BlocklistEntry entry) {
        BlocklistEntry saved = blocklistRepository.save(entry);
        return ResponseEntity.created(URI.create("/api/admin/blocklist/" + saved.getId())).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        blocklistRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
