package com.vektor.mail.filter.blocklist;

import com.vektor.mail.core.model.BlocklistEntry;
import com.vektor.mail.core.plugin.FilterDecision;
import com.vektor.mail.core.plugin.FilterPlugin;
import com.vektor.mail.core.plugin.MailContext;
import com.vektor.mail.storage.db.repository.BlocklistRepository;
import lombok.RequiredArgsConstructor;
import org.pf4j.Extension;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Rejects mail from blocked IPs or domains.
 * Runs with priority 10 (very early in the filter chain).
 */
@Extension
@Component
@RequiredArgsConstructor
public class BlocklistFilterPlugin implements FilterPlugin {

    private final BlocklistRepository blocklistRepository;

    @Override
    public String getPluginId() { return "vektor-filter-blocklist"; }

    @Override
    public int getPriority() { return 10; }

    @Override
    public FilterDecision evaluate(MailContext ctx) {
        Instant now = Instant.now();

        // Check sender IP
        if (ctx.getSenderIp() != null && !ctx.getSenderIp().isBlank()) {
            if (!blocklistRepository.findActiveByValueAndType(ctx.getSenderIp(), BlocklistEntry.Type.IP, now).isEmpty()) {
                ctx.setRejectReason("Sender IP " + ctx.getSenderIp() + " is blocklisted");
                return FilterDecision.REJECT;
            }
        }

        // Check sender domain
        if (ctx.getSenderDomain() != null && !ctx.getSenderDomain().isBlank()) {
            if (!blocklistRepository.findActiveByValueAndType(ctx.getSenderDomain(), BlocklistEntry.Type.DOMAIN, now).isEmpty()) {
                ctx.setRejectReason("Sender domain " + ctx.getSenderDomain() + " is blocklisted");
                return FilterDecision.REJECT;
            }
        }

        return FilterDecision.CONTINUE;
    }
}
