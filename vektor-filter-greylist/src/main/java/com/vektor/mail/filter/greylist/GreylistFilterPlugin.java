package com.vektor.mail.filter.greylist;

import com.vektor.mail.core.model.GreylistEntry;
import com.vektor.mail.core.plugin.FilterDecision;
import com.vektor.mail.core.plugin.FilterPlugin;
import com.vektor.mail.core.plugin.MailContext;
import com.vektor.mail.storage.db.repository.GreylistRepository;
import lombok.RequiredArgsConstructor;
import org.pf4j.Extension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Greylisting: defers unknown sender/IP/recipient triplets for a configurable delay.
 * Legitimate servers retry; most spam bots don't.
 */
@Extension
@Component
@RequiredArgsConstructor
public class GreylistFilterPlugin implements FilterPlugin {

    private final GreylistRepository greylistRepository;

    @Value("${vektor.greylist.delay-minutes:5}")
    private int delayMinutes;

    @Override
    public String getPluginId() { return "vektor-filter-greylist"; }

    @Override
    public int getPriority() { return 20; }

    @Override
    @Transactional
    public FilterDecision evaluate(MailContext ctx) {
        String ip = ctx.getSenderIp();
        String domain = ctx.getSenderDomain();
        String recipient = ctx.getEnvelopeTo().isEmpty() ? "" : ctx.getEnvelopeTo().get(0);

        var existing = greylistRepository.findBySenderIpAndSenderDomainAndRecipientAddr(ip, domain, recipient);
        Instant now = Instant.now();

        if (existing.isEmpty()) {
            // First contact — create entry and defer
            GreylistEntry entry = new GreylistEntry();
            entry.setSenderIp(ip);
            entry.setSenderDomain(domain);
            entry.setRecipientAddr(recipient);
            entry.setFirstSeen(now);
            entry.setRetryCount(0);
            greylistRepository.save(entry);
            ctx.setRejectReason("Greylisting: please retry in " + delayMinutes + " minutes");
            return FilterDecision.DEFER;
        }

        GreylistEntry entry = existing.get();
        entry.setRetryCount(entry.getRetryCount() + 1);

        if (entry.getPassedAt() != null) {
            // Already passed — accept without updating
            return FilterDecision.CONTINUE;
        }

        // Check if enough time has passed since first contact
        if (Duration.between(entry.getFirstSeen(), now).toMinutes() >= delayMinutes) {
            entry.setPassedAt(now);
            greylistRepository.save(entry);
            return FilterDecision.CONTINUE;
        }

        // Still within delay window
        greylistRepository.save(entry);
        ctx.setRejectReason("Greylisting: please retry later");
        return FilterDecision.DEFER;
    }
}
