
# NFC 预设卡方案说明

## 1. 当前实现方式

系统已经支持两种 NFC 卡：

- **普通增量卡**：只识别 UID，通过后端 [`mapUidToDelta()`](src/main/java/com/example/NFC_system/service/NfcMappingService.java:42) 判断是 `+1/-1`
- **预设卡**：读取卡内数据块中的编号字符串，例如 `E01`、`F03`、`A05`，由后端按编号应用预设

后端入口在 [`apply()`](src/main/java/com/example/NFC_system/controller/NfcController.java:97)，预设定义在 [`NfcPresetService`](src/main/java/com/example/NFC_system/service/NfcPresetService.java) 中，ESP32 读取逻辑在 [`readPresetCode()`](c:/Users/Lenovo/Documents/PlatformIO/Projects/Hardwaveend/src/main.cpp:400)。

---

## 2. 15 张卡推荐分组

建议你把 15 张卡固定分成 3 组，每组 5 张：

### 2.1 表情预设卡 `E01 ~ E05`

| 编号 | 名称 | 用途 |
|---|---|---|
| E01 | Happy | 开心大笑，嘴角大幅上扬、眼睛弯弯 |
| E02 | Amazed | 惊讶万分，眼睛睁大、嘴巴微张 |
| E03 | Angry | 怒火中烧，眉头紧皱、眼神凌厉 |
| E04 | Sad | 悲伤落泪，眼角下垂、嘴角下拉 |
| E05 | Speechless | 一脸无语，嘴巴紧闭、眼神放空 |

### 2.2 脸型预设卡 `F01 ~ F05`

| 编号 | 名称 | 用途 |
|---|---|---|
| F01 | 圆脸 | 饱满圆润 |
| F02 | 瓜子脸 | 上宽下窄 |
| F03 | 方脸 | 棱角分明 |
| F04 | 大眼萌 | 偏可爱风 |
| F05 | 精灵脸 | 偏幻想风 |

### 2.3 动作/设定预设卡 `A01 ~ A05`

| 编号 | 名称 | 用途 |
|---|---|---|
| A01 | 正面站姿 | 标准角色展示 |
| A02 | 战斗姿势 | 动态战斗表现 |
| A03 | 休闲坐姿 | 日常状态 |
| A04 | 奔跑动态 | 高动势动作 |
| A05 | 魔法释放 | 技能展示动作 |

---

## 3. 卡里到底写什么

### 最推荐：只写一个短编号

你卡里只写下面这种 3 字符编号即可：

- `E01`
- `E02`
- `F01`
- `F05`
- `A03`

这是最稳的方案，因为：

1. ESP32 读取简单
2. 后端方便改配置
3. 后期你想换图床地址、动作描述、标签名字，不需要重写所有卡
4. 同一个编号可以映射到新的图床图片、prompt、标签文案

也就是说：

- **卡里存“编号”**
- **后端存“编号 -> 真正内容”**

这正符合你说的“我这边传一个编号，那边按编号去修标签，导入图床图片”的思路。

---

## 4. 建议写入格式

### 最简单写法

直接把卡的用户区前几个字节写成纯文本：

```txt
E01
```

或：

```txt
F03
```

或：

```txt
A05
```

ESP32 当前实现会读取用户数据区前 16 字节，并取前 3 个可打印字符当预设编号。

---

## 5. 你后面主要改什么

如果你以后想换预设内容，只需要改后端 [`NfcPresetService.java`](src/main/java/com/example/NFC_system/service/NfcPresetService.java)：

- 改表情参数
- 改脸型参数
- 改动作 prompt
- 改参考图地址
- 改卡片显示名称

不需要重新改前端结构，也不需要重刷所有卡。

---

## 6. 预设实际存放位置

### 表情 / 脸型卡

在 [`NfcPresetService`](src/main/java/com/example/NFC_system/service/NfcPresetService.java) 中：

- `E01~E05` 存在 `EXPRESSION_PRESETS`
- `F01~F05` 存在 `FACE_PRESETS`

后端收到后会在 [`applyFacePreset()`](src/main/java/com/example/NFC_system/controller/NfcController.java:225) 中直接写入当前角色的 `faceParams`。

### 动作卡

`A01~A05` 存在 `ACTION_PRESETS`。

后端收到后会在 [`applyActionPreset()`](src/main/java/com/example/NFC_system/controller/NfcController.java:282) 中更新：

- `actionPrompt`
- `poseImageUrl`

并通过轮询返回给前端。

---

## 7. 你的 15 张卡最终建议贴标签方式

建议实体标签直接印这套：

### 表情组
- `E01 Happy`
- `E02 Amazed`
- `E03 Angry`
- `E04 Sad`
- `E05 Speechless`

### 脸型组
- `F01 圆脸`
- `F02 瓜子脸`
- `F03 方脸`
- `F04 大眼萌`
- `F05 精灵脸`

### 动作组
- `A01 正面站姿`
- `A02 战斗姿势`
- `A03 休闲坐姿`
- `A04 奔跑动态`
- `A05 魔法释放`

---

## 8. 写卡方式

写卡功能已直接集成在 ESP32 主固件 [`main.cpp`](c:/Users/Lenovo/Documents/PlatformIO/Projects/Hardwaveend/src/main.cpp) 中，不需要额外工具。

### 方式一：ESP32 串口写卡（推荐）

1. 用 PlatformIO 的 Serial Monitor 连接 ESP32
2. 输入命令，例如 `WRITE E01`
3. 串口提示"已进入写卡模式"
4. 把空白 NFC 卡贴到任意一个 RC522 读卡器上
5. 写入成功后自动恢复正常读卡模式

可用命令：
- `WRITE E01` ~ `WRITE E05`：写入表情预设编号
- `WRITE F01` ~ `WRITE F05`：写入脸型预设编号
- `WRITE A01` ~ `WRITE A05`：写入动作预设编号
- `CANCEL`：取消当前写卡等待
- `HELP`：查看命令帮助

支持 NTAG213/215/216 和 Mifare Classic 1K/4K 两种卡类型。

### 方式二：手机 NFC 写卡 App

如果你的卡是 NTAG213/215/216，也可以用手机 NFC App（如 NFC Tools）直接写入纯文本 `E01`。

---

