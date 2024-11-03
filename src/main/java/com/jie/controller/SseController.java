package com.jie.controller;

import com.jie.response.EiInfo;
import com.jie.server.SseEmitterServer;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@CrossOrigin(maxAge = 3600)
public class SseController {

    @RequestMapping(value = "/sse/connect/{id}",method = RequestMethod.GET)
    public SseEmitter connect(@PathVariable Integer id){
        SseEmitter sseEmitter = SseEmitterServer.connect(id);
        return sseEmitter;
    }

    /**
     * 向指定用户发送消息
     */
    @RequestMapping(value = "/sse/send/{id}", method = RequestMethod.GET)
    public EiInfo sendMsg(@PathVariable Integer id,@RequestParam("message") String message) {
        EiInfo eiInfo = new EiInfo();
        SseEmitterServer.sendMessage(id,message);
        eiInfo.sysSetMsg("向"+id+"号用户发送信息，"+message+"，消息发送成功");
        return eiInfo;
    }

    /**
     * 向所有用户发送消息
     */
    @RequestMapping(value = "/sse/send/all", method = RequestMethod.GET)
    public EiInfo sendMsg2AllUser(@RequestParam("message") String message) {
        EiInfo eiInfo = new EiInfo();
        SseEmitterServer.batchSendMessage(message);
        eiInfo.sysSetMsg("向所有用户发送信息，"+message+"，消息发送成功");
        return eiInfo;
    }

    /**
     * 关闭用户连接
     */
    @RequestMapping(value = "/sse/close/{id}", method = RequestMethod.GET)
    public EiInfo closeSse(@PathVariable Integer id) {
        EiInfo eiInfo = new EiInfo();
        SseEmitterServer.removeUser(id);
        eiInfo.sysSetMsg("关闭"+id+"号连接。当前连接用户有："+SseEmitterServer.getIds());
        return eiInfo;
    }
}

