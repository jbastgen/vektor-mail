package com.vektor.mail.storage.db.repository;

import com.vektor.mail.core.model.GreylistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GreylistRepository extends JpaRepository<GreylistEntry, UUID> {

    Optional<GreylistEntry> findBySenderIpAndSenderDomainAndRecipientAddr(
            String senderIp, String senderDomain, String recipientAddr);

    @Modifying
    @Query("DELETE FROM GreylistEntry e WHERE e.passedAt < :before")
    int deleteOldPassed(Instant before);
}
