package com.example.fleamarketsystem.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.service.UserService;

@ControllerAdvice
public class GlobalControllerAdvice {

	private final UserService userService;

	public GlobalControllerAdvice(UserService userService) {
		this.userService = userService;
	}

	@ModelAttribute("currentUser")
	public User getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
		if (userDetails != null) {
			return userService.getUserByEmail(userDetails.getUsername()).orElse(null);
		}
		return null;
	}

	@ModelAttribute("currentUserNameDisplay")
	public String getCurrentUserNameDisplay(@AuthenticationPrincipal UserDetails userDetails) {
		if (userDetails != null) {
			User user = userService.getUserByEmail(userDetails.getUsername()).orElse(null);
			if (user != null) {
				String name = user.getName();
				if (name.length() > 6) {
					return name.substring(0, 6) + "...";
				}
				return name;
			}
		}
		return null;
	}
}
