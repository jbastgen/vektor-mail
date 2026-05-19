package com.vektor.mail.admin.controller;

import com.vektor.mail.admin.dto.CreateAccountRequest;
import com.vektor.mail.admin.service.AccountAdminService;
import com.vektor.mail.core.model.Account;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/accounts")
@PreAuthorize("hasRole('ADMIN') or hasRole('DOMAIN_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin - Accounts")
public class AccountController {

    private final AccountAdminService accountAdminService;

    @GetMapping
    @Operation(summary = "List all accounts")
    public List<Account> list() {
        return accountAdminService.findAll();
    }

    @PostMapping
    @Operation(summary = "Create an account")
    public ResponseEntity<Account> create(@RequestBody CreateAccountRequest req) {
        Account account = accountAdminService.create(req);
        return ResponseEntity.created(URI.create("/api/admin/accounts/" + account.getId())).body(account);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> get(@PathVariable UUID id) {
        return accountAdminService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        accountAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
