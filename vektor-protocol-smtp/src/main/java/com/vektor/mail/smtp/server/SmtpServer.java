package com.vektor.mail.smtp.server;

import com.vektor.mail.smtp.config.SmtpProperties;
import com.vektor.mail.smtp.handler.SmtpServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Starts Netty SMTP listener(s) on the configured ports.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SmtpServer {

    private static final int MAX_LINE_LENGTH = 4096;

    private final SmtpProperties props;
    private final ProducerTemplate camelProducer;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private final List<Channel> serverChannels = new ArrayList<>();

    @EventListener(ApplicationReadyEvent.class)
    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        SmtpServerHandler handler = new SmtpServerHandler(props, camelProducer);

        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new LineBasedFrameDecoder(MAX_LINE_LENGTH));
                        p.addLast(new StringDecoder(StandardCharsets.UTF_8));
                        p.addLast(new StringEncoder(StandardCharsets.UTF_8));
                        p.addLast(handler);
                    }
                });

        bindPort(bootstrap, props.port(), "MTA");
        bindPort(bootstrap, props.submissionPort(), "MSA");
        log.info("SMTP server started on ports {}/{}", props.port(), props.submissionPort());
    }

    private void bindPort(ServerBootstrap bootstrap, int port, String label) throws InterruptedException {
        ChannelFuture future = bootstrap.bind(port).sync();
        serverChannels.add(future.channel());
        log.info("SMTP {} listening on port {}", label, port);
    }

    @PreDestroy
    public void stop() {
        serverChannels.forEach(Channel::close);
        if (workerGroup != null) workerGroup.shutdownGracefully();
        if (bossGroup != null) bossGroup.shutdownGracefully();
        log.info("SMTP server stopped");
    }
}
