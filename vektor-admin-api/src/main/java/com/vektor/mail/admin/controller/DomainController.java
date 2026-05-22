package com.vektor.mail.admin.controller;

import com.vektor.mail.core.model.Domain;
import com.vektor.mail.storage.db.repository.DomainRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/domains")
@PreAuthorize("hasRole('ADMIN') or hasRole('DOMAIN_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin - Domains")
public class DomainController {

    private final DomainRepository domainRepository;

    @GetMapping
    @Operation(summary = "List all domains")
    public List<Domain> list() {
        return domainRepository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a domain")
    public ResponseEntity<Domain> create(@RequestBody Domain domain) {
        Domain saved = domainRepository.save(domain);
        return ResponseEntity.created(URI.create("/api/admin/domains/" + saved.getId())).body(saved);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get domain by ID")
    public ResponseEntity<Domain> get(@PathVariable UUID id) {
        return domainRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a domain")
    public ResponseEntity<Domain> update(@PathVariable UUID id, @RequestBody Domain body) {
        return domainRepository.findById(id).map(existing -> {
            existing.setName(body.getName());
            existing.setActive(body.isActive());
            existing.setDkimSelector(body.getDkimSelector());
            existing.setSpfPolicy(body.getSpfPolicy());
            existing.setDmarcPolicy(body.getDmarcPolicy());
            return ResponseEntity.ok(domainRepository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a domain")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!domainRepository.existsById(id)) return ResponseEntity.notFound().build();
        domainRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
