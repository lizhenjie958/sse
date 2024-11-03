package com.jie.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * SSE处理器
 *
 * @author admin
 */
@Slf4j
@ChannelHandler.Sharable
public class SseHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final String PREFIX = "events";

    // 定义一个 AttributeKey 用于存储 ScheduledFuture
    private static final AttributeKey<ScheduledFuture<?>> SCHEDULED_FUTURE_KEY = AttributeKey.valueOf("scheduledFuture");

    @Override
    public void channelActive(io.netty.channel.ChannelHandlerContext ctx) throws Exception {
        // 获取远程地址
        String remoteAddress = ctx.channel().remoteAddress().toString();
        log.debug(">>>>>>>>>>>>>>>>>>>>>>>>SseHandler: channelActive, remoteAddress={}", remoteAddress);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 获取远程地址
        String remoteAddress = ctx.channel().remoteAddress().toString();
        log.debug(">>>>>>>>>>>>>>>>>>>>>>>>SseHandler: channelInactive, remoteAddress={}", remoteAddress);
        // 从 ChannelHandlerContext 中获取定时任务并取消
        ScheduledFuture<?> scheduledFuture = ctx.channel().attr(SCHEDULED_FUTURE_KEY).get();
        if (scheduledFuture != null) {
            // 显式取消定时任务
            scheduledFuture.cancel(false);
        }
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (request.method() == HttpMethod.OPTIONS) {
            // 处理预检请求
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, OPTIONS");
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type");
            ctx.writeAndFlush(response);
            return;
        }

        if (HttpUtil.is100ContinueExpected(request)) {
            send100Continue(ctx);
        }

        // 检查请求的 URI 是否以指定的前缀开始
        String uri = request.uri();
        // 解析 GET 参数
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri);
        Map<String, List<String>> parameters = queryStringDecoder.parameters();
        if (parameters != null && !parameters.isEmpty()) {
            log.debug(">>>>>>>>>>>>>>>>>>>>>>>>SseHandler: parameters={}", parameters);
        }
        if (!uri.startsWith("/" + PREFIX)) {
            ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND));
            return;
        }

        // 设置 CORS 头
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/event-stream");
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache");
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

        // CORS 头
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*"); // 允许所有域
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, OPTIONS"); // 允许的请求方法
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type"); // 允许的请求头

        ctx.write(response);

        // 发送初始 SSE 事件
        sendSseEvent(ctx, "Connected to SSE server");

        // 定期发送 SSE 事件
        long initialDelay = 0L;
        long period = 5L;

        ScheduledFuture<?> scheduledFuture = ctx.executor().scheduleAtFixedRate(
                () -> sendSseEvent(ctx, "CurrentTimeMillis: " + System.currentTimeMillis()),
                initialDelay, period, TimeUnit.SECONDS);

        // 将定时任务的引用存储在 ChannelHandlerContext 的属性中
        ctx.channel().attr(SCHEDULED_FUTURE_KEY).set(scheduledFuture);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 获取远程地址
        String remoteAddress = ctx.channel().remoteAddress().toString();
        log.error(">>>>>>>>>>>>>>>>>>>>>>>>SseHandler: exceptionCaught, remoteAddress={}", remoteAddress, cause);
        // 关闭连接，自动释放相关资源
        ctx.close();
    }

    protected static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
        ctx.write(response);
    }

    protected void sendSseEvent(ChannelHandlerContext ctx, String data) {
        ByteBuf buffer = ctx.alloc().buffer();
        buffer.writeBytes(("data: " + data + "\n\n").getBytes(StandardCharsets.UTF_8));
        ctx.writeAndFlush(new DefaultHttpContent(buffer));
    }

}
