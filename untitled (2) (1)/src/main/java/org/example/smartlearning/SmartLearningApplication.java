package org.example.smartlearning;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI智能学习平台主应用
 * 基于"学-练-评-优"闭环，实现精准提升
 */
@SpringBootApplication
@MapperScan("org.example.smartlearning.mapper")
public class SmartLearningApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartLearningApplication.class, args);
    }
}
