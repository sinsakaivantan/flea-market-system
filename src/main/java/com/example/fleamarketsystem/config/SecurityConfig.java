package com.example.fleamarketsystem.config;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.example.fleamarketsystem.entity.Ban;
import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.repository.BanRepository;
import com.example.fleamarketsystem.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

	private final UserDetailsService userDetailsService;
	private final BanRepository banRepository; 
	private final UserRepository userRepository;


	@Bean
	public PasswordEncoder passwordEncoder() {
		// {bcrypt},{noop} など委譲エンコーダ
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
		authProvider.setUserDetailsService(userDetailsService);
		authProvider.setPasswordEncoder(passwordEncoder());
		return new ProviderManager(authProvider);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/login",
								"/register",
								"/css/**", "/js/**", "/images/**", "/webjars/**",
								"/banned")
						.permitAll()
						.requestMatchers("/admin/**").hasRole("ADMIN")
						.anyRequest().authenticated())
				.formLogin(form -> form
						.loginPage("/login")
						.usernameParameter("username")  // メールアドレスを使用
						.passwordParameter("password")
						.successHandler(customSuccessHandler()) 
						//.defaultSuccessUrl("/items", true) // ログイン成功後
						.permitAll())
				.logout(logout -> logout
						.logoutUrl("/logout") // POST /logout
						.logoutSuccessUrl("/login?logout")
						.permitAll())
				.csrf(Customizer.withDefaults());

		return http.build();
	}
	//これ一時利用停止されてるかどうかのチェックね。
	private AuthenticationSuccessHandler customSuccessHandler() {
        return (request, response, authentication) -> {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();
            User uo = userRepository.findByEmail(username)
            		.orElseThrow(() -> new IllegalStateException("No order found for payment intent: " ));
            Optional<Ban> banOpt = banRepository.findTopByUserIdOrderByEndDesc(uo);
            if (banOpt.isPresent()) {
                Ban ban = banOpt.get();
                LocalDateTime now = LocalDateTime.now();

                if (now.isBefore(ban.getEnd()) || now.isEqual(ban.getEnd())) {
                    String reason = URLEncoder.encode("アカウントが一時停止中です。解除日時: " + ban.getEnd(), StandardCharsets.UTF_8);
                    response.sendRedirect("/banned?reason=" + reason);
                    return;
                }
            }
            response.sendRedirect("/items");
        };
    }
}
