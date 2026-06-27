package com.example.pricing_calculation.web;

import static com.example.pricing_calculation.dto.ExtendedAuthDtos.*;
import com.example.pricing_calculation.dto.*;
import com.example.pricing_calculation.service.PaymentModuleAuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api/auth")
@Tag(name="Authentication")
public class PaymentModuleAuthController {
    private final PaymentModuleAuthService auth;
    public PaymentModuleAuthController(PaymentModuleAuthService auth){this.auth=auth;}

    @PostMapping("/register") public OtpChallengeResponse register(@RequestBody AuthRegistrationRequest r){return auth.requestRegistration(r);}
    @PostMapping("/register/verify") public ResponseEntity<AuthRegistrationResponse> verifyRegistration(@RequestBody VerifyOtpRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(auth.verifyRegistration(r));}
    @PostMapping("/register/direct") public ResponseEntity<AuthRegistrationResponse> registerDirect(@RequestBody AuthRegistrationRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(auth.registerDirect(r));}

    @PostMapping("/login") public OtpChallengeResponse login(@RequestBody AuthLoginRequest r){return auth.requestLogin(r);}
    @PostMapping("/login/verify") public AuthLoginResponse verifyLogin(@RequestBody VerifyOtpRequest r){return auth.verifyLogin(r);}
    @PostMapping("/login/direct") public AuthLoginResponse loginDirect(@RequestBody AuthLoginRequest r){return auth.loginDirect(r);}

    @PostMapping("/forgot-password") public OtpChallengeResponse forgotPassword(@RequestBody ForgotPasswordRequest r){return auth.forgotPassword(r);}
    @PostMapping("/reset-password") public AuthLogoutResponse resetPassword(@RequestBody ResetPasswordRequest r){return auth.resetPassword(r);}
    @PostMapping("/change-password") @SecurityRequirement(name="bearerAuth") public AuthLogoutResponse changePassword(@RequestHeader("Authorization")String h,@RequestBody ChangePasswordRequest r){return auth.changePassword(h,r);}
    @GetMapping("/me") @SecurityRequirement(name="bearerAuth") public UserProfileResponse me(@RequestHeader("Authorization")String h){return auth.profile(h);}
    @PatchMapping("/profile") @SecurityRequirement(name="bearerAuth") public UserProfileResponse updateProfile(@RequestHeader("Authorization")String h,@RequestBody UpdateProfileRequest r){return auth.updateProfile(h,r);}
    @PostMapping("/logout") @SecurityRequirement(name="bearerAuth") public AuthLogoutResponse logout(@RequestHeader("Authorization")String h){return auth.logout(h);}

    @GetMapping("/google") public RedirectView google(){return new RedirectView("/oauth2/authorization/google");}
    @GetMapping("/oauth2/success") public AuthLoginResponse googleSuccess(@AuthenticationPrincipal OAuth2User principal){if(principal==null)throw new com.example.pricing_calculation.service.UnauthorizedException("Google authentication is not available");return auth.loginWithGoogle(principal.getAttribute("email"),principal.getAttribute("name"));}
}
