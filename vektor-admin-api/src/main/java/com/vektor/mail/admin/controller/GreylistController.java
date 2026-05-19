package com.vektor.mail.admin.controller;

import com.vektor.mail.core.model.GreylistEntry;
import com.vektor.mail.storage.db.repository.GreylistRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/greylist")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin - Greylist")
public class GreylistController {

    private final GreylistRepository greylistRepository;

    @GetMapping
    public Page<GreylistEntry> list(Pageable pageable) {
        return greylistRepository.findAll(pageable);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        greylistRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> flush() {
        greylistRepository.deleteAll();
        return ResponseEntity.noContent().build();
    }
}
