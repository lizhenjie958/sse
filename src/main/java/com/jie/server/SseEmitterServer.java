package com.jie.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class SseEmitterServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SseEmitterServer.class);

    /**
     * 当前连接数
     */
    private static AtomicInteger count = new AtomicInteger(0);

    private static Map<Integer, SseEmitter> sseEmitterMap = new ConcurrentHashMap<>();

    public static SseEmitter connect(Integer userId){
        // 设置超时日期，0表示不过期
        SseEmitter sseEmitter = new SseEmitter(20000L);

        // 注册回调
        sseEmitter.onCompletion(completionCallBack(userId));
        sseEmitter.onError(errorCallBack(userId));
        sseEmitter.onTimeout(timeoutCallBack(userId));
        sseEmitterMap.put(userId,sseEmitter);
        count.getAndIncrement();
        LOGGER.info("创建新SSE连接，连接用户编号:{}",userId);
        LOGGER.info("现有连接用户："+sseEmitterMap.keySet());
        return sseEmitter;
    }

    /**
     * 给指定用户发信息
     */
    public static void sendMessage(Integer userId,String message){
        if (!sseEmitterMap.containsKey(userId)) {
            connect(userId);
        }
        try {
            sseEmitterMap.get(userId).send(message);
            LOGGER.info("给" + userId + "号发送消息：" + message);
        } catch (IOException e) {
            LOGGER.error("userId:{},发送信息出错:{}", userId, e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 群发消息
     */
    public static void batchSendMessage(String message){
        if (sseEmitterMap != null&&!sseEmitterMap.isEmpty()) {
            sseEmitterMap.forEach((k,v)->{
                try {
                    v.send(message, MediaType.APPLICATION_JSON);
                } catch (IOException e) {
                    LOGGER.error("userId:{},发送信息出错:{}",k,e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }

    public static void batchSendMessage(Set<Integer> userIds, String message){
        userIds.forEach(userId->sendMessage(userId,message));
    }

    /**
     * 移出用户
     */
    public static void removeUser(Integer userId){
        sseEmitterMap.remove(userId);
        count.getAndDecrement();
        LOGGER.info("remove user id:{}",userId);
        LOGGER.info("remain user id:"+sseEmitterMap.keySet());
    }

    public static List<Integer> getIds(){
        return new ArrayList<>(sseEmitterMap.keySet());
    }

    public static int getUserCount(){
        return count.intValue();
    }

    private static Runnable completionCallBack(Integer userId){
        return ()->{
            LOGGER.info("结束连接,{}",userId);
            removeUser(userId);
        };
    }

    private static Runnable timeoutCallBack(Integer userId){
        return ()->{
            LOGGER.info("连接超时,{}",userId);
            removeUser(userId);
        };
    }

    private static Consumer<Throwable> errorCallBack(Integer userId){
        return throwable -> {
            LOGGER.error("连接异常,{}",userId);
            removeUser(userId);
        };
    }
}

