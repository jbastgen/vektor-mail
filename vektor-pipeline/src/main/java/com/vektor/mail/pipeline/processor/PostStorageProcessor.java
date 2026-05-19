package com.vektor.mail.pipeline.processor;

import com.vektor.mail.core.event.MailStoredSpringEvent;
import com.vektor.mail.core.model.Message;
import com.vektor.mail.core.plugin.MailContext;
import lombok.RequiredArgsConstructor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Fires a {@link MailStoredSpringEvent} after successful storage.
 * Downstream listeners (sync, search) subscribe to this event.
 */
@Component
@RequiredArgsConstructor
public class PostStorageProcessor implements Processor {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void process(Exchange exchange) {
        MailContext ctx = exchange.getIn().getBody(MailContext.class);
        Message stored = ctx.getAttribute(MessageStorageProcessor.ATTR_STORED_MESSAGE);
        if (stored != null) {
            eventPublisher.publishEvent(new MailStoredSpringEvent(this, stored));
        }
    }
}
