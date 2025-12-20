package com.example.fleamarketsystem.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class StripeService {

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1秒

    public StripeService(@Value("${stripe.api.secretKey}") String secretKey) {
        // APIキーの前後の空白を削除
        String trimmedKey = secretKey != null ? secretKey.trim() : "";
        
        // APIキーの形式を検証
        if (!trimmedKey.startsWith("sk_test_") && !trimmedKey.startsWith("sk_live_")) {
            log.error("Stripe APIキーの形式が正しくありません。'sk_test_' または 'sk_live_' で始まる必要があります。");
            log.error("APIキーの先頭10文字: {}", trimmedKey.length() > 10 ? trimmedKey.substring(0, 10) : trimmedKey);
            throw new IllegalArgumentException("Invalid Stripe API key format. Must start with 'sk_test_' or 'sk_live_'");
        }
        
        Stripe.apiKey = trimmedKey;
        
        // Java HTTPクライアントのシステムプロパティを設定（Broken pipeエラー対策）
        System.setProperty("http.keepAlive", "true");
        System.setProperty("http.keepAlive.timeout", "30000");
        System.setProperty("http.maxConnections", "20");
        System.setProperty("https.keepAlive", "true");
        System.setProperty("https.keepAlive.timeout", "30000");
        System.setProperty("https.maxConnections", "20");
        
        // Stripe API接続設定
        Stripe.setConnectTimeout(60 * 1000);
        Stripe.setReadTimeout(60 * 1000);
        Stripe.setMaxNetworkRetries(2);
    }

    public PaymentIntent createPaymentIntent(BigDecimal amount, String currency, String description) throws StripeException, RuntimeException {
        // リトライロジック付きでPaymentIntentを作成
        return createPaymentIntentWithRetry(amount, currency, description, 0);
    }
    
    private PaymentIntent createPaymentIntentWithRetry(BigDecimal amount, String currency, String description, int retryCount) throws StripeException, RuntimeException {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amount.multiply(new BigDecimal(100)).longValue()) // Amount in cents
                    .setCurrency(currency.toLowerCase()) // 小文字に変換
                    .setDescription(description)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .build();
            
            PaymentIntent paymentIntent = PaymentIntent.create(params);
            return paymentIntent;
        } catch (StripeException e) {
            log.error("Stripe API呼び出しエラー (試行 {}/{}): type={}, message={}, code={}", 
                    retryCount + 1, MAX_RETRIES, e.getClass().getSimpleName(), e.getMessage(), e.getCode());
            
            // 接続エラーの場合は詳細な情報をログに記録
            boolean isConnectionError = e.getMessage() != null && 
                    (e.getMessage().contains("Broken pipe") || 
                     e.getMessage().contains("Connection") ||
                     e.getMessage().contains("timeout") ||
                     e.getMessage().contains("IOException"));
            
            if (isConnectionError) {
                // リトライ可能な場合はリトライ
                if (retryCount < MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("リトライ待機中に割り込みが発生しました", ie);
                    }
                    return createPaymentIntentWithRetry(amount, currency, description, retryCount + 1);
                } else {
                    log.error("最大リトライ回数に達しました。Stripe APIへの接続に失敗しました。");
                }
            }
            throw e;
        } catch (Exception e) {
            log.error("Stripe API呼び出し中に予期しないエラーが発生しました: {}", e.getMessage(), e);
            // StripeExceptionは抽象クラスのため、RuntimeExceptionでラップしてStripeExceptionとして扱う
            throw new RuntimeException("予期しないエラーが発生しました: " + e.getMessage(), e);
        }
    }

    public PaymentIntent confirmPaymentIntent(String paymentIntentId) throws StripeException {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            return paymentIntent.confirm();
        } catch (StripeException e) {
            log.error("Stripe API呼び出しエラー: {}", e.getMessage());
            throw e;
        }
    }

    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException, RuntimeException {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            return paymentIntent;
        } catch (StripeException e) {
            log.error("Stripe API呼び出しエラー: type={}, message={}, code={}", 
                    e.getClass().getSimpleName(), e.getMessage(), e.getCode());
            throw e;
        } catch (Exception e) {
            log.error("Stripe API呼び出し中に予期しないエラーが発生しました: {}", e.getMessage(), e);
            // StripeExceptionは抽象クラスのため、RuntimeExceptionでラップしてStripeExceptionとして扱う
            throw new RuntimeException("予期しないエラーが発生しました: " + e.getMessage(), e);
        }
    }
}
