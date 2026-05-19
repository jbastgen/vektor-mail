package com.vektor.mail.admin.controller;

import com.vektor.mail.core.model.Alias;
import com.vektor.mail.storage.db.repository.AliasRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/aliases")
@PreAuthorize("hasRole('ADMIN') or hasRole('DOMAIN_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin - Aliases")
public class AliasController {

    private final AliasRepository aliasRepository;

    @GetMapping
    public List<Alias> list(@RequestParam(required = false) UUID domainId) {
        return domainId != null
                ? aliasRepository.findByDomainId(domainId)
                : aliasRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Alias> create(@RequestBody Alias alias) {
        Alias saved = aliasRepository.save(alias);
        return ResponseEntity.created(URI.create("/api/admin/aliases/" + saved.getId())).body(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Alias> get(@PathVariable UUID id) {
        return aliasRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Alias> update(@PathVariable UUID id, @RequestBody Alias body) {
        return aliasRepository.findById(id).map(existing -> {
            existing.setSourceAddress(body.getSourceAddress());
            existing.setTargetAddress(body.getTargetAddress());
            existing.setActive(body.isActive());
            return ResponseEntity.ok(aliasRepository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        aliasRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
