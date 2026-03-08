package com.taxworkbench.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan(basePackages = "com.taxworkbench.api.infrastructure.mybatis.mapper")
public class TaxWorkbenchApplication {
    public static void main(String[] args) {
        SpringApplication.run(TaxWorkbenchApplication.class, args);
    }
}
