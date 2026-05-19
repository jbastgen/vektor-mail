package com.vektor.mail.pipeline.route;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/**
 * Outgoing mail route: applies DKIM signing, rate limiting, and delivers via SMTP relay.
 * Triggered from {@link com.vektor.mail.webmail.controller.ComposeController}.
 */
@Component
public class OutgoingMailRoute extends RouteBuilder {

    @Override
    public void configure() {
        errorHandler(deadLetterChannel("direct:mail.outgoing.error").maximumRedeliveries(2));

        from("direct:mail.outgoing")
            .routeId("outgoing-mail-default")
            .description("Outgoing mail: sign, rate-limit, deliver")
            .log("Outgoing mail from ${body.envelopeFrom} to ${body.envelopeTo}")
            // Future: add DKIM signer processor, rate limiter, SMTP relay processor
            .to("direct:mail.outgoing.relay");

        from("direct:mail.outgoing.relay")
            .routeId("outgoing-mail-relay")
            .log("Relaying mail from ${body.envelopeFrom}");

        from("direct:mail.outgoing.error")
            .routeId("outgoing-mail-error")
            .log("Outgoing mail error for ${body.envelopeFrom}");
    }
}
