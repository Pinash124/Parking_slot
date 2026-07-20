package com.example.pricing_calculation.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
public class DotenvConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setIgnoreResourceNotFound(true);
        configurer.setFileEncoding("UTF-8");

        Path envPath = Paths.get(System.getProperty("user.dir"), ".env");
        Resource[] resources = { new FileSystemResource(envPath.toFile()) };
        configurer.setLocations(resources);
        return configurer;
    }
}
