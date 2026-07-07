package com.example.NFC_system.controller;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.example.NFC_system.entity.CharacterVersion;
import com.example.NFC_system.repository.CharacterVersionRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.NFC_system.service.AIService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/version")
public class VersionController {

    @Autowired
    private CharacterVersionRepository versionRepository;

    @Autowired
    private AIService aiService;

    // 仅创建空版本（保存基本数据，不生成2D/3D）
    @PostMapping("/create")
public CharacterVersion createVersion(
        @RequestParam Long characterId,
        @RequestParam(required = false) String characterPrompt,
        @RequestParam(required = false) String scenePrompt,
        @RequestParam(required = false) String actionPrompt,
        @RequestParam(required = false) String designImageUrl,
        @RequestParam(required = false) String poseImageUrl,
        @RequestParam(required = false) String extraImageUrl,
        @RequestParam(required = false) String audioUrl
) {
    CharacterVersion version = new CharacterVersion();
    version.setCharacterId(characterId);
    version.setCharacterPrompt(characterPrompt);
    version.setScenePrompt(scenePrompt);
    version.setActionPrompt(actionPrompt);

    version.setDesignImageUrl(designImageUrl);
    version.setPoseImageUrl(poseImageUrl);
    version.setExtraImageUrl(extraImageUrl);
    version.setAudioUrl(audioUrl);

    // 🔹 初始化表情为5（中间值，1~11范围，5=不变）
    version.setExpression1(5);
    version.setExpression2(5);
    version.setExpression3(5);
    version.setExpression4(5);
    version.setExpression5(5);
    version.setExpression6(5);
    version.setExpression7(5);

    version.setStatus("created");
    int nextSortOrder = versionRepository.findByCharacterId(characterId).size();
    version.setSortOrder(nextSortOrder);
    version.setCreatedAt(LocalDateTime.now());

    return versionRepository.save(version);
}

    // 保存面部细节参数（JSON字符串）以及修改后的模型URL
    @PutMapping("/saveFaceParams")
    public CharacterVersion saveFaceParams(
            @RequestParam Long versionId,
            @RequestParam(required = false) String sculptedModelUrl,
            @RequestBody String faceParams
    ) {
        CharacterVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("version not found"));
        version.setFaceParams(faceParams);
        if (sculptedModelUrl != null && !sculptedModelUrl.isEmpty()) {
            version.setSculptedModelUrl(sculptedModelUrl);
        }
        return versionRepository.save(version);
    }

    // 获取版本列表
    @GetMapping("/list")
    public List<CharacterVersion> getVersions(@RequestParam Long characterId) {
        return versionRepository.findByCharacterIdOrderBySortOrderAsc(characterId);
    }
    @PostMapping("/generate2D")
public CharacterVersion generate2D(@RequestParam Long versionId) {
    return aiService.generate2D(versionId);
}

    @PostMapping("/generate3D")
public CharacterVersion generate3D(@RequestParam Long versionId) throws Exception {
    return aiService.generate3D(versionId);
}

    @PutMapping("/edit")
public CharacterVersion editVersion(
        @RequestParam Long versionId,

        // 文本
        @RequestParam(required = false) String characterPrompt,
        @RequestParam(required = false) String scenePrompt,
        @RequestParam(required = false) String actionPrompt,

        // 图片
        @RequestParam(required = false) String designImageUrl,
        @RequestParam(required = false) String poseImageUrl,
        @RequestParam(required = false) String extraImageUrl,

        // 音频
        @RequestParam(required = false) String audioUrl,

        // 面部细节参数（JSON字符串）
        @RequestParam(required = false) String faceParams,

        // 表情（明确字段）
        @RequestParam(required = false) Integer expression1,
        @RequestParam(required = false) Integer expression2,
        @RequestParam(required = false) Integer expression3,
        @RequestParam(required = false) Integer expression4,
        @RequestParam(required = false) Integer expression5,
        @RequestParam(required = false) Integer expression6,
        @RequestParam(required = false) Integer expression7
) {
    return aiService.updateVersion(
            versionId,
            characterPrompt,
            scenePrompt,
            actionPrompt,
            designImageUrl,
            poseImageUrl,
            extraImageUrl,
            audioUrl,
            faceParams,
            expression1,
            expression2,
            expression3,
            expression4,
            expression5,
            expression6,
            expression7
    );
}

@GetMapping("/get")
public CharacterVersion getById(@RequestParam Long versionId) {
    return versionRepository.findById(versionId)
            .orElseThrow(() -> new RuntimeException("not found"));
}

    // 🖨️ 3D打印：启动Bambu Studio并导入模型
    @PostMapping("/print3D")
    public Map<String, Object> openBambuStudio(@RequestParam Long versionId) {
        Map<String, Object> result = new HashMap<>();

        CharacterVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("version not found"));

        String modelUrl = version.getModelUrl();
        if (modelUrl == null || modelUrl.isEmpty()) {
            result.put("success", false);
            result.put("message", "该版本没有3D模型，请先生成3D模型");
            return result;
        }

        // modelUrl 格式: /models/xxx.obj → 实际路径: D:/models/unzip/xxx.obj
        String fileName = modelUrl.replace("/models/", "");
        String localPath = "D:/models/unzip/" + fileName;

        File modelFile = new File(localPath);
        if (!modelFile.exists()) {
            result.put("success", false);
            result.put("message", "模型文件不存在: " + localPath);
            return result;
        }

        String bambuPath = "E:\\打印机\\Bambu Studio\\bambu-studio.exe";
        File bambuExe = new File(bambuPath);
        if (!bambuExe.exists()) {
            result.put("success", false);
            result.put("message", "Bambu Studio 未找到: " + bambuPath);
            return result;
        }

        try {
            // 启动 Bambu Studio 并传入模型文件路径
            ProcessBuilder pb = new ProcessBuilder(bambuPath, localPath);
            pb.start();

            result.put("success", true);
            result.put("message", "Bambu Studio 已启动，正在导入模型");
            result.put("modelPath", localPath);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "启动 Bambu Studio 失败: " + e.getMessage());
        }

        return result;
    }

    @DeleteMapping("/delete")
    public Map<String, Object> deleteVersion(@RequestParam Long versionId) {
        Map<String, Object> result = new HashMap<>();
        try {
            versionRepository.deleteById(versionId);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping("/reorder")
    public Map<String, Object> reorderVersions(@RequestBody List<Map<String, Object>> orderList) {
        Map<String, Object> result = new HashMap<>();
        try {
            for (Map<String, Object> item : orderList) {
                Long id = Long.valueOf(item.get("id").toString());
                Integer sortOrder = Integer.valueOf(item.get("sortOrder").toString());
                versionRepository.findById(id).ifPresent(v -> {
                    v.setSortOrder(sortOrder);
                    versionRepository.save(v);
                });
            }
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }


}
