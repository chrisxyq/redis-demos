package com.example.springbootredisdemo.config;

import org.redisson.Redisson;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class RedissonConfig {
    /**
     * 单机模式
     * redis服务只有启动状态下，才能成功获得redisson
     * @return
     */
    @Bean
    public Redisson redisson() throws IOException {
        Config config = new Config();
        // 两种读取方式，Config.fromYAML 和 Config.fromJSON
        //Config config = Config.fromJSON(RedissonConfig.class.getClassLoader().getResource("redisson-config.json"));
        //Config config = Config.fromYAML(RedissonConfig.class.getClassLoader().getResource("redisson-config.yml"));
        config.useSingleServer().setAddress("redis://localhost:6379").setDatabase(0);
        return (Redisson) Redisson.create(config);
    }
}
