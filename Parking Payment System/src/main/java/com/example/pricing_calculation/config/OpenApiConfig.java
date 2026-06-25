package com.example.pricing_calculation.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Parking Payment System API",
                version = "1.0.0",
                description = "REST API cho đăng nhập, đặt chỗ, phiên gửi xe, tính phí, thanh toán và dashboard.",
                contact = @Contact(name = "Parking Payment System Team"),
                license = @License(name = "Internal Project")
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "Opaque access token",
        description = "Nhập accessToken nhận được từ POST /api/auth/login"
)
public class OpenApiConfig {
}
