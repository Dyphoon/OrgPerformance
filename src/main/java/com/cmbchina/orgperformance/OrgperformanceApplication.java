package com.cmbchina.orgperformance;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.cmbchina.orgperformance.mapper")
public class OrgperformanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrgperformanceApplication.class, args);
    }
}
