
#include <Arduino.h>
#include <SPI.h>
#include <MFRC522.h>
#include <WiFi.h>
#include <HTTPClient.h>
#include <WebServer.h>
#include <DNSServer.h>
#include <Preferences.h>
#include <ArduinoJson.h>
#include <WiFiUdp.h>

// ===== RC522 读卡器配置 =====
#define RST_PIN 22
// 根据原理图，7个RC522的SDA(SS)引脚分别为:
// J1=GPIO5, J2=GPIO17, J3=GPIO16, J4=GPIO4, J5=GPIO2, J6=GPIO15, J7=GPIO21(未连接)
// 由于D21未连接，实际只使用前6个读卡器
#define READER_COUNT 6
byte ssPins[READER_COUNT] = {5, 17, 16, 4, 2, 15};
MFRC522 mfrc522[READER_COUNT];

// 6个读卡器对应的面部部位（根据实际物理安装位置映射）
const char* partNames[READER_COUNT] = {
  "右脸颊", "下巴", "左脸颊", "左眼", "额头", "右眼"
};

// ===== 读卡器防重复检测（替代 delay(1000) 的去抖方案） =====
unsigned long lastDetectTime[READER_COUNT] = {0};
String lastDetectUid[READER_COUNT] = {""};
const unsigned long DEBOUNCE_MS = 800; // 同一张卡在同一读卡器上的最小间隔

// ===== 天线定期重初始化（防止RC522进入低功耗导致射频减弱） =====
unsigned long lastAntennaRefresh = 0;
const unsigned long ANTENNA_REFRESH_INTERVAL = 30000; // 每30秒刷新一次天线

// ===== 配置存储 =====
Preferences prefs;
String cfgSSID;
String cfgPassword;
String cfgServerUrl;
bool autoDiscovered = false;

// ===== 自动发现重试控制 =====
unsigned long lastDiscoverAttempt = 0;
const unsigned long DISCOVER_RETRY_INTERVAL = 5000; // 每5秒重试一次

// ===== UDP 发现 =====
WiFiUDP udp;
const int DISCOVER_PORT = 8089;

// ===== AP 配网模式 =====
const char* AP_SSID = "NFC_Setup";
const char* AP_PASS = "12345678";
WebServer server(80);
DNSServer dnsServer;
bool apMode = false;

// ===== 写卡模式（串口 + 后端轮询） =====
// 串口输入：WRITE E01 / WRITE F03 / WRITE V78
// 或前端通过后端下发写卡指令，ESP32 轮询获取
// 下一张刷到任意 RC522 的卡会被写入该编号，写完后自动恢复正常读卡模式
bool pendingWriteMode = false;
String pendingWriteCode = "";

// ===== 后端轮询写卡指令 =====
unsigned long lastWritePollTime = 0;
const unsigned long WRITE_POLL_INTERVAL = 2000; // 每2秒轮询一次

// ===== 加载配置 =====
void loadConfig() {
  prefs.begin("nfc_cfg", true);
  cfgSSID = prefs.getString("ssid", "");
  cfgPassword = prefs.getString("pass", "");
  cfgServerUrl = prefs.getString("server", "");
  prefs.end();

  Serial.println("已加载配置:");
  Serial.println("  SSID: " + cfgSSID);
  Serial.println("  Server: " + cfgServerUrl);
}

// ===== 保存配置 =====
void saveConfig(String ssid, String pass, String srvUrl) {
  prefs.begin("nfc_cfg", false);
  prefs.putString("ssid", ssid);
  prefs.putString("pass", pass);
  prefs.putString("server", srvUrl);
  prefs.end();
  Serial.println("配置已保存");
}

// ===== 保存服务器地址 =====
void saveServerOnly(String srvUrl) {
  prefs.begin("nfc_cfg", false);
  prefs.putString("server", srvUrl);
  prefs.end();
  cfgServerUrl = srvUrl;
  Serial.println("服务器地址已更新: " + srvUrl);
}

// ===== 自动发现服务器地址（UDP广播方式） =====
// ESP32 发送 UDP 广播 "NFC_DISCOVER" 到端口 8089
// 后端收到后回复 "NFC_SERVER:IP:PORT"
bool autoDiscoverServer() {
  if (WiFi.status() != WL_CONNECTED) return false;

  Serial.println("🔍 UDP 广播发现服务器...");

  // 发送广播
  IPAddress broadcastIp = WiFi.localIP();
  broadcastIp[3] = 255; // x.x.x.255 广播地址

  udp.begin(DISCOVER_PORT + 1); // 用不同端口发送，避免冲突
  udp.beginPacket(broadcastIp, DISCOVER_PORT);
  udp.print("NFC_DISCOVER");
  udp.endPacket();

  Serial.println("  广播已发送到 " + broadcastIp.toString() + ":" + String(DISCOVER_PORT));

  // 等待回复（最多2秒）
  unsigned long start = millis();
  while (millis() - start < 2000) {
    int packetSize = udp.parsePacket();
    if (packetSize > 0) {
      char buf[128];
      int len = udp.read(buf, sizeof(buf) - 1);
      buf[len] = '\0';
      String response = String(buf);

      Serial.println("  收到回复: " + response);

      // 解析 "NFC_SERVER:192.168.x.x:8088"
      if (response.startsWith("NFC_SERVER:")) {
        String rest = response.substring(11); // 去掉 "NFC_SERVER:"
        int colonIdx = rest.lastIndexOf(':');
        String ip = rest.substring(0, colonIdx);
        String port = rest.substring(colonIdx + 1);

        String nfcUrl = "http://" + ip + ":" + port + "/api/nfc/apply";
        cfgServerUrl = nfcUrl;
        saveServerOnly(nfcUrl);
        Serial.println("✅ 发现成功! NFC URL: " + nfcUrl);
        udp.stop();
        return true;
      }
    }
    delay(50);
  }

  udp.stop();

  // 备用：如果已有地址，验证它
  if (cfgServerUrl.length() > 0) {
    Serial.println("  UDP未回复，验证已有地址: " + cfgServerUrl);
    String baseUrl = cfgServerUrl;
    int apiIdx = baseUrl.indexOf("/api/nfc/apply");
    if (apiIdx > 0) baseUrl = baseUrl.substring(0, apiIdx);

    HTTPClient http;
    http.begin(baseUrl + "/api/system/discover");
    http.setTimeout(2000);
    int code = http.GET();
    if (code == 200) {
      String body = http.getString();
      StaticJsonDocument<512> doc;
      if (deserializeJson(doc, body) == DeserializationError::Ok) {
        String nfcUrl = doc["nfcUrl"].as<String>();
        if (nfcUrl.length() > 0) {
          cfgServerUrl = nfcUrl;
          saveServerOnly(nfcUrl);
          Serial.println("✅ HTTP 验证成功! NFC URL: " + nfcUrl);
          http.end();
          return true;
        }
      }
    }
    http.end();
  }

  Serial.println("  自动发现失败，5秒后重试...");
  return false;
}

// ===== 生成配网页面（AP模式） =====
String buildAPPage() {
  String page = "<!DOCTYPE html><html><head>";
  page += "<meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>";
  page += "<title>NFC 设备配网</title><style>";
  page += "body{font-family:sans-serif;max-width:400px;margin:40px auto;padding:0 20px}";
  page += "input{width:100%;padding:10px;margin:6px 0 16px;box-sizing:border-box;border:1px solid #ccc;border-radius:4px}";
  page += "button{width:100%;padding:12px;background:#2196F3;color:#fff;border:none;border-radius:4px;font-size:16px;cursor:pointer}";
  page += "button:hover{background:#1976D2} h2{color:#333}";
  page += ".info{background:#e8f5e9;padding:10px;border-radius:4px;margin-bottom:20px;font-size:14px}";
  page += "</style></head><body>";
  page += "<h2>NFC 设备配网</h2>";
  page += "<div class='info'>配置WiFi后，设备会自动发现服务器地址。也可手动填写。</div>";
  page += "<form action='/save' method='POST'>";
  page += "<label>WiFi 名称 (SSID)</label>";
  page += "<input name='ssid' placeholder='WiFi名称' required value='" + cfgSSID + "'>";
  page += "<label>WiFi 密码</label>";
  page += "<input name='pass' type='password' placeholder='WiFi密码' value='" + cfgPassword + "'>";
  page += "<label>后端服务器地址（留空=自动发现）</label>";
  page += "<input name='server' placeholder='留空自动发现，或填 http://IP:8088/api/nfc/apply' value='" + cfgServerUrl + "'>";
  page += "<button type='submit'>保存并重启</button>";
  page += "</form></body></html>";
  return page;
}

// ===== 生成设置页面（STA模式，已连WiFi） =====
String buildSTAPage() {
  String page = "<!DOCTYPE html><html><head>";
  page += "<meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>";
  page += "<title>NFC 设备设置</title><style>";
  page += "body{font-family:sans-serif;max-width:400px;margin:40px auto;padding:0 20px}";
  page += "input{width:100%;padding:10px;margin:6px 0 16px;box-sizing:border-box;border:1px solid #ccc;border-radius:4px}";
  page += "button{width:100%;padding:12px;background:#2196F3;color:#fff;border:none;border-radius:4px;font-size:16px;cursor:pointer;margin-bottom:10px}";
  page += "button:hover{background:#1976D2} h2{color:#333}";
  page += ".info{background:#e8f5e9;padding:10px;border-radius:4px;margin-bottom:20px;font-size:14px}";
  page += ".warn{background:#fff3e0;padding:10px;border-radius:4px;margin-bottom:20px;font-size:14px}";
  page += ".status{background:#e3f2fd;padding:10px;border-radius:4px;margin-bottom:20px;font-size:14px}";
  page += ".ok{background:#e8f5e9;padding:10px;border-radius:4px;margin-bottom:20px;font-size:14px}";
  page += "</style></head><body>";
  page += "<h2>NFC 面部控制设备</h2>";
  page += "<div class='status'>当前WiFi: <b>" + cfgSSID + "</b><br>设备IP: <b>" + WiFi.localIP().toString() + "</b></div>";

  if (autoDiscovered) {
    page += "<div class='ok'>✅ 服务器已自动发现<br>NFC URL: <b>" + cfgServerUrl + "</b></div>";
  } else if (cfgServerUrl.length() > 0) {
    page += "<div class='info'>服务器地址: <b>" + cfgServerUrl + "</b></div>";
  } else {
    page += "<div class='warn'>⚠️ 未配置服务器地址，请手动填写或点击自动发现</div>";
  }

  // 自动发现按钮
  page += "<form action='/auto_discover' method='POST'>";
  page += "<button type='submit' style='background:#4CAF50'>🔍 自动发现服务器</button>";
  page += "</form>";

  // 手动修改
  page += "<form action='/update_server' method='POST'>";
  page += "<label>手动设置服务器地址</label>";
  page += "<input name='server' placeholder='http://192.168.x.x:8088/api/nfc/apply' value='" + cfgServerUrl + "'>";
  page += "<button type='submit'>更新服务器地址</button>";
  page += "</form>";

  // 读卡器状态
  page += "<h3>读卡器状态 (" + String(READER_COUNT) + "个部位)</h3>";
  page += "<div class='info'>";
  for (int i = 0; i < READER_COUNT; i++) {
    page += "NFC #" + String(i) + " (GPIO " + String(ssPins[i]) + ") → " + String(partNames[i]) + "<br>";
  }
  page += "</div>";

  page += "<div class='warn'>如需更换WiFi，点击下方按钮。</div>";
  page += "<form action='/reset_wifi' method='POST'>";
  page += "<button type='submit' style='background:#ff9800'>重新配网（清除WiFi设置）</button>";
  page += "</form></body></html>";
  return page;
}

// ===== AP模式路由处理 =====
void handleAPRoot() {
  server.send(200, "text/html", buildAPPage());
}

void handleAPSave() {
  String newSSID = server.arg("ssid");
  String newPass = server.arg("pass");
  String newServer = server.arg("server");
  saveConfig(newSSID, newPass, newServer);

  String html = "<!DOCTYPE html><html><head>";
  html += "<meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>";
  html += "<title>配置已保存</title><style>";
  html += "body{font-family:sans-serif;max-width:420px;margin:40px auto;padding:0 20px;line-height:1.6}";
  html += ".ok{background:#e8f5e9;padding:14px;border-radius:8px;margin-bottom:16px}";
  html += ".info{background:#e3f2fd;padding:14px;border-radius:8px;margin-bottom:16px}";
  html += ".tip{background:#fff3e0;padding:14px;border-radius:8px}";
  html += "b{color:#1565c0}";
  html += "</style></head><body>";
  html += "<h2>✅ 配置已保存</h2>";
  html += "<div class='ok'>WiFi 配置和服务器地址已经写入设备。</div>";
  html += "<div class='info'>设备将在 <b id='sec'>8</b> 秒后自动重启，并尝试连接新网络：<br><b>" + newSSID + "</b></div>";
  html += "<div class='tip'>页面随后会断开，这是正常现象。<br>重启后请让手机切回原来的 WiFi，再访问设备新的 IP，或等待设备自动发现服务器。</div>";
  html += "<script>";
  html += "let s=8;setInterval(function(){s--;var el=document.getElementById('sec');if(el&&s>=0)el.textContent=s;},1000);";
  html += "</script></body></html>";

  server.send(200, "text/html", html);
  delay(8000);
  ESP.restart();
}

void handleCaptiveDetect() {
  server.sendHeader("Location", "http://192.168.4.1/");
  server.send(302, "text/plain", "");
}

void handleNotFound() {
  server.sendHeader("Location", "http://192.168.4.1/");
  server.send(302, "text/plain", "");
}

// ===== STA模式路由处理 =====
void handleSTARoot() {
  server.send(200, "text/html", buildSTAPage());
}

void handleUpdateServer() {
  String newServer = server.arg("server");
  saveServerOnly(newServer);
  autoDiscovered = false;

  server.send(200, "text/html",
    "<html><head><meta charset='utf-8'><meta http-equiv='refresh' content='2;url=/'></head><body>"
    "<h2>服务器地址已更新!</h2><p>新地址: " + newServer + "</p>"
    "<p>2秒后返回...</p></body></html>");
}

void handleAutoDiscover() {
  bool found = autoDiscoverServer();
  if (found) {
    autoDiscovered = true;
    server.send(200, "text/html",
      "<html><head><meta charset='utf-8'><meta http-equiv='refresh' content='2;url=/'></head><body>"
      "<h2>✅ 服务器发现成功!</h2><p>NFC URL: " + cfgServerUrl + "</p>"
      "<p>2秒后返回...</p></body></html>");
  } else {
    server.send(200, "text/html",
      "<html><head><meta charset='utf-8'><meta http-equiv='refresh' content='3;url=/'></head><body>"
      "<h2>❌ 自动发现失败</h2><p>请确保后端服务器已启动且在同一网络。</p>"
      "<p>3秒后返回...</p></body></html>");
  }
}

void handleResetWifi() {
  prefs.begin("nfc_cfg", false);
  prefs.remove("ssid");
  prefs.remove("pass");
  prefs.end();

  String html = "<!DOCTYPE html><html><head>";
  html += "<meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>";
  html += "<title>WiFi 配置已清除</title><style>";
  html += "body{font-family:sans-serif;max-width:420px;margin:40px auto;padding:0 20px;line-height:1.6}";
  html += ".warn{background:#fff3e0;padding:14px;border-radius:8px;margin-bottom:16px}";
  html += ".info{background:#e3f2fd;padding:14px;border-radius:8px;margin-bottom:16px}";
  html += "b{color:#ef6c00}";
  html += "</style></head><body>";
  html += "<h2>⚠️ WiFi 配置已清除</h2>";
  html += "<div class='warn'>设备将在 <b id='sec'>8</b> 秒后重启，并重新进入配网模式。</div>";
  html += "<div class='info'>重启后请连接热点 <b>NFC_Setup</b>，然后访问 <b>http://192.168.4.1</b> 重新配置。</div>";
  html += "<script>";
  html += "let s=8;setInterval(function(){s--;var el=document.getElementById('sec');if(el&&s>=0)el.textContent=s;},1000);";
  html += "</script></body></html>";

  server.send(200, "text/html", html);
  delay(8000);
  ESP.restart();
}

// ===== 启动 AP 配网模式 =====
void startAPMode() {
  apMode = true;
  WiFi.mode(WIFI_AP);
  WiFi.softAP(AP_SSID, AP_PASS);
  dnsServer.start(53, "*", WiFi.softAPIP());

  Serial.println("========== AP 配网模式 ==========");
  Serial.print("AP SSID: ");
  Serial.println(AP_SSID);
  Serial.print("AP 密码: ");
  Serial.println(AP_PASS);
  Serial.print("配置页面: http://");
  Serial.println(WiFi.softAPIP());

  server.on("/", handleAPRoot);
  server.on("/save", HTTP_POST, handleAPSave);
  server.on("/generate_204", handleCaptiveDetect);
  server.on("/gen_204", handleCaptiveDetect);
  server.on("/connecttest.txt", handleCaptiveDetect);
  server.on("/hotspot-detect.html", handleCaptiveDetect);
  server.on("/library/test/success.html", handleCaptiveDetect);
  server.on("/success.txt", handleCaptiveDetect);
  server.on("/canonical.html", handleCaptiveDetect);
  server.on("/redirect", handleCaptiveDetect);
  server.on("/fwlink", handleCaptiveDetect);
  server.onNotFound(handleNotFound);
  server.begin();
}

// ===== 启动 STA 模式下的 Web 服务器 =====
void startSTAServer() {
  server.on("/", handleSTARoot);
  server.on("/update_server", HTTP_POST, handleUpdateServer);
  server.on("/auto_discover", HTTP_POST, handleAutoDiscover);
  server.on("/reset_wifi", HTTP_POST, handleResetWifi);
  server.begin();

  Serial.println("========== STA 设置页面已启动 ==========");
  Serial.print("浏览器访问: http://");
  Serial.println(WiFi.localIP());
}

// ===== WiFi 连接（带超时） =====
bool connectWiFi() {
  if (cfgSSID.length() == 0) {
    Serial.println("未配置WiFi，进入配网模式");
    return false;
  }

  WiFi.mode(WIFI_STA);
  WiFi.begin(cfgSSID.c_str(), cfgPassword.c_str());
  Serial.print("连接WiFi [" + cfgSSID + "] 中");

  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 30) {
    delay(500);
    Serial.print(".");
    attempts++;
  }
  Serial.println();

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("WiFi连接成功！");
    Serial.print("IP: ");
    Serial.println(WiFi.localIP());
    return true;
  } else {
    Serial.println("WiFi连接失败！");
    return false;
  }
}

// ===== 校验预设编号（E01~E05 / F01~F05 / A01~A05 / C01~C99） =====
bool isValidPresetCode(String code) {
  code.trim();
  code.toUpperCase();

  if (code.length() < 3) return false;

  char typeChar = code.charAt(0);

  // E/F/A 类型：固定3位，X0[1-5]
  if (typeChar == 'E' || typeChar == 'F' || typeChar == 'A') {
    if (code.length() != 3) return false;
    char tens = code.charAt(1);
    char ones = code.charAt(2);
    if (tens != '0') return false;
    if (ones < '1' || ones > '5') return false;
    return true;
  }

  // C 类型自定义面部卡：C + 2~3位数字（C01~C999）
  if (typeChar == 'C') {
    if (code.length() < 3 || code.length() > 4) return false;
    for (int i = 1; i < (int)code.length(); i++) {
      if (code.charAt(i) < '0' || code.charAt(i) > '9') return false;
    }
    return true;
  }

  return false;
}

// ===== 校验所有合法卡片编号（预设卡 E/F/A/C + 社区/版本卡 V{数字}） =====
bool isValidCardCode(String code) {
  code.trim();
  code.toUpperCase();

  // 先检查是否是预设编号（含 C 类型）
  if (isValidPresetCode(code)) return true;

  // 再检查是否是版本/社区卡：V + 1~6位数字
  if (code.length() >= 2 && code.length() <= 7 && code.charAt(0) == 'V') {
    for (int i = 1; i < (int)code.length(); i++) {
      if (code.charAt(i) < '0' || code.charAt(i) > '9') return false;
    }
    return true;
  }

  return false;
}

// ===== 串口命令处理：WRITE E01 / WRITE V78 / CANCEL / HELP =====
void handleSerialCommands() {
  if (!Serial.available()) return;

  String command = Serial.readStringUntil('\n');
  command.trim();

  if (command.length() == 0) return;

  String upperCommand = command;
  upperCommand.toUpperCase();

  if (upperCommand == "HELP" || upperCommand == "?") {
    Serial.println("========== NFC 写卡命令 ==========");
    Serial.println("WRITE E01  -> 写入表情预设 E01");
    Serial.println("WRITE F03  -> 写入脸型预设 F03");
    Serial.println("WRITE A05  -> 写入动作预设 A05");
    Serial.println("WRITE V78  -> 写入社区/版本卡 V78");
    Serial.println("CANCEL     -> 取消等待写卡");
    Serial.println("格式范围: E01-E05 / F01-F05 / A01-A05 / V{数字}");
    return;
  }

  if (upperCommand == "CANCEL") {
    pendingWriteMode = false;
    pendingWriteCode = "";
    Serial.println("✅ 已取消写卡模式，恢复正常读卡。");
    return;
  }

  if (upperCommand.startsWith("WRITE ")) {
    String code = upperCommand.substring(6);
    code.trim();

    if (!isValidCardCode(code)) {
      Serial.println("❌ 编号格式错误，请使用 E01-E05 / F01-F05 / A01-A05 / V{数字}，例如 WRITE E01 或 WRITE V78");
      return;
    }

    pendingWriteCode = code;
    pendingWriteMode = true;
    Serial.println("📝 已进入写卡模式，待写入编号: " + pendingWriteCode);
    Serial.println("请把要写入的 NFC 卡贴到任意一个 RC522 读卡器上。输入 CANCEL 可取消。");
    return;
  }

  Serial.println("未知命令: " + command);
  Serial.println("输入 HELP 查看可用命令。");
}

// ===== 写入NFC卡片数据块中的编号（支持变长：E01 / V78 / V12345） =====
// NTAG213/215/216：写 Page 4~7（每页4字节，共16字节可用）
// Mifare Classic 1K/4K：写 Block 4（16字节，需要默认Key A认证）
// 返回 true 表示写入成功
bool writePresetCode(int readerIdx, String code) {
  code.trim();
  code.toUpperCase();

  if (!isValidCardCode(code)) {
    Serial.println("❌ 写入失败：编号格式不合法");
    return false;
  }

  MFRC522::MIFARE_Key key;
  for (byte k = 0; k < 6; k++) key.keyByte[k] = 0xFF;

  MFRC522::PICC_Type piccType = mfrc522[readerIdx].PICC_GetType(mfrc522[readerIdx].uid.sak);

  // 准备写入数据：编号裸文本 + 0x00 填充，最多16字节
  int codeLen = code.length();
  if (codeLen > 15) codeLen = 15; // 留一个字节给终止符

  if (piccType == MFRC522::PICC_TYPE_MIFARE_UL) {
    // NTAG: 每页4字节，写 Page 4~7
    byte pages[4][4] = {{0},{0},{0},{0}};
    for (int ci = 0; ci < codeLen; ci++) {
      pages[ci / 4][ci % 4] = (byte)code.charAt(ci);
    }
    // 终止符位置
    int termIdx = codeLen;
    if (termIdx < 16) pages[termIdx / 4][termIdx % 4] = 0x00;

    for (int p = 0; p < 4; p++) {
      MFRC522::StatusCode status = mfrc522[readerIdx].MIFARE_Ultralight_Write(4 + p, pages[p], 4);
      if (status != MFRC522::STATUS_OK && p == 0) {
        Serial.print("❌ NTAG写入Page4失败: ");
        Serial.println(mfrc522[readerIdx].GetStatusCodeName(status));
        return false;
      }
    }

    Serial.println("✅ NTAG写入成功 Page4-7 = " + code + " (已清理旧数据)");
    return true;
  }

  if (piccType == MFRC522::PICC_TYPE_MIFARE_1K || piccType == MFRC522::PICC_TYPE_MIFARE_4K) {
    MFRC522::StatusCode authStatus = mfrc522[readerIdx].PCD_Authenticate(
      MFRC522::PICC_CMD_MF_AUTH_KEY_A, 4, &key, &(mfrc522[readerIdx].uid));

    if (authStatus != MFRC522::STATUS_OK) {
      Serial.print("❌ Classic认证Block4失败: ");
      Serial.println(mfrc522[readerIdx].GetStatusCodeName(authStatus));
      return false;
    }

    byte blockData[16] = {0};
    for (int ci = 0; ci < codeLen; ci++) {
      blockData[ci] = (byte)code.charAt(ci);
    }

    MFRC522::StatusCode writeStatus = mfrc522[readerIdx].MIFARE_Write(4, blockData, 16);
    if (writeStatus != MFRC522::STATUS_OK) {
      Serial.print("❌ Classic写入Block4失败: ");
      Serial.println(mfrc522[readerIdx].GetStatusCodeName(writeStatus));
      return false;
    }

    Serial.println("✅ Classic写入成功 Block4 = " + code);
    return true;
  }

  Serial.println("❌ 写入失败：不支持的卡片类型");
  return false;
}

// ===== 从缓冲区中扫描预设编号（含 C 类型自定义面部卡） =====
String findPresetCodeInBuffer(const byte* data, int len) {
  for (int i = 0; i <= len - 3; i++) {
    char c0 = (char)data[i];
    char c1 = (char)data[i + 1];
    char c2 = (char)data[i + 2];

    // E/F/A 类型：X0[1-5]
    if ((c0 == 'E' || c0 == 'F' || c0 == 'A' || c0 == 'e' || c0 == 'f' || c0 == 'a') &&
        c1 == '0' &&
        c2 >= '1' && c2 <= '5') {
      String code = "";
      code += c0;
      code += c1;
      code += c2;
      code.toUpperCase();
      return code;
    }

    // C 类型自定义面部卡：C + 2~3位数字
    if ((c0 == 'C' || c0 == 'c') && c1 >= '0' && c1 <= '9' && c2 >= '0' && c2 <= '9') {
      String code = "C";
      code += c1;
      code += c2;
      // 检查是否有第3位数字（C100+）
      if (i + 3 < len) {
        char c3 = (char)data[i + 3];
        if (c3 >= '0' && c3 <= '9') {
          code += c3;
        }
      }
      return code;
    }
  }
  return "";
}

// ===== 从缓冲区中扫描社区/版本卡编号 V + 数字 =====
String findVersionCodeInBuffer(const byte* data, int len) {
  for (int i = 0; i <= len - 2; i++) {
    char c0 = (char)data[i];
    if (c0 == 'V' || c0 == 'v') {
      String numStr = "";
      for (int j = i + 1; j < len && j < i + 7; j++) {
        char cj = (char)data[j];
        if (cj >= '0' && cj <= '9') {
          numStr += cj;
        } else {
          break;
        }
      }
      if (numStr.length() > 0) {
        return "V" + numStr;
      }
    }
  }
  return "";
}

// ===== 读取NFC卡片数据块中的编号 =====
// 支持：
// 1) 裸文本：直接写 E01 / F03 / A05 / V78
// 2) NDEF 文本记录：手机 NFC Tools 常见写法
// 3) NTAG 多读两次(Page4-7 + Page8-11)拼成32字节，覆盖跨页NDEF
String readPresetCode(int readerIdx) {
  MFRC522::MIFARE_Key key;
  for (byte k = 0; k < 6; k++) key.keyByte[k] = 0xFF;

  MFRC522::PICC_Type piccType = mfrc522[readerIdx].PICC_GetType(mfrc522[readerIdx].uid.sak);

  // 用更大的缓冲区：NTAG 读两次(Page4-7 + Page8-11)拼成 32 字节
  byte bigBuf[32];
  memset(bigBuf, 0, 32);
  int totalBytes = 0;

  if (piccType == MFRC522::PICC_TYPE_MIFARE_UL) {
    byte buffer[18];
    byte bufferSize = sizeof(buffer);
    MFRC522::StatusCode status = mfrc522[readerIdx].MIFARE_Read(4, buffer, &bufferSize);
    if (status != MFRC522::STATUS_OK) {
      Serial.print("  NTAG读取Page4失败: ");
      Serial.println(mfrc522[readerIdx].GetStatusCodeName(status));
      return "";
    }
    memcpy(bigBuf, buffer, 16);
    totalBytes = 16;

    // 再读 Page 8~11（额外16字节），覆盖跨页NDEF文本
    byte buf2[18];
    byte buf2Size = sizeof(buf2);
    MFRC522::StatusCode status2 = mfrc522[readerIdx].MIFARE_Read(8, buf2, &buf2Size);
    if (status2 == MFRC522::STATUS_OK) {
      memcpy(bigBuf + 16, buf2, 16);
      totalBytes = 32;
    }
  } else if (piccType == MFRC522::PICC_TYPE_MIFARE_1K || piccType == MFRC522::PICC_TYPE_MIFARE_4K) {
    MFRC522::StatusCode authStatus = mfrc522[readerIdx].PCD_Authenticate(
      MFRC522::PICC_CMD_MF_AUTH_KEY_A, 4, &key, &(mfrc522[readerIdx].uid));
    if (authStatus != MFRC522::STATUS_OK) {
      Serial.print("  Classic认证Block4失败: ");
      Serial.println(mfrc522[readerIdx].GetStatusCodeName(authStatus));
      return "";
    }
    byte buffer[18];
    byte bufferSize = sizeof(buffer);
    MFRC522::StatusCode readStatus = mfrc522[readerIdx].MIFARE_Read(4, buffer, &bufferSize);
    if (readStatus != MFRC522::STATUS_OK) {
      Serial.print("  Classic读取Block4失败: ");
      Serial.println(mfrc522[readerIdx].GetStatusCodeName(readStatus));
      return "";
    }
    memcpy(bigBuf, buffer, 16);
    totalBytes = 16;
  } else {
    Serial.println("  不支持的卡片类型，跳过数据块读取");
    return "";
  }

  // 调试输出
  Serial.print("  原始数据 HEX (" + String(totalBytes) + "B): ");
  for (int i = 0; i < totalBytes; i++) {
    if (bigBuf[i] < 0x10) Serial.print("0");
    Serial.print(bigBuf[i], HEX);
    Serial.print(" ");
  }
  Serial.println();

  Serial.print("  原始数据 ASCII: ");
  for (int i = 0; i < totalBytes; i++) {
    char c = (char)bigBuf[i];
    if (c >= 32 && c <= 126) {
      Serial.print(c);
    } else {
      Serial.print(".");
    }
  }
  Serial.println();

  // 1) 先扫描预设编号 E01/F03/A05
  String presetCode = findPresetCodeInBuffer(bigBuf, totalBytes);
  if (presetCode.length() > 0) {
    Serial.println("  📋 读取到预设编号: " + presetCode);
    return presetCode;
  }

  // 2) 扫描社区/版本卡 V + 数字
  String versionCode = findVersionCodeInBuffer(bigBuf, totalBytes);
  if (versionCode.length() > 0) {
    Serial.println("  📋 读取到社区卡编号: " + versionCode);
    return versionCode;
  }

  // 3) 尝试 NDEF Text 结构中的文本内容扫描
  for (int i = 0; i < totalBytes; i++) {
    if (bigBuf[i] == 0x54) { // 'T' = Text Record Type
      // 跳过语言码（通常 02 + 2字节语言码如 "en"）
      int langLen = (i + 1 < totalBytes) ? (bigBuf[i + 1] & 0x3F) : 0;
      int textStart = i + 2 + langLen;
      if (textStart < totalBytes) {
        // 在NDEF文本内容中扫描预设编号
        String ndefPreset = findPresetCodeInBuffer(&bigBuf[textStart], totalBytes - textStart);
        if (ndefPreset.length() > 0) {
          Serial.println("  📋 从NDEF文本中解析到预设编号: " + ndefPreset);
          return ndefPreset;
        }
        // 在NDEF文本内容中扫描版本卡
        String ndefVersion = findVersionCodeInBuffer(&bigBuf[textStart], totalBytes - textStart);
        if (ndefVersion.length() > 0) {
          Serial.println("  📋 从NDEF文本中解析到社区卡编号: " + ndefVersion);
          return ndefVersion;
        }
      }
    }
  }

  Serial.println("  未找到有效编号，按普通UID卡处理");
  return "";
}

// ===== 获取后端基础URL（从 cfgServerUrl 中提取） =====
String getBaseUrl() {
  String baseUrl = cfgServerUrl;
  int apiIdx = baseUrl.indexOf("/api/nfc/apply");
  if (apiIdx > 0) baseUrl = baseUrl.substring(0, apiIdx);
  return baseUrl;
}

// ===== 轮询后端写卡指令 =====
void pollPendingWriteCommand() {
  if (WiFi.status() != WL_CONNECTED || cfgServerUrl.length() == 0) return;
  if (pendingWriteMode) return; // 已经在写卡模式，不需要再轮询

  unsigned long now = millis();
  if (now - lastWritePollTime < WRITE_POLL_INTERVAL) return;
  lastWritePollTime = now;

  String baseUrl = getBaseUrl();
  HTTPClient http;
  http.begin(baseUrl + "/api/nfc/pendingWrite");
  http.setTimeout(2000);

  int code = http.GET();
  if (code == 200) {
    String body = http.getString();
    StaticJsonDocument<256> doc;
    if (deserializeJson(doc, body) == DeserializationError::Ok) {
      String status = doc["status"].as<String>();
      if (status == "pending") {
        String writeCode = doc["code"].as<String>();
        writeCode.trim();
        writeCode.toUpperCase();
        if (writeCode.length() > 0 && isValidCardCode(writeCode)) {
          pendingWriteCode = writeCode;
          pendingWriteMode = true;
          Serial.println("📝 收到后端写卡指令: " + pendingWriteCode);
          Serial.println("请把 NFC 卡贴到任意 RC522 上进行写入。");
        }
      }
    }
  }
  http.end();
}

// ===== 通知后端写卡完成 =====
void notifyWriteDone(String code, bool success) {
  if (WiFi.status() != WL_CONNECTED || cfgServerUrl.length() == 0) return;

  String baseUrl = getBaseUrl();
  HTTPClient http;
  http.begin(baseUrl + "/api/nfc/writeDone");
  http.addHeader("Content-Type", "application/json");
  http.setTimeout(2000);

  String json = "{\"code\":\"" + code + "\",\"success\":" + (success ? "true" : "false") + "}";
  http.POST(json);
  http.end();
}

// ===== HTTP 发送函数（非阻塞优化版 + 预设支持） =====
void sendToServer(String uid, int readerIndex, String presetCode) {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi未连接，跳过发送");
    return;
  }

  // 如果服务器地址为空，刷卡时立即尝试发现
  if (cfgServerUrl.length() == 0) {
    Serial.println("服务器地址未配置，尝试自动发现...");
    if (!autoDiscoverServer()) {
      Serial.println("自动发现失败，跳过发送。请确保后端已启动。");
      return;
    }
    autoDiscovered = true;
  }

  HTTPClient http;
  http.begin(cfgServerUrl);
  http.addHeader("Content-Type", "application/json");
  http.setTimeout(2000); // ★ 缩短HTTP超时到2秒，避免阻塞扫描

  String json;
  if (presetCode.length() > 0) {
    // 预设卡：附带 presetCode 字段
    json = "{\"readerIndex\":" + String(readerIndex) + ",\"uid\":\"" + uid + "\",\"presetCode\":\"" + presetCode + "\"}";
    Serial.print("发送预设卡 [" + presetCode + "]: ");
  } else {
    // 普通增量卡：只发 readerIndex + uid
    json = "{\"readerIndex\":" + String(readerIndex) + ",\"uid\":\"" + uid + "\"}";
    Serial.print("发送 [" + String(partNames[readerIndex]) + "]: ");
  }
  Serial.println(json);

  int code = http.POST(json);

  Serial.print("HTTP Response: ");
  Serial.println(code);

  if (code > 0) {
    String response = http.getString();
    Serial.print("Body: ");
    Serial.println(response);
  }

  http.end();
}

// ===== 刷新所有读卡器天线（定期调用，防止RF场衰减） =====
void refreshAllAntennas() {
  for (byte i = 0; i < READER_COUNT; i++) {
    digitalWrite(ssPins[i], LOW);
    
    // 关闭再打开天线，强制重新激励RF场
    mfrc522[i].PCD_AntennaOff();
    delayMicroseconds(500);
    mfrc522[i].PCD_AntennaOn();
    
    // 重新设置最大增益
    mfrc522[i].PCD_SetAntennaGain(mfrc522[i].RxGain_max);
    
    // ★ 直接写寄存器：将 ConductorReg(调制电导) 设为更激进的值
    // 提升天线驱动电流，增强近场耦合能力（对FPC小标签效果显著）
    mfrc522[i].PCD_WriteRegister(MFRC522::GsNReg, 0xFF);       // 接收端增益N
    mfrc522[i].PCD_WriteRegister(MFRC522::CWGsPReg, 0x3F);     // 发送端P-MOS驱动（最大）
    mfrc522[i].PCD_WriteRegister(MFRC522::ModGsPReg, 0x3F);    // 调制P-MOS驱动（最大）
    
    // ★ 调整 RFCfgReg 接收增益寄存器，确保设置为最大
    mfrc522[i].PCD_WriteRegister(MFRC522::RFCfgReg, 0x70);     // 48dB 接收增益
    
    // ★ 优化定时器配置：增加等待时间，给小标签更多响应时间
    mfrc522[i].PCD_WriteRegister(MFRC522::TModeReg, 0x8D);     // TAuto=1, 预分频器高位
    mfrc522[i].PCD_WriteRegister(MFRC522::TPrescalerReg, 0x3E); // 预分频器低位
    mfrc522[i].PCD_WriteRegister(MFRC522::TReloadRegH, 0x00);   // 重载值高字节
    mfrc522[i].PCD_WriteRegister(MFRC522::TReloadRegL, 0x70);   // 重载值低字节 → 更长的超时

    digitalWrite(ssPins[i], HIGH);
  }
  Serial.println("🔄 天线已刷新 - RF场/增益/驱动电流已重新配置");
}

// ===== Setup =====
void setup() {
  Serial.begin(115200);
  Serial.println();
  Serial.println("========== NFC 面部控制系统 v2.0 ==========");
  Serial.println("6个读卡器 → 右脸颊/下巴/左脸颊/左眼/额头/右眼");
  Serial.println("(D21未连接，鼻子读卡器不使用)");

  loadConfig();

  if (!connectWiFi()) {
    startAPMode();
    return;
  }

  // WiFi连接成功，尝试自动发现服务器（UDP广播）
  if (cfgServerUrl.length() == 0) {
    Serial.println("服务器地址为空，尝试自动发现...");
    autoDiscovered = autoDiscoverServer();
  } else {
    // 验证已有地址是否可用，顺便更新
    Serial.println("验证已有服务器地址...");
    autoDiscovered = autoDiscoverServer();
  }

  // 启动STA模式Web服务器
  startSTAServer();

  // 初始化 SPI 和读卡器
  SPI.begin();

  for (byte i = 0; i < READER_COUNT; i++) {
    pinMode(ssPins[i], OUTPUT);
    digitalWrite(ssPins[i], HIGH);
  }

  for (byte i = 0; i < READER_COUNT; i++) {
    mfrc522[i] = MFRC522(ssPins[i], RST_PIN);
    digitalWrite(ssPins[i], LOW);
    mfrc522[i].PCD_Init();
    
    // ★ 设置最大天线增益（0x70 = 48dB），提升对小型FPC标签的检测能力
    mfrc522[i].PCD_SetAntennaGain(mfrc522[i].RxGain_max);
    
    // ★ 增强射频驱动配置
    mfrc522[i].PCD_WriteRegister(MFRC522::GsNReg, 0xFF);
    mfrc522[i].PCD_WriteRegister(MFRC522::CWGsPReg, 0x3F);
    mfrc522[i].PCD_WriteRegister(MFRC522::ModGsPReg, 0x3F);
    mfrc522[i].PCD_WriteRegister(MFRC522::RFCfgReg, 0x70);
    
    // ★ 优化定时器：给小标签更充裕的响应时间
    mfrc522[i].PCD_WriteRegister(MFRC522::TModeReg, 0x8D);
    mfrc522[i].PCD_WriteRegister(MFRC522::TPrescalerReg, 0x3E);
    mfrc522[i].PCD_WriteRegister(MFRC522::TReloadRegH, 0x00);
    mfrc522[i].PCD_WriteRegister(MFRC522::TReloadRegL, 0x70);
    
    // 验证增益设置
    byte gain = mfrc522[i].PCD_GetAntennaGain();
    
    digitalWrite(ssPins[i], HIGH);

    Serial.print("Reader ");
    Serial.print(i);
    Serial.print(" (GPIO ");
    Serial.print(ssPins[i]);
    Serial.print(") → ");
    Serial.print(partNames[i]);
    Serial.print(" | 增益: 0x");
    Serial.print(gain, HEX);
    Serial.println(" ✓");
  }

  // 初始化去抖状态
  for (int i = 0; i < READER_COUNT; i++) {
    lastDetectTime[i] = 0;
    lastDetectUid[i] = "";
  }

  Serial.println("========== 等待刷卡 ==========");
  Serial.println("★ v2.0 优化:");
  Serial.println("  - 去掉 delay(1000) 阻塞，改用去抖机制");
  Serial.println("  - RF驱动电流提升至最大 (CWGsPReg=0x3F)");
  Serial.println("  - 接收增益 48dB + 定期天线刷新");
  Serial.println("  - 每30秒自动刷新天线RF场");
  Serial.println("  - HTTP超时缩短为2秒，减少阻塞");
  Serial.println("★ 写预设卡:");
  Serial.println("  - 串口输入 WRITE E01 / WRITE F03 / WRITE A05");
  Serial.println("  - 然后把卡贴到任意一个 RC522 上即可写入");
  Serial.println("  - 输入 HELP 查看命令，输入 CANCEL 取消写卡");
}

// ===== Loop（v2.0 优化版：去掉 delay(1000)，改用去抖） =====
void loop() {
  // AP 配网模式
  if (apMode) {
    dnsServer.processNextRequest();
    server.handleClient();
    return;
  }

  // STA模式：处理Web请求 + 串口命令 + 后端写卡轮询 + 扫描NFC卡
  server.handleClient();
  handleSerialCommands();
  pollPendingWriteCommand();

  // 如果服务器地址为空，定期自动重试发现
  if (cfgServerUrl.length() == 0 && WiFi.status() == WL_CONNECTED) {
    unsigned long now = millis();
    if (now - lastDiscoverAttempt > DISCOVER_RETRY_INTERVAL) {
      lastDiscoverAttempt = now;
      Serial.println("⏳ 定期重试自动发现服务器...");
      if (autoDiscoverServer()) {
        autoDiscovered = true;
        Serial.println("✅ 服务器发现成功！开始工作。");
      }
    }
  }

  // ★ 定期刷新天线（每30秒），防止RF场长时间运行后衰减
  unsigned long now = millis();
  if (now - lastAntennaRefresh > ANTENNA_REFRESH_INTERVAL) {
    lastAntennaRefresh = now;
    refreshAllAntennas();
  }

  // ===== 扫描所有读卡器（无 delay 阻塞版） =====
  for (byte i = 0; i < READER_COUNT; i++) {
    digitalWrite(ssPins[i], LOW);
    
    // ★ 给 RF 场 1ms 建立时间（切换 SS 后天线需要时间稳定）
    delayMicroseconds(1000);

    // ★ 多次重试检测，提升小型标签的检测成功率
    bool cardDetected = false;
    byte retryCount = 0;
    const byte MAX_RETRIES = 3;

    while (!cardDetected && retryCount < MAX_RETRIES) {
      if (mfrc522[i].PICC_IsNewCardPresent()) {
        if (mfrc522[i].PICC_ReadCardSerial()) {
          cardDetected = true;
          break;
        }
      }
      retryCount++;
      if (retryCount < MAX_RETRIES) {
        delayMicroseconds(500); // ★ 重试间隔从 100us 提升到 500us，让RF场充分激励标签
      }
    }

    if (!cardDetected) {
      digitalWrite(ssPins[i], HIGH);
      continue;
    }

    // ★ 获取 UID
    String uidStr = "";
    for (byte j = 0; j < mfrc522[i].uid.size; j++) {
      uidStr += String(mfrc522[i].uid.uidByte[j], HEX);
    }
    uidStr.toUpperCase();

    // ★ 去抖判断：同一张卡在同一读卡器上，800ms 内不重复发送
    unsigned long currentTime = millis();
    if (uidStr == lastDetectUid[i] && (currentTime - lastDetectTime[i]) < DEBOUNCE_MS) {
      // 重复检测，跳过发送
      mfrc522[i].PICC_HaltA();
      mfrc522[i].PCD_StopCrypto1();
      digitalWrite(ssPins[i], HIGH);
      continue;
    }

    // 更新去抖状态
    lastDetectUid[i] = uidStr;
    lastDetectTime[i] = currentTime;

    // 获取卡类型（调试用）
    MFRC522::PICC_Type piccType = mfrc522[i].PICC_GetType(mfrc522[i].uid.sak);
    String cardType = String(mfrc522[i].PICC_GetTypeName(piccType));

    Serial.print("📡 Reader ");
    Serial.print(i);
    Serial.print(" [");
    Serial.print(partNames[i]);
    Serial.print("] UID: ");
    Serial.print(uidStr);
    Serial.print(" | 类型: ");
    Serial.print(cardType);
    Serial.print(" | SAK: 0x");
    Serial.print(mfrc522[i].uid.sak, HEX);
    Serial.print(" | 大小: ");
    Serial.print(mfrc522[i].uid.size);
    Serial.println(" bytes");

    // ★ 如果处于写卡模式（串口或后端下发），则写入编号，不发送到后端
    if (pendingWriteMode && pendingWriteCode.length() > 0) {
      String codeToWrite = pendingWriteCode;
      Serial.println("📝 检测到待写卡片，准备写入编号: " + codeToWrite);
      bool writeOk = writePresetCode(i, codeToWrite);

      if (writeOk) {
        Serial.println("🎉 写卡完成，已退出写卡模式。");
        pendingWriteMode = false;
        pendingWriteCode = "";
        notifyWriteDone(codeToWrite, true);
      } else {
        Serial.println("⚠️ 写卡失败，仍保持写卡模式。可重试刷卡，或输入 CANCEL 取消。");
        notifyWriteDone(codeToWrite, false);
      }

      mfrc522[i].PICC_HaltA();
      mfrc522[i].PCD_StopCrypto1();
      digitalWrite(ssPins[i], HIGH);
      continue;
    }

    // ★ 尝试读取卡片数据块中的预设编号
    String presetCode = readPresetCode(i);

    sendToServer(uidStr, i, presetCode);

    mfrc522[i].PICC_HaltA();
    mfrc522[i].PCD_StopCrypto1();

    digitalWrite(ssPins[i], HIGH);

    // ★ 不再 delay(1000)！去抖机制已替代
  }

  // WiFi 断线自动重连
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi断线，重连中...");
    if (!connectWiFi()) {
      Serial.println("重连失败，进入配网模式");
      startAPMode();
    } else {
      // 重连成功后重新发现服务器
      autoDiscoverServer();
    }
  }
}