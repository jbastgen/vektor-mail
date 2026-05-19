package com.vektor.mail.filter.spam;

import com.vektor.mail.core.plugin.FilterDecision;
import com.vektor.mail.core.plugin.FilterPlugin;
import com.vektor.mail.core.plugin.MailContext;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Spam filter using RSpamd HTTP API.
 * Sends the raw message to RSpamd and reads the spam score.
 * Falls back to CONTINUE on RSpamd unavailability (fail-open).
 */
@Extension
@Component
@Slf4j
public class SpamFilterPlugin implements FilterPlugin {

    @Value("${vektor.spam.rspamd-url:http://rspamd:11333}")
    private String rspamdUrl;

    @Value("${vektor.spam.reject-score:15.0}")
    private double rejectScore;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getPluginId() { return "vektor-filter-spam"; }

    @Override
    public int getPriority() { return 50; }

    @Override
    public FilterDecision evaluate(MailContext ctx) {
        byte[] rawMessage = ctx.getRawMessage();
        if (rawMessage == null || rawMessage.length == 0) return FilterDecision.CONTINUE;

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    rspamdUrl + "/checkv2",
                    new String(rawMessage, StandardCharsets.UTF_8),
                    Map.class
            );

            if (response.getBody() != null) {
                Object score = response.getBody().get("score");
                double spamScore = score instanceof Number n ? n.doubleValue() : 0.0;
                ctx.setAttribute("spamScore", spamScore);
                log.debug("RSpamd score for message from {}: {}", ctx.getEnvelopeFrom(), spamScore);

                if (spamScore >= rejectScore) {
                    ctx.setRejectReason("Message spam score " + spamScore + " exceeds threshold " + rejectScore);
                    return FilterDecision.REJECT;
                }
            }
        } catch (Exception e) {
            // Fail open: if RSpamd is unavailable, let the message through
            log.warn("RSpamd unavailable, skipping spam check: {}", e.getMessage());
        }

        return FilterDecision.CONTINUE;
    }
}
