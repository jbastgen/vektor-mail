package com.vektor.mail.pipeline.route;

import com.vektor.mail.core.plugin.FilterDecision;
import com.vektor.mail.pipeline.processor.FilterChainProcessor;
import com.vektor.mail.pipeline.processor.MessageEncryptorProcessor;
import com.vektor.mail.pipeline.processor.MessageStorageProcessor;
import com.vektor.mail.pipeline.processor.PostStorageProcessor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/**
 * Default incoming mail processing route.
 *
 * Protocol servers (SMTP) post a {@link com.vektor.mail.core.plugin.MailContext}
 * to the {@code direct:mail.incoming} endpoint.
 */
@Component
public class IncomingMailRoute extends RouteBuilder {

    @Override
    public void configure() {
        // Global error handling: log and stop — protocol server handles SMTP response
        errorHandler(deadLetterChannel("direct:mail.error")
                .maximumRedeliveries(0));

        from("direct:mail.incoming")
            .routeId("incoming-mail-default")
            .description("Incoming mail processing pipeline")

            // 1-5. Run all FilterPlugins (blocklist, greylist, DKIM/SPF, spam)
            .process(FilterChainProcessor.class.getName())

            // Branch: reject/defer if any filter said so
            .choice()
                .when(header(FilterChainProcessor.HEADER_DECISION).isEqualTo(FilterDecision.REJECT.name()))
                    .to("direct:mail.reject")
                .when(header(FilterChainProcessor.HEADER_DECISION).isEqualTo(FilterDecision.DEFER.name()))
                    .to("direct:mail.defer")
            .end()

            // 6. Server-side rules (may change target mailbox)
            .to("direct:mail.rules")

            // 7. Encrypt message content + wrap DEK
            .process(MessageEncryptorProcessor.class.getName())

            // 8. Store via active StoragePlugin
            .process(MessageStorageProcessor.class.getName())

            // 9. Post-storage: fire Spring event, optional indexing + sync
            .process(PostStorageProcessor.class.getName());

        from("direct:mail.reject")
            .routeId("mail-reject")
            .log("Mail REJECTED from ${body.envelopeFrom}: ${header.VektorRejectReason}");

        from("direct:mail.defer")
            .routeId("mail-defer")
            .log("Mail DEFERRED from ${body.envelopeFrom}: greylisting");

        from("direct:mail.rules")
            .routeId("mail-rules")
            .log("Applying server-side rules for message from ${body.envelopeFrom}");

        from("direct:mail.error")
            .routeId("mail-error")
            .log("Pipeline error for message from ${body.envelopeFrom}");
    }
}
