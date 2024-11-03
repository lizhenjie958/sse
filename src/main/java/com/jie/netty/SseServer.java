package com.jie.netty;

import com.jie.server.SseEmitterServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Service
public class SseServer {

    private static final Logger log = LoggerFactory.getLogger(SseServer.class);


    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    private boolean started = false;

    @PostConstruct
    public void init() {
        log.debug("SSE服务初始化完毕");
    }

    public void shutdown() {
        log.debug("SSE服务 Shutting down server...");
        // 优雅关闭 workerGroup
        if (!workerGroup.isShutdown()) {
            workerGroup.shutdownGracefully(5, 10, TimeUnit.SECONDS);
        }
        // 优雅关闭 bossGroup
        if (!bossGroup.isShutdown()) {
            bossGroup.shutdownGracefully(5, 10, TimeUnit.SECONDS);
        }
        log.debug("SSE服务 Server shut down gracefully.");
    }

    @Async
    public void start() throws Exception {
        if (started) {
            return;
        }
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new HttpServerCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator( 1024 * 1024));
                            ch.pipeline().addLast(new ChunkedWriteHandler());
                            ch.pipeline().addLast(new SseHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            // 绑定端口并同步
            ChannelFuture f = b.bind(8849).sync();
            log.debug("SSE服务启动完成，绑定端口：{}", 8849);
            started = true;
            // 添加关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
            // 等待服务器通道关闭
            f.channel().closeFuture().sync();
        } finally {
            shutdown();
        }
    }
}
