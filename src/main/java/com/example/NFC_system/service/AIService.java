package com.example.NFC_system.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.example.NFC_system.entity.CharacterVersion;

import org.springframework.stereotype.Service;

import com.example.NFC_system.repository.CharacterVersionRepository;

import com.example.NFC_system.service.PromptBuilder;

import com.example.NFC_system.service.DoubaoService;

import com.example.NFC_system.service.HunyuanService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AIService {

    @Autowired
    private CharacterVersionRepository versionRepository;

    @Autowired
    private PromptBuilder promptBuilder;

    @Autowired
    private DoubaoService doubaoService;

    @Autowired
    private HunyuanService hunyuanService;

    @Autowired
    private R2UploadService r2UploadService;

    @Value("${app.local.model-dir:D:/models/unzip/}")
    private String modelDir;

    @Value("${app.local.model-temp-dir:D:/models/}")
    private String modelTempDir;

    // 生成2D版本
    public CharacterVersion generate2D(Long versionId) {

    CharacterVersion version = versionRepository.findById(versionId)
            .orElseThrow(() -> new RuntimeException("version not found"));

    String prompt = promptBuilder.build(version);

    // 🔥 调图生图（豆包返回临时URL，有时效性）
    String tempImageUrl = doubaoService.generateImage(prompt, version);

    // 🔥 转存到R2图床，获取永久URL
    String permanentUrl = r2UploadService.uploadImageFromUrl(tempImageUrl);

    // 🔥 存数据库（使用永久URL）
    version.setPreviewImageUrl(permanentUrl);
    version.setStatus("2D_done");

    return versionRepository.save(version); // ✅ 返回实体
}

    // 生成3D模型


  public CharacterVersion generate3D(Long versionId) throws Exception {

    CharacterVersion version = versionRepository.findById(versionId)
            .orElseThrow(() -> new RuntimeException("Version not found"));

    version.setStatus("generating_3D");

    String imageUrl = version.getPreviewImageUrl();

    String jobId = hunyuanService.submitTask(imageUrl);

    String result = null;

    // 🔥 轮询直到拿到真正URL
    for (int i = 0; i < 50; i++) {

        Thread.sleep(20000); // 10秒一次（更合理）

        result = hunyuanService.queryTask(jobId);

        System.out.println("3D状态：" + result);

        // ❗关键判断
        if (result != null && result.startsWith("http")) {
            break;
        }
    }

    // ❗再次确认（双保险）
    if (result != null && result.startsWith("http")) {

        System.out.println("开始解压: " + result);

        String objPath = unzipModel(result);

        System.out.println("解压完成 OBJ路径: " + objPath);

        String fileName = new File(objPath).getName();
        String objUrl = "/models/" + fileName;

        version.setModelUrl(objUrl);
        version.setStatus("3D_done");

    } else {
        version.setStatus("3D_failed");
    }

    return versionRepository.save(version);
}

    // 编辑版本
    public CharacterVersion updateVersion(
        Long versionId,
        String characterPrompt,
        String scenePrompt,
        String actionPrompt,
        String designImageUrl,
        String poseImageUrl,
        String extraImageUrl,
        String audioUrl,
        String faceParams,
        Integer e1,
        Integer e2,
        Integer e3,
        Integer e4,
        Integer e5,
        Integer e6,
        Integer e7
) {

    CharacterVersion version = versionRepository.findById(versionId)
            .orElseThrow(() -> new RuntimeException("version not found"));

    // 文本
    if (characterPrompt != null) version.setCharacterPrompt(characterPrompt);
    if (scenePrompt != null) version.setScenePrompt(scenePrompt);
    if (actionPrompt != null) version.setActionPrompt(actionPrompt);

    // 图片（空字符串视为清空）
    if (designImageUrl != null) version.setDesignImageUrl(designImageUrl.isEmpty() ? null : designImageUrl);
    if (poseImageUrl != null) version.setPoseImageUrl(poseImageUrl.isEmpty() ? null : poseImageUrl);
    if (extraImageUrl != null) version.setExtraImageUrl(extraImageUrl.isEmpty() ? null : extraImageUrl);

    // 音频
    if (audioUrl != null) version.setAudioUrl(audioUrl.isEmpty() ? null : audioUrl);

    // 面部细节参数（JSON字符串）
    if (faceParams != null) version.setFaceParams(faceParams);

    //  表情（核心：null → 0）
    if (e1 != null) version.setExpression1(e1);
    if (e2 != null) version.setExpression2(e2);
    if (e3 != null) version.setExpression3(e3);
    if (e4 != null) version.setExpression4(e4);
    if (e5 != null) version.setExpression5(e5);
    if (e6 != null) version.setExpression6(e6);
    if (e7 != null) version.setExpression7(e7);

    version.setStatus("edited");

    return versionRepository.save(version);
}

    // 解压zip文件(混元3d接口返回的是zip)


public String unzipModel(String zipUrl) throws Exception {

    // 👉 下载zip
    URL url = new URL(zipUrl);
    InputStream inputStream = url.openStream();

    // 临时存放路径（注意：macOS/Linux 下不能用 D:/models/）
    String tempDir = modelTempDir.endsWith("/") || modelTempDir.endsWith("\\") ? modelTempDir : modelTempDir + "/";
    String zipPath = tempDir + "temp.zip";
    File tempFile = new File(zipPath);
    if (tempFile.getParentFile() != null) tempFile.getParentFile().mkdirs();
    Files.copy(inputStream, Paths.get(zipPath), StandardCopyOption.REPLACE_EXISTING);

    // 👉 解压目录
    String destDir = modelDir.endsWith("/") || modelDir.endsWith("\\") ? modelDir : modelDir + "/";
    File dir = new File(destDir);
    if (!dir.exists()) dir.mkdirs();

    byte[] buffer = new byte[1024];
    ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath));
    ZipEntry zipEntry;

    String objFilePath = null;

    while ((zipEntry = zis.getNextEntry()) != null) {

        File newFile = new File(destDir + zipEntry.getName());

        if (zipEntry.isDirectory()) {
            newFile.mkdirs();
        } else {

            new File(newFile.getParent()).mkdirs();

            FileOutputStream fos = new FileOutputStream(newFile);

            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }

            fos.close();

            // 👉 找.obj文件
            if (newFile.getName().endsWith(".obj")) {
                objFilePath = newFile.getAbsolutePath();
            }
        }
    }

    zis.closeEntry();
    zis.close();

    if (objFilePath == null) {
        throw new RuntimeException("没有找到OBJ模型");
    }

    return objFilePath;
}




}