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
    public String banned(@RequestParam(required = false) String reason, Model model) {
        model.addAttribute("reason", reason != null ? URLDecoder.decode(reason, StandardCharsets.UTF_8) : "BANされています。");
        return "banned";  // banned.html テンプレート
    }
}
