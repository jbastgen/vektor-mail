package com.vektor.mail.storage.db.repository;

import com.vektor.mail.core.model.BackupJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface BackupJobRepository extends JpaRepository<BackupJob, UUID> {
    List<BackupJob> findByEnabledAndNextRunAtBefore(boolean enabled, Instant before);
}
