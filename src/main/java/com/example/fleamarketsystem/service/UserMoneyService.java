package com.example.fleamarketsystem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.entity.UserMoneys;
import com.example.fleamarketsystem.repository.UserMoneysRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserMoneyService {

    private final UserMoneysRepository moneysRepository;

    /**
     * ユーザーの財布を取得する。
     * データが存在しない場合は新規作成する（初期化）。
     * これを使えば NullPointerException を防げます。
     */
    @Transactional
    public UserMoneys getWallet(User user) {
        return moneysRepository.findByUser(user).orElseGet(() -> {
            UserMoneys newWallet = new UserMoneys();
            newWallet.setUser(user);
            newWallet.setSingularityToken(0); // 初期値 (Entityの名前に合わせました)
            newWallet.setFleaCoin(0);         // 初期値
            return moneysRepository.save(newWallet);
        });
    }

    /**
     * 現在の所持金（シンギュラリティトークン）を取得
     */
    public Integer getBalance(User user) {
        // getMoney() ではなく getSingularityToken() に修正
        return getWallet(user).getSingularityToken();
    }

    /**
     * お金（シンギュラリティトークン）を使う（減算）
     */
    @Transactional
    public void spend(User user, Integer amount) {
        UserMoneys wallet = getWallet(user);
        
        // Entityの名前に合わせて修正
        if (wallet.getSingularityToken() == null || wallet.getSingularityToken() < amount) {
            throw new RuntimeException("シンギュラリティトークンが足りません！");
        }
        
        wallet.setSingularityToken(wallet.getSingularityToken() - amount);
        moneysRepository.save(wallet);
    }

    /**
     * お金（シンギュラリティトークン）を増やす（加算）
     * ※ゲームクリア時などに使用
     */
    @Transactional
    public void add(User user, Integer amount) {
        UserMoneys wallet = getWallet(user);
        // Entityの名前に合わせて修正
        int current = wallet.getSingularityToken() == null ? 0 : wallet.getSingularityToken();
        wallet.setSingularityToken(current + amount);
        moneysRepository.save(wallet);
    }
    
    // --- フリマコイン関連 ---

    /**
     * フリマコインを加算するメソッド
     */
    @Transactional
    public void addFleaCoin(User user, int amount) {
        UserMoneys wallet = getWallet(user);
        
        // nullケア
        int currentCoin = wallet.getFleaCoin() == null ? 0 : wallet.getFleaCoin();
        
        wallet.setFleaCoin(currentCoin + amount);
        moneysRepository.save(wallet);
    }
    
    /**
     * フリマコイン残高取得
     */
    public Integer getFleaCoin(User user) {
        UserMoneys wallet = getWallet(user);
        return wallet.getFleaCoin() == null ? 0 : wallet.getFleaCoin();
    }
}