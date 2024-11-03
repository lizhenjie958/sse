package com.jie.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

@Data
public class NettyResp<T> implements Serializable {
    private T data;
    private LocalDateTime localDateTime;

    public static<T> NettyResp success(T t, LocalDateTime localDateTime){
        NettyResp<T> tNettyResp = new NettyResp<>();
        tNettyResp.setData(t);
        tNettyResp.setLocalDateTime(localDateTime);
        return tNettyResp;
    }

    public static<T> NettyResp fail(T t, LocalDateTime localDateTime){
        NettyResp<T> tNettyResp = new NettyResp<>();
        tNettyResp.setData(t);
        tNettyResp.setLocalDateTime(localDateTime);
        return tNettyResp;
    }
}
