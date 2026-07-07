package com.example.NFC_system.service;

import org.springframework.stereotype.Service;

import com.example.NFC_system.entity.CharacterVersion;

@Service
public class PromptBuilder {

    // 外观修改关键词，用于判断文本是否涉及角色外观变更
    private static final String[] APPEARANCE_KEYWORDS = {
        "衣服", "服装", "穿着", "头发", "发型", "发色", "眼睛", "眼色",
        "肤色", "皮肤", "配饰", "帽子", "鞋子", "裙子", "裤子", "上衣",
        "外套", "披风", "盔甲", "武器", "翅膀", "尾巴", "耳朵", "角",
        "纹身", "伤疤", "妆容", "换装", "改变外观", "修改外观",
        "clothing", "hair", "outfit", "costume", "dress", "armor"
    };

    public String build(CharacterVersion v) {

        StringBuilder p = new StringBuilder();

        boolean hasDesignImage = isNotEmpty(v.getDesignImageUrl());
        boolean hasPoseImage = isNotEmpty(v.getPoseImageUrl());
        boolean hasExtraImage = isNotEmpty(v.getExtraImageUrl());
        boolean hasCharacterText = isNotEmpty(v.getCharacterPrompt());
        boolean hasSceneText = isNotEmpty(v.getScenePrompt());
        boolean hasActionText = isNotEmpty(v.getActionPrompt());

        // 检测文本描述中是否涉及外观修改
        boolean textModifiesAppearance = containsAppearanceKeywords(v.getCharacterPrompt())
                || containsAppearanceKeywords(v.getScenePrompt())
                || containsAppearanceKeywords(v.getActionPrompt());

        // ============================================
        // 第一优先级：角色外观基准（图片 or 文本覆盖）
        // ============================================
        if (hasDesignImage && textModifiesAppearance) {
            // 文本涉及外观修改 → 文本优先于角色参考图
            p.append("【核心指令 - 外观重定义】以下文本描述的外观修改具有最高优先级，必须严格执行文本中的外观变更要求。");
            p.append("参考图片仅作为基础体型和面部特征的参考，但服装、发型、配色等外观特征以文本描述为准。");
            p.append("\n");
            appendIfNotNull(p, "【角色描述（最高优先级）】", v.getCharacterPrompt());
        } else if (hasDesignImage) {
            // 无外观修改 → 角色参考图最高优先级
            p.append("【核心指令 - 角色外观锁定】必须严格按照第一张参考图片还原角色外观，包括脸型、发型、五官、服装、配色、体型，不允许改变人物身份和风格。");
            p.append("第一张图片是角色的权威参考，所有生成内容必须与之一致。");
            p.append("\n");
            if (hasCharacterText) {
                p.append("【角色补充描述】").append(v.getCharacterPrompt()).append("（注意：此描述仅作为补充，不得与参考图片的外观冲突）。");
            }
        } else if (hasCharacterText) {
            // 没有角色参考图，纯文本驱动
            p.append("【核心指令 - 角色描述】").append(v.getCharacterPrompt()).append("。");
        }

        // ============================================
        // 第二优先级：文本描述（场景 + 动作）
        // ============================================
        p.append("\n");
        if (hasSceneText || hasActionText) {
            p.append("【场景与动作指令】以下文本描述的场景和动作必须严格执行：");
            appendIfNotNull(p, "场景环境：", v.getScenePrompt());
            appendIfNotNull(p, "动作要求：", v.getActionPrompt());
            if (hasPoseImage) {
                p.append("（注意：文本描述的动作优先级高于动作参考图，若有冲突以文本为准）");
            }
        }

        // ============================================
        // 第三优先级：动作姿势参考图
        // ============================================
        if (hasPoseImage) {
            p.append("\n");
            if (hasActionText) {
                p.append("【动作参考图 - 辅助参考】第二张图片仅作为动作姿势的辅助参考，帮助理解动作细节。");
                p.append("但最终动作以上方文本描述为准。");
            } else {
                p.append("【动作参考图 - 主要参考】参考第二张图片的动作姿势，将角色调整为该动作。");
            }
            p.append("禁止使用第二张图片的人物外观、风格或其他特征，仅提取动作信息。");
        }

        // ============================================
        // 补充参考图
        // ============================================
        if (hasExtraImage) {
            p.append("\n");
            p.append("【补充参考】第三张图片为补充参考素材，可用于丰富细节，但不得覆盖以上任何指令。");
        }

        // ============================================
        // 音频参考
        // ============================================
        if (isNotEmpty(v.getAudioUrl())) {
            p.append("\n");
            p.append("【音频参考】根据音频内容补充角色的情绪和氛围特征。");
        }

        // ============================================
        // 表情参数
        // ============================================
        p.append("\n");
        p.append("【表情控制】表情参数：")
         .append(v.getExpression1()).append(",")
         .append(v.getExpression2()).append(",")
         .append(v.getExpression3()).append(",")
         .append(v.getExpression4()).append(",")
         .append(v.getExpression5()).append(",")
         .append(v.getExpression6()).append(",")
         .append(v.getExpression7()).append("。");

        // ============================================
        // 通用质量约束（放在最后，不影响优先级）
        // ============================================
        p.append("\n");
        p.append("【质量约束】");
        p.append("人体结构必须正确，符合真实人体比例，关节位置自然。");
        p.append("手部五指清晰，大小与身体比例一致。");
        p.append("透视关系合理，动作符合人体运动逻辑。");

        p.append("\n");
        p.append("生成3D风格角色立绘，高清，细节清晰。");

        return p.toString();
    }

    /**
     * 判断文本中是否包含外观修改相关的关键词
     */
    private boolean containsAppearanceKeywords(String text) {
        if (text == null || text.isEmpty()) return false;
        String lower = text.toLowerCase();
        for (String keyword : APPEARANCE_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否有文本描述涉及外观修改，用于外部调用（如调整strength）
     */
    public boolean isAppearanceModification(CharacterVersion v) {
        return containsAppearanceKeywords(v.getCharacterPrompt())
                || containsAppearanceKeywords(v.getScenePrompt())
                || containsAppearanceKeywords(v.getActionPrompt());
    }

    private boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    private void appendIfNotNull(StringBuilder p, String prefix, String value) {
        if (value != null && !value.isEmpty()) {
            p.append(prefix).append(value).append("。");
        }
    }
}