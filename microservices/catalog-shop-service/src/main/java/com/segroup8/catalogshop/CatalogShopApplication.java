package com.segroup8.catalogshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CatalogShopApplication {
    public static void main(String[] args) { SpringApplication.run(CatalogShopApplication.class, args); }
}
