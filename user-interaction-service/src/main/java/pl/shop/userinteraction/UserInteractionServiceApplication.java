package pl.shop.userinteraction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class UserInteractionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserInteractionServiceApplication.class, args);
    }
}
