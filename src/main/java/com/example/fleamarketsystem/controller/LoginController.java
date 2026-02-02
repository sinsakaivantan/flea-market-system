package com.example.fleamarketsystem.controller;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {
	@GetMapping("/login")
	public String login() {
		return "login"; // templates/login.html
	}
	@GetMapping("/banned")
	public String banned(@RequestParam(required = false) String reason,
			@RequestParam(value = "permanent", required = false) String permanent, Model model) {
		boolean isPermanent = "1".equals(permanent);
		model.addAttribute("permanent", isPermanent);
		model.addAttribute("reason", reason != null ? URLDecoder.decode(reason, StandardCharsets.UTF_8) : null);
		return "banned";
	}
}
