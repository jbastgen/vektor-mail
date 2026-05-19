package com.vektor.mail.webmail.controller;

import com.vektor.mail.core.model.Account;
import com.vektor.mail.core.model.Mailbox;
import com.vektor.mail.storage.db.repository.AccountRepository;
import com.vektor.mail.storage.db.repository.MailboxRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/webmail/folders")
@RequiredArgsConstructor
@Tag(name = "Webmail - Folders")
public class FolderController {

    private final AccountRepository accountRepository;
    private final MailboxRepository mailboxRepository;

    @GetMapping
    public ResponseEntity<List<Mailbox>> list(@AuthenticationPrincipal UserDetails user) {
        Account account = accountRepository.findByEmail(user.getUsername()).orElse(null);
        if (account == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(mailboxRepository.findByAccountId(account.getId()));
    }

    @PostMapping
    public ResponseEntity<Mailbox> create(@RequestBody Mailbox body,
                                           @AuthenticationPrincipal UserDetails user) {
        Account account = accountRepository.findByEmail(user.getUsername()).orElse(null);
        if (account == null) return ResponseEntity.status(401).build();

        Mailbox mb = new Mailbox();
        mb.setAccount(account);
        mb.setName(body.getName());
        mb.setPath(body.getPath());
        mb.setUidValidity(System.currentTimeMillis());
        Mailbox saved = mailboxRepository.save(mb);
        return ResponseEntity.created(URI.create("/api/webmail/folders/" + saved.getId())).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                        @AuthenticationPrincipal UserDetails user) {
        Account account = accountRepository.findByEmail(user.getUsername()).orElse(null);
        if (account == null) return ResponseEntity.status(401).build();

        boolean deleted = mailboxRepository.findById(id)
                .filter(mb -> mb.getAccount().getId().equals(account.getId()))
                .map(mb -> { mailboxRepository.deleteById(id); return true; })
                .orElse(false);
        return deleted ? ResponseEntity.noContent().<Void>build() : ResponseEntity.notFound().build();
    }
}
