package com.springguard.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration. Allows the deployed frontend (any *.vercel.app domain) and
 * local development. Specific patterns rather than a blanket "*" — the secure way.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(
                        "https://*.vercel.app",
                        "http://localhost:5173",
                        "http://localhost:3000")
                .allowedMethods("GET", "POST");
    }
}
