package com.github.linyuzai.dynamicfeign;

import com.github.linyuzai.dynamicfeign.annotation.EnableDynamicFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableDynamicFeignClients
@SpringBootApplication
public class DynamicFeignApplication {

    public static void main(String[] args) {
        SpringApplication.run(DynamicFeignApplication.class, args);
    }
}
