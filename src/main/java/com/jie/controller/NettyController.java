package com.jie.controller;

import com.jie.netty.SseServer;
import com.jie.response.NettyResp;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.annotation.Resource;
import java.time.LocalDateTime;

@RestController
public class NettyController {
    @Lazy
    @Resource
    private SseServer sseServer;

    @PostMapping("startSseServer")
    public NettyResp<String> startSseServer() {
        try {
            sseServer.start();
        } catch (Exception e) {
            return NettyResp.fail(e.getMessage(), LocalDateTime.now());
        }
        return NettyResp.success("success", LocalDateTime.now());
    }
}
