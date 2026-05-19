package com.vektor.mail.storage.db.repository;

import com.vektor.mail.core.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByMailboxId(UUID mailboxId);
    Page<Message> findByMailboxId(UUID mailboxId, Pageable pageable);
    Optional<Message> findByMessageId(String messageId);
    void deleteByMailboxId(UUID mailboxId);

    @Query("SELECT COALESCE(SUM(m.sizeBytes),0) FROM Message m WHERE m.mailbox.account.id = :accountId")
    long sumSizeByAccountId(UUID accountId);

    @Query("SELECT MAX(m.uid) FROM Message m WHERE m.mailbox.id = :mailboxId")
    Optional<Long> findMaxUidByMailboxId(UUID mailboxId);
}
