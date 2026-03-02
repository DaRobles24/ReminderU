package com.reminderU.reminderU;

import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAsync
public class ReminderUApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReminderUApplication.class, args);
    }
}
