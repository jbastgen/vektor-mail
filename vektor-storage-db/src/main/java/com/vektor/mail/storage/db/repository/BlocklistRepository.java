package com.vektor.mail.storage.db.repository;

import com.vektor.mail.core.model.BlocklistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface BlocklistRepository extends JpaRepository<BlocklistEntry, UUID> {

    @Query("SELECT e FROM BlocklistEntry e WHERE e.value = :value AND e.type = :type AND (e.expiresAt IS NULL OR e.expiresAt > :now)")
    List<BlocklistEntry> findActiveByValueAndType(String value, BlocklistEntry.Type type, Instant now);

    @Query("SELECT e FROM BlocklistEntry e WHERE (e.expiresAt IS NULL OR e.expiresAt > :now)")
    List<BlocklistEntry> findAllActive(Instant now);
}
