package com.superexercisebook.nbtdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.superexercisebook.nbtdemo"} )
public class NbtDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(NbtDemoApplication.class, args);
    }
}
