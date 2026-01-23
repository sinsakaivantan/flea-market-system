// src/main/java/com/example/fleamarketsystem/security/CustomUserDetailsService.java
package com.example.fleamarketsystem.security;

import java.util.List;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository users;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// usernameParameter("email") にしているので username はメール
		User u = users.findByEmailIgnoreCase(username)
				.orElseThrow(() -> {
					log.error("User not found: {}", username);
					return new UsernameNotFoundException("User not found: " + username);
				});

		if (!u.isEnabled()) {
			log.warn("Account disabled for user: {}", username);
			throw new DisabledException("Account disabled");
		}
		if (u.isBanned()) {
			log.warn("Account banned for user: {}", username);
			throw new DisabledException("Account banned");
		}

		return new org.springframework.security.core.userdetails.User(
				u.getEmail(),
				u.getPassword(),
				List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole())));
	}
}
