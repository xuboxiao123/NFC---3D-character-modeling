package com.example.NFC_system.controller;

import com.example.NFC_system.entity.CharacterVersion;
import com.example.NFC_system.entity.CommunityShare;
import com.example.NFC_system.entity.NfcFaceCard;
import com.example.NFC_system.repository.CharacterVersionRepository;
import com.example.NFC_system.repository.CommunityShareRepository;
import com.example.NFC_system.repository.NfcFaceCardRepository;
import com.example.NFC_system.service.NfcMappingService;
import com.example.NFC_system.service.NfcPresetService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/nfc")
public class NfcController {

    private final CharacterVersionRepository repository;
    private final CommunityShareRepository communityShareRepository;
    private final NfcFaceCardRepository nfcFaceCardRepository;
    private final NfcMappingService nfcMappingService;
    private final NfcPresetService nfcPresetService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 6个NFC读卡器对应的面部部位名称（根据实际物理安装位置映射）
    private static final String[] PART_NAMES = {
        "右脸颊", "下巴", "左脸颊", "左眼", "额头", "右眼"
    };

    // 最近一次预设指令（供前端轮询获取）
    private volatile Map<String, Object> lastPresetCommand = null;
    private volatile long lastPresetTimestamp = 0;

    // 最近一次 NFC 笔操作（供前端轮询获取，区分笔和卡片）
    private volatile Map<String, Object> lastPenCommand = null;
    private volatile long lastPenTimestamp = 0;

    // 写卡指令状态（前端下发 → ESP32 轮询获取 → 写卡完成回调）
    private volatile String pendingWriteCode = null;
    private volatile String pendingWriteStatus = "idle"; // idle / pending / writing / done / failed
    private volatile long pendingWriteTimestamp = 0;

    public NfcController(CharacterVersionRepository repository,
                         CommunityShareRepository communityShareRepository,
                         NfcFaceCardRepository nfcFaceCardRepository,
                         NfcMappingService nfcMappingService,
                         NfcPresetService nfcPresetService) {
        this.repository = repository;
        this.communityShareRepository = communityShareRepository;
        this.nfcFaceCardRepository = nfcFaceCardRepository;
        this.nfcMappingService = nfcMappingService;
        this.nfcPresetService = nfcPresetService;
    }

    // 当前绑定的版本
    private Long currentVersionId = null;

    // =========================
    // 前端调用：设置版本ID
    // =========================
    @GetMapping("/setCurrent")
    public Map<String, Object> setCurrent(@RequestParam Long versionId) {
        currentVersionId = versionId;
        System.out.println("当前版本设为: " + versionId);
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        result.put("versionId", versionId);
        return result;
    }

    // =========================
    // 前端调用：获取当前绑定的版本ID
    // =========================
    @GetMapping("/current")
    public Map<String, Object> getCurrent() {
        Map<String, Object> result = new HashMap<>();
        result.put("versionId", currentVersionId);
        result.put("partNames", PART_NAMES);
        return result;
    }

    // =========================
    // 前端轮询：获取当前版本的 faceParams + 预设指令（合并轮询）
    // =========================
    @GetMapping("/faceParams")
    public Map<String, Object> getFaceParams(@RequestParam(required = false) Long lastTs) {
        Map<String, Object> result = new HashMap<>();
        if (currentVersionId == null) {
            result.put("status", "error");
            result.put("message", "no version set");
            return result;
        }
        CharacterVersion v = repository.findById(currentVersionId).orElse(null);
        if (v == null) {
            result.put("status", "error");
            result.put("message", "version not found");
            return result;
        }
        String fp = v.getFaceParams();
        result.put("status", "ok");
        result.put("versionId", currentVersionId);
        result.put("faceParams", fp);

        // 如果有新的预设指令且前端还没消费过，附带返回
        if (lastPresetCommand != null) {
            long ts = lastPresetTimestamp;
            if (lastTs == null || ts > lastTs) {
                result.put("presetCommand", lastPresetCommand);
                result.put("presetTimestamp", ts);
            }
        }

        // 如果有新的笔操作且前端还没消费过，附带返回
        if (lastPenCommand != null) {
            long pts = lastPenTimestamp;
            if (lastTs == null || pts > lastTs) {
                result.put("penCommand", lastPenCommand);
                result.put("penTimestamp", pts);
            }
        }
        return result;
    }

    // =========================
    // 前端轮询：获取最新预设指令（不依赖 currentVersionId，用于版本卡跳转等全局场景）
    // =========================
    @GetMapping("/presetCommand")
    public Map<String, Object> getPresetCommand(@RequestParam(required = false) Long lastTs) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");

        if (lastPresetCommand != null) {
            long ts = lastPresetTimestamp;
            if (lastTs == null || ts > lastTs) {
                result.put("presetCommand", lastPresetCommand);
                result.put("presetTimestamp", ts);
            }
        }
        return result;
    }

    // =========================
    // 获取所有预设列表（供前端展示卡片信息）
    // =========================
    @GetMapping("/presets")
    public Map<String, Object> getPresets() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        result.put("presets", nfcPresetService.getAllPresets());
        return result;
    }

    // =========================
    // ESP32 调用：刷卡（增量模式）
    // 接收 readerIndex(0~5) 和 uid
    // 两张卡：凸一点(+1) / 凹一点(-1)
    // 数据库存 1~11，5=中间值(0)
    // =========================
    @PostMapping("/apply")
    public Map<String, Object> apply(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();

        // ★ 新增：检查是否携带预设编号（presetCode字段）
        if (body.containsKey("presetCode")) {
            return handlePresetCard(body, result);
        }

        // 兼容旧版 pin 字段和新版 readerIndex 字段
        Integer readerIndex;
        if (body.containsKey("readerIndex")) {
            readerIndex = Integer.valueOf(body.get("readerIndex").toString());
        } else if (body.containsKey("pin")) {
            int pin = Integer.valueOf(body.get("pin").toString());
            readerIndex = pinToIndex(pin);
        } else {
            result.put("status", "error");
            result.put("message", "missing readerIndex or pin");
            return result;
        }

        String uid = body.get("uid").toString();
        String cardType = nfcMappingService.getCardType(uid);

        System.out.println("NFC readerIndex=" + readerIndex + " uid=" + uid + " type=" + cardType);

        if (currentVersionId == null) {
            System.out.println("没有设置版本");
            result.put("status", "error");
            result.put("message", "no version set");
            return result;
        }

        if (readerIndex < 0 || readerIndex >= PART_NAMES.length) {
            result.put("status", "error");
            result.put("message", "invalid readerIndex: " + readerIndex);
            return result;
        }

        CharacterVersion v = repository.findById(currentVersionId).orElse(null);
        if (v == null) {
            result.put("status", "error");
            result.put("message", "version not found");
            return result;
        }

        // 获取增量值
        int delta = nfcMappingService.mapUidToDelta(uid);
        if (delta == 0) {
            result.put("status", "error");
            result.put("message", "unknown card uid: " + uid);
            return result;
        }

        // 获取当前值，应用增量
        Integer currentValue = getExpression(v, readerIndex);
        int newValue = nfcMappingService.applyDelta(currentValue, delta);

        // 写入新值
        setExpression(v, readerIndex, newValue);

        // 同步更新 faceParams JSON 中对应的参数
        syncExpressionToFaceParams(v, readerIndex, newValue);

        repository.save(v);

        String partName = PART_NAMES[readerIndex];
        System.out.println("✅ [笔] " + partName + " " + (delta > 0 ? "凸" : "凹") + " → " + newValue);

        // 记录笔操作，供前端轮询区分笔和卡片
        Map<String, Object> penCmd = new LinkedHashMap<>();
        penCmd.put("deviceType", "pen");
        penCmd.put("readerIndex", readerIndex);
        penCmd.put("paramName", NfcMappingService.getParamName(readerIndex));
        penCmd.put("delta", delta);
        penCmd.put("newValue", newValue);
        penCmd.put("partName", partName);
        lastPenCommand = penCmd;
        lastPenTimestamp = System.currentTimeMillis();

        result.put("status", "ok");
        result.put("deviceType", "pen");
        result.put("part", partName);
        result.put("readerIndex", readerIndex);
        result.put("cardType", cardType);
        result.put("delta", delta);
        result.put("oldValue", currentValue != null ? currentValue : 5);
        result.put("newValue", newValue);
        return result;
    }

    // =========================
    // 前端调用：请求写卡（前端 → 后端存储 → ESP32 轮询获取）
    // =========================
    @PostMapping("/writeCard")
    public Map<String, Object> requestWriteCard(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        String code = body.containsKey("code") ? body.get("code").toString().toUpperCase().trim() : "";

        if (code.isEmpty()) {
            result.put("status", "error");
            result.put("message", "missing code");
            return result;
        }

        pendingWriteCode = code;
        pendingWriteStatus = "pending";
        pendingWriteTimestamp = System.currentTimeMillis();

        System.out.println("📝 收到写卡请求: " + code);

        result.put("status", "ok");
        result.put("code", code);
        result.put("writeStatus", pendingWriteStatus);
        return result;
    }

    // =========================
    // ESP32 轮询：获取待写卡指令
    // =========================
    @GetMapping("/pendingWrite")
    public Map<String, Object> getPendingWrite() {
        Map<String, Object> result = new HashMap<>();
        if ("pending".equals(pendingWriteStatus) && pendingWriteCode != null) {
            result.put("status", "pending");
            result.put("code", pendingWriteCode);
            // ESP32 拿到后，标记为 writing
            pendingWriteStatus = "writing";
        } else {
            result.put("status", pendingWriteStatus != null ? pendingWriteStatus : "idle");
        }
        return result;
    }

    // =========================
    // ESP32 回调：写卡完成通知
    // =========================
    @PostMapping("/writeDone")
    public Map<String, Object> writeDone(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        boolean success = body.containsKey("success") && Boolean.parseBoolean(body.get("success").toString());
        String code = body.containsKey("code") ? body.get("code").toString() : "";

        pendingWriteStatus = success ? "done" : "failed";
        System.out.println("📝 写卡" + (success ? "成功" : "失败") + ": " + code);

        result.put("status", "ok");
        result.put("writeStatus", pendingWriteStatus);
        result.put("code", code);
        return result;
    }

    // =========================
    // 前端轮询：获取写卡状态
    // =========================
    @GetMapping("/writeStatus")
    public Map<String, Object> getWriteStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("writeStatus", pendingWriteStatus);
        result.put("code", pendingWriteCode);
        result.put("timestamp", pendingWriteTimestamp);

        // 如果已完成或失败，前端获取后重置为 idle
        if ("done".equals(pendingWriteStatus) || "failed".equals(pendingWriteStatus)) {
            String finalStatus = pendingWriteStatus;
            // 延迟重置，让前端有机会读到
            result.put("writeStatus", finalStatus);
            pendingWriteStatus = "idle";
            pendingWriteCode = null;
        }
        return result;
    }

    // =========================
    // 前端调用：保存当前面部参数到 NFC 卡
    // 1. 存入数据库 nfc_face_card 表
    // 2. 生成 C 编号（C01, C02...）
    // 3. 下发写卡指令给 ESP32
    // =========================
    @PostMapping("/saveFaceCard")
    public Map<String, Object> saveFaceCard(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();

        String faceParamsJson;
        try {
            Object fp = body.get("faceParams");
            if (fp instanceof String) {
                faceParamsJson = (String) fp;
            } else {
                faceParamsJson = objectMapper.writeValueAsString(fp);
            }
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "invalid faceParams");
            return result;
        }

        String name = body.containsKey("name") ? body.get("name").toString() : "Custom Face";

        Long versionId = currentVersionId;
        if (body.containsKey("versionId") && body.get("versionId") != null && !"null".equals(body.get("versionId").toString())) {
            versionId = Long.valueOf(body.get("versionId").toString());
        }

        if (versionId == null) {
            result.put("status", "error");
            result.put("message", "no version selected");
            return result;
        }

        // 生成下一个 C 编号
        Integer maxNum = nfcFaceCardRepository.findMaxCodeNumber();
        int nextNum = (maxNum != null ? maxNum : 0) + 1;
        String code = "C" + String.format("%02d", nextNum);

        // 保存到数据库
        NfcFaceCard card = new NfcFaceCard();
        card.setCode(code);
        card.setName(name);
        card.setVersionId(versionId);
        card.setFaceParams(faceParamsJson);
        nfcFaceCardRepository.save(card);

        System.out.println("💾 自定义面部卡保存: " + code + " (" + name + ")");

        // 下发写卡指令给 ESP32
        pendingWriteCode = code;
        pendingWriteStatus = "pending";
        pendingWriteTimestamp = System.currentTimeMillis();

        result.put("status", "ok");
        result.put("code", code);
        result.put("name", name);
        result.put("cardId", card.getId());
        result.put("writeStatus", "pending");
        return result;
    }

    // =========================
    // 处理预设卡片（E/F/A/C 类型 + V 社区/版本卡）
    // =========================
    private Map<String, Object> handlePresetCard(Map<String, Object> body, Map<String, Object> result) {
        String presetCode = body.get("presetCode").toString().toUpperCase().trim();
        String uid = body.containsKey("uid") ? body.get("uid").toString() : "unknown";
        int readerIndex = body.containsKey("readerIndex") ? Integer.valueOf(body.get("readerIndex").toString()) : -1;

        System.out.println("🎴 预设卡片: code=" + presetCode + " uid=" + uid + " reader=" + readerIndex);

        // ★ 优先处理 V 类型社区/版本卡
        if (presetCode.startsWith("V")) {
            return handleVersionCard(presetCode, result);
        }

        // ★ 处理 C 类型自定义面部参数卡
        if (presetCode.startsWith("C")) {
            return handleCustomFaceCard(presetCode, result);
        }

        String presetType = nfcPresetService.getPresetType(presetCode);
        if (presetType == null) {
            result.put("status", "error");
            result.put("message", "invalid preset code: " + presetCode);
            return result;
        }

        Map<String, Object> presetData = nfcPresetService.getPresetData(presetCode);
        Map<String, String> presetMeta = nfcPresetService.getPresetMeta(presetCode);
        if (presetData == null) {
            result.put("status", "error");
            result.put("message", "preset not found: " + presetCode);
            return result;
        }

        // 根据预设类型处理
        switch (presetType) {
            case NfcPresetService.TYPE_EXPRESSION:
            case NfcPresetService.TYPE_FACE:
                // 表情/脸型预设：直接覆盖 faceParams
                return applyFacePreset(presetCode, presetType, presetData, presetMeta, result);

            case NfcPresetService.TYPE_ACTION:
                // 动作预设：存储指令供前端轮询获取
                return applyActionPreset(presetCode, presetData, presetMeta, result);

            default:
                result.put("status", "error");
                result.put("message", "unknown preset type: " + presetType);
                return result;
        }
    }

    /**
     * 应用表情/脸型预设 - 直接覆盖当前版本的 faceParams
     */
    private Map<String, Object> applyFacePreset(String code, String type, Map<String, Object> presetData,
                                                  Map<String, String> meta, Map<String, Object> result) {
        if (currentVersionId == null) {
            result.put("status", "error");
            result.put("message", "no version set");
            return result;
        }

        CharacterVersion v = repository.findById(currentVersionId).orElse(null);
        if (v == null) {
            result.put("status", "error");
            result.put("message", "version not found");
            return result;
        }

        try {
            // 如果是复位卡(E05)，清空所有参数
            Map<String, Object> newParams;
            if (presetData.isEmpty()) {
                newParams = new LinkedHashMap<>();
            } else {
                // 获取现有参数
                Map<String, Object> existingParams;
                String existingJson = v.getFaceParams();
                if (existingJson != null && !existingJson.trim().isEmpty()) {
                    existingParams = objectMapper.readValue(existingJson, new TypeReference<LinkedHashMap<String, Object>>() {});
                } else {
                    existingParams = new LinkedHashMap<>();
                }
                // 用预设值覆盖
                existingParams.putAll(presetData);
                newParams = existingParams;
            }

            String newJson = objectMapper.writeValueAsString(newParams);
            v.setFaceParams(newJson);

            // 同步更新 expression 字段
            for (int i = 0; i < NfcMappingService.NFC_PARAM_MAPPING.length; i++) {
                String paramName = NfcMappingService.NFC_PARAM_MAPPING[i];
                if (newParams.containsKey(paramName)) {
                    double val = ((Number) newParams.get(paramName)).doubleValue();
                    setExpression(v, i, NfcMappingService.faceParamToExpression(val));
                } else if (presetData.isEmpty()) {
                    // 复位：expression 也重置为中间值
                    setExpression(v, i, NfcMappingService.NEUTRAL);
                }
            }

            repository.save(v);

            String presetName = meta != null ? meta.get("name") : code;
            System.out.println("✅ 预设应用成功: " + code + " (" + presetName + ") → faceParams=" + newJson.substring(0, Math.min(newJson.length(), 100)));

            result.put("status", "ok");
            result.put("presetCode", code);
            result.put("presetType", type);
            result.put("presetName", presetName);
            result.put("faceParams", newJson);
            return result;

        } catch (Exception e) {
            System.err.println("⚠️ 预设应用失败: " + e.getMessage());
            result.put("status", "error");
            result.put("message", "preset apply failed: " + e.getMessage());
            return result;
        }
    }

    /**
     * 应用动作预设 - 存储指令供前端轮询获取，同时更新数据库
     */
    private Map<String, Object> applyActionPreset(String code, Map<String, Object> presetData,
                                                    Map<String, String> meta, Map<String, Object> result) {
        if (currentVersionId == null) {
            result.put("status", "error");
            result.put("message", "no version set");
            return result;
        }

        CharacterVersion v = repository.findById(currentVersionId).orElse(null);
        if (v == null) {
            result.put("status", "error");
            result.put("message", "version not found");
            return result;
        }

        // 动作卡只更新参考图，不写入 actionPrompt
        String refImage = (String) presetData.get("referenceImageUrl");
        if (refImage != null) {
            v.setPoseImageUrl(refImage);
        }
        repository.save(v);

        // 存储预设指令供前端轮询
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("presetCode", code);
        command.put("presetType", NfcPresetService.TYPE_ACTION);
        command.put("presetName", meta != null ? meta.get("name") : code);
        command.putAll(presetData);
        lastPresetCommand = command;
        lastPresetTimestamp = System.currentTimeMillis();

        String presetName = meta != null ? meta.get("name") : code;
        System.out.println("✅ 动作预设应用: " + code + " (" + presetName + ")");

        result.put("status", "ok");
        result.put("presetCode", code);
        result.put("presetType", NfcPresetService.TYPE_ACTION);
        result.put("presetName", presetName);
        result.put("referenceImageUrl", refImage);
        return result;
    }

    /**
     * 处理社区/版本卡（V{versionId}）
     * 例如：V78 → 查找版本78
     * - 若该版本已分享到社区，则跳转到编辑页 /editor/{characterId}?versionId={versionId}
     * - 若未分享到社区，则跳 /model-view/{versionId}
     * 同时把跳转指令写入 lastPresetCommand，供前端轮询后自动跳转
     */
    private Map<String, Object> handleVersionCard(String presetCode, Map<String, Object> result) {
        try {
            if (presetCode.length() < 2) {
                result.put("status", "error");
                result.put("message", "invalid version card code: " + presetCode);
                return result;
            }

            Long versionId = Long.valueOf(presetCode.substring(1));
            CharacterVersion version = repository.findById(versionId).orElse(null);
            if (version == null) {
                result.put("status", "error");
                result.put("message", "version not found: " + versionId);
                return result;
            }

            Optional<CommunityShare> shareOpt = communityShareRepository.findByVersionId(versionId);

            String targetType;
            String targetUrl;
            Long shareId = null;

            if (shareOpt.isPresent()) {
                shareId = shareOpt.get().getId();
                targetType = "editor";
                targetUrl = "/editor/" + version.getCharacterId() + "?versionId=" + versionId;
            } else {
                targetType = "version";
                targetUrl = "/model-view/" + versionId;
            }

            // 存储指令供前端轮询
            Map<String, Object> command = new LinkedHashMap<>();
            command.put("presetCode", presetCode);
            command.put("presetType", "V");
            command.put("cardType", "version");
            command.put("versionId", versionId);
            command.put("characterId", version.getCharacterId());
            command.put("targetType", targetType);
            command.put("targetUrl", targetUrl);
            if (shareId != null) {
                command.put("shareId", shareId);
            }
            lastPresetCommand = command;
            lastPresetTimestamp = System.currentTimeMillis();

            System.out.println("✅ 社区/版本卡: " + presetCode + " -> " + targetUrl);

            result.put("status", "ok");
            result.put("presetCode", presetCode);
            result.put("presetType", "V");
            result.put("cardType", "version");
            result.put("versionId", versionId);
            result.put("characterId", version.getCharacterId());
            result.put("targetType", targetType);
            result.put("targetUrl", targetUrl);
            if (shareId != null) {
                result.put("shareId", shareId);
            }
            return result;

        } catch (NumberFormatException e) {
            result.put("status", "error");
            result.put("message", "invalid version card code: " + presetCode);
            return result;
        }
    }

    /**
     * 处理自定义面部参数卡（C01, C02...）
     * 从数据库查询参数，应用到当前版本，并通知前端刷新
     */
    private Map<String, Object> handleCustomFaceCard(String presetCode, Map<String, Object> result) {
        Optional<NfcFaceCard> cardOpt = nfcFaceCardRepository.findByCode(presetCode);
        if (cardOpt.isEmpty()) {
            result.put("status", "error");
            result.put("message", "custom face card not found: " + presetCode);
            return result;
        }

        if (currentVersionId == null) {
            result.put("status", "error");
            result.put("message", "no version set");
            return result;
        }

        CharacterVersion v = repository.findById(currentVersionId).orElse(null);
        if (v == null) {
            result.put("status", "error");
            result.put("message", "version not found");
            return result;
        }

        NfcFaceCard card = cardOpt.get();

        try {
            v.setFaceParams(card.getFaceParams());

            Map<String, Object> params = objectMapper.readValue(
                card.getFaceParams(),
                new TypeReference<LinkedHashMap<String, Object>>() {}
            );

            for (int i = 0; i < NfcMappingService.NFC_PARAM_MAPPING.length; i++) {
                String paramName = NfcMappingService.NFC_PARAM_MAPPING[i];
                if (params.containsKey(paramName) && params.get(paramName) instanceof Number) {
                    double val = ((Number) params.get(paramName)).doubleValue();
                    setExpression(v, i, NfcMappingService.faceParamToExpression(val));
                }
            }

            repository.save(v);

            Map<String, Object> command = new LinkedHashMap<>();
            command.put("presetCode", presetCode);
            command.put("presetType", "C");
            command.put("presetName", card.getName());
            command.put("faceParams", params);
            lastPresetCommand = command;
            lastPresetTimestamp = System.currentTimeMillis();

            System.out.println("✅ 自定义面部卡应用: " + presetCode + " (" + card.getName() + ")");

            result.put("status", "ok");
            result.put("presetCode", presetCode);
            result.put("presetType", "C");
            result.put("presetName", card.getName());
            result.put("faceParams", params);
            return result;
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "apply custom card failed: " + e.getMessage());
            return result;
        }
    }

    // 获取指定 expression 的当前值（readerIndex 0~5 → expression1~expression6）
    private Integer getExpression(CharacterVersion v, int index) {
        switch (index) {
            case 0: return v.getExpression1();  // 右脸颊
            case 1: return v.getExpression2();  // 下巴
            case 2: return v.getExpression3();  // 左脸颊
            case 3: return v.getExpression4();  // 左眼
            case 4: return v.getExpression5();  // 额头
            case 5: return v.getExpression6();  // 右眼
            default: return null;
        }
    }

    // 设置指定 expression 的值（readerIndex 0~5 → expression1~expression6）
    private void setExpression(CharacterVersion v, int index, int value) {
        switch (index) {
            case 0: v.setExpression1(value); break;  // 右脸颊
            case 1: v.setExpression2(value); break;  // 下巴
            case 2: v.setExpression3(value); break;  // 左脸颊
            case 3: v.setExpression4(value); break;  // 左眼
            case 4: v.setExpression5(value); break;  // 额头
            case 5: v.setExpression6(value); break;  // 右眼
        }
    }

    /**
     * 将 expression 值同步写入 faceParams JSON
     * expression 整数值(1~11) → faceParam 浮点值(-1.0~1.0)
     */
    private void syncExpressionToFaceParams(CharacterVersion v, int readerIndex, int newExprValue) {
        try {
            String paramName = NfcMappingService.getParamName(readerIndex);
            if (paramName == null) return;

            // 解析现有的 faceParams JSON，如果为空则创建新的
            Map<String, Object> paramsMap;
            String existingJson = v.getFaceParams();
            if (existingJson != null && !existingJson.trim().isEmpty()) {
                paramsMap = objectMapper.readValue(existingJson, new TypeReference<LinkedHashMap<String, Object>>() {});
            } else {
                paramsMap = new LinkedHashMap<>();
            }

            // 将 expression 整数值转换为 faceParam 浮点值
            double faceParamValue = NfcMappingService.expressionToFaceParam(newExprValue);
            paramsMap.put(paramName, faceParamValue);

            // 写回 JSON
            v.setFaceParams(objectMapper.writeValueAsString(paramsMap));
            System.out.println("📝 faceParams 同步: " + paramName + " = " + faceParamValue);
        } catch (Exception e) {
            System.err.println("⚠️ faceParams 同步失败: " + e.getMessage());
        }
    }

    // 旧版 pin 到 readerIndex 的映射
    private int pinToIndex(int pin) {
        switch (pin) {
            case 5:  return 0;
            case 17: return 1;
            case 16: return 2;
            case 4:  return 3;
            case 2:  return 4;
            case 15: return 5;
            case 13: return 6;
            default: return -1;
        }
    }
}