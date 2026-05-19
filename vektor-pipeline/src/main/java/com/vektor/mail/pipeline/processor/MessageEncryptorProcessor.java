package com.vektor.mail.pipeline.processor;

import com.vektor.mail.core.plugin.MailContext;
import com.vektor.mail.security.crypto.MessageCipher;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * Encrypts the raw message bytes using AES-256-GCM.
 * Generates a fresh DEK per message, encrypts it with the master key,
 * and stores both on the MailContext for the storage processor to persist.
 */
@Component
public class MessageEncryptorProcessor implements Processor {

    public static final String ATTR_ENCRYPTED_CONTENT = "encryptedContent";
    public static final String ATTR_WRAPPED_DEK = "wrappedDek";

    private final MessageCipher cipher;
    private final byte[] masterKey;

    public MessageEncryptorProcessor(MessageCipher cipher,
                                      @Value("${vektor.security.master-key}") String masterKeyBase64) {
        this.cipher = cipher;
        this.masterKey = Base64.getDecoder().decode(masterKeyBase64);
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        MailContext ctx = exchange.getIn().getBody(MailContext.class);
        byte[] raw = ctx.getRawMessage();
        if (raw == null || raw.length == 0) return;

        byte[] dek = cipher.generateDek();
        byte[] encrypted = cipher.encrypt(raw, dek);
        byte[] wrappedDek = cipher.wrapDek(dek, masterKey);

        ctx.setAttribute(ATTR_ENCRYPTED_CONTENT, encrypted);
        ctx.setAttribute(ATTR_WRAPPED_DEK, wrappedDek);
    }
}
