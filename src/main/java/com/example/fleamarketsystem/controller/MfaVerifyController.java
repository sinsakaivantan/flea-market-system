package com.example.fleamarketsystem.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.repository.UserRepository;

import dev.samstevens.totp.code.CodeVerifier;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/mfa")
@RequiredArgsConstructor
public class MfaVerifyController {

    private final CodeVerifier verifier;
    private final UserRepository userRepository;
    
    // ★重要: セッションに保存するためのリポジトリを手動で用意
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @GetMapping("/verify")
    public String showVerifyPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }

        // 既にMFA完了しているならトップへ
        boolean alreadyVerified = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MFA_VERIFIED"));
        if (alreadyVerified) {
            return "redirect:/items";
        }

        return "mfa-verify";
    }

    @PostMapping("/verify")
    public String verifyCode(
            @RequestParam("de") String code, 
            HttpServletRequest request,    // ★追加
            HttpServletResponse response,  // ★追加
            RedirectAttributes redirectAttributes) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByEmail(username).orElseThrow();

        // ログ出力で確認（デバッグ用）
        System.out.println("User: " + username + " trying code: " + code);

        boolean valid = verifier.isValidCode(user.getTotpSecret(), code.trim());

        if (valid) {
            System.out.println("Code is valid! Updating security context...");

            // 1. 新しい権限リストを作成
            List<GrantedAuthority> updatedAuthorities = new ArrayList<>(auth.getAuthorities());
            updatedAuthorities.add(new SimpleGrantedAuthority("ROLE_MFA_VERIFIED"));

            // 2. 新しいAuthenticationオブジェクトを作成
            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                auth.getPrincipal(),
                auth.getCredentials(),
                updatedAuthorities
            );

            // 3. SecurityContextを更新
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(newAuth);
            SecurityContextHolder.setContext(context);

            // 4. ★ここが最重要！変更したContextをセッションに永続化する
            securityContextRepository.saveContext(context, request, response);

            return "redirect:/items";
        } else {
            System.out.println("Code is invalid.");
            redirectAttributes.addFlashAttribute("error", "コードが間違っています。");
            return "redirect:/mfa/verify";
        }
    }
}