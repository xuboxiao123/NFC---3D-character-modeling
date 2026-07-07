package com.example.NFC_system.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    @GetMapping("/info")
    public Map<String, Object> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        List<String> ips = getLocalIps();

        info.put("ips", ips);
        info.put("frontendPort", 5173);
        info.put("backendPort", 8088);

        // 生成访问地址
        List<String> accessUrls = new ArrayList<>();
        for (String ip : ips) {
            accessUrls.add("https://" + ip + ":5173");
        }
        info.put("accessUrls", accessUrls);

        return info;
    }

    /**
     * ESP32 专用：自动发现服务器地址
     * ESP32 连上同一WiFi后，访问此接口获取 NFC apply 的完整URL
     * 返回格式: { "nfcUrl": "http://192.168.x.x:8088/api/nfc/apply", "ips": [...] }
     */
    @GetMapping("/discover")
    public Map<String, Object> discover() {
        Map<String, Object> result = new HashMap<>();
        List<String> ips = getLocalIps();

        result.put("ips", ips);

        // 优先选择192.168开头的局域网IP
        String bestIp = ips.stream()
                .filter(ip -> ip.startsWith("192.168."))
                .findFirst()
                .orElse(ips.isEmpty() ? "127.0.0.1" : ips.get(0));

        result.put("serverIp", bestIp);
        result.put("nfcUrl", "http://" + bestIp + ":8088/api/nfc/apply");
        result.put("backendPort", 8088);

        return result;
    }

    private List<String> getLocalIps() {
        List<String> ips = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;

                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof java.net.Inet4Address) {
                        ips.add(addr.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            ips.add("unknown");
        }
        return ips;
    }
}