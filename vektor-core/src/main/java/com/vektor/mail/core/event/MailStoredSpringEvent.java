package com.vektor.mail.core.event;

import com.vektor.mail.core.model.Message;
import org.springframework.context.ApplicationEvent;

/**
 * Spring ApplicationEvent fired after a message is successfully stored.
 * Listeners in pipeline, search, sync modules subscribe to this.
 */
public class MailStoredSpringEvent extends ApplicationEvent {

    private final Message message;

    public MailStoredSpringEvent(Object source, Message message) {
        super(source);
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }
}
