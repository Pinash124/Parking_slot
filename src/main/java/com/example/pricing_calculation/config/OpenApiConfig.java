package com.example.pricing_calculation.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springdoc.core.customizers.OpenApiCustomizer;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Smart Parking Unified API",
                version = "2.0.0",
                description = "API thong nhat cho Manager, Staff, Parking User va System Administrator.",
                contact = @Contact(name = "Smart Parking Team"),
                license = @License(name = "Internal Project")
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "Opaque access token",
        description = "Nhap accessToken nhan duoc tu POST /api/auth/login"
)
public class OpenApiConfig {
    @Bean
    OpenApiCustomizer removeSwaggerDescriptions() {
        return openApi -> {
            if (openApi.getInfo() != null) openApi.getInfo().setDescription(null);
            if (openApi.getTags() != null) openApi.getTags().forEach(tag -> tag.setDescription(null));
            if (openApi.getPaths() != null) {
                openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                    operation.setSummary(null);
                    operation.setDescription(null);
                }));
            }
            if (openApi.getComponents() != null && openApi.getComponents().getSecuritySchemes() != null) {
                openApi.getComponents().getSecuritySchemes().values()
                        .forEach(scheme -> scheme.setDescription(null));
            }
        };
    }
}
