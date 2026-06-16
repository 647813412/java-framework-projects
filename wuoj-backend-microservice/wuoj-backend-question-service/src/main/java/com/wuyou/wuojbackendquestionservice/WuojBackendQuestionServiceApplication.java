package com.wuyou.wuojbackendquestionservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.wuyou.wuojbackendquestionservice.mapper")
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@ComponentScan("com.wuyou")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.wuyou.wuojbackendserviceclient.service"})
public class WuojBackendQuestionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WuojBackendQuestionServiceApplication.class, args);
    }

}