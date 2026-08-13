package com.shiyq.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Mybatis-plus配置类
 */
@MapperScan("com.shiyq.mapper")
@EnableTransactionManagement
@Configuration
public class MybatisPlusConfig {
}
