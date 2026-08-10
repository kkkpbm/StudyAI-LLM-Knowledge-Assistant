package com.ka;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan({
        "com.ka.module.user.mapper",
        "com.ka.module.knowledge.mapper",
        "com.ka.module.learning.mapper",
        "com.ka.module.chat.mapper"
})
@EnableAsync
public class KnowledgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(KnowledgeApplication.class, args);
    }
}

