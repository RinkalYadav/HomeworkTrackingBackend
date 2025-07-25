package com.example.homework;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOriginPatterns("*") // ✅ Allow all origins
            .allowedMethods("*")        // ✅ Allow all HTTP methods (GET, POST, etc.)
            .allowedHeaders("*")        // ✅ Allow all headers
            .allowCredentials(true);    // ✅ Allow credentials (e.g., cookies, auth headers)
    }
}
