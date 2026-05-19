package com.vektor.mail.webmail.controller;

import com.vektor.mail.core.model.Account;
import com.vektor.mail.core.plugin.MailContext;
import com.vektor.mail.storage.db.repository.AccountRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/webmail/compose")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webmail - Compose")
public class ComposeController {

    private final AccountRepository accountRepository;
    private final ProducerTemplate camelProducer;

    @PostMapping
    public ResponseEntity<Void> send(@RequestBody SendRequest request,
                                     @AuthenticationPrincipal UserDetails user) {
        Account account = accountRepository.findByEmail(user.getUsername()).orElse(null);
        if (account == null) return ResponseEntity.status(401).build();

        MailContext ctx = new MailContext();
        ctx.setEnvelopeFrom(account.getEmail());
        ctx.setEnvelopeTo(request.to() != null ? request.to() : List.of());
        ctx.setRawMessage(buildRawMessage(account.getEmail(), request));

        try {
            camelProducer.sendBody("direct:mail.outgoing", ctx);
        } catch (Exception e) {
            log.error("Failed to send message for {}", account.getEmail(), e);
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.accepted().build();
    }

    private byte[] buildRawMessage(String from, SendRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("From: ").append(from).append("\r\n");
        if (req.to() != null) {
            sb.append("To: ").append(String.join(", ", req.to())).append("\r\n");
        }
        if (req.cc() != null && !req.cc().isEmpty()) {
            sb.append("Cc: ").append(String.join(", ", req.cc())).append("\r\n");
        }
        sb.append("Subject: ").append(req.subject() != null ? req.subject() : "").append("\r\n");
        sb.append("Content-Type: text/plain; charset=UTF-8\r\n");
        sb.append("\r\n");
        sb.append(req.body() != null ? req.body() : "");
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public record SendRequest(
            List<String> to,
            List<String> cc,
            String subject,
            String body
    ) {}
}
