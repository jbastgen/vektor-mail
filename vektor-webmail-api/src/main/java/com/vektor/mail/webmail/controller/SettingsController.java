package com.vektor.mail.webmail.controller;

import com.vektor.mail.core.model.Account;
import com.vektor.mail.core.model.Rule;
import com.vektor.mail.storage.db.repository.AccountRepository;
import com.vektor.mail.storage.db.repository.RuleRepository;
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
@RequestMapping("/api/webmail/settings")
@RequiredArgsConstructor
@Tag(name = "Webmail - Settings")
public class SettingsController {

    private final AccountRepository accountRepository;
    private final RuleRepository ruleRepository;

    @GetMapping("/rules")
    public ResponseEntity<List<Rule>> listRules(@AuthenticationPrincipal UserDetails user) {
        Account account = accountRepository.findByEmail(user.getUsername()).orElse(null);
        if (account == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(ruleRepository.findByAccountIdAndActiveOrderByPriorityAsc(account.getId(), true));
    }

    @PostMapping("/rules")
    public ResponseEntity<Rule> createRule(@RequestBody Rule rule,
                                            @AuthenticationPrincipal UserDetails user) {
        Account account = accountRepository.findByEmail(user.getUsername()).orElse(null);
        if (account == null) return ResponseEntity.status(401).build();
        rule.setAccount(account);
        Rule saved = ruleRepository.save(rule);
        return ResponseEntity.created(URI.create("/api/webmail/settings/rules/" + saved.getId())).body(saved);
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id,
                                            @AuthenticationPrincipal UserDetails user) {
        Account account = accountRepository.findByEmail(user.getUsername()).orElse(null);
        if (account == null) return ResponseEntity.status(401).build();

        boolean deleted = ruleRepository.findById(id)
                .filter(r -> r.getAccount().getId().equals(account.getId()))
                .map(r -> { ruleRepository.deleteById(id); return true; })
                .orElse(false);
        return deleted ? ResponseEntity.noContent().<Void>build() : ResponseEntity.notFound().build();
    }
}
