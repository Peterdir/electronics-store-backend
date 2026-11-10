package com.ecommerce.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // Chuyển chuỗi thành Path.
        // Chuyển sang đường dẫn tuyệt đối.
        // Chuẩn hóa đường dẫn.
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

        // Chuyển Path thành URI dạng chuỗi.
        String uploadUri = uploadPath.toUri().toString();

        // Ánh xạ URL /uploads/**
        // đến thư mục upload trên server.
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadUri);
    }
}
