package com.vektor.mail.storage.db.repository;

import com.vektor.mail.core.model.SyncPeer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SyncPeerRepository extends JpaRepository<SyncPeer, UUID> {
    List<SyncPeer> findByActive(boolean active);
}
