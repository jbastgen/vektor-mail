package com.vektor.mail.smtp.handler;

import com.vektor.mail.core.plugin.MailContext;
import com.vektor.mail.smtp.config.SmtpProperties;
import io.netty.channel.*;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Netty channel handler implementing the SMTP command state machine.
 * One handler instance per channel (not shared).
 */
@Slf4j
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class SmtpServerHandler extends SimpleChannelInboundHandler<String> {

    private static final AttributeKey<SmtpSession> SESSION_KEY = AttributeKey.valueOf("smtpSession");

    private final SmtpProperties props;
    private final ProducerTemplate camelProducer;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        SmtpSession session = new SmtpSession();
        ctx.channel().attr(SESSION_KEY).set(session);
        String clientIp = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
        session.setState(SmtpSession.State.CONNECTED);
        write(ctx, "220 " + props.hostname() + " ESMTP Vektor Mail Server ready");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String line) {
        SmtpSession session = ctx.channel().attr(SESSION_KEY).get();

        if (session.isDataMode()) {
            handleDataLine(ctx, session, line);
            return;
        }

        String upper = line.toUpperCase();

        if (upper.startsWith("EHLO") || upper.startsWith("HELO")) {
            String[] parts = line.split(" ", 2);
            session.setClientHostname(parts.length > 1 ? parts[1] : "unknown");
            session.setState(SmtpSession.State.GREETED);
            if (upper.startsWith("EHLO")) {
                write(ctx, "250-" + props.hostname() + " greets " + session.getClientHostname());
                write(ctx, "250-SIZE " + props.maxMessageSizeBytes());
                write(ctx, "250-STARTTLS");
                write(ctx, "250-AUTH PLAIN LOGIN");
                write(ctx, "250 8BITMIME");
            } else {
                write(ctx, "250 " + props.hostname());
            }

        } else if (upper.startsWith("MAIL FROM:")) {
            if (session.getState() == SmtpSession.State.CONNECTED) {
                write(ctx, "503 Bad sequence: send EHLO first");
                return;
            }
            String address = extractAddress(line.substring(10));
            session.setEnvelopeFrom(address);
            session.setState(SmtpSession.State.MAIL);
            write(ctx, "250 OK");

        } else if (upper.startsWith("RCPT TO:")) {
            if (session.getState() != SmtpSession.State.MAIL && session.getState() != SmtpSession.State.RCPT) {
                write(ctx, "503 Bad sequence");
                return;
            }
            String address = extractAddress(line.substring(8));
            session.getEnvelopeTo().add(address);
            session.setState(SmtpSession.State.RCPT);
            write(ctx, "250 OK");

        } else if (upper.equals("DATA")) {
            if (session.getState() != SmtpSession.State.RCPT) {
                write(ctx, "503 Bad sequence: send RCPT TO first");
                return;
            }
            session.setDataMode(true);
            session.setState(SmtpSession.State.DATA);
            write(ctx, "354 Start mail input; end with <CRLF>.<CRLF>");

        } else if (upper.equals("RSET")) {
            session.reset();
            write(ctx, "250 OK");

        } else if (upper.equals("NOOP")) {
            write(ctx, "250 OK");

        } else if (upper.equals("QUIT")) {
            write(ctx, "221 " + props.hostname() + " closing connection");
            ctx.close();

        } else {
            write(ctx, "500 Command not recognized");
        }
    }

    private void handleDataLine(ChannelHandlerContext ctx, SmtpSession session, String line) {
        if (".".equals(line)) {
            // End of DATA
            session.setDataMode(false);
            dispatchToPipeline(ctx, session);
            session.reset();
            write(ctx, "250 OK: message queued");
        } else {
            // Un-stuff leading dot
            session.getDataBuffer().append(line.startsWith("..") ? line.substring(1) : line).append("\r\n");
        }
    }

    private void dispatchToPipeline(ChannelHandlerContext ctx, SmtpSession session) {
        String clientIp = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();

        MailContext mailCtx = new MailContext();
        mailCtx.setSenderIp(clientIp);
        mailCtx.setEnvelopeFrom(session.getEnvelopeFrom());
        mailCtx.setEnvelopeTo(new java.util.ArrayList<>(session.getEnvelopeTo()));
        mailCtx.setRawMessage(session.getDataBuffer().toString().getBytes(StandardCharsets.UTF_8));

        try {
            camelProducer.sendBody("direct:mail.incoming", mailCtx);
        } catch (Exception e) {
            log.error("Pipeline dispatch failed for message from {}", session.getEnvelopeFrom(), e);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("SMTP handler error", cause);
        ctx.close();
    }

    private void write(ChannelHandlerContext ctx, String response) {
        ctx.writeAndFlush(response + "\r\n");
    }

    private String extractAddress(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("<") && trimmed.contains(">")) {
            return trimmed.substring(1, trimmed.indexOf('>'));
        }
        return trimmed.split("\\s+")[0];
    }
}
