package com.example.pricing_calculation.service;

import static com.example.pricing_calculation.dto.ExtendedAuthDtos.*;
import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.domain.UserRole;
import com.example.pricing_calculation.dto.*;
import com.example.pricing_calculation.repository.UserAccountRepository;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentModuleAuthService {
    private static final Pattern EMAIL_PATTERN=Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int TOKEN_BYTES=32, SESSION_HOURS=8, OTP_MINUTES=5, MAX_ATTEMPTS=5;
    private final UserAccountRepository users; private final PasswordHashService passwords; private final OtpDeliveryService otpDelivery;
    private final SecureRandom random=new SecureRandom();
    private final Map<String,AuthSession> sessions=new ConcurrentHashMap<>();
    private final Map<String,PendingRegistration> registrations=new ConcurrentHashMap<>();
    private final Map<String,OtpState> loginOtps=new ConcurrentHashMap<>(), resetOtps=new ConcurrentHashMap<>();
    private final Map<String,PendingPasswordChange> changePasswordOtps=new ConcurrentHashMap<>();

    public PaymentModuleAuthService(UserAccountRepository users,PasswordHashService passwords,OtpDeliveryService otpDelivery){this.users=users;this.passwords=passwords;this.otpDelivery=otpDelivery;}

    public OtpChallengeResponse requestRegistration(AuthRegistrationRequest r){validateRegistration(r);String email=email(r.email());if(users.existsByEmailIgnoreCase(email))throw new BadRequestException("Email is already registered");String otp=otp();LocalDateTime expires=LocalDateTime.now().plusMinutes(OTP_MINUTES);registrations.put(email,new PendingRegistration(r.fullName().trim(),email,r.phone(),passwords.hash(r.password()),otp,expires,0));otpDelivery.send(email,otp,"REGISTER");return challenge(email,"REGISTER",expires,otp);}
    @Transactional public AuthRegistrationResponse verifyRegistration(VerifyOtpRequest r){String email=requiredEmail(r==null?null:r.email());PendingRegistration p=registrations.get(email);verify(p==null?null:p.otp(),p==null?null:p.expiresAt(),p==null?0:p.attempts(),r==null?null:r.otp(),()->registrations.computeIfPresent(email,(k,v)->v.moreAttempts()));if(users.existsByEmailIgnoreCase(email))throw new BadRequestException("Email is already registered");UserAccount u=new UserAccount();u.setFullName(p.fullName());u.setEmail(email);u.setPhone(p.phone());u.setPasswordHash(p.passwordHash());u.setStatus("ACTIVE");u.setRole(UserRole.PARKING_USER.code());u.setCreatedAt(LocalDateTime.now());registrations.remove(email);return registrationResponse(users.save(u),"Registration completed");}

    @Transactional
    public AuthLoginResponse verifyOtp(VerifyOtpRequest r) {
        String email = requiredEmail(r == null ? null : r.email());
        if (registrations.containsKey(email)) {
            PendingRegistration p = registrations.get(email);
            verify(p == null ? null : p.otp(), p == null ? null : p.expiresAt(), p == null ? 0 : p.attempts(), r == null ? null : r.otp(), () -> registrations.computeIfPresent(email, (k, v) -> v.moreAttempts()));
            if (users.existsByEmailIgnoreCase(email)) throw new BadRequestException("Email is already registered");
            UserAccount u = new UserAccount();
            u.setFullName(p.fullName());
            u.setEmail(email);
            u.setPhone(p.phone());
            u.setPasswordHash(p.passwordHash());
            u.setStatus("ACTIVE");
            u.setRole(UserRole.PARKING_USER.code());
            u.setCreatedAt(LocalDateTime.now());
            registrations.remove(email);
            return issue(users.save(u));
        } else if (loginOtps.containsKey(email)) {
            OtpState state = loginOtps.get(email);
            verify(state == null ? null : state.otp(), state == null ? null : state.expiresAt(), state == null ? 0 : state.attempts(), r == null ? null : r.otp(), () -> loginOtps.computeIfPresent(email, (k, v) -> v.moreAttempts()));
            UserAccount u = users.findById(state.userId()).orElseThrow(() -> new UnauthorizedException("User not found"));
            loginOtps.remove(email);
            return issue(u);
        } else if (changePasswordOtps.containsKey(email)) {
            PendingPasswordChange p = changePasswordOtps.get(email);
            verify(p == null ? null : p.otp(), p == null ? null : p.expiresAt(), p == null ? 0 : p.attempts(), r == null ? null : r.otp(), () -> changePasswordOtps.computeIfPresent(email, (k, v) -> v.moreAttempts()));
            UserAccount u = users.findById(p.userId()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
            u.setPasswordHash(p.newPasswordHash());
            UserAccount saved = users.save(u);
            changePasswordOtps.remove(email);
            revokeUser(u.getId());
            return issue(saved);
        } else {
            throw new BadRequestException("OTP challenge not found");
        }
    }

    public OtpChallengeResponse requestLogin(AuthLoginRequest r){UserAccount u=credentials(r);String otp=otp();LocalDateTime expires=LocalDateTime.now().plusMinutes(OTP_MINUTES);loginOtps.put(u.getEmail(),new OtpState(u.getId(),otp,expires,0));otpDelivery.send(u.getEmail(),otp,"LOGIN");return challenge(u.getEmail(),"LOGIN",expires,otp);}
    @Transactional(readOnly=true) public AuthLoginResponse verifyLogin(VerifyOtpRequest r){String email=requiredEmail(r==null?null:r.email());OtpState state=loginOtps.get(email);verify(state==null?null:state.otp(),state==null?null:state.expiresAt(),state==null?0:state.attempts(),r==null?null:r.otp(),()->loginOtps.computeIfPresent(email,(k,v)->v.moreAttempts()));UserAccount u=users.findById(state.userId()).orElseThrow(()->new UnauthorizedException("User not found"));loginOtps.remove(email);return issue(u);}

    @Transactional public AuthRegistrationResponse registerDirect(AuthRegistrationRequest r){validateRegistration(r);String email=email(r.email());if(users.existsByEmailIgnoreCase(email))throw new BadRequestException("Email is already registered");UserAccount u=new UserAccount();u.setFullName(r.fullName());u.setEmail(email);u.setPhone(r.phone());u.setPasswordHash(passwords.hash(r.password()));u.setStatus("ACTIVE");u.setRole(UserRole.PARKING_USER.code());u.setCreatedAt(LocalDateTime.now());return registrationResponse(users.save(u),"Registration completed");}
    @Transactional(readOnly=true) public AuthLoginResponse loginDirect(AuthLoginRequest r){return issue(credentials(r));}

    public OtpChallengeResponse forgotPassword(ForgotPasswordRequest r){String email=requiredEmail(r==null?null:r.email());UserAccount u=users.findByEmailIgnoreCase(email).orElseThrow(()->new ResourceNotFoundException("Email is not registered"));String otp=otp();LocalDateTime expires=LocalDateTime.now().plusMinutes(OTP_MINUTES);resetOtps.put(email,new OtpState(u.getId(),otp,expires,0));otpDelivery.send(email,otp,"RESET_PASSWORD");return challenge(email,"RESET_PASSWORD",expires,otp);}
    @Transactional public AuthLogoutResponse resetPassword(ResetPasswordRequest r){validatePassword(r==null?null:r.newPassword());String email=requiredEmail(r==null?null:r.email());OtpState state=resetOtps.get(email);verify(state==null?null:state.otp(),state==null?null:state.expiresAt(),state==null?0:state.attempts(),r==null?null:r.otp(),()->resetOtps.computeIfPresent(email,(k,v)->v.moreAttempts()));UserAccount u=users.findById(state.userId()).orElseThrow(()->new ResourceNotFoundException("User not found"));u.setPasswordHash(passwords.hash(r.newPassword()));users.save(u);resetOtps.remove(email);revokeUser(u.getId());return new AuthLogoutResponse("Password reset completed");}
    @Transactional
    public OtpChallengeResponse changePassword(String header,ChangePasswordRequest r){
        UserAccount u=authenticate(header);
        if(r==null||!passwords.matches(r.currentPassword(),u.getPasswordHash()))throw new UnauthorizedException("Current password is incorrect");
        validatePassword(r.newPassword());
        if(passwords.matches(r.newPassword(),u.getPasswordHash()))throw new BadRequestException("New password must be different");
        String otp=otp();
        LocalDateTime expires=LocalDateTime.now().plusMinutes(OTP_MINUTES);
        changePasswordOtps.put(u.getEmail(),new PendingPasswordChange(u.getId(),passwords.hash(r.newPassword()),otp,expires,0));
        otpDelivery.send(u.getEmail(),otp,"CHANGE_PASSWORD");
        return challenge(u.getEmail(),"CHANGE_PASSWORD",expires,otp);
    }

    @Transactional(readOnly=true) public UserProfileResponse profile(String header){return profile(authenticate(header));}
    @Transactional public UserProfileResponse updateProfile(String header,UpdateProfileRequest r){UserAccount u=authenticate(header);if(r!=null&&r.fullName()!=null&&!r.fullName().isBlank())u.setFullName(r.fullName());if(r!=null&&r.phone()!=null)u.setPhone(r.phone());return profile(users.save(u));}
    @Transactional public AuthLoginResponse loginWithGoogle(String email,String fullName){String normalized=requiredEmail(email);UserAccount u=users.findByEmailIgnoreCase(normalized).orElseGet(()->{UserAccount n=new UserAccount();n.setEmail(normalized);n.setFullName(fullName==null||fullName.isBlank()?normalized:fullName);n.setPasswordHash(passwords.hash(generateToken()));n.setStatus("ACTIVE");n.setRole(UserRole.PARKING_USER.code());n.setCreatedAt(LocalDateTime.now());return users.save(n);});if(!"ACTIVE".equalsIgnoreCase(u.getStatus()))throw new UnauthorizedException("Account is not active");return issue(u);}

    public AuthLogoutResponse logout(String header){String token=extract(header);AuthSession removed=sessions.remove(hashToken(token));if(removed==null)throw new UnauthorizedException("Invalid or expired access token");return new AuthLogoutResponse("Logout completed");}
    @Transactional(readOnly=true) public UserAccount authenticate(String header){String token=extract(header);AuthSession s=sessions.get(hashToken(token));if(s==null||s.expiresAt().isBefore(LocalDateTime.now()))throw new UnauthorizedException("Invalid or expired access token");return users.findById(s.userId()).orElseThrow(()->new UnauthorizedException("Invalid or expired access token"));}
    @Transactional(readOnly=true) public UserAccount requireAnyRole(String h,UserRole...roles){UserAccount u=authenticate(h);if(!Set.of(roles).contains(UserRole.fromCode(u.getRole())))throw new ForbiddenException("Access denied for role "+u.getRole());return u;}

    private UserAccount credentials(AuthLoginRequest r){if(r==null)throw new UnauthorizedException("Invalid email or password");UserAccount u=users.findByEmailIgnoreCase(email(r.email())).orElseThrow(()->new UnauthorizedException("Invalid email or password"));if(!"ACTIVE".equalsIgnoreCase(u.getStatus())||!passwords.matches(r.password(),u.getPasswordHash()))throw new UnauthorizedException("Invalid email or password");return u;}
    private AuthLoginResponse issue(UserAccount u){purge();String raw=generateToken();LocalDateTime expires=LocalDateTime.now().plusHours(SESSION_HOURS);sessions.put(hashToken(raw),new AuthSession(u.getId(),expires));return new AuthLoginResponse(raw,"Bearer",expires,u.getId(),u.getFullName(),u.getEmail(),UserRole.fromCode(u.getRole()).code());}
    private void validateRegistration(AuthRegistrationRequest r){if(r==null||r.fullName()==null||r.fullName().trim().length()<2)throw new BadRequestException("fullName is required");requiredEmail(r.email());validatePassword(r.password());}
    private void validatePassword(String p){
        if(p==null||p.length()<8||p.length()>128)throw new BadRequestException("Mật khẩu phải chứa từ 8 đến 128 ký tự");
        boolean hasUppercase = p.chars().anyMatch(Character::isUpperCase);
        boolean hasLowercase = p.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = p.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = p.chars().anyMatch(c -> !Character.isLetterOrDigit(c));
        if(!hasUppercase)throw new BadRequestException("Mật khẩu bắt buộc phải có ít nhất 1 chữ cái viết hoa");
        if(!hasSpecial)throw new BadRequestException("Mật khẩu bắt buộc phải có ít nhất 1 kí tự đặc biệt");
        if(!hasDigit||!(hasUppercase||hasLowercase))throw new BadRequestException("Mật khẩu phải có cả số và chữ");
    }
    private void verify(String expected,LocalDateTime expires,int attempts,String actual,Runnable fail){if(expected==null||expires==null)throw new BadRequestException("OTP challenge not found");if(expires.isBefore(LocalDateTime.now()))throw new BadRequestException("OTP has expired");if(attempts>=MAX_ATTEMPTS)throw new BadRequestException("Too many invalid OTP attempts");if(actual==null||!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),actual.trim().getBytes(StandardCharsets.UTF_8))){fail.run();throw new BadRequestException("Invalid OTP");}}
    private OtpChallengeResponse challenge(String email,String purpose,LocalDateTime expires,String otp){return new OtpChallengeResponse(email,purpose,expires,"OTP sent",otpDelivery.developmentOtp(otp));}
    private AuthRegistrationResponse registrationResponse(UserAccount u,String message){return new AuthRegistrationResponse(u.getId(),u.getFullName(),u.getEmail(),u.getPhone(),u.getStatus(),UserRole.fromCode(u.getRole()).code(),message);}
    private UserProfileResponse profile(UserAccount u){return new UserProfileResponse(u.getId(),u.getFullName(),u.getEmail(),u.getPhone(),u.getStatus(),UserRole.fromCode(u.getRole()).code());}
    private String requiredEmail(String v){String e=email(v);if(e.length()>100||!EMAIL_PATTERN.matcher(e).matches())throw new BadRequestException("A valid email is required");return e;}
    private String email(String v){return v==null?"":v.trim().toLowerCase(Locale.ROOT);}
    private String otp(){return String.format("%06d",random.nextInt(1_000_000));}
    private String generateToken(){byte[]b=new byte[TOKEN_BYTES];random.nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
    private String hashToken(String t){try{return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(t.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private String extract(String h){if(h==null||!h.regionMatches(true,0,"Bearer ",0,7))throw new UnauthorizedException("Bearer access token is required");String t=h.substring(7).trim();if(t.isEmpty())throw new UnauthorizedException("Bearer access token is required");return t;}
    private void purge(){LocalDateTime n=LocalDateTime.now();sessions.entrySet().removeIf(e->e.getValue().expiresAt().isBefore(n));registrations.entrySet().removeIf(e->e.getValue().expiresAt().isBefore(n));loginOtps.entrySet().removeIf(e->e.getValue().expiresAt().isBefore(n));resetOtps.entrySet().removeIf(e->e.getValue().expiresAt().isBefore(n));changePasswordOtps.entrySet().removeIf(e->e.getValue().expiresAt().isBefore(n));}
    private void revokeUser(Long id){sessions.entrySet().removeIf(e->e.getValue().userId().equals(id));}
    private record AuthSession(Long userId,LocalDateTime expiresAt){}
    private record OtpState(Long userId,String otp,LocalDateTime expiresAt,int attempts){OtpState moreAttempts(){return new OtpState(userId,otp,expiresAt,attempts+1);}}
    private record PendingRegistration(String fullName,String email,String phone,String passwordHash,String otp,LocalDateTime expiresAt,int attempts){PendingRegistration moreAttempts(){return new PendingRegistration(fullName,email,phone,passwordHash,otp,expiresAt,attempts+1);}}
    private record PendingPasswordChange(Long userId, String newPasswordHash, String otp, LocalDateTime expiresAt, int attempts) {PendingPasswordChange moreAttempts() {return new PendingPasswordChange(userId, newPasswordHash, otp, expiresAt, attempts + 1);}}
}
