package io.uresolvr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * UResolvr — Universal URI Resolution & Secure Routing Platform.
 *
 * <p>Run with: {@code java -jar uresolvr.jar}
 * <p>Default profile uses embedded H2 + Caffeine cache (zero configuration).
 * <p>Production profile: {@code java -jar uresolvr.jar --spring.profiles.active=postgres}
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class UResolvrApplication {

    public static void main(String[] args) {
        SpringApplication.run(UResolvrApplication.class, args);
    }
}
