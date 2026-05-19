package com.vektor.mail.core.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.UUID;

/**
 * Sealed hierarchy of mail events — used for both Spring in-process events
 * and inter-instance sync payloads.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = MailEvent.MessageStoredEvent.class, name = "MESSAGE_STORED"),
    @JsonSubTypes.Type(value = MailEvent.MessageFlagsUpdatedEvent.class, name = "MESSAGE_FLAGS_UPDATED"),
    @JsonSubTypes.Type(value = MailEvent.MessageDeletedEvent.class, name = "MESSAGE_DELETED"),
    @JsonSubTypes.Type(value = MailEvent.MailboxCreatedEvent.class, name = "MAILBOX_CREATED"),
    @JsonSubTypes.Type(value = MailEvent.MailboxDeletedEvent.class, name = "MAILBOX_DELETED"),
    @JsonSubTypes.Type(value = MailEvent.AccountUpdatedEvent.class, name = "ACCOUNT_UPDATED")
})
public sealed interface MailEvent
        permits MailEvent.MessageStoredEvent,
                MailEvent.MessageFlagsUpdatedEvent,
                MailEvent.MessageDeletedEvent,
                MailEvent.MailboxCreatedEvent,
                MailEvent.MailboxDeletedEvent,
                MailEvent.AccountUpdatedEvent {

    Instant occurredAt();
    String originNodeId();

    record MessageStoredEvent(UUID messageId, UUID mailboxId, UUID accountId,
                               Instant occurredAt, String originNodeId) implements MailEvent {}

    record MessageFlagsUpdatedEvent(UUID messageId, java.util.Set<String> newFlags,
                                     Instant occurredAt, String originNodeId) implements MailEvent {}

    record MessageDeletedEvent(UUID messageId, UUID mailboxId,
                                Instant occurredAt, String originNodeId) implements MailEvent {}

    record MailboxCreatedEvent(UUID mailboxId, UUID accountId, String path,
                                Instant occurredAt, String originNodeId) implements MailEvent {}

    record MailboxDeletedEvent(UUID mailboxId, UUID accountId,
                                Instant occurredAt, String originNodeId) implements MailEvent {}

    record AccountUpdatedEvent(UUID accountId, Instant occurredAt, String originNodeId) implements MailEvent {}
}
