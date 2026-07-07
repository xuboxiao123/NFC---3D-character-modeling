package com.example.NFC_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * NFC 自定义面部参数卡
 * 用户在 Sculpt 模式下将当前面部参数写入 NFC 卡片
 * code 格式: C01, C02, ... (Custom 类型)
 */
@Entity
@Table(name = "nfc_face_card")
public class NfcFaceCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 10)
    private String code;  // 卡片编号，如 C01, C02

    private String name;  // 用户自定义名称

    private Long versionId;  // 关联的版本ID

    @Column(columnDefinition = "TEXT")
    private String faceParams;  // JSON 格式的面部参数

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // ===== getter setter =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getVersionId() { return versionId; }
    public void setVersionId(Long versionId) { this.versionId = versionId; }

    public String getFaceParams() { return faceParams; }
    public void setFaceParams(String faceParams) { this.faceParams = faceParams; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}