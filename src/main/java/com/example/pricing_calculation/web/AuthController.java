package com.example.pricing_calculation.web;

import com.example.pricing_calculation.dto.AuthLoginRequest;
import com.example.pricing_calculation.dto.AuthLoginResponse;
import com.example.pricing_calculation.dto.AuthLogoutResponse;
import com.example.pricing_calculation.dto.AuthRegistrationRequest;
import com.example.pricing_calculation.dto.ChangePasswordRequest;
import com.example.pricing_calculation.dto.VerifyOtpRequest;
import com.example.pricing_calculation.dto.OtpResponse;
import com.example.pricing_calculation.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Đăng ký, đăng nhập, OTP và đổi mật khẩu")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản (Gửi OTP)", description = "Yêu cầu đăng ký tài khoản mới và gửi mã OTP qua email.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP đã được gửi",
                    content = @Content(schema = @Schema(implementation = OtpResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc email đã tồn tại")
    })
    public ResponseEntity<OtpResponse> register(@RequestBody AuthRegistrationRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập (Yêu cầu OTP)", description = "Xác thực email/mật khẩu và gửi mã OTP qua email.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mật khẩu đúng, OTP đã được gửi",
                    content = @Content(schema = @Schema(implementation = OtpResponse.class))),
            @ApiResponse(responseCode = "401", description = "Email hoặc mật khẩu không đúng")
    })
    public OtpResponse login(@RequestBody AuthLoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Xác nhận mã OTP", description = "Xác nhận mã OTP cho cả đăng ký và đăng nhập để nhận Bearer access token truy cập API.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP chính xác, đăng nhập thành công và trả về access token",
                    content = @Content(schema = @Schema(implementation = AuthLoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "OTP không hợp lệ hoặc đã hết hạn")
    })
    public ResponseEntity<AuthLoginResponse> verifyOtp(@RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Thay đổi mật khẩu (Gửi OTP)", description = "Yêu cầu đổi mật khẩu cho người dùng hiện tại (yêu cầu xác thực Bearer token). Gửi mã OTP xác nhận qua email.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Yêu cầu đổi mật khẩu thành công, OTP đã được gửi",
                    content = @Content(schema = @Schema(implementation = OtpResponse.class))),
            @ApiResponse(responseCode = "400", description = "Mật khẩu cũ không chính xác hoặc mật khẩu mới không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Token không hợp lệ hoặc đã hết hạn")
    })
    public ResponseEntity<OtpResponse> changePassword(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(authService.changePassword(authorizationHeader, request));
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
