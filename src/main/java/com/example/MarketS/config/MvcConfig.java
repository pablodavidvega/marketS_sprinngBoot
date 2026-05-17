package com.example.MarketS.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.path:/tmp/uploads/}")
    private String uploadPath;

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("index");
        registry.addViewController("/acces-denied").setViewName("acces-denied");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadPath.startsWith("file:") ? uploadPath : "file:" + uploadPath;
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
