package com.vektor.mail.core.plugin;

import org.pf4j.ExtensionPoint;

/**
 * Extension point for mail processing filters (spam, blocklist, greylist, …).
 * Implementations are loaded by PF4J and sorted by {@link #getPriority()} ascending.
 */
public interface FilterPlugin extends ExtensionPoint {

    /** Unique plugin identifier, e.g. "vektor-filter-blocklist". */
    String getPluginId();

    /**
     * Lower value = runs earlier in the filter chain.
     * Suggested values: blocklist=10, greylist=20, spam=50.
     */
    int getPriority();

    /**
     * Evaluate the incoming mail context and return a decision.
     * Mutate {@code ctx} to annotate it (e.g. spam score, reject reason).
     */
    FilterDecision evaluate(MailContext ctx);
}
