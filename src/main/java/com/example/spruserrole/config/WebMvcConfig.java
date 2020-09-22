package com.example.spruserrole.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final long MAX_AGE_SECS = 3600;
    public void addCorsMappings(CorsRegistry corsRegistration){
        corsRegistration.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("HEAD","OPTIONS","GET","POST","PUT","PATH","DELETE")
                .maxAge(MAX_AGE_SECS);
    }
}
