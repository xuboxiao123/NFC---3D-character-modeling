package com.example.NFC_system.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.UUID;

/**
 * R2图床上传服务
 * 用于将AI生成的临时图片URL转存到Cloudflare R2，获得永久访问链接
 */
@Service
public class R2UploadService {

    private S3Client s3Client;

    @Value("${r2.endpoint:}")
    private String endpoint;

    @Value("${r2.access-key:}")
    private String accessKey;

    @Value("${r2.secret-key:}")
    private String secretKey;

    @Value("${r2.bucket:capstone}")
    private String bucket;

    @Value("${r2.public-domain:}")
    private String publicDomain;

    @PostConstruct
    public void init() {
        s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .region(Region.of("auto"))
                .forcePathStyle(true) // R2 需要 path-style
                .build();
    }

    /**
     * 将远程图片URL下载并上传到R2图床
     *
     * @param imageUrl 远程图片URL（如豆包AI返回的临时URL）
     * @return R2图床的永久访问URL
     */
    public String uploadImageFromUrl(String imageUrl) {
        try {
            System.out.println("🔄 开始转存图片到R2图床...");
            System.out.println("👉 原始URL: " + imageUrl);

            // 1. 下载远程图片
            URL url = new URL(imageUrl);
            byte[] imageBytes;
            try (InputStream is = url.openStream()) {
                imageBytes = is.readAllBytes();
            }

            System.out.println("✅ 图片下载完成，大小: " + imageBytes.length + " bytes");

            // 2. 生成唯一文件名
            String ext = guessExtension(imageUrl);
            String key = "ai-generated/" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

            // 3. 确定 Content-Type
            String contentType = guessContentType(ext);

            // 4. 上传到R2
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(imageBytes));

            // 5. 返回公网访问URL
            String permanentUrl = publicDomain + key;

            System.out.println("✅ 图片已转存到R2图床: " + permanentUrl);

            return permanentUrl;

        } catch (Exception e) {
            System.err.println("❌ 图片转存R2失败: " + e.getMessage());
            e.printStackTrace();
            // 转存失败时返回原始URL，避免阻断主流程
            System.out.println("⚠️ 回退使用原始临时URL: " + imageUrl);
            return imageUrl;
        }
    }

    /**
     * 从URL猜测文件扩展名
     */
    private String guessExtension(String imageUrl) {
        try {
            // 去掉查询参数
            String path = new URL(imageUrl).getPath().toLowerCase();
            if (path.endsWith(".png")) return ".png";
            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return ".jpg";
            if (path.endsWith(".webp")) return ".webp";
            if (path.endsWith(".gif")) return ".gif";
        } catch (Exception ignored) {
        }
        // 豆包默认返回png
        return ".png";
    }

    /**
     * 根据扩展名推断Content-Type
     */
    private String guessContentType(String ext) {
        return switch (ext) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".webp" -> "image/webp";
            case ".gif" -> "image/gif";
            default -> "image/png";
        };
    }
}