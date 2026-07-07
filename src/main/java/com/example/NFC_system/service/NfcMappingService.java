package com.example.NFC_system.service;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class NfcMappingService {

    // 数据库存储范围: 1~11, 中间值5代表"不变"(0)
    // 凸卡: +1 (向外凸), 凹卡: -1 (向内凹)
    // 每刷一次卡 expression ±1，外部显示值 ±0.1（即 1/6 ≈ 0.1667）
    public static final int MIN_VALUE = 1;
    public static final int MAX_VALUE = 11;
    public static final int NEUTRAL = 5;  // 中间值，代表0

    // UID -> 增量映射
    private static final Map<String, Integer> UID_DELTA_MAP = new HashMap<>();

    static {
        UID_DELTA_MAP.put("F37E287", +1);   // 凸卡: +1
        UID_DELTA_MAP.put("243A2B7", -1);   // 凹卡: -1
        UID_DELTA_MAP.put("4D7AA9EF", +1);  // 凸卡: +1
        UID_DELTA_MAP.put("5D7AA9EF", -1);  // 凹卡: -1
    }

    // NFC 读卡器索引 → faceParam 参数名 映射
    // 根据实际物理安装位置重新映射：
    // reader 0=右脸颊, 1=下巴, 2=左脸颊, 3=左眼, 4=额头, 5=右眼
    public static final String[] NFC_PARAM_MAPPING = {
        "cheekRight",      // reader 0 → 右脸颊
        "chinLength",      // reader 1 → 下巴
        "cheekLeft",       // reader 2 → 左脸颊
        "eyeSizeLeft",     // reader 3 → 左眼大小
        "foreheadHeight",  // reader 4 → 额头高度
        "eyeSizeRight",    // reader 5 → 右眼大小
    };

    /**
     * 根据 UID 返回增量值 (+1 或 -1)
     */
    public int mapUidToDelta(String uid) {
        return UID_DELTA_MAP.getOrDefault(uid.toUpperCase(), 0);
    }

    /**
     * 应用增量，限制在 1~11 范围内
     */
    public int applyDelta(Integer currentValue, int delta) {
        // 兼容旧数据：null 或 0 都当作中间值5
        int current = (currentValue != null && currentValue != 0) ? currentValue : NEUTRAL;
        int newValue = current + delta;
        return Math.max(MIN_VALUE, Math.min(MAX_VALUE, newValue));
    }

    /**
     * 根据 UID 返回卡片类型
     */
    public String getCardType(String uid) {
        Integer delta = UID_DELTA_MAP.get(uid.toUpperCase());
        if (delta == null) return "unknown";
        return delta > 0 ? "convex" : "concave";
    }

    /**
     * expression 值 (整数 1~11, 中性=5) 转换为 faceParam 值 (浮点 -1.0~1.0)
     * 1 → -0.667, 5 → 0.0, 11 → 1.0
     * 每 ±1 步 → ±0.1667
     */
    public static double expressionToFaceParam(int exprValue) {
        return Math.max(-1.0, Math.min(1.0, (exprValue - NEUTRAL) / 6.0));
    }

    /**
     * faceParam 值 (浮点 -1.0~1.0) 转换为 expression 值 (整数 1~11)
     */
    public static int faceParamToExpression(double paramValue) {
        return Math.max(MIN_VALUE, Math.min(MAX_VALUE, (int) Math.round(paramValue * 6.0 + NEUTRAL)));
    }

    /**
     * 根据 readerIndex 获取对应的 faceParam 参数名
     */
    public static String getParamName(int readerIndex) {
        if (readerIndex >= 0 && readerIndex < NFC_PARAM_MAPPING.length) {
            return NFC_PARAM_MAPPING[readerIndex];
        }
        return null;
    }
}