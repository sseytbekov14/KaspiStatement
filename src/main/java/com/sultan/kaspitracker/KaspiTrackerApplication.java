package com.sultan.kaspitracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class KaspiTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KaspiTrackerApplication.class, args);
    }
}