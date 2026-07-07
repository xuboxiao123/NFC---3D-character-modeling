package com.example.NFC_system.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.net.*;
import java.util.Enumeration;

/**
 * UDP 广播服务发现
 * 后端启动后持续监听 UDP 端口 8089
 * ESP32 发送 "NFC_DISCOVER" 广播，后端回复自己的IP和端口
 * 比 mDNS 更可靠，特别是在手机热点等环境下
 */
@Component
public class MdnsConfig {

    private DatagramSocket udpSocket;
    private Thread listenerThread;
    private volatile boolean running = true;

    @PostConstruct
    public void startDiscoveryListener() {
        listenerThread = new Thread(() -> {
            try {
                udpSocket = new DatagramSocket(8089);
                udpSocket.setBroadcast(true);
                udpSocket.setSoTimeout(1000); // 1秒超时，方便退出

                String localIp = getLocalIp();
                System.out.println("✅ UDP 发现服务已启动 (端口 8089)");
                System.out.println("   本机IP: " + localIp);
                System.out.println("   ESP32 可通过 UDP 广播自动发现此服务器");

                byte[] buf = new byte[256];

                while (running) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        udpSocket.receive(packet);

                        String message = new String(packet.getData(), 0, packet.getLength()).trim();

                        if ("NFC_DISCOVER".equals(message)) {
                            // 回复: IP:PORT
                            localIp = getLocalIp(); // 每次重新获取，防止IP变化
                            String response = "NFC_SERVER:" + localIp + ":8088";
                            byte[] responseData = response.getBytes();

                            DatagramPacket reply = new DatagramPacket(
                                    responseData, responseData.length,
                                    packet.getAddress(), packet.getPort()
                            );
                            udpSocket.send(reply);

                            System.out.println("📡 UDP 发现请求来自 " + packet.getAddress().getHostAddress()
                                    + " → 回复: " + response);
                        }
                    } catch (SocketTimeoutException e) {
                        // 正常超时，继续循环
                    }
                }
            } catch (Exception e) {
                System.out.println("❌ UDP 发现服务启动失败: " + e.getMessage());
            }
        }, "udp-discovery");

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    @PreDestroy
    public void stopDiscoveryListener() {
        running = false;
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
        System.out.println("UDP 发现服务已停止");
    }

    private String getLocalIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            String fallback = "127.0.0.1";
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith("192.168.")) return ip;
                        fallback = ip;
                    }
                }
            }
            return fallback;
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}