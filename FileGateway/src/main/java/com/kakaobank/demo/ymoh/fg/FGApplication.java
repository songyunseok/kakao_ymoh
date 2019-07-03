package com.kakaobank.demo.ymoh.fg;

import com.kakaobank.demo.ymoh.FileQueueLocator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;

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
            //SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            //sslContext.init(null, null, null);
            /*KeyStore ks = KeyStore.getInstance("JKS");
            try (InputStream keystoreFile =
                         FGApplication.class.getClassLoader().getResourceAsStream("keystore.jks")) {
                ks.load(keystoreFile, "password".toCharArray());
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(ks);
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(ks, "password".toCharArray());
                sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
                return sslContext;
            }*/
            return SSLContext.getDefault();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(FGApplication.class, args);
    }

}
