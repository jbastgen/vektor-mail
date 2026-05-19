package com.vektor.mail.pipeline.processor;

import com.vektor.mail.core.plugin.FilterDecision;
import com.vektor.mail.core.plugin.FilterPlugin;
import com.vektor.mail.core.plugin.MailContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Camel Processor that runs all registered {@link FilterPlugin}s in priority order.
 * Sets a header on the exchange to signal the final decision.
 */
@Component
public class FilterChainProcessor implements Processor {

    public static final String HEADER_DECISION = "VektorFilterDecision";
    public static final String HEADER_REJECT_REASON = "VektorRejectReason";

    private final List<FilterPlugin> filters;

    public FilterChainProcessor(List<FilterPlugin> filters) {
        this.filters = filters.stream()
                .sorted(Comparator.comparingInt(FilterPlugin::getPriority))
                .toList();
    }

    @Override
    public void process(Exchange exchange) {
        MailContext ctx = exchange.getIn().getBody(MailContext.class);
        if (ctx == null) {
            exchange.setException(new IllegalStateException("No MailContext in exchange body"));
            return;
        }

        for (FilterPlugin filter : filters) {
            FilterDecision decision = filter.evaluate(ctx);
            ctx.setDecision(decision);
            if (decision == FilterDecision.REJECT || decision == FilterDecision.DEFER) {
                break;
            }
            if (decision == FilterDecision.ACCEPT) {
                break;
            }
        }

        exchange.getIn().setHeader(HEADER_DECISION, ctx.getDecision().name());
        if (ctx.getRejectReason() != null) {
            exchange.getIn().setHeader(HEADER_REJECT_REASON, ctx.getRejectReason());
        }
    }
}
