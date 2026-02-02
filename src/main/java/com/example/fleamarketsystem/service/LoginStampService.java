package com.example.fleamarketsystem.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fleamarketsystem.entity.LoginStamp;
import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.repository.LoginStampRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginStampService {

    private final LoginStampRepository loginStampRepository;

    // ログイン時に呼び出すメソッド
    @Transactional
    public void recordLogin(User user) {
        LocalDate today = LocalDate.now();
        // 今日まだスタンプがなければ保存
        if (!loginStampRepository.existsByUserAndStampDate(user, today)) {
            LoginStamp stamp = new LoginStamp();
            stamp.setUser(user);
            stamp.setStampDate(today);
            loginStampRepository.save(stamp);
        }
    }

    // ユーザーのスタンプ日付リストを取得（API用）
    public List<String> getStampDates(User user) {
        return loginStampRepository.findByUserOrderByStampDateAsc(user).stream()
                .map(stamp -> stamp.getStampDate().toString()) // "yyyy-MM-dd"
                .collect(Collectors.toList());
    }
}