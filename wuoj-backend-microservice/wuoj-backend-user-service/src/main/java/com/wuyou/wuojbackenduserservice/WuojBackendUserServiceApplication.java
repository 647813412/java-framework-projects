package com.wuyou.wuojbackenduserservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.wuyou.wuojbackenduserservice.mapper")
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@ComponentScan("com.wuyou")//因为全局异常处理器不在同一个项目下，所以需要去扫描一下
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.wuyou.wuojbackendserviceclient.service"})
public class WuojBackendUserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WuojBackendUserServiceApplication.class, args);
    }

}
