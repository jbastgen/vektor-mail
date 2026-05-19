package com.vektor.mail.storage.db.repository;

import com.vektor.mail.storage.db.entity.MessageContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MessageContentRepository extends JpaRepository<MessageContent, UUID> {}
