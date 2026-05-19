package com.vektor.mail.webmail.controller;

import com.vektor.mail.core.model.Account;
import com.vektor.mail.core.model.Message;
import com.vektor.mail.storage.db.repository.AccountRepository;
import com.vektor.mail.storage.db.repository.MailboxRepository;
import com.vektor.mail.storage.db.repository.MessageRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/webmail/messages")
@RequiredArgsConstructor
@Tag(name = "Webmail - Messages")
public class MessageController {

    private final AccountRepository accountRepository;
    private final MailboxRepository mailboxRepository;
    private final MessageRepository messageRepository;

    @GetMapping
    public ResponseEntity<Page<Message>> list(
            @RequestParam UUID mailboxId,
            Pageable pageable,
            @AuthenticationPrincipal UserDetails user) {

        Account account = accountRepository.findByEmail(user.getUsername()).orElse(null);
        if (account == null) return ResponseEntity.status(401).build();

        // Verify ownership
        boolean owned = mailboxRepository.findById(mailboxId)
                .map(mb -> mb.getAccount().getId().equals(account.getId()))
                .orElse(false);
        if (!owned) return ResponseEntity.status(403).build();

        return ResponseEntity.ok(messageRepository.findByMailboxId(mailboxId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Message> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {

        Account account = accountRepository.findByEmail(user.getUsername()).orElse(null);
        if (account == null) return ResponseEntity.status(401).build();

        return messageRepository.findById(id)
                .filter(msg -> msg.getMailbox().getAccount().getId().equals(account.getId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {

        Account account = accountRepository.findByEmail(user.getUsername()).orElse(null);
        if (account == null) return ResponseEntity.status(401).build();

        boolean deleted = messageRepository.findById(id)
                .filter(msg -> msg.getMailbox().getAccount().getId().equals(account.getId()))
                .map(msg -> { messageRepository.deleteById(id); return true; })
                .orElse(false);
        return deleted ? ResponseEntity.noContent().<Void>build() : ResponseEntity.notFound().build();
    }
}
