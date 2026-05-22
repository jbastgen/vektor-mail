package com.vektor.mail.admin.controller;

import com.vektor.mail.core.model.BackupJob;
import com.vektor.mail.storage.db.repository.BackupJobRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/backup")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin - Backup")
public class BackupController {

    private final BackupJobRepository backupJobRepository;

    @GetMapping
    public List<BackupJob> list() {
        return backupJobRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<BackupJob> create(@RequestBody BackupJob job) {
        BackupJob saved = backupJobRepository.save(job);
        return ResponseEntity.created(URI.create("/api/admin/backup/" + saved.getId())).body(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BackupJob> get(@PathVariable UUID id) {
        return backupJobRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<BackupJob> update(@PathVariable UUID id, @RequestBody BackupJob body) {
        return backupJobRepository.findById(id).map(existing -> {
            existing.setSchedule(body.getSchedule());
            existing.setEnabled(body.isEnabled());
            return ResponseEntity.ok(backupJobRepository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        backupJobRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
