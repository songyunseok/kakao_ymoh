package com.kakaobank.demo.ymoh.fg;

import com.kakaobank.demo.ymoh.FileQueueLocator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FGApplication {

    private FileQueueLocator locator = new FileQueueLocator();

    @Bean
    public FileQueueLocator queueLocator() {
        return locator;
    }

    public static void main(String[] args) {
        SpringApplication.run(FGApplication.class, args);
    }

}