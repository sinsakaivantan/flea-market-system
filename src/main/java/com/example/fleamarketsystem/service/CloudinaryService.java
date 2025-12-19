package com.example.fleamarketsystem.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final boolean isDummyConfig;
    private Path uploadDir;

    public CloudinaryService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {
        // ダミー設定かどうかを判定（"dummy"が含まれているか、空文字列の場合）
        isDummyConfig = cloudName.contains("dummy") || apiKey.contains("dummy") || apiSecret.contains("dummy")
                || cloudName.isEmpty() || apiKey.isEmpty() || apiSecret.isEmpty();
        
        if (!isDummyConfig) {
            // Cloudinary APIを使用する場合
            cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret));
            log.info("=== Cloudinary APIを使用します ===");
            log.info("Cloud Name: {}", cloudName);
            log.info("API Key: {}...{}", apiKey.substring(0, Math.min(10, apiKey.length())), 
                    apiKey.length() > 10 ? apiKey.substring(apiKey.length() - 4) : "");
            uploadDir = null;
        } else {
            // ダミー設定の場合はローカル保存用ディレクトリを作成
            log.warn("=== Cloudinary APIがダミー設定として認識されました ===");
            log.warn("Cloudinary APIを使用するには、以下の環境変数を設定してください:");
            log.warn("  CLOUDINARY_CLOUD_NAME=あなたのCloud Name");
            log.warn("  CLOUDINARY_API_KEY=あなたのAPI Key");
            log.warn("  CLOUDINARY_API_SECRET=あなたのAPI Secret");
            log.warn("現在の設定: cloud-name={}, api-key={}, api-secret={}", 
                    cloudName.isEmpty() ? "(空)" : cloudName.substring(0, Math.min(10, cloudName.length())) + "...",
                    apiKey.isEmpty() ? "(空)" : apiKey.substring(0, Math.min(10, apiKey.length())) + "...",
                    apiSecret.isEmpty() ? "(空)" : apiSecret.substring(0, Math.min(10, apiSecret.length())) + "...");
            
            cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret));
            try {
                // 常にsrc/main/resources/static/imagesに保存
                // mvn cleanを実行しても画像が保持されるようにするため
                uploadDir = Paths.get("src/main/resources/static/images");
                
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                    log.info("画像保存ディレクトリを作成しました: {}", uploadDir.toAbsolutePath());
                } else {
                    log.info("画像保存ディレクトリを使用します: {}", uploadDir.toAbsolutePath());
                }
            } catch (IOException e) {
                log.error("画像保存ディレクトリの作成に失敗しました: {}", e.getMessage());
                uploadDir = Paths.get("src/main/resources/static/images");
            }
        }
    }

    public String uploadFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return null;
        }
        
        // ダミー設定の場合はローカルファイルシステムに保存
        if (isDummyConfig && uploadDir != null) {
            try {
                String originalFilename = file.getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String filename = UUID.randomUUID().toString() + extension;
                Path filePath = uploadDir.resolve(filename);
                
                // まずsrc/main/resources/static/imagesに保存
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                
                // target/classes/static/imagesにもコピー（Spring Bootが認識できるように）
                Path targetDir = Paths.get("target/classes/static/images");
                if (Files.exists(Paths.get("target"))) {
                    try {
                        if (!Files.exists(targetDir)) {
                            Files.createDirectories(targetDir);
                        }
                        Path targetFilePath = targetDir.resolve(filename);
                        Files.copy(filePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
                        log.debug("画像をtarget/classes/static/imagesにもコピーしました: {}", targetFilePath.toAbsolutePath());
                    } catch (IOException e) {
                        log.warn("target/classes/static/imagesへのコピーに失敗しましたが、続行します: {}", e.getMessage());
                    }
                }
                
                String imageUrl = "/images/" + filename;
                log.info("画像をローカルに保存しました: {} -> {}", filePath.toAbsolutePath(), imageUrl);
                return imageUrl;
            } catch (IOException e) {
                log.error("ローカル画像保存に失敗しました: {}", e.getMessage());
                throw new IOException("画像の保存に失敗しました: " + e.getMessage(), e);
            }
        }
        
        // Cloudinary APIを使用してアップロード
        try {
            log.info("Cloudinaryに画像をアップロード中...");
            log.debug("ファイル名: {}, サイズ: {} bytes", file.getOriginalFilename(), file.getSize());
            
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "auto", // 画像、動画などを自動検出
                            "folder", "flea-market" // オプション: フォルダーに整理
                    ));
            
            // アップロード結果の詳細をログに出力
            log.info("=== Cloudinaryアップロード結果 ===");
            log.info("URL: {}", uploadResult.get("url"));
            log.info("Secure URL: {}", uploadResult.get("secure_url"));
            log.info("Public ID: {}", uploadResult.get("public_id"));
            log.info("Format: {}", uploadResult.get("format"));
            log.info("Width: {}", uploadResult.get("width"));
            log.info("Height: {}", uploadResult.get("height"));
            log.info("Bytes: {}", uploadResult.get("bytes"));
            log.info("================================");
            
            // secure_urlがあればそれを使用、なければurlを使用
            String imageUrl = uploadResult.containsKey("secure_url") && uploadResult.get("secure_url") != null
                    ? uploadResult.get("secure_url").toString()
                    : uploadResult.get("url").toString();
            
            String publicId = uploadResult.get("public_id").toString();
            log.info("Cloudinaryへのアップロード成功: URL={}, Public ID={}", imageUrl, publicId);
            
            // URLが正しく取得できているか確認
            if (imageUrl == null || imageUrl.isEmpty()) {
                log.error("Cloudinaryから返されたURLが空です。アップロード結果: {}", uploadResult);
                throw new IOException("Cloudinaryから返されたURLが空です");
            }
            
            return imageUrl;
        } catch (Exception e) {
            log.error("Cloudinaryへの画像アップロードに失敗しました: {}", e.getMessage(), e);
            if (e.getCause() != null) {
                log.error("根本原因: {}", e.getCause().getMessage());
            }
            throw new IOException("画像のアップロードに失敗しました: " + e.getMessage(), e);
        }
    }

    public void deleteFile(String imageUrl) throws IOException {
        if (imageUrl == null) {
            return;
        }
        
        // ダミー設定の場合はローカルファイルを削除
        if (isDummyConfig && uploadDir != null) {
            try {
                // /images/filename の形式からファイル名を抽出
                String filename = imageUrl.replace("/images/", "");
                Path filePath = uploadDir.resolve(filename);
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("ローカル画像を削除しました: {}", imageUrl);
                }
            } catch (IOException e) {
                log.error("ローカル画像の削除に失敗しました: {}", e.getMessage());
                // 削除エラーは無視（既に削除されている可能性があるため）
            }
            return;
        }
        
        // Cloudinary APIを使用して削除
        try {
            // CloudinaryのURLからpublic IDを抽出
            // URL形式: https://res.cloudinary.com/{cloud_name}/{resource_type}/upload/{version}/{public_id}.{format}
            String publicId = extractPublicIdFromUrl(imageUrl);
            
            if (publicId != null && !publicId.isEmpty()) {
                log.info("Cloudinaryから画像を削除中: Public ID={}", publicId);
                Map<?, ?> deleteResult = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("Cloudinaryからの削除成功: {}", deleteResult);
            } else {
                log.warn("Cloudinary URLからpublic IDを抽出できませんでした: {}", imageUrl);
            }
        } catch (Exception e) {
            log.error("Cloudinaryからの画像削除に失敗しました: {}", e.getMessage(), e);
            // 削除エラーは無視（既に削除されている可能性があるため）
        }
    }
    
    /**
     * CloudinaryのURLからpublic IDを抽出
     * URL形式: https://res.cloudinary.com/{cloud_name}/{resource_type}/upload/{version}/{public_id}.{format}
     * または: https://res.cloudinary.com/{cloud_name}/image/upload/{version}/{public_id}.{format}
     */
    private String extractPublicIdFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }
        
        try {
            // CloudinaryのURL形式を解析
            // 例: https://res.cloudinary.com/mycloud/image/upload/v1234567890/folder/image.jpg
            // または: https://res.cloudinary.com/mycloud/image/upload/folder/image.jpg
            
            if (imageUrl.contains("cloudinary.com")) {
                // /upload/ の後の部分を取得
                int uploadIndex = imageUrl.indexOf("/upload/");
                if (uploadIndex != -1) {
                    String afterUpload = imageUrl.substring(uploadIndex + "/upload/".length());
                    
                    // バージョン番号（v1234567890）をスキップ
                    if (afterUpload.startsWith("v") && afterUpload.indexOf("/") != -1) {
                        afterUpload = afterUpload.substring(afterUpload.indexOf("/") + 1);
                    }
                    
                    // 拡張子を除去
                    int lastDotIndex = afterUpload.lastIndexOf(".");
                    if (lastDotIndex != -1) {
                        afterUpload = afterUpload.substring(0, lastDotIndex);
                    }
                    
                    return afterUpload;
                }
            }
        } catch (Exception e) {
            log.warn("URLからpublic IDを抽出中にエラーが発生しました: {}", e.getMessage());
        }
        
        return null;
    }
}
