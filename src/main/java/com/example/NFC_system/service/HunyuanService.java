package com.example.NFC_system.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class HunyuanService {

    @Value("${hunyuan.api-key:}")
    private String API_KEY;

    @Value("${hunyuan.submit-url:https://api.ai3d.cloud.tencent.com/v1/ai3d/submit}")
    private String SUBMIT_URL;

    @Value("${hunyuan.query-url:https://api.ai3d.cloud.tencent.com/v1/ai3d/query}")
    private String QUERY_URL;

    private final RestTemplate restTemplate = new RestTemplate();

    // =========================
    // 🥇 提交3D任务
    // =========================
    public String submitTask(String imageUrl) throws Exception {

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", API_KEY);

    Map<String, Object> body = new HashMap<>();
    body.put("ImageUrl", imageUrl);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

    // 🔥 关键：用String接收
    String responseStr = restTemplate.postForObject(
            "https://api.ai3d.cloud.tencent.com/v1/ai3d/submit",
            request,
            String.class
    );

    // 🔥 手动转JSON
    ObjectMapper mapper = new ObjectMapper();
    Map response = mapper.readValue(responseStr, Map.class);

    Map res = (Map) response.get("Response");

    return res.get("JobId").toString();
    }

    // =========================
    // 🥈 查询结果
    // =========================
    public String queryTask(String jobId) throws Exception {

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", API_KEY);

    Map<String, Object> body = new HashMap<>();
    body.put("JobId", jobId);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

    // 🔥 还是用String接收
    String responseStr = restTemplate.postForObject(
            "https://api.ai3d.cloud.tencent.com/v1/ai3d/query",
            request,
            String.class
    );

    ObjectMapper mapper = new ObjectMapper();
    Map response = mapper.readValue(responseStr, Map.class);

    Map res = (Map) response.get("Response");

    String status = res.get("Status").toString();

    if ("DONE".equals(status)) {
        List list = (List) res.get("ResultFile3Ds");
        Map file = (Map) list.get(0);
        return file.get("Url").toString();
    }

    return status; // WAIT / RUN / FAIL
   }
}