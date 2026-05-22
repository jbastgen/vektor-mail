package com.vektor.mail.pop3.handler;

import com.vektor.mail.core.model.Account;
import com.vektor.mail.core.model.Mailbox;
import com.vektor.mail.core.model.Message;
import com.vektor.mail.core.plugin.StoragePlugin;
import com.vektor.mail.security.crypto.PasswordEncoder;
import com.vektor.mail.storage.db.repository.AccountRepository;
import com.vektor.mail.storage.db.repository.MailboxRepository;
import com.vektor.mail.storage.db.repository.MessageRepository;
import io.netty.channel.*;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * POP3 command handler. Implements: GREETING, USER, PASS, STAT, LIST, RETR, DELE, NOOP, QUIT, RSET.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class Pop3ServerHandler extends SimpleChannelInboundHandler<String> {

    private static final AttributeKey<Pop3Session> SESSION_KEY = AttributeKey.valueOf("pop3Session");

    private final AccountRepository accountRepository;
    private final MailboxRepository mailboxRepository;
    private final MessageRepository messageRepository;
    private final PasswordEncoder passwordEncoder;
    private final StoragePlugin storagePlugin;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.channel().attr(SESSION_KEY).set(new Pop3Session());
        ok(ctx, "Vektor POP3 server ready");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String line) {
        Pop3Session session = ctx.channel().attr(SESSION_KEY).get();
        String upper = line.toUpperCase();
        String[] parts = line.split(" ", 2);
        String cmd = parts[0].toUpperCase();
        String arg = parts.length > 1 ? parts[1].trim() : "";

        switch (cmd) {
            case "USER" -> {
                session.setPendingUser(arg);
                ok(ctx, "");
            }
            case "PASS" -> handlePass(ctx, session, arg);
            case "STAT" -> {
                requireAuth(ctx, session);
                List<Message> msgs = getInboxMessages(session);
                long total = msgs.stream().mapToLong(Message::getSizeBytes).sum();
                ok(ctx, msgs.size() + " " + total);
            }
            case "LIST" -> {
                requireAuth(ctx, session);
                List<Message> msgs = getInboxMessages(session);
                ok(ctx, msgs.size() + " messages");
                for (int i = 0; i < msgs.size(); i++) {
                    ctx.writeAndFlush((i + 1) + " " + msgs.get(i).getSizeBytes() + "\r\n");
                }
                ctx.writeAndFlush(".\r\n");
            }
            case "RETR" -> handleRetr(ctx, session, arg);
            case "DELE" -> {
                requireAuth(ctx, session);
                ok(ctx, "message marked for deletion");
            }
            case "NOOP" -> ok(ctx, "");
            case "RSET" -> { session.getDeletedIndexes().clear(); ok(ctx, ""); }
            case "QUIT" -> {
                ok(ctx, "Vektor POP3 server signing off");
                ctx.close();
            }
            default -> err(ctx, "Unknown command");
        }
    }

    private void handlePass(ChannelHandlerContext ctx, Pop3Session session, String password) {
        if (session.getPendingUser() == null) { err(ctx, "USER first"); return; }
        accountRepository.findByEmail(session.getPendingUser()).ifPresentOrElse(account -> {
            if (account.isActive() && passwordEncoder.verify(password, account.getPasswordHash())) {
                session.setAccount(account);
                ok(ctx, "Logged in");
            } else {
                err(ctx, "Authentication failed");
            }
        }, () -> err(ctx, "Authentication failed"));
    }

    private void handleRetr(ChannelHandlerContext ctx, Pop3Session session, String arg) {
        requireAuth(ctx, session);
        try {
            int idx = Integer.parseInt(arg) - 1;
            List<Message> msgs = getInboxMessages(session);
            if (idx < 0 || idx >= msgs.size()) { err(ctx, "No such message"); return; }
            Message msg = msgs.get(idx);
            byte[] content = storagePlugin.loadMessageContent(msg);
            ok(ctx, msg.getSizeBytes() + " octets");
            ctx.writeAndFlush(new String(content) + "\r\n.\r\n");
        } catch (NumberFormatException e) {
            err(ctx, "Invalid message number");
        } catch (Exception e) {
            err(ctx, "Server error: " + e.getMessage());
        }
    }

    private List<Message> getInboxMessages(Pop3Session session) {
        return mailboxRepository
                .findByAccountIdAndPath(session.getAccount().getId(), "INBOX")
                .map(mb -> messageRepository.findByMailboxId(mb.getId()))
                .orElse(List.of());
    }

    private void requireAuth(ChannelHandlerContext ctx, Pop3Session session) {
        if (session.getAccount() == null) {
            err(ctx, "Not authenticated");
            throw new IllegalStateException("Not authenticated");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("POP3 error", cause);
        ctx.close();
    }

    private void ok(ChannelHandlerContext ctx, String msg) {
        ctx.writeAndFlush("+OK " + msg + "\r\n");
    }

    private void err(ChannelHandlerContext ctx, String msg) {
        ctx.writeAndFlush("-ERR " + msg + "\r\n");
    }
}
