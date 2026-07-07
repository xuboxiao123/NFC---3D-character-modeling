package com.example.NFC_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "character_version")
public class CharacterVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long characterId;

    private String designImageUrl;
    private String poseImageUrl;
    private String extraImageUrl;

    @Column(columnDefinition = "TEXT")
    private String characterPrompt;

    @Column(columnDefinition = "TEXT")
    private String scenePrompt;

    @Column(columnDefinition = "TEXT")
    private String actionPrompt;

    private String audioUrl;

    @Column(columnDefinition = "TEXT")
    private String faceParams;  // JSON字符串，存储12个面部细节参数

    @Column(columnDefinition = "INT DEFAULT 5")
    private Integer expression1 = 5;
    @Column(columnDefinition = "INT DEFAULT 5")
    private Integer expression2 = 5;
    @Column(columnDefinition = "INT DEFAULT 5")
    private Integer expression3 = 5;
    @Column(columnDefinition = "INT DEFAULT 5")
    private Integer expression4 = 5;
    @Column(columnDefinition = "INT DEFAULT 5")
    private Integer expression5 = 5;
    @Column(columnDefinition = "INT DEFAULT 5")
    private Integer expression6 = 5;
    @Column(columnDefinition = "INT DEFAULT 5")
    private Integer expression7 = 5;

    private String previewImageUrl;  // 2D结果
    private String modelUrl;         // 3D模型原始版本
    private String sculptedModelUrl; // 雕刻/修改后的3D模型版本

    private String status;

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer sortOrder = 0;

    private LocalDateTime createdAt;

    // ===== getter setter =====

    public Long getId() { return id; }
    public Long getCharacterId() { return characterId; }
    public String getDesignImageUrl() { return designImageUrl; }
    public String getPoseImageUrl() { return poseImageUrl; }
    public String getExtraImageUrl() { return extraImageUrl; }
    public String getCharacterPrompt() { return characterPrompt; }
    public String getScenePrompt() { return scenePrompt; }
    public String getActionPrompt() { return actionPrompt; }
    public String getAudioUrl() { return audioUrl; }
    public String getFaceParams() { return faceParams; }
    public Integer getExpression1() { return expression1; }
    public Integer getExpression2() { return expression2; }
    public Integer getExpression3() { return expression3; }
    public Integer getExpression4() { return expression4; }
    public Integer getExpression5() { return expression5; }
    public Integer getExpression6() { return expression6; }
    public Integer getExpression7() { return expression7; }
    public String getPreviewImageUrl() { return previewImageUrl; }
    public String getModelUrl() { return modelUrl; }
    public String getSculptedModelUrl() { return sculptedModelUrl; }
    public String getStatus() { return status; }
    public Integer getSortOrder() { return sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setCharacterId(Long characterId) { this.characterId = characterId; }
    public void setDesignImageUrl(String designImageUrl) { this.designImageUrl = designImageUrl; }
    public void setPoseImageUrl(String poseImageUrl) { this.poseImageUrl = poseImageUrl; }
    public void setExtraImageUrl(String extraImageUrl) { this.extraImageUrl = extraImageUrl; }
    public void setCharacterPrompt(String characterPrompt) { this.characterPrompt = characterPrompt; }
    public void setScenePrompt(String scenePrompt) { this.scenePrompt = scenePrompt; }
    public void setActionPrompt(String actionPrompt) { this.actionPrompt = actionPrompt; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    public void setFaceParams(String faceParams) { this.faceParams = faceParams; }
    public void setExpression1(Integer expression1) { this.expression1 = expression1; }
    public void setExpression2(Integer expression2) { this.expression2 = expression2; }
    public void setExpression3(Integer expression3) { this.expression3 = expression3; }
    public void setExpression4(Integer expression4) { this.expression4 = expression4; }
    public void setExpression5(Integer expression5) { this.expression5 = expression5; }
    public void setExpression6(Integer expression6) { this.expression6 = expression6; }
    public void setExpression7(Integer expression7) { this.expression7 = expression7; }
    public void setPreviewImageUrl(String previewImageUrl) { this.previewImageUrl = previewImageUrl; }
    public void setModelUrl(String modelUrl) { this.modelUrl = modelUrl; }
    public void setSculptedModelUrl(String sculptedModelUrl) { this.sculptedModelUrl = sculptedModelUrl; }
    public void setStatus(String status) { this.status = status; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}