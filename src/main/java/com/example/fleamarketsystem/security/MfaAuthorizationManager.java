package com.example.fleamarketsystem.security;

import java.util.function.Supplier;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MfaAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final UserRepository userRepository;

    /**
     * エラーログ "must implement ... authorize(Supplier<? extends Authentication> ...)" に完全対応。
     * ジェネリクスのワイルドカード "? extends" を追加しました。
     */
    @Override
    public AuthorizationDecision authorize(Supplier<? extends Authentication> authentication, RequestAuthorizationContext object) {
        
        // Supplier<? extends Authentication> から get() すると Authentication 型で受け取れます
        Authentication auth = authentication.get();

        // 1. ログインしていない(null または anonymousUser)なら拒否
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return new AuthorizationDecision(false);
        }

        // 2. ユーザー情報をDBから取得
        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElse(null);

        // ユーザーが存在しない場合は拒否
        if (user == null) {
            return new AuthorizationDecision(false);
        }

        // 3. MFAが無効なユーザーなら許可(true)
        if (!user.isMfaEnabled()) {
            return new AuthorizationDecision(true);
        }

        // 4. MFA有効ユーザーの場合、「MFA完了権限(ROLE_MFA_VERIFIED)」を持っているか確認
        boolean isMfaVerified = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MFA_VERIFIED"));

        return new AuthorizationDecision(isMfaVerified);
    }
}