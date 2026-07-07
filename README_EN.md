# NFC-Interactive AI Character Creation System

> **[中文版 (Chinese Version)](README.md) | English Version**

> Undergraduate capstone (graduation) project — An AI-driven character creation system that combines physical NFC cards, AI image generation, and 3D modeling.
> Users tap NFC cards to trigger preset facial expressions, face-shape adjustments, and pose switches, and then, combined with AI image-to-image generation, quickly create personalized character avatars that can be shared to a community gallery.

---

## ✨ Key Features

- 🎴 **Physical NFC Interaction**: An ESP32 + RC522 reader taps cards to trigger preset expressions, face shapes, and poses
- 🎨 **AI Image Generation**: Integration with Doubao (Volcengine Ark) for text-to-image / image-to-image, supporting multi-modal generation with character reference image + pose reference image + text prompt
- 🧊 **3D Model Generation**: Integration with Tencent Hunyuan 3D API to convert 2D character art into editable 3D models
- ✋ **Gesture-Based Sculpting**: Hand-gesture recognition powered by MediaPipe Hands enables touchless 3D model sculpting
- 🌐 **Community Sharing**: Publish works, like, comment, and version management
- ☁️ **Image Hosting**: AI-generated temporary image URLs are automatically migrated to Cloudflare R2 for permanent public links

---

## 🏗️ System Architecture

```
┌──────────────────┐    HTTPS     ┌────────────────────┐    HTTP     ┌──────────────┐
│  Vue 3 Frontend  │ ───────────► │ Spring Boot Backend│ ─────────► │   MySQL      │
│  (Vite + Three)  │              │  (Java 17/21)      │            └──────────────┘
└──────────────────┘              └────────────────────┘
        ▲                                    │
        │                                    ├──► Doubao AI (image-to-image)
        │                                    ├──► Tencent Hunyuan (3D generation)
        │                                    └──► Cloudflare R2 (image hosting)
        │
        │  HTTP (mDNS)
        │
┌──────────────────┐
│  ESP32 + RC522   │
│  (NFC Reader)    │
└──────────────────┘
```

**Technology Stack Overview:**

| Layer | Technologies |
|---|---|
| Frontend | Vue 3, Vite, Three.js, MediaPipe Hands, Vue Router, Axios |
| Backend | Spring Boot 3.2, Spring Data JPA, MySQL, Lombok |
| Hardware | ESP32, RC522 NFC module (firmware maintained separately) |
| AI Services | Doubao (Volcengine Ark), Tencent Hunyuan 3D |
| Storage | MySQL (business data), Cloudflare R2 (image / model files) |
| Deployment | Docker, Docker Compose, nginx |

---

## 📁 Directory Layout

```
NFC-system/
├── docker-compose.yml                # One-command Docker deployment orchestration
├── .env.example                      # Environment variable template (for Docker)
├── backend/
│   └── Dockerfile                    # Backend image build script
├── src/                              # Spring Boot backend source
│   ├── main/java/com/example/NFC_system/
│   │   ├── controller/               # REST API layer
│   │   ├── service/                  # Business logic (AI calls, NFC mapping, etc.)
│   │   ├── entity/                   # JPA entities
│   │   ├── repository/               # Data access layer
│   │   └── config/                   # CORS, mDNS, exception handling
│   └── main/resources/
│       ├── application.properties            # Local backend config (already in .gitignore)
│       └── application.properties.example    # Config template
├── fontend/                          # Vue 3 frontend (note: typo intentional as fontend)
│   ├── Dockerfile                    # Frontend image build script
│   ├── nginx.conf                    # Production nginx config
│   ├── src/
│   │   ├── views/                    # Page views
│   │   ├── components/               # Components (3D editor, sculpting tool)
│   │   └── api/                      # API wrappers
│   ├── public/mediapipe/             # MediaPipe hand gesture recognition models
│   └── vite.config.js
├── setup.md                          # NFC card numbering scheme and write-card instructions
└── README.md                         # This document (Chinese / English: README_EN.md)
```

---

## 🚀 Quick Start (Option A: One-Command Docker Deployment, ⭐ Recommended)

**This is the easiest way for paper reviewers to reproduce the experiment.** You only need Docker; everything else runs inside containers.

### 1. Prerequisites

| Dependency | Version | Notes |
|---|---|---|
| Docker Desktop | 20.10+ | Install Docker Desktop on Windows / macOS; install `docker` + `docker-compose` on Linux |
| RAM | ≥ 8 GB | Three containers run concurrently (MySQL + backend + frontend) |
| Disk | ≥ 5 GB | Images take about 2 GB |

### 2. Three Commands to Run

```bash
# ① Clone the project
git clone https://github.com/xuboxiao123/NFC---3D-character-modeling.git
cd NFC---3D-character-modeling

# ② Prepare environment variables (copy template and fill in API keys as needed)
cp .env.example .env
# Open .env in an editor and fill in DOUBAO_API_KEY / HUNYUAN_API_KEY, etc.
# The system runs without them, but AI features will fail (basic NFC / community / character management still work for demo)

# ③ Start all services
docker-compose up -d --build
```

The first build takes about **5 ~ 10 minutes** (pulling images, Maven downloading dependencies, npm install). Subsequent startups take only a few seconds.

### 3. Access

- **Frontend home**: `http://localhost` (default port 80)
- **Backend API**: `http://localhost:8088`
- **MySQL**: `localhost:3306` (root / `DB_PASSWORD` in `.env`)

### 4. Common Operations

```bash
# Check running status
docker-compose ps

# View logs
docker-compose logs -f backend
docker-compose logs -f frontend

# Stop all services
docker-compose down

# Stop and delete data (caution — clears MySQL data)
docker-compose down -v

# Rebuild images (after code changes)
docker-compose up -d --build
```

### 5. Will the Database Be Created Automatically?

**Yes.** When the MySQL container starts, the following happens automatically:
1. The `MYSQL_DATABASE=ai_character_system` environment variable creates the database
2. `createDatabaseIfNotExist=true` in the backend JDBC URL acts as a fallback
3. Spring Data JPA's `ddl-auto=update` creates tables automatically

Reviewers **do not need to run any SQL manually**.

---

## 🛠️ Option B: Local Traditional Startup (for Development and Debugging)

If you want to modify and debug the code, or you do not want to install Docker, use this path.

### 1. Environment Requirements

| Dependency | Version |
|---|---|
| JDK | **17 or 21** |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Maven | The project ships with `mvnw`; no separate installation needed |

### 2. Backend

**(1) Create database (optional — `createDatabaseIfNotExist=true` will create it automatically):**

```sql
CREATE DATABASE ai_character_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**(2) Configuration:**

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Edit `application.properties` and fill in your MySQL password and API keys.

**(3) Start:**

```bash
# Windows
.\mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

Defaults to listening on `http://localhost:8088`.

### 3. Frontend

```bash
cd fontend
npm install
npm run dev
```

Defaults to listening on `https://localhost:5173` (Vite is configured with HTTPS — the MediaPipe camera API requires HTTPS).

The browser will warn that the certificate is untrusted; choose "Proceed anyway".

### 4. NFC Hardware (Optional)

The ESP32 firmware is not included in this repository — see [`setup.md`](setup.md) for the card numbering scheme and write-card instructions.

**You can test without the hardware** by simulating a card tap via curl:

```bash
curl -X POST http://localhost:8088/api/nfc/apply \
  -H "Content-Type: application/json" \
  -d '{"uid":"test-uid","presetCode":"E01","characterId":1}'
```

---

## 🔑 Configuration Reference

Key entries in `application.properties` / `.env`:

| Environment Variable | Corresponding Config | Description |
|---|---|---|
| `DB_URL` | `spring.datasource.url` | MySQL JDBC URL |
| `DB_USER` / `DB_PASSWORD` | Database credentials | Defaults to root inside Docker |
| `DOUBAO_API_KEY` | `doubao.api-key` | Doubao image-to-image key |
| `DOUBAO_MODEL` | `doubao.model` | Doubao model endpoint ID |
| `HUNYUAN_API_KEY` | `hunyuan.api-key` | Tencent Hunyuan 3D key |
| `R2_*` | `r2.*` | Cloudflare R2 image hosting credentials |
| `LOCAL_MODEL_DIR` | `app.local.model-dir` | Local model storage path (in Docker: `/app/storage/models/`) |

---

## 📷 Screenshots

See the [`screenshots/`](screenshots/) directory.

---

## ❓ Frequently Asked Questions

**Q: CORS issues when the frontend calls the backend?**
A: In Docker deployment, the frontend proxies `/api/**` to the backend through nginx, so there is no CORS. In dev mode, Vite has its own proxy, so no CORS either. If errors still occur, double-check that you are going through the `/api` prefix.

**Q: MediaPipe hand-gesture recognition does not work?**
A: You must access the page over HTTPS (in local dev mode) and grant the browser camera permission. Docker deployment runs over HTTP without gesture features; you can add nginx SSL certificates yourself if needed.

**Q: 3D models are large and slow to load?**
A: `multipart.max-file-size` has been raised to 500 MB and nginx layer to 500m. Increase further if needed.

**Q: I do not have a Doubao / Hunyuan API key — can I still run it?**
A: Yes. The system will start, but AI-generation features will return errors. Basic character management, community, and NFC mapping logic all work for demo.

**Q: Ports 80 / 8088 / 3306 already in use?**
A: Edit `docker-compose.yml` and change the **left** side of the `ports:` mappings. For example, `"8080:80"` maps the frontend to port 8080.

**Q: Docker Desktop on Windows starts very slowly?**
A: Make sure WSL2 is enabled, and in Docker Desktop settings check "Use the WSL 2 based engine".

---

## 📄 Reproducibility Statement (for Paper Reviewers)

This project follows these **reproducible research principles**:

### 1. Environment Reproducibility

- **Containerized deployment**: All services are orchestrated through [`docker-compose.yml`](docker-compose.yml); a single `docker-compose up -d` rebuilds the experiment consistently on any Docker-capable OS (Windows 10+, macOS, Linux).
- **Pinned dependency versions**: Java 21, Node 20, MySQL 8.0, nginx 1.27 — all pinned in the [`Dockerfile`](backend/Dockerfile).
- **Automatic database initialization**: No manual database / table creation is required. JPA + `createDatabaseIfNotExist=true` handle it at container startup.

### 2. Data Reproducibility

- **NFC card numbering scheme**: see [`setup.md`](setup.md)
- **AI prompt templates**: see backend [`PromptBuilder.java`](src/main/java/com/example/NFC_system/service/PromptBuilder.java)
- **NFC → expression / pose mapping table**: see [`NfcPresetService.java`](src/main/java/com/example/NFC_system/service/NfcPresetService.java)

### 3. External Dependencies

This project **does not train any deep-learning model locally**; all AI capabilities are delivered through third-party commercial APIs:

| Service | Purpose | Apply at | Approx. Cost |
|---|---|---|---|
| Volcengine Ark Doubao | Image-to-image | https://console.volcengine.com/ark/ | Free tier for new users; ~¥0.02/image after |
| Tencent Hunyuan 3D | 2D-to-3D conversion | https://cloud.tencent.com/product/hunyuan3d | Free tier for new users; ~¥0.10/request after |
| Cloudflare R2 | Object storage | https://dash.cloudflare.com/ | 10 GB / month free |

**Reviewers who wish to fully reproduce AI features** need to apply for their own keys (see the table above) and fill them into `.env`. The total cost is very low (a few RMB is enough for full functional testing).

### 4. Hardware Optionality

The NFC hardware portion (ESP32 + RC522) is **not required**. The software layer exposes a complete REST API, and you can simulate card-tap events with HTTP requests (see example in the FAQ above), enabling functional verification **without any hardware**.

---

## 📦 Other Outputs from the Capstone

- **Thesis (main text)**: not included in this repository; see the capstone-specific submission platform
- **Trained weights / datasets**: this project **does not train a local model**, so there are no related artifacts
- **Demo video**: see GitHub Release attachments
- **NFC firmware source code**: not in this repository; maintained separately

---

## 📜 License

This project is for learning and capstone defense demonstration only; commercial licensing is not granted.

---

## 👤 Author

- **Author**: Xu Boxiao (许博骁)
- **Institution**: Hangzhou City University (浙大城市学院)
- **Year**: 2026