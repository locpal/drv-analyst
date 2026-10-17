package ru.sewaiper.derivative.analyst;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@ConfigurationPropertiesScan(basePackages = "ru.sewaiper.derivative.analyst.properties")
public class AnalystApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalystApplication.class, args);
    }
}
