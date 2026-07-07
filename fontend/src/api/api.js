import axios from "axios";

const API = axios.create({
    baseURL: "/api",
    timeout: 60000, // 默认60秒超时
});

// 生成2D
export const generate2D = (id) => {
    return API.post(`/version/generate2D?versionId=${id}`, null, {
        timeout: 300000 // 5分钟超时（AI生成较慢）
    });
};

// 生成3D
export const generate3D = (id) => {
    return API.post(`/version/generate3D?versionId=${id}`, null, {
        timeout: 300000 // 5分钟超时（AI生成较慢）
    });
};

// 3D打印 - 启动Bambu Studio
export const print3D = (id) => {
    return API.post(`/version/print3D?versionId=${id}`);
};

// 保存面部细节参数到数据库（可选带sculptedModelUrl）
export const saveFaceParams = (versionId, faceParams, sculptedModelUrl) => {
    let url = `/version/saveFaceParams?versionId=${versionId}`;
    if (sculptedModelUrl) {
        url += `&sculptedModelUrl=${encodeURIComponent(sculptedModelUrl)}`;
    }
    return API.put(url, JSON.stringify(faceParams), {
        headers: { 'Content-Type': 'application/json' },
        timeout: 30000 // 30秒超时
    });
};

// 保存修改后的3D模型文件（OBJ）- 直接写入本地文件，不走multipart，无大小限制
export const uploadSculptedModel = (file, prefix) => {
    // 使用新的本地保存接口：直接将OBJ内容作为请求体发送
    // 不走multipart，避免MaxUploadSizeExceededException
    return API.post(`/upload/model/local?prefix=${encodeURIComponent(prefix || '')}`, file, {
        headers: { 'Content-Type': 'application/octet-stream' },
        timeout: 120000, // 2分钟超时（大文件）
        maxContentLength: Infinity,
        maxBodyLength: Infinity,
    });
};

// 获取版本详情（含面部参数）
export const getVersion = (versionId) => {
    return API.get(`/version/get?versionId=${versionId}`);
};

// NFC 轻量轮询：获取当前绑定版本的 faceParams + 预设指令
// lastTs: 上次预设时间戳，用于避免重复消费
export const getNfcFaceParams = (lastTs) => {
    const params = lastTs ? { lastTs } : {};
    return API.get('/nfc/faceParams', { params, timeout: 1500 });
};

// 获取所有NFC预设列表
export const getNfcPresets = () => {
    return API.get('/nfc/presets');
};

// NFC 写卡：前端请求后端下发写卡指令给 ESP32
export const requestNfcWriteCard = (payload) => {
    return API.post('/nfc/writeCard', payload);
};

// NFC 写卡状态轮询
export const getNfcWriteStatus = () => {
    return API.get('/nfc/writeStatus', { timeout: 2000 });
};

// NFC 预设指令轮询（不依赖 currentVersionId，用于版本卡跳转等全局场景）
export const getNfcPresetCommand = (lastTs) => {
    const params = lastTs ? { lastTs } : {};
    return API.get('/nfc/presetCommand', { params, timeout: 2000 });
};

// NFC 保存自定义面部参数卡（存入数据库 + 下发写卡指令给 ESP32）
export const saveFaceCard = (faceParams, versionId, name) => {
    return API.post('/nfc/saveFaceCard', { faceParams, versionId, name });
};