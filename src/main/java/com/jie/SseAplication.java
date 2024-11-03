package com.jie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class SseAplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(SseAplication.class, args);
        System.err.println("start ok");
    }
}
