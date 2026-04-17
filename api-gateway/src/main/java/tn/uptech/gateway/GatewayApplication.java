package tn.uptech.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"tn.uptech.gateway", "com.example.apigateway"})
public class GatewayApplication {

	public static void main(String[] args) {

		SpringApplication.run(GatewayApplication.class, args);
	}

}


