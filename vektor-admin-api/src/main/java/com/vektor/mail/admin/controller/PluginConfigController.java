package com.vektor.mail.admin.controller;

import com.vektor.mail.core.model.PluginConfig;
import com.vektor.mail.storage.db.repository.PluginConfigRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/plugins")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin - Plugins")
public class PluginConfigController {

    private final PluginConfigRepository pluginConfigRepository;

    @GetMapping
    public List<PluginConfig> list() {
        return pluginConfigRepository.findAll();
    }

    @GetMapping("/{pluginId}")
    public ResponseEntity<PluginConfig> get(@PathVariable String pluginId) {
        return pluginConfigRepository.findByPluginId(pluginId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{pluginId}")
    public ResponseEntity<PluginConfig> upsert(@PathVariable String pluginId, @RequestBody PluginConfig body) {
        PluginConfig config = pluginConfigRepository.findByPluginId(pluginId)
                .orElseGet(() -> { PluginConfig c = new PluginConfig(); c.setPluginId(pluginId); return c; });
        config.setConfig(body.getConfig());
        return ResponseEntity.ok(pluginConfigRepository.save(config));
    }

    @DeleteMapping("/{pluginId}")
    public ResponseEntity<Void> delete(@PathVariable String pluginId) {
        pluginConfigRepository.findByPluginId(pluginId).ifPresent(pluginConfigRepository::delete);
        return ResponseEntity.noContent().build();
    }
}
