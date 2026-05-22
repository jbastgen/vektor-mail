package com.vektor.mail.core.plugin;

import java.time.Instant;
import java.util.Set;

/**
 * Criteria for listing/searching messages within a mailbox.
 */
public record MessageSearchCriteria(
        Set<String> requiredFlags,
        Set<String> forbiddenFlags,
        Instant receivedAfter,
        Instant receivedBefore,
        String subjectContains,
        String fromContains,
        int offset,
        int limit
) {
    public static MessageSearchCriteria all() {
        return new MessageSearchCriteria(null, null, null, null, null, null, 0, Integer.MAX_VALUE);
    }
}
