package com.example.fleamarketsystem.controller;

import static dev.samstevens.totp.util.Utils.*;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.repository.UserRepository;
import com.example.fleamarketsystem.service.AdminUserService;

import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.SecretGenerator;

@Controller
public class MfaSetupController {
	
	private final UserRepository userRepository;
	private final AdminUserService service;
	private final SecretGenerator secretGenerator;
	private final QrGenerator qrGenerator;
	public MfaSetupController(UserRepository userRepository, AdminUserService service,SecretGenerator secretGenerator,QrGenerator qrGenerator) {
		this.userRepository = userRepository;
		this.service = service;
		this.secretGenerator = secretGenerator;
		this.qrGenerator = qrGenerator;
	}

    


	//↓乗っ取りできるから本番環境で使わないように
    @GetMapping("/mfa/setup/{id}")
    public String setupDevice(@PathVariable Long id,Model model) throws QrGenerationException {
        // 秘密鍵を生成して保存
        String secret = secretGenerator.generate();
        User user = service.findUser(id);
        user.setTotpSecret(secret);
        String aoa = user.getEmail();
        userRepository.save(user);
        QrData data = new QrData.Builder()
            .label(aoa)
            .secret(secret)
            .issuer("🐬vantansinsakai")
            .build();

        // QRコード画像をBase64文字列として生成（<img>タグで使用可能）
        String qrCodeImage = getDataUriForImage(
          qrGenerator.generate(data), 
          qrGenerator.getImageMimeType()
        );
        model.addAttribute("qr", qrCodeImage);
        // ...
        return "qr";
    }
}
