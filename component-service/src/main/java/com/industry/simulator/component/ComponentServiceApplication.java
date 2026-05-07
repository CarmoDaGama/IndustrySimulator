package com.industry.simulator.component;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.industry.simulator"})
public class ComponentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComponentServiceApplication.class, args);
    }
}
