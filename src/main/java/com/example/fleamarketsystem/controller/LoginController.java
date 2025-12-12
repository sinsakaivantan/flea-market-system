package com.example.fleamarketsystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.repository.UserRepository;

@Controller
public class LoginController {
	@Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    
	@GetMapping("/login")
	public String login() {
		return "login"; // templates/login.html
	}
	@GetMapping("/createac")
	public String createac(Model model) {
		model.addAttribute("user", new User());
		return "createac";
	}

	@PostMapping("/createac")
	public String ac(
			@ModelAttribute User user,
	    @RequestParam String passwordConfirm,
	    Model model) {

	// 入力チェック
	if (!user.getPassword().equals(passwordConfirm)) {
	    model.addAttribute("error", "パスワードが一致しません");
	    return "createac";
	}
	if (userRepository.findByEmail(user.getEmail()).isPresent()) {
	    model.addAttribute("error", "このメールアドレスは既に登録されています");
	    return "createac";
	}

	// パスワードをBCryptで暗号化して保存
	user.setPassword(passwordEncoder.encode(user.getPassword()));
	userRepository.save(user);

	// 登録成功 → ログイン画面へ
	return "redirect:/login?registered";
	}

}
