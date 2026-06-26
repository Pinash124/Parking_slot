package com.example.pricing_calculation.web;

import com.example.pricing_calculation.dto.AuthLoginRequest;
import com.example.pricing_calculation.dto.AuthLoginResponse;
import com.example.pricing_calculation.dto.AuthLogoutResponse;
import com.example.pricing_calculation.dto.AuthRegistrationRequest;
import com.example.pricing_calculation.dto.AuthRegistrationResponse;
import com.example.pricing_calculation.service.PaymentModuleAuthService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment-module/auth")
@Hidden
@Tag(name = "Authentication", description = "Đăng ký, đăng nhập và đăng xuất")
public class PaymentModuleAuthController {

    private final PaymentModuleAuthService authService;

    public PaymentModuleAuthController(PaymentModuleAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản", description = "Tạo tài khoản mới với role PARKING_USER và status ACTIVE.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Đăng ký thành công",
                    content = @Content(schema = @Schema(implementation = AuthRegistrationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc email đã tồn tại")
    })
    public ResponseEntity<AuthRegistrationResponse> register(@RequestBody AuthRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập", description = "Xác thực email/mật khẩu và trả Bearer access token có hạn 8 giờ.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công",
                    content = @Content(schema = @Schema(implementation = AuthLoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Email hoặc mật khẩu không đúng")
    })
    public AuthLoginResponse login(@RequestBody AuthLoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất", description = "Thu hồi Bearer access token hiện tại.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đăng xuất thành công",
                    content = @Content(schema = @Schema(implementation = AuthLogoutResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token thiếu, không hợp lệ hoặc đã hết hạn")
    })
    public AuthLogoutResponse logout(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return authService.logout(authorizationHeader);
    }
}
