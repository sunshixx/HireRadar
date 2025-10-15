package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 应用入口。
 * <p>
 * 启用 Spring Scheduling，用于每日自动刷新投递链接（可按需配置）。
 * </p>
 */
@SpringBootApplication
@EnableScheduling
public class DemoApplication {

    /**
     * 应用启动方法。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}
