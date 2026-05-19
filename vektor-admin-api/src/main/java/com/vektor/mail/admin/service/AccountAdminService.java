package com.vektor.mail.admin.service;

import com.vektor.mail.admin.dto.CreateAccountRequest;
import com.vektor.mail.core.model.Account;
import com.vektor.mail.core.model.Domain;
import com.vektor.mail.core.model.Mailbox;
import com.vektor.mail.security.crypto.PasswordEncoder;
import com.vektor.mail.storage.db.repository.AccountRepository;
import com.vektor.mail.storage.db.repository.DomainRepository;
import com.vektor.mail.storage.db.repository.MailboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountAdminService {

    private final AccountRepository accountRepository;
    private final DomainRepository domainRepository;
    private final MailboxRepository mailboxRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    public Optional<Account> findById(UUID id) {
        return accountRepository.findById(id);
    }

    @Transactional
    public Account create(CreateAccountRequest req) {
        Domain domain = domainRepository.findById(req.domainId())
                .orElseThrow(() -> new IllegalArgumentException("Domain not found: " + req.domainId()));

        Account account = new Account();
        account.setEmail(req.email());
        account.setPasswordHash(passwordEncoder.hash(req.password()));
        account.setDomain(domain);
        account.setRoles(req.roles() != null ? req.roles() : Set.of("ROLE_USER"));
        account.setQuotaBytes(req.quotaBytes());
        account = accountRepository.save(account);

        // Create default mailboxes
        for (String box : List.of("INBOX", "Sent", "Drafts", "Trash", "Junk")) {
            Mailbox mb = new Mailbox();
            mb.setAccount(account);
            mb.setName(box);
            mb.setPath(box);
            mb.setUidValidity(System.currentTimeMillis());
            mailboxRepository.save(mb);
        }

        return account;
    }

    @Transactional
    public void delete(UUID id) {
        accountRepository.deleteById(id);
    }
}
