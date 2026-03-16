package com.example.bff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class KeycloakBffApplication {

    public static void main(String[] args) {
        // Set a timezone at the very beginning of the process
        System.setProperty("user.timezone", "UTC");
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        SpringApplication.run(KeycloakBffApplication.class, args);
    }
}
