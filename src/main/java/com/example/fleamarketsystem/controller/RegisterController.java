package com.example.fleamarketsystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RegisterController {

	private final UserService userService;

	@GetMapping("/register")
	public String registerForm() {
		return "register";
	}

	@PostMapping("/register")
	public String register(
			@RequestParam String name,
			@RequestParam String email,
			@RequestParam String password,
			@RequestParam String passwordConfirm,
			Model model) {

		// バリデーション
		if (name.trim().isEmpty()) {
			model.addAttribute("error", "名前を入力してください。");
			return "register";
		}

		if (email.trim().isEmpty()) {
			model.addAttribute("error", "メールアドレスを入力してください。");
			return "register";
		}

		if (password.isEmpty()) {
			model.addAttribute("error", "パスワードを入力してください。");
			return "register";
		}

		if (!password.equals(passwordConfirm)) {
			model.addAttribute("error", "パスワードが一致しません。");
			return "register";
		}

		// メールアドレスの正規化と重複チェック
		String normalizedEmail = email.trim().toLowerCase();
		if (userService.getUserByEmail(normalizedEmail).isPresent()) {
			model.addAttribute("error", "このメールアドレスは既に登録されています。");
			return "register";
		}

		// ユーザー作成
		User user = new User();
		user.setName(name.trim());
		user.setEmail(normalizedEmail);
		// パスワードをエンコード（{noop}プレフィックスを使用）
		user.setPassword("{noop}" + password);
		user.setRole("USER");
		user.setEnabled(true);
		user.setBanned(false);

		try {
			userService.saveUser(user);
			model.addAttribute("success", "登録が完了しました。ログインページからログインしてください。");
			return "register";
		} catch (Exception e) {
			model.addAttribute("error", "登録に失敗しました: " + e.getMessage());
			return "register";
		}
	}
}

