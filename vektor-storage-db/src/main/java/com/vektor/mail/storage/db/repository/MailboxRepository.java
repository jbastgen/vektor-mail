package com.vektor.mail.storage.db.repository;

import com.vektor.mail.core.model.Mailbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MailboxRepository extends JpaRepository<Mailbox, UUID> {
    List<Mailbox> findByAccountId(UUID accountId);
    Optional<Mailbox> findByAccountIdAndPath(UUID accountId, String path);
}
