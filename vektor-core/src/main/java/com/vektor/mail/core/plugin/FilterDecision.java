package com.vektor.mail.core.plugin;

/**
 * Decision returned by a {@link FilterPlugin}.
 */
public enum FilterDecision {
    /** Continue to the next filter. */
    CONTINUE,
    /** Accept the message, skip remaining filters. */
    ACCEPT,
    /** Reject permanently (SMTP 5xx). */
    REJECT,
    /** Defer temporarily (SMTP 4xx), e.g. for greylisting. */
    DEFER
}
