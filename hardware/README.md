# ESP32 / RC522 NFC firmware

[Software project](../README.md) · **[Browse firmware source](src/main.cpp)**

来自毕业设计最终硬件代码归档。使用 ESP32 与多个 RC522 读卡器，实现 NFC 预设／版本卡的读写、HTTP 上报、配网和服务发现。

## Implemented functions
- Wi-Fi setup through an ESP32 access point and captive-portal web interface.
- Persisted Wi-Fi/server configuration with Preferences.
- Server auto-discovery and HTTP communication with the Spring Boot application.
- Read preset/version codes from NFC cards and send them to the backend.
- Write preset/version codes, including pending write commands polled from the backend.
- Serial commands, reader initialisation and multi-reader polling.

## Build
1. Install PlatformIO and open this hardware directory as a project.
2. Copy `src/firmware_config.h.example` to `src/firmware_config.h`.
3. Set your own access-point name/password in that local header.
4. Run `pio run` to build, then upload for your ESP32 board and check the 115200-baud serial console.

The original `platformio.ini` targets `esp32dev` with the Arduino framework and declares MFRC522 1.4.10 and ArduinoJson 6.x. Match the pin mapping in the source to your actual wiring.

## Archive and publication changes
Source: final hardware archive, `Hardwaveend/src/hardwave/main.cpp`. The source was moved to this project's `src/main.cpp`. Setup access-point constants were externalised to a local configuration header; the NFC and network logic was preserved. PlatformIO build caches, installed dependencies, saved credentials and precompiled binaries are excluded.

## Validation status
Source structure, dependencies and configuration handling were reviewed for publication. No fresh ESP32 compilation, firmware flashing or physical NFC test was performed in this session.
