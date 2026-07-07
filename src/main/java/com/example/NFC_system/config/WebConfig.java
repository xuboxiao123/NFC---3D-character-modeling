package com.example.NFC_system.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.local.model-dir:D:/models/unzip/}")
    private String modelDir;

    @Value("${app.local.image-dir:D:/models/images/}")
    private String imageDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // 3D 模型静态资源
        registry.addResourceHandler("/models/**")
                .addResourceLocations("file:" + modelDir);

        // 图片静态资源
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + imageDir);
    }
}