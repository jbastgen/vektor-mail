package com.vektor.mail.core.plugin;

import com.vektor.mail.core.model.Rule;
import org.pf4j.ExtensionPoint;

import java.util.List;

/**
 * Extension point for server-side mail rule engines.
 */
public interface RulesPlugin extends ExtensionPoint {

    String getPluginId();

    /**
     * Apply the given rules to the mail context.
     * Implementations may modify ctx (e.g. set target mailbox) or trigger side effects.
     */
    void applyRules(MailContext ctx, List<Rule> rules);
}
