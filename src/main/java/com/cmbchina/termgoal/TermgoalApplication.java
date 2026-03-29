package com.cmbchina.termgoal;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.cmbchina.termgoal.mapper")
public class TermgoalApplication {

    public static void main(String[] args) {
        SpringApplication.run(TermgoalApplication.class, args);
    }
}
