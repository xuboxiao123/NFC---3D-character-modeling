package com.example.NFC_system.service;

import org.springframework.stereotype.Service;
import java.util.*;

/**
 * NFC 预设卡片服务
 * 
 * 15张NFC卡分3类：
 * - E01~E05: 表情预设（快速应用一组面部参数）
 * - F01~F05: 脸型预设（改变整体脸型）
 * - A01~A05: 动作/设定预设（导入角色设定信息）
 * 
 * 卡片数据块中写入编号字符串（如 "E01"），ESP32读取后发送给后端
 */
@Service
public class NfcPresetService {

    // 预设类型枚举
    public static final String TYPE_EXPRESSION = "E";  // 表情
    public static final String TYPE_FACE = "F";         // 脸型
    public static final String TYPE_ACTION = "A";       // 动作/设定

    // ===== 表情预设 (E01~E05) =====
    // 每个预设是一组 faceParams 值，直接覆盖当前面部参数
    private static final Map<String, Map<String, Object>> EXPRESSION_PRESETS = new LinkedHashMap<>();

    // ===== 脸型预设 (F01~F05) =====
    private static final Map<String, Map<String, Object>> FACE_PRESETS = new LinkedHashMap<>();

    // ===== 动作/设定预设 (A01~A05) =====
    // 只包含参考图URL，不写入 actionPrompt
    private static final Map<String, Map<String, Object>> ACTION_PRESETS = new LinkedHashMap<>();

    // 所有预设的元信息（名称、描述、图标等）
    private static final Map<String, Map<String, String>> PRESET_META = new LinkedHashMap<>();

    static {
        // ========== 表情预设 ==========
        // E01: Happy - 开心笑，嘴角上扬、眼睛微眯
        // 注意：补 0 是为了清掉上一次表情预设残留，避免多次刷卡后变形叠加。
        Map<String, Object> happy = new LinkedHashMap<>();
        happy.put("faceWidth", 0.0);
        happy.put("jawWidth", 0.0);
        happy.put("chinLength", 0.0);
        happy.put("cheekBone", 0.1);
        happy.put("eyeSize", -0.1);
        happy.put("eyeDistance", 0.0);
        happy.put("noseHeight", 0.0);
        happy.put("noseWidth", 0.0);
        happy.put("mouthWidth", 0.4);
        happy.put("lipThickness", 0.1);
        happy.put("foreheadHeight", 0.0);
        happy.put("foreheadDepth", 0.0);
        happy.put("eyeSizeLeft", -0.05);
        happy.put("eyeSizeRight", -0.05);
        happy.put("cheekLeft", 0.0);
        happy.put("cheekRight", 0.0);
        EXPRESSION_PRESETS.put("E01", happy);
        PRESET_META.put("E01", metaOf("Happy", "轻微开心，嘴巴略宽、脸颊微鼓、眼睛轻微眯起", "😄"));

        // E02: Amazed - 惊讶，眼睛睁大、嘴巴微张成O形
        Map<String, Object> amazed = new LinkedHashMap<>();
        amazed.put("faceWidth", 0.0);
        amazed.put("jawWidth", 0.0);
        amazed.put("chinLength", 0.0);
        amazed.put("cheekBone", -0.5);
        amazed.put("eyeSize", 0.3);
        amazed.put("eyeDistance", 0.0);
        amazed.put("noseHeight", 0.0);
        amazed.put("noseWidth", 0.0);
        amazed.put("mouthWidth", -0.5);
        amazed.put("lipThickness", -0.35);
        amazed.put("foreheadHeight", 0.35);
        amazed.put("foreheadDepth", 0.0);
        amazed.put("eyeSizeLeft", 0.25);
        amazed.put("eyeSizeRight", 0.25);
        amazed.put("cheekLeft", 0.0);
        amazed.put("cheekRight", 0.0);
        EXPRESSION_PRESETS.put("E02", amazed);
        PRESET_META.put("E02", metaOf("Amazed", "惊讶万分，眼睛睁大、嘴巴微张", "😲"));

        // E03: Angry - 生气，眉头紧皱前突、眼睛瞪大、嘴角下拉
        Map<String, Object> angry = new LinkedHashMap<>();
        angry.put("faceWidth", 0.2);
        angry.put("jawWidth", 0.4);
        angry.put("chinLength", 0.0);
        angry.put("cheekBone", 0.2);
        angry.put("eyeSize", 0.2);
        angry.put("eyeDistance", 0.0);
        angry.put("noseHeight", 0.0);
        angry.put("noseWidth", 0.35);
        angry.put("mouthWidth", -0.5);
        angry.put("lipThickness", -0.35);
        angry.put("foreheadHeight", -0.6);
        angry.put("foreheadDepth", 0.7);
        angry.put("eyeSizeLeft", 0.1);
        angry.put("eyeSizeRight", 0.1);
        angry.put("cheekLeft", 0.0);
        angry.put("cheekRight", 0.0);
        EXPRESSION_PRESETS.put("E03", angry);
        PRESET_META.put("E03", metaOf("Angry", "怒火中烧，眉头紧皱、怒目圆睁", "😠"));

        // E04: Sad - 悲伤，眼睛眯起、嘴角下拉、脸颊凹陷
        // (使用之前 E03 调出的效果，用户确认适合做悲伤表情)
        Map<String, Object> sad = new LinkedHashMap<>();
        sad.put("faceWidth", 0.1);
        sad.put("jawWidth", 0.2);
        sad.put("chinLength", 0.0);
        sad.put("cheekBone", 0.15);
        sad.put("eyeSize", -0.35);
        sad.put("eyeDistance", 0.0);
        sad.put("noseHeight", 0.0);
        sad.put("noseWidth", 0.1);
        sad.put("mouthWidth", -0.35);
        sad.put("lipThickness", -0.25);
        sad.put("foreheadHeight", -0.25);
        sad.put("foreheadDepth", 0.35);
        sad.put("eyeSizeLeft", -0.3);
        sad.put("eyeSizeRight", -0.3);
        sad.put("cheekLeft", 0.0);
        sad.put("cheekRight", 0.0);
        EXPRESSION_PRESETS.put("E04", sad);
        PRESET_META.put("E04", metaOf("Sad", "悲伤落泪，眼睛眯起、嘴角下拉", "😢"));

        // E05: Speechless - 无语，嘴巴横向拉宽紧闭、眉头下压、面部扁平
        // (使用之前 E03 调出的效果，用户确认适合做无语表情)
        Map<String, Object> speechless = new LinkedHashMap<>();
        speechless.put("faceWidth", 0.15);
        speechless.put("jawWidth", 0.3);
        speechless.put("chinLength", 0.0);
        speechless.put("cheekBone", 0.2);
        speechless.put("eyeSize", -0.15);
        speechless.put("eyeDistance", 0.0);
        speechless.put("noseHeight", 0.0);
        speechless.put("noseWidth", 0.15);
        speechless.put("mouthWidth", 0.2);
        speechless.put("lipThickness", -0.35);
        speechless.put("foreheadHeight", -0.3);
        speechless.put("foreheadDepth", 0.4);
        speechless.put("eyeSizeLeft", -0.1);
        speechless.put("eyeSizeRight", -0.1);
        speechless.put("cheekLeft", 0.0);
        speechless.put("cheekRight", 0.0);
        EXPRESSION_PRESETS.put("E05", speechless);
        PRESET_META.put("E05", metaOf("Speechless", "一脸无语，嘴巴紧闭、眉头下压", "😑"));

        // ========== 脸型预设 ==========
        // F01: 圆脸
        Map<String, Object> roundFace = new LinkedHashMap<>();
        roundFace.put("faceWidth", 0.5);
        roundFace.put("jawWidth", 0.3);
        roundFace.put("cheekBone", 0.2);
        roundFace.put("chinLength", -0.2);
        FACE_PRESETS.put("F01", roundFace);
        PRESET_META.put("F01", metaOf("圆脸", "饱满圆润的脸型", "🟡"));

        // F02: 瓜子脸
        Map<String, Object> ovalFace = new LinkedHashMap<>();
        ovalFace.put("faceWidth", -0.2);
        ovalFace.put("jawWidth", -0.5);
        ovalFace.put("chinLength", 0.3);
        FACE_PRESETS.put("F02", ovalFace);
        PRESET_META.put("F02", metaOf("瓜子脸", "上宽下窄的精致瓜子脸", "🔻"));

        // F03: 方脸 - 四四方方，下颌宽大、下巴短平、脸宽
        Map<String, Object> squareFace = new LinkedHashMap<>();
        squareFace.put("faceWidth", 0.5);
        squareFace.put("jawWidth", 0.8);
        squareFace.put("cheekBone", -0.1);
        squareFace.put("chinLength", -0.5);
        squareFace.put("foreheadHeight", -0.1);
        squareFace.put("foreheadDepth", 0.15);
        FACE_PRESETS.put("F03", squareFace);
        PRESET_META.put("F03", metaOf("方脸", "四四方方棱角分明的方形脸", "⬛"));

        // F04: 大眼萌脸
        Map<String, Object> cuteFace = new LinkedHashMap<>();
        cuteFace.put("eyeSize", 0.7);
        cuteFace.put("eyeDistance", 0.1);
        cuteFace.put("noseWidth", -0.2);
        cuteFace.put("chinLength", -0.2);
        cuteFace.put("faceWidth", 0.1);
        FACE_PRESETS.put("F04", cuteFace);
        PRESET_META.put("F04", metaOf("大眼萌", "大眼睛小脸蛋的可爱脸型", "🥺"));

        // F05: 精灵脸
        Map<String, Object> elfFace = new LinkedHashMap<>();
        elfFace.put("faceWidth", -0.3);
        elfFace.put("chinLength", 0.4);
        elfFace.put("eyeSize", 0.3);
        elfFace.put("noseHeight", 0.4);
        elfFace.put("foreheadHeight", 0.2);
        FACE_PRESETS.put("F05", elfFace);
        PRESET_META.put("F05", metaOf("精灵脸", "尖下巴大眼睛的精灵风格", "🧝"));

        // ========== 动作/设定预设 ==========
        // A01~A05: 只包含参考图URL
        ACTION_PRESETS.put("A01", actionOf(
            "站立正面姿势",
            "https://xuboxiao.top/A1.png"
        ));
        PRESET_META.put("A01", metaOf("正面站姿", "标准正面站立姿势", "🧍"));

        ACTION_PRESETS.put("A02", actionOf(
            "战斗姿势",
            "https://xuboxiao.top/A2.png"
        ));
        PRESET_META.put("A02", metaOf("战斗姿势", "充满力量感的战斗准备姿态", "⚔️"));

        ACTION_PRESETS.put("A03", actionOf(
            "坐姿休闲",
            "https://xuboxiao.top/A3.png"
        ));
        PRESET_META.put("A03", metaOf("休闲坐姿", "悠闲放松的坐姿", "🪑"));

        ACTION_PRESETS.put("A04", actionOf(
            "奔跑动态",
            "https://xuboxiao.top/A4.png"
        ));
        PRESET_META.put("A04", metaOf("奔跑动态", "充满动感的奔跑姿态", "🏃"));

        ACTION_PRESETS.put("A05", actionOf(
            "魔法释放",
            "https://xuboxiao.top/A5.png"
        ));
        PRESET_META.put("A05", metaOf("魔法释放", "释放魔法的帅气姿态", "✨"));
    }

    private static Map<String, String> metaOf(String name, String desc, String icon) {
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("name", name);
        meta.put("description", desc);
        meta.put("icon", icon);
        return meta;
    }

    private static Map<String, Object> actionOf(String title, String imageUrl) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("title", title);
        action.put("referenceImageUrl", imageUrl);
        return action;
    }

    /**
     * 解析预设编号，返回类型 (E/F/A)
     */
    public String getPresetType(String code) {
        if (code == null || code.length() < 2) return null;
        String type = code.substring(0, 1).toUpperCase();
        if (TYPE_EXPRESSION.equals(type) || TYPE_FACE.equals(type) || TYPE_ACTION.equals(type)) {
            return type;
        }
        return null;
    }

    /**
     * 根据编号获取预设数据
     */
    public Map<String, Object> getPresetData(String code) {
        if (code == null) return null;
        code = code.toUpperCase().trim();
        String type = getPresetType(code);
        if (type == null) return null;

        switch (type) {
            case TYPE_EXPRESSION: return EXPRESSION_PRESETS.get(code);
            case TYPE_FACE: return FACE_PRESETS.get(code);
            case TYPE_ACTION: return ACTION_PRESETS.get(code);
            default: return null;
        }
    }

    /**
     * 获取预设元信息
     */
    public Map<String, String> getPresetMeta(String code) {
        if (code == null) return null;
        return PRESET_META.get(code.toUpperCase().trim());
    }

    /**
     * 获取所有预设列表（供前端展示）
     */
    public List<Map<String, Object>> getAllPresets() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> entry : PRESET_META.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", entry.getKey());
            item.putAll(entry.getValue());
            item.put("type", getPresetType(entry.getKey()));
            list.add(item);
        }
        return list;
    }

    /**
     * 判断编号是否为有效预设
     */
    public boolean isValidPreset(String code) {
        return getPresetData(code) != null;
    }
}