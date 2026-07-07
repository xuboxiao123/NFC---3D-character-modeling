package com.example.NFC_system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;

import com.example.NFC_system.entity.CharacterVersion;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DoubaoService {

    @Value("${doubao.api-key:}")
    private String API_KEY;

    @Value("${doubao.model:}")
    private String MODEL;

    @Value("${doubao.url:https://ark.cn-beijing.volces.com/api/v3/images/generations}")
    private String URL;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private PromptBuilder promptBuilder;

    public String generateImage(String prompt, CharacterVersion v) {

        System.out.println("========== 图生图开始 ==========");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + API_KEY);

            // prompt 已经在 PromptBuilder 中按优先级构建好了，直接使用
            String finalPrompt = prompt;

            Map<String, Object> body = new HashMap<>();
            body.put("model", MODEL);
            body.put("prompt", finalPrompt);
            body.put("response_format", "url");
            body.put("size", "2048x2048");

            // =========================
            // 图片列表 + 动态 strength
            // =========================

            List<String> images = new ArrayList<>();
            boolean hasDesignImage = v.getDesignImageUrl() != null && !v.getDesignImageUrl().isEmpty();
            boolean hasPoseImage = v.getPoseImageUrl() != null && !v.getPoseImageUrl().isEmpty();
            boolean hasExtraImage = v.getExtraImageUrl() != null && !v.getExtraImageUrl().isEmpty();
            boolean textModifiesAppearance = promptBuilder.isAppearanceModification(v);

            System.out.println("👉 designImage: " + v.getDesignImageUrl());
            System.out.println("👉 poseImage: " + v.getPoseImageUrl());
            System.out.println("👉 extraImage: " + v.getExtraImageUrl());
            System.out.println("👉 文本涉及外观修改: " + textModifiesAppearance);

            // 角色参考图始终排第一
            if (hasDesignImage) {
                images.add(v.getDesignImageUrl());
            }

            // 动作参考图排第二
            if (hasPoseImage) {
                images.add(v.getPoseImageUrl());
            }

            // 补充参考图排第三
            if (hasExtraImage) {
                images.add(v.getExtraImageUrl());
            }

            System.out.println("👉 最终图片数组: " + images);

            if (!images.isEmpty()) {
                body.put("image", images);

                // 动态 strength 策略：
                // - 文本涉及外观修改 → 降低 strength，让文本主导生成
                // - 只有动作参考图（无角色图）→ 降低 strength，避免动作图影响角色外观
                // - 有角色参考图且无外观修改 → 较高 strength，保持角色一致性
                double strength;
                if (textModifiesAppearance) {
                    strength = 0.5;  // 文本改外观时，降低图片影响
                } else if (!hasDesignImage && hasPoseImage) {
                    strength = 0.55; // 只有动作图，适度参考
                } else if (hasDesignImage && hasPoseImage) {
                    strength = 0.75; // 角色图+动作图，偏向保持角色
                } else {
                    strength = 0.8;  // 只有角色图，强参考
                }

                body.put("strength", strength);
                System.out.println("👉 动态strength: " + strength);
            }

            System.out.println("👉 最终请求体: " + body);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(URL, request, String.class);

            System.out.println("👉 HTTP状态: " + response.getStatusCode());
            System.out.println("👉 原始响应: " + response.getBody());

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("接口状态异常：" + response.getStatusCode());
            }

            Map<String, Object> responseBody =
                    new ObjectMapper().readValue(response.getBody(), Map.class);

            if (responseBody.containsKey("error")) {
                Map<String, Object> error = (Map<String, Object>) responseBody.get("error");
                throw new RuntimeException("豆包错误：" + error);
            }

            List<Map<String, Object>> data =
                    (List<Map<String, Object>>) responseBody.get("data");

            if (data == null || data.isEmpty()) {
                throw new RuntimeException("没有返回图片");
            }

            String result = data.get(0).get("url").toString();

            System.out.println("✅ 最终图片URL: " + result);
            System.out.println("========== 图生图结束 ==========");

            return result;

        } catch (Exception e) {

            System.out.println("========== 图生图失败 ==========");
            e.printStackTrace();

            throw new RuntimeException("图生图失败：" + e.getMessage());
        }
    }
}