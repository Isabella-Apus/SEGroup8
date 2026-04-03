package com.segroup8.platform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    private final UploadProperties uploadProperties;

    public StaticResourceConfig(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String normalizedPath = Paths.get(uploadProperties.getDir()).toAbsolutePath().normalize().toString()
                .replace("\\", "/");
        String uploadPath = Paths.get(uploadProperties.getDir()).toAbsolutePath().normalize().toUri().toString();
        if (!uploadPath.endsWith("/")) {
            uploadPath = uploadPath + "/";
        }
        String filePath = "file:" + normalizedPath + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath, filePath);
    }
}
