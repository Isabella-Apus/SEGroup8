package com.segroup8.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BenefitsFinanceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BenefitsFinanceApplication.class, args);
    }
}
