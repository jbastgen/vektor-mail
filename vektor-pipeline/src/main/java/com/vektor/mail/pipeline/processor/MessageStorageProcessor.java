package com.vektor.mail.pipeline.processor;

import com.vektor.mail.core.model.Account;
import com.vektor.mail.core.model.Mailbox;
import com.vektor.mail.core.model.Message;
import com.vektor.mail.core.plugin.MailContext;
import com.vektor.mail.core.plugin.StorageContext;
import com.vektor.mail.core.plugin.StoragePlugin;
import com.vektor.mail.storage.db.repository.AccountRepository;
import com.vektor.mail.storage.db.repository.MailboxRepository;
import com.vektor.mail.storage.db.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Persists the incoming message via the active {@link StoragePlugin}.
 * Reads encrypted content and wrapped DEK from MailContext attributes
 * set by {@link MessageEncryptorProcessor}.
 */
@Component
@RequiredArgsConstructor
public class MessageStorageProcessor implements Processor {

    public static final String ATTR_STORED_MESSAGE = "storedMessage";

    private final StoragePlugin storagePlugin;
    private final AccountRepository accountRepository;
    private final MailboxRepository mailboxRepository;
    private final MessageRepository messageRepository;

    @Override
    public void process(Exchange exchange) throws Exception {
        MailContext ctx = exchange.getIn().getBody(MailContext.class);

        String recipient = ctx.getEnvelopeTo().isEmpty() ? null : ctx.getEnvelopeTo().get(0);
        Mailbox inbox = mailboxRepository
                .findByAccountIdAndPath(resolveAccountId(recipient), "INBOX")
                .orElseThrow(() -> new IllegalStateException("No INBOX for " + recipient));

        long uid = (messageRepository.findMaxUidByMailboxId(inbox.getId()).orElse(0L)) + 1;

        Message message = new Message();
        message.setMailbox(inbox);
        message.setFromAddress(ctx.getEnvelopeFrom());
        message.setToAddresses(ctx.getEnvelopeTo().toString());
        message.setReceivedAt(Instant.now());
        message.setEncryptedDek(ctx.getAttribute(MessageEncryptorProcessor.ATTR_WRAPPED_DEK));
        message.setSizeBytes(ctx.getRawMessage() != null ? ctx.getRawMessage().length : 0);
        message.setUid(uid);
        Object score = ctx.getAttribute("spamScore");
        if (score instanceof Number n) {
            message.setSpamScore(BigDecimal.valueOf(n.doubleValue()));
        }

        byte[] encryptedContent = ctx.getAttribute(MessageEncryptorProcessor.ATTR_ENCRYPTED_CONTENT);
        storagePlugin.storeMessage(new StorageContext(null), message, encryptedContent);

        ctx.setAttribute(ATTR_STORED_MESSAGE, message);
    }

    private java.util.UUID resolveAccountId(String email) {
        String addr = email != null ? email.replaceAll(".*<(.+)>.*", "$1").trim() : null;
        return accountRepository.findByEmail(addr)
                .map(Account::getId)
                .orElseThrow(() -> new IllegalStateException("No account for address: " + addr));
    }
}
