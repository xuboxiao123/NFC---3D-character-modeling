package com.example.NFC_system.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

// 🔥 AWS SDK（R2核心）
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@RestController
@RequestMapping("/api")
public class UploadController {

    // ==========================
    // 🔥 本地上传（你原来的，保留）
    // ==========================
    @Value("${app.local.image-dir:D:/models/images/}")
    private String localImageDir;

    @Value("${app.local.model-dir:D:/models/unzip/}")
    private String localModelDir;

    @Value("${app.local.public-base-url:http://localhost:8088}")
    private String localPublicBaseUrl;

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file) throws Exception {

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

        File dest = new File(localImageDir + fileName);

        dest.getParentFile().mkdirs();
        file.transferTo(dest);

        return localPublicBaseUrl + "/images/" + fileName;
    }

    // ==========================
    // 🔥 上传修改后的3D模型（OBJ文件） - multipart方式（保留兼容）
    // ==========================
    @PostMapping("/upload/model")
    public Map<String, String> uploadModel(@RequestParam("file") MultipartFile file,
                                           @RequestParam(required = false) String prefix) throws Exception {
        String originalName = file.getOriginalFilename();
        String ext = ".obj";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.'));
        }
        // 生成唯一文件名：sculpted_时间戳_随机数.obj
        String fileName = (prefix != null ? prefix + "_" : "sculpted_")
                + System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString().substring(0, 6) + ext;

        File dest = new File(localModelDir + fileName);
        dest.getParentFile().mkdirs();
        file.transferTo(dest);

        Map<String, String> result = new HashMap<>();
        result.put("modelUrl", "/models/" + fileName);
        result.put("fileName", fileName);
        return result;
    }

    // ==========================
    // 🔥 直接保存雕刻模型到本地（不走multipart，无大小限制）
    // 前端将OBJ文本内容直接POST过来，后端直接写入本地文件
    // ==========================
    @PostMapping("/upload/model/local")
    public Map<String, String> saveModelLocal(
            @RequestParam(required = false) String prefix,
            HttpServletRequest request) throws Exception {

        // 生成唯一文件名
        String fileName = (prefix != null ? prefix + "_" : "sculpted_")
                + System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString().substring(0, 6) + ".obj";

        File dest = new File(localModelDir + fileName);
        dest.getParentFile().mkdirs();

        // 直接从请求体读取原始数据，写入本地文件（流式，不受multipart限制）
        long bytesWritten = 0;
        try (InputStream is = request.getInputStream();
             FileOutputStream fos = new FileOutputStream(dest)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
                bytesWritten += read;
            }
        }

        System.out.println("✅ 雕刻模型已保存到本地: " + dest.getAbsolutePath()
                + " (" + (bytesWritten / 1024) + "KB)");

        Map<String, String> result = new HashMap<>();
        result.put("modelUrl", "/models/" + fileName);
        result.put("fileName", fileName);
        result.put("size", String.valueOf(bytesWritten));
        return result;
    }

    // ==========================
    // 🔥 R2 预签名上传（核心🔥）
    // ==========================

    private S3Presigner presigner;

    @Value("${r2.endpoint:}")
    private String endpoint;

    @Value("${r2.access-key:}")
    private String accessKey;

    @Value("${r2.secret-key:}")
    private String secretKey;

    @Value("${r2.bucket:capstone}")
    private String bucket;

    @Value("${r2.public-domain:}")
    private String r2PublicDomain;

    @PostConstruct
    public void init() {
        presigner = S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .region(Region.of("auto"))
                .build();
    }

    // ==========================
    // 🔥 获取上传URL
    // ==========================
    @GetMapping("/upload/presign")
    public Map<String, String> presign(@RequestParam String fileName,
                                       @RequestParam(defaultValue = "image/png") String contentType) {

        // 只保留扩展名，避免中文文件名导致 URL 编码问题
        String ext = "";
        int dotIdx = fileName.lastIndexOf('.');
        if (dotIdx >= 0) {
            ext = fileName.substring(dotIdx);
        }
        String key = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(10))
                        .putObjectRequest(objectRequest)
                        .build();

        PresignedPutObjectRequest presignedRequest =
                presigner.presignPutObject(presignRequest);

        String uploadUrl = presignedRequest.url().toString();

        // 公网访问地址（在 application.properties 中配置 r2.public-domain）
        String fileUrl = r2PublicDomain + key;

        Map<String, String> res = new HashMap<>();
        res.put("uploadUrl", uploadUrl);
        res.put("fileUrl", fileUrl);

        return res;
    }
}