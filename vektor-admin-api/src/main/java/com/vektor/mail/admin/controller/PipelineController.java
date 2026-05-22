package com.vektor.mail.admin.controller;

import com.vektor.mail.core.model.PipelineRoute;
import com.vektor.mail.pipeline.service.PipelineRouteLoader;
import com.vektor.mail.storage.db.repository.PipelineRouteRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/pipeline")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin - Pipeline")
public class PipelineController {

    private final PipelineRouteRepository routeRepository;
    private final PipelineRouteLoader routeLoader;

    @GetMapping
    public List<PipelineRoute> list() {
        return routeRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<PipelineRoute> create(@RequestBody PipelineRoute route) {
        PipelineRoute saved = routeRepository.save(route);
        routeLoader.loadRoute(saved);
        return ResponseEntity.created(URI.create("/api/admin/pipeline/" + saved.getId())).body(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PipelineRoute> get(@PathVariable UUID id) {
        return routeRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<PipelineRoute> update(@PathVariable UUID id, @RequestBody PipelineRoute body) {
        return routeRepository.findById(id).map(existing -> {
            existing.setName(body.getName());
            existing.setDefinition(body.getDefinition());
            existing.setEnabled(body.isEnabled());
            PipelineRoute saved = routeRepository.save(existing);
            routeLoader.reloadRoute(saved);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        routeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
