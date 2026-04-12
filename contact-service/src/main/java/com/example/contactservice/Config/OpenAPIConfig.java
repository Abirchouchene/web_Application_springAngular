package com.example.contactservice.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class OpenAPIConfig {
    // CORS is handled by the API Gateway — no duplicate headers
}
