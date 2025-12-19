package com.example.fleamarketsystem.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Account;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class StripeService {

    private final boolean isDummyConfig;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1秒

    public StripeService(@Value("${stripe.api.secretKey}") String secretKey) {
        // APIキーの前後の空白を削除
        String trimmedKey = secretKey != null ? secretKey.trim() : "";
        isDummyConfig = trimmedKey.contains("dummy");
        
        if (!isDummyConfig) {
            // APIキーの形式を検証
            if (!trimmedKey.startsWith("sk_test_") && !trimmedKey.startsWith("sk_live_")) {
                log.error("Stripe APIキーの形式が正しくありません。'sk_test_' または 'sk_live_' で始まる必要があります。");
                log.error("APIキーの先頭10文字: {}", trimmedKey.length() > 10 ? trimmedKey.substring(0, 10) : trimmedKey);
                throw new IllegalArgumentException("Invalid Stripe API key format. Must start with 'sk_test_' or 'sk_live_'");
            }
            
            Stripe.apiKey = trimmedKey;
            
            // Java HTTPクライアントのシステムプロパティを設定（Broken pipeエラー対策）
            // HTTP接続のKeep-Aliveを有効化
            System.setProperty("http.keepAlive", "true");
            System.setProperty("http.keepAlive.timeout", "30000"); // 30秒
            System.setProperty("http.maxConnections", "20");
            // HTTPS接続の設定
            System.setProperty("https.keepAlive", "true");
            System.setProperty("https.keepAlive.timeout", "30000");
            System.setProperty("https.maxConnections", "20");
            
            // Stripe API接続設定を改善
            Stripe.setConnectTimeout(60 * 1000); // 60秒に延長
            Stripe.setReadTimeout(60 * 1000); // 60秒に延長
            // HTTP接続の設定を改善（接続プールのサイズなど）
            Stripe.setMaxNetworkRetries(2); // ネットワークレベルのリトライを有効化
            log.info("Stripe APIキーを設定しました。APIキーの形式: {} (長さ: {})", 
                    trimmedKey.startsWith("sk_test_") ? "テストキー" : "本番キー",
                    trimmedKey.length());
            log.info("Stripe API接続設定: 接続タイムアウト={}秒, 読み取りタイムアウト={}秒, ネットワークリトライ={}回",
                    60, 60, 2);
            log.info("Java HTTPクライアント設定: Keep-Alive有効, タイムアウト=30秒, 最大接続数=20");
            
            // APIキーの有効性をテスト（起動時に1回だけ、非同期で実行して起動をブロックしない）
            // 注意: 起動時の検証は非同期で実行し、エラーが発生しても起動を続行
            new Thread(() -> {
                try {
                    Thread.sleep(2000); // アプリケーション起動後に実行
                    log.info("Stripe APIキーの有効性を確認中...");
                    Account account = Account.retrieve(); // アカウント情報を取得してAPIキーを検証
                    log.info("Stripe APIキーは有効です。アカウントID: {}", account.getId());
                } catch (StripeException e) {
                    log.error("Stripe APIキーの検証に失敗しました: {}", e.getMessage());
                    if (e.getMessage() != null && e.getMessage().contains("Invalid API Key")) {
                        log.error("APIキーが無効です。Stripeダッシュボードで正しいキーを確認してください。");
                    } else if (e.getMessage() != null && e.getMessage().contains("Broken pipe")) {
                        log.error("APIキーの検証中に接続エラーが発生しました。");
                        log.error("考えられる原因:");
                        log.error("1. APIキーが無効または期限切れ");
                        log.error("2. ネットワーク接続の問題（プロキシ、ファイアウォール）");
                        log.error("3. Stripe Java SDKのHTTPクライアント設定の問題");
                        log.error("対処方法:");
                        log.error("- StripeダッシュボードでAPIキーが有効か確認してください");
                        log.error("- 環境変数 STRIPE_SECRET_KEY の値を再確認してください");
                        log.error("- プロキシ設定がある場合は、Stripe Java SDKがプロキシを経由できるか確認してください");
                    } else {
                        log.warn("APIキーの検証中にエラーが発生しました: {}", e.getMessage());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.warn("APIキーの検証中に予期しないエラーが発生しました: {}", e.getMessage());
                }
            }, "Stripe-API-Key-Validation").start();
        } else {
            log.info("Stripe APIキーがダミー設定として認識されました。");
        }
    }

    /**
     * ダミー設定かどうかを確認
     */
    public boolean isDummyConfig() {
        return isDummyConfig;
    }

    public PaymentIntent createPaymentIntent(BigDecimal amount, String currency, String description) throws StripeException, RuntimeException {
        // ダミー設定の場合は例外を投げる（AppOrderServiceで処理）
        if (isDummyConfig) {
            log.warn("Stripe設定がダミーのため、PaymentIntentの作成をスキップします。");
            throw new UnsupportedOperationException("Stripe設定がダミーのため、PaymentIntentを作成できません。");
        }
        
        // リトライロジック付きでPaymentIntentを作成
        return createPaymentIntentWithRetry(amount, currency, description, 0);
    }
    
    private PaymentIntent createPaymentIntentWithRetry(BigDecimal amount, String currency, String description, int retryCount) throws StripeException, RuntimeException {
        try {
            log.info("Stripe PaymentIntent作成開始 (試行 {}/{}): amount={}, currency={}, description={}", 
                    retryCount + 1, MAX_RETRIES, amount, currency, description);
            log.debug("現在のStripe APIキー設定: {}", Stripe.apiKey != null ? 
                    (Stripe.apiKey.length() > 20 ? Stripe.apiKey.substring(0, 20) + "..." : Stripe.apiKey) : "null");
            
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
            
            log.debug("PaymentIntent作成パラメータ: amount={}, currency={}", 
                    params.getAmount(), params.getCurrency());
            
            PaymentIntent paymentIntent = PaymentIntent.create(params);
            log.info("Stripe PaymentIntent作成成功: id={}", paymentIntent.getId());
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
                log.error("Stripe APIへの接続エラーが発生しました。");
                log.error("考えられる原因:");
                log.error("1. ネットワーク接続の問題（インターネット接続、ファイアウォール、プロキシ）");
                log.error("2. Stripe APIキーが無効または形式が間違っている");
                log.error("3. Stripe APIサービスの一時的な障害");
                log.error("エラー詳細: {}", e.getMessage());
                
                // リトライ可能な場合はリトライ
                if (retryCount < MAX_RETRIES - 1) {
                    log.info("{}秒後にリトライします...", RETRY_DELAY_MS / 1000);
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("リトライ待機中に割り込みが発生しました", ie);
                    }
                    return createPaymentIntentWithRetry(amount, currency, description, retryCount + 1);
                } else {
                    log.error("最大リトライ回数に達しました。Stripe APIへの接続に失敗しました。");
                    log.error("対処方法:");
                    log.error("- 環境変数 STRIPE_SECRET_KEY が正しく設定されているか確認してください");
                    log.error("- StripeダッシュボードでAPIキーが有効か確認してください");
                    log.error("- ネットワーク接続を確認してください");
                    log.error("- Stripeのサービスステータスを確認: https://status.stripe.com/");
                    // より詳細なエラー情報を提供
                    if (e.getCause() != null) {
                        log.error("根本原因: {}", e.getCause().getMessage());
                    }
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
        if (isDummyConfig) {
            throw new UnsupportedOperationException("Stripe設定がダミーのため、PaymentIntentを確認できません。");
        }
        
        try {
        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
        return paymentIntent.confirm();
        } catch (StripeException e) {
            log.error("Stripe API呼び出しエラー: {}", e.getMessage());
            throw e;
        }
    }

    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException, RuntimeException {
        if (isDummyConfig) {
            throw new UnsupportedOperationException("Stripe設定がダミーのため、PaymentIntentを取得できません。");
        }
        
        try {
            log.info("Stripe PaymentIntent取得開始: id={}", paymentIntentId);
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            log.info("Stripe PaymentIntent取得成功: id={}, status={}", paymentIntentId, paymentIntent.getStatus());
            return paymentIntent;
        } catch (StripeException e) {
            log.error("Stripe API呼び出しエラー: type={}, message={}, code={}", 
                    e.getClass().getSimpleName(), e.getMessage(), e.getCode());
            // 接続エラーの場合は詳細な情報をログに記録
            if (e.getMessage() != null && e.getMessage().contains("Broken pipe")) {
                log.error("Stripe APIへの接続が切断されました。ネットワーク接続またはAPIキーを確認してください。");
            }
            throw e;
        } catch (Exception e) {
            log.error("Stripe API呼び出し中に予期しないエラーが発生しました: {}", e.getMessage(), e);
            // StripeExceptionは抽象クラスのため、RuntimeExceptionでラップしてStripeExceptionとして扱う
            throw new RuntimeException("予期しないエラーが発生しました: " + e.getMessage(), e);
        }
    }
}
