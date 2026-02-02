package com.example.fleamarketsystem.config;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder; // 追加
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;


import com.example.fleamarketsystem.entity.Ban;
import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.repository.BanRepository;
import com.example.fleamarketsystem.repository.UserRepository;
import com.example.fleamarketsystem.security.MfaAuthorizationManager;
import com.example.fleamarketsystem.service.LoginStampService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;
    private final BanRepository banRepository; 
    private final MfaAuthorizationManager mfaAuthorizationManager;
    private final LoginStampService loginStampService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/login", "/register", "/css/**", "/js/**", "/error",
                    "/banned", "/a", "/igimousitate/**" 
                ).permitAll()
                .requestMatchers("/mfa/**").authenticated()
                .requestMatchers("/admin/**").access((authentication, context) -> {
                    boolean isAdmin = authentication.get().getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                    if (!isAdmin) return new AuthorizationDecision(false);
                    return mfaAuthorizationManager.authorize(authentication, context);
                })
                .anyRequest().access(mfaAuthorizationManager)
            )
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(customSuccessHandler())
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.sendRedirect("/mfa/verify");
                })
            );
        return http.build();
    }
    private AuthenticationSuccessHandler customSuccessHandler() {

        return (request, response, authentication) -> {
            String username = authentication.getName();
            User user = userRepository.findByEmail(username).orElseThrow();
            HttpSession session = request.getSession();
            if (user.isBanned()) {
                session.setAttribute("aoaoa", user);
                SecurityContextHolder.clearContext();
                session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
                
                response.sendRedirect("/a");
                return;
            }
            Optional<Ban> banOpt = banRepository.findTopByUserIdOrderByEndDesc(user);
            if (banOpt.isPresent()) {
                Ban ban = banOpt.get();
                LocalDateTime now = LocalDateTime.now();
                if (now.isBefore(ban.getEnd())) {
                    session.setAttribute("aoaoa", user);
                    SecurityContextHolder.clearContext();
                    session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
                    
                    String reason = URLEncoder.encode("解除日時: " + ban.getEnd(), StandardCharsets.UTF_8);
                    response.sendRedirect("/banned?reason=" + reason);
                    return;
                }
            }
            loginStampService.recordLogin(user);
            if (user.isMfaEnabled()) {
                response.sendRedirect("/mfa/verify");
            } else {
                response.sendRedirect("/items");
            }
        };
    }
}

