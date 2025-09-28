package com.example.melms;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${upload.base-dir}")
    private String baseDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/manuals/**")
                .addResourceLocations("file:" + baseDir.replace("\\", "/") + "/manuals/");

        registry.addResourceHandler("/warranties/**")
                .addResourceLocations("file:" + baseDir.replace("\\", "/") + "/warranties/");
    }
}
