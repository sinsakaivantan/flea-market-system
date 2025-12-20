package com.example.fleamarketsystem.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret));
    }

    public String uploadFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return null;
        }
        
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "auto",
                            "folder", "flea-market"
                    ));
            
            String imageUrl = uploadResult.containsKey("secure_url") && uploadResult.get("secure_url") != null
                    ? uploadResult.get("secure_url").toString()
                    : uploadResult.get("url").toString();
            
            if (imageUrl == null || imageUrl.isEmpty()) {
                throw new IOException("Cloudinaryから返されたURLが空です");
            }
            
            return imageUrl;
        } catch (Exception e) {
            log.error("画像のアップロードに失敗しました: {}", e.getMessage());
            throw new IOException("画像のアップロードに失敗しました: " + e.getMessage(), e);
        }
    }

    public void deleteFile(String imageUrl) throws IOException {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }
        
        try {
            String publicId = extractPublicIdFromUrl(imageUrl);
            if (publicId != null && !publicId.isEmpty()) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            }
        } catch (Exception e) {
            log.error("画像の削除に失敗しました: {}", e.getMessage());
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
