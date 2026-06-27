package com.example.pricing_calculation.config;

import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.domain.UserRole;
import com.example.pricing_calculation.service.PaymentModuleAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class BearerTokenFilter extends OncePerRequestFilter {
    private final PaymentModuleAuthService authService;
    public BearerTokenFilter(PaymentModuleAuthService authService){this.authService=authService;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        String header=request.getHeader("Authorization");
        if(header!=null&&header.startsWith("Bearer ")){
            try{
                UserAccount user=authService.authenticate(header);
                String role=UserRole.fromCode(user.getRole()).code();
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user.getId(),null,List.of(new SimpleGrantedAuthority("ROLE_"+role))));
            }catch(RuntimeException ignored){SecurityContextHolder.clearContext();}
        }
        chain.doFilter(request,response);
    }
}
