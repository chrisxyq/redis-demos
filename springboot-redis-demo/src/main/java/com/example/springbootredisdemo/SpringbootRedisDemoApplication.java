package com.example.springbootredisdemo;

import org.redisson.Redisson;
import org.redisson.config.Config;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * spring boot 项目默认只会扫描主类同级的包，而不会扫描上一级的包
 */
@SpringBootApplication(scanBasePackages="com.example.springbootredisdemo")
public class SpringbootRedisDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootRedisDemoApplication.class, args);
    }


}
