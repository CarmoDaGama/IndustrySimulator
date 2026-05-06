package com.industry.simulator.assembly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.industry.simulator"})
public class AssemblyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssemblyServiceApplication.class, args);
    }
}
