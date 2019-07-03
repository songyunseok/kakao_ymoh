package com.kakaobank.demo.ymoh.fg;

import com.kakaobank.demo.ymoh.FileQueueLocator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.net.ssl.SSLContext;

@SpringBootApplication
public class FGApplication {

    private FileQueueLocator locator = new FileQueueLocator();

    @Bean
    public FileQueueLocator queueLocator() {
        return locator;
    }

    @Bean
    public SSLContext sslContext() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, null, null);
            return sslContext;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(FGApplication.class, args);
    }

}
