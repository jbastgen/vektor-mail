package com.vektor.mail.storage.db.repository;

import com.vektor.mail.core.model.PluginConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PluginConfigRepository extends JpaRepository<PluginConfig, UUID> {
    Optional<PluginConfig> findByPluginId(String pluginId);
}
