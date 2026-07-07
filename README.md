# AI 角色创作系统（NFC 交互式）

> **[English Version](README_EN.md) | 中文版**

> 本科毕业设计项目 —— 一套结合 NFC 物理卡片、AI 图像生成与 3D 建模的角色创作系统。
> 用户通过 NFC 卡刷卡即可触发表情切换、脸型调整、动作切换等操作，配合 AI 图生图能力，快速创作出个性化角色形象，并可分享到社区。

---

## ✨ 项目特色

- 🎴 **NFC 物理交互**：使用 ESP32 + RC522 读卡器，通过刷卡触发预设的表情、脸型、动作
- 🎨 **AI 图生图**：接入豆包（Doubao）文生图/图生图 API，支持角色参考图 + 动作参考图 + 文本 Prompt 的多模态生成
- 🧊 **3D 模型生成**：接入腾讯混元 3D API，将 2D 角色图转为可编辑的 3D 模型
- ✋ **手势雕刻**：基于 MediaPipe Hands 的手势识别，实现无接触 3D 模型雕刻
- 🌐 **社区分享**：作品发布、点赞、评论、版本管理
- ☁️ **图床托管**：AI 生成的临时图片自动转存到 Cloudflare R2，获得永久链接

---

## 🏗️ 系统架构

```
┌──────────────────┐    HTTPS     ┌────────────────────┐    HTTP     ┌──────────────┐
│  Vue 3 前端      │ ───────────► │  Spring Boot 后端  │ ─────────► │   MySQL      │
│  (Vite + Three)  │              │  (Java 17/21)      │            └──────────────┘
└──────────────────┘              └────────────────────┘
        ▲                                    │
        │                                    ├──► 豆包 AI (图生图)
        │                                    ├──► 腾讯混元 (3D 生成)
        │                                    └──► Cloudflare R2 (图床)
        │
        │  HTTP (mDNS)
        │
┌──────────────────┐
│  ESP32 + RC522   │
│  (NFC 读卡器)    │
└──────────────────┘
```

**技术栈总览：**

| 层级 | 技术 |
|---|---|
| 前端 | Vue 3, Vite, Three.js, MediaPipe Hands, Vue Router, Axios |
| 后端 | Spring Boot 3.2, Spring Data JPA, MySQL, Lombok |
| 硬件 | ESP32, RC522 NFC 模块（固件仓库单独维护） |
| AI 服务 | 豆包（Doubao/Volcengine Ark）、腾讯混元 3D |
| 存储 | MySQL（业务数据）、Cloudflare R2（图片/模型文件） |
| 部署 | Docker、Docker Compose、nginx |

---

## 📁 目录结构

```
NFC-system/
├── docker-compose.yml                # Docker 一键部署编排文件
├── .env.example                      # 环境变量模板（Docker 用）
├── backend/
│   └── Dockerfile                    # 后端镜像构建脚本
├── src/                              # Spring Boot 后端源码
│   ├── main/java/com/example/NFC_system/
│   │   ├── controller/               # REST 接口层
│   │   ├── service/                  # 业务逻辑层（AI 调用、NFC 映射等）
│   │   ├── entity/                   # JPA 实体
│   │   ├── repository/               # 数据访问层
│   │   └── config/                   # 跨域、mDNS、异常等配置
│   └── main/resources/
│       ├── application.properties            # 后端本地配置（已在 .gitignore）
│       └── application.properties.example    # 配置模板
├── fontend/                          # Vue 3 前端（注：目录拼写为 fontend）
│   ├── Dockerfile                    # 前端镜像构建脚本
│   ├── nginx.conf                    # 生产环境 nginx 配置
│   ├── src/
│   │   ├── views/                    # 页面
│   │   ├── components/               # 组件（含 3D 编辑器、雕刻器）
│   │   └── api/                      # 接口封装
│   ├── public/mediapipe/             # MediaPipe 手势识别模型
│   └── vite.config.js
├── setup.md                          # NFC 卡片编号方案与写卡说明
└── README.md                         # 本文档
```

---

## 🚀 快速开始（方案 A：Docker 一键部署，⭐ 推荐）

**这是论文审稿人复现实验最省事的方式。** 你只需要装 Docker，剩下的都在容器里。

### 1. 前置要求

| 依赖 | 版本 | 说明 |
|---|---|---|
| Docker Desktop | 20.10+ | Windows / macOS 装 Docker Desktop；Linux 装 docker + docker-compose |
| 内存 | ≥ 8 GB | 三个容器同时跑（MySQL + 后端 + 前端） |
| 磁盘 | ≥ 5 GB | 镜像大约占 2 GB |

### 2. 三条命令跑起来

```bash
# ① 克隆项目
git clone https://github.com/<你的用户名>/NFC-system.git
cd NFC-system/NFC-system

# ② 准备环境变量（复制模板并按需填写 AI 密钥）
cp .env.example .env
# 用编辑器打开 .env，把 DOUBAO_API_KEY / HUNYUAN_API_KEY 等填好
# 不填也能跑，但 AI 功能会失败（基础的 NFC/社区/角色管理仍可演示）

# ③ 启动全部服务
docker-compose up -d --build
```

首次构建大约需要 **5 ~ 10 分钟**（拉镜像、Maven 下依赖、npm install）。之后再启动只要几秒。

### 3. 访问

- **前端主页**：`http://localhost`（默认 80 端口）
- **后端 API**：`http://localhost:8088`
- **MySQL**：`localhost:3306`（root / .env 里的 `DB_PASSWORD`）

### 4. 常用运维命令

```bash
# 查看运行状态
docker-compose ps

# 查看日志
docker-compose logs -f backend
docker-compose logs -f frontend

# 停止所有服务
docker-compose down

# 停止并删除数据（谨慎，会清空 MySQL 数据）
docker-compose down -v

# 重新构建镜像（改了代码后）
docker-compose up -d --build
```

### 5. 数据库会自动创建吗？

**会。** MySQL 容器启动时会自动执行：
1. 通过 `MYSQL_DATABASE=ai_character_system` 环境变量创建数据库
2. 后端 JDBC URL 里的 `createDatabaseIfNotExist=true` 兜底
3. Spring Data JPA 的 `ddl-auto=update` 自动建表

审稿人**无需手动执行任何 SQL**。

---

## 🛠️ 方案 B：本地传统方式启动（开发调试用）

如果你要修改代码调试，或者不想装 Docker，可以走这条路。

### 1. 环境要求

| 依赖 | 版本 |
|---|---|
| JDK | **17 或 21** |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Maven | 已内置 `mvnw`，无需单独装 |

### 2. 后端

**① 建库（可选，不做也行，`createDatabaseIfNotExist=true` 会自动建）：**

```sql
CREATE DATABASE ai_character_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**② 配置：**

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

编辑 `application.properties`，填入 MySQL 密码和各种 API Key。

**③ 启动：**

```bash
# Windows
.\mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

默认监听 `http://localhost:8088`。

### 3. 前端

```bash
cd fontend
npm install
npm run dev
```

默认监听 `https://localhost:5173`（Vite 已配 HTTPS，MediaPipe 摄像头 API 要求 HTTPS）。

浏览器会提示证书不安全，选"继续访问"即可。

### 4. NFC 硬件（可选）

ESP32 固件不在本仓库中，见 [`setup.md`](setup.md) 了解卡片编号方案和写卡方法。

**没有硬件也能测试**，通过 curl 模拟刷卡：

```bash
curl -X POST http://localhost:8088/api/nfc/apply \
  -H "Content-Type: application/json" \
  -d '{"uid":"test-uid","presetCode":"E01","characterId":1}'
```

---

## 🔑 配置项说明

`application.properties` / `.env` 关键配置：

| 环境变量 | 对应配置 | 说明 |
|---|---|---|
| `DB_URL` | `spring.datasource.url` | MySQL JDBC URL |
| `DB_USER` / `DB_PASSWORD` | 数据库账号密码 | Docker 内默认为 root |
| `DOUBAO_API_KEY` | `doubao.api-key` | 豆包图生图密钥 |
| `DOUBAO_MODEL` | `doubao.model` | 豆包模型 endpoint ID |
| `HUNYUAN_API_KEY` | `hunyuan.api-key` | 腾讯混元 3D 密钥 |
| `R2_*` | `r2.*` | Cloudflare R2 图床凭据 |
| `LOCAL_MODEL_DIR` | `app.local.model-dir` | 本地模型存储路径（Docker 内为 `/app/storage/models/`） |

---

## 📷 功能截图

见 [`screenshots/`](screenshots/) 目录。

---

## ❓ 常见问题

**Q: 前端访问后端跨域？**
A: Docker 部署下，前端通过 nginx 反代 `/api/**` 到 backend，不存在跨域。开发模式下 Vite 已配代理，也不会有跨域。若仍报错请检查是否走了代理路径（`/api` 前缀）。

**Q: MediaPipe 手势识别不工作？**
A: 必须用 HTTPS 访问（本地开发模式），且允许浏览器摄像头权限。Docker 部署走 HTTP 无手势功能，可自行加 nginx SSL 证书。

**Q: 3D 模型太大加载慢？**
A: 已把 `multipart.max-file-size` 提到 500MB，nginx 层也提到 500m。若还不够可继续调大。

**Q: 我没有豆包 / 混元 API Key，还能跑吗？**
A: 可以。系统会启动成功，但 AI 生成相关功能会返回错误。基础的角色管理、社区、NFC 映射逻辑均可正常演示。

**Q: 端口 80 / 8088 / 3306 被占用？**
A: 编辑 `docker-compose.yml`，把 `ports:` 下的**左边**端口改成其他值。例如 `"8080:80"` 就是把前端映射到 8080。

**Q: Windows 下 Docker Desktop 启动很慢？**
A: 确保 WSL2 已启用，并在 Docker Desktop 设置里勾选 "Use the WSL 2 based engine"。

---

## 📄 论文可复现性说明

本项目遵循以下**可复现研究原则**：

### 1. 环境可复现

- **容器化部署**：全部服务通过 [`docker-compose.yml`](docker-compose.yml) 编排，一条 `docker-compose up -d` 即可在任意支持 Docker 的操作系统（Windows 10+、macOS、Linux）上一致地重建实验环境
- **依赖版本固定**：Java 21、Node 20、MySQL 8.0、nginx 1.27，均在 [`Dockerfile`](backend/Dockerfile) 中固定
- **数据库自动初始化**：无需手动建库建表，容器启动时通过 JPA + `createDatabaseIfNotExist=true` 完成

### 2. 数据可复现

- **NFC 卡片编号方案**：见 [`setup.md`](setup.md)
- **AI 提示词模板**：见后端 [`PromptBuilder.java`](src/main/java/com/example/NFC_system/service/PromptBuilder.java)
- **NFC → 表情/动作 映射表**：见 [`NfcPresetService.java`](src/main/java/com/example/NFC_system/service/NfcPresetService.java)

### 3. 外部依赖说明

本项目**未在本地训练任何深度学习模型**，AI 能力全部通过调用第三方商业 API 实现：

| 服务 | 用途 | 申请入口 | 大致成本 |
|---|---|---|---|
| 火山方舟豆包（Doubao） | 图生图 | https://console.volcengine.com/ark/ | 新用户免费额度，超出后约 ¥0.02/张 |
| 腾讯混元 3D | 2D 转 3D 模型 | https://cloud.tencent.com/product/hunyuan3d | 新用户免费额度，超出后约 ¥0.10/次 |
| Cloudflare R2 | 对象存储图床 | https://dash.cloudflare.com/ | 每月 10GB 免费 |

**审稿人若需完整复现 AI 功能**，需按上表自行申请密钥填入 `.env`。所需费用极低（几元人民币即可完成完整功能测试）。

### 4. 硬件可选性

NFC 硬件部分（ESP32 + RC522）**并非必需**。系统在软件层面提供了完整的 REST API，可以通过 HTTP 请求模拟刷卡事件（见"常见问题"部分示例），从而在**没有硬件**的情况下完成功能验证。

---

## 📦 关于毕设的其他产物

- **论文正文**：不包含在本仓库中，见毕设专用提交平台
- **训练权重 / 数据集**：本项目**未训练本地模型**，故无相关产物
- **演示视频**：见 GitHub Release 附件
- **NFC 固件源码**：不在本仓库，单独维护

---

## 📜 License

本项目仅用于学习与毕业设计答辩演示，未开放商用授权。

---

## 👤 作者

- **作者**：许博骁 (Xu Boxiao)
- **学校**：浙大城市学院 (Hangzhou City University)
- **年份**：2026
