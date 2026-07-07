<template>
  <div id="app-root">
    <nav v-if="!hideNav" class="top-nav" :class="{ scrolled: navScrolled }">
      <div class="nav-inner">
        <div class="nav-brand" @click="$router.push('/home')">
          <span class="brand-icon">◆</span>
          <span class="brand-text">NFC 3D</span>
        </div>
        <div class="nav-right">
          <div class="nav-links">
            <router-link to="/home">Home</router-link>
            <router-link to="/community">Community</router-link>
            <router-link to="/">Login</router-link>
          </div>
          <button class="theme-toggle" @click="toggle" :title="isDark ? 'Switch to Light' : 'Switch to Dark'">
            {{ isDark ? '☀️' : '🌙' }}
          </button>
        </div>
      </div>
    </nav>

    <!-- 全局 NFC 刷卡提示 -->
    <div v-if="nfcToast" class="global-nfc-toast" @click="nfcToast = ''">
      {{ nfcToast }}
    </div>

    <!-- 局域网快速访问二维码 -->
    <div v-if="!hideNav" class="quick-qr" :class="{ open: showQuickQr }">
      <button class="quick-qr-toggle" @click="showQuickQr = !showQuickQr" title="Scan to open this page">
        <span class="quick-qr-icon">▦</span>
        <span class="quick-qr-text">QR</span>
      </button>

      <div class="quick-qr-panel" v-if="showQuickQr">
        <div class="quick-qr-head">
          <div>
            <p class="quick-qr-title">Scan to open</p>
            <p class="quick-qr-sub">Connect to the hotspot first</p>
          </div>
          <button class="quick-qr-close" @click="showQuickQr = false">×</button>
        </div>

        <div class="quick-qr-code">
          <img v-if="quickQrDataUrl" :src="quickQrDataUrl" alt="Current page QR code" />
          <span v-else>Generating...</span>
        </div>

        <p class="quick-qr-url">{{ quickQrUrl }}</p>

        <div class="quick-qr-actions">
          <button @click="copyQuickQrUrl">Copy URL</button>
          <button @click="generateQuickQr">Refresh</button>
        </div>
      </div>
    </div>

    <!-- 全局 Guide 浮窗 -->
    <div v-if="!hideNav" class="guide-float" :class="{ open: showGuide }">
      <button class="guide-float-toggle" @click="showGuide = !showGuide">
        <span class="guide-float-icon">?</span>
        <span class="guide-float-text">Guide</span>
      </button>
      <div class="guide-float-panel" v-if="showGuide">
        <div class="guide-float-head">
          <div>
            <p class="guide-float-title">Getting Started</p>
            <p class="guide-float-sub">{{ String(guideStep + 1).padStart(2, '0') }} / {{ String(guideSteps.length).padStart(2, '0') }}</p>
          </div>
          <button class="guide-float-close" @click="showGuide = false">&#215;</button>
        </div>
        <div class="guide-float-steps">
          <div v-for="(step, i) in guideSteps" :key="i" class="gf-step" :class="{ active: guideStep === i }" @click="guideStep = i">
            <span class="gf-step-num">{{ String(i + 1).padStart(2, '0') }}</span>
            <div class="gf-step-body">
              <span class="gf-step-title">{{ step.title }}</span>
              <p class="gf-step-desc" v-if="guideStep === i">{{ step.desc }}</p>
            </div>
          </div>
        </div>
        <div class="guide-float-actions">
          <button @click="guideStep > 0 && guideStep--" :disabled="guideStep === 0">&#8592; Prev</button>
          <button @click="guideStep < guideSteps.length - 1 && guideStep++" :disabled="guideStep === guideSteps.length - 1">Next &#8594;</button>
        </div>
      </div>
    </div>

    <router-view v-slot="{ Component, route: currentRoute }">
      <Transition :name="currentRoute.meta.transition || 'page-slide'" mode="out-in">
        <component :is="Component" :key="currentRoute.path" />
      </Transition>
    </router-view>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import QRCode from 'qrcode'
import { useTheme } from './composables/useTheme'
import { getNfcPresetCommand } from './api/api.js'

const { isDark, toggle } = useTheme()
const router = useRouter()
const route = useRoute()

const navScrolled = ref(false)
const hideNav = computed(() => route.path.startsWith('/model-view'))
const nfcToast = ref('')
const showQuickQr = ref(false)
const quickQrDataUrl = ref('')
const quickQrUrl = ref('')
const showGuide = ref(false)
const guideStep = ref(0)
const guideSteps = [
  { title: 'Create a character', desc: 'Give your character a name to create a workspace for AI generation.' },
  { title: 'Upload reference images', desc: 'Upload design, pose, and extra reference images.' },
  { title: 'Write prompts', desc: 'Describe traits, scene, and desired pose for AI guidance.' },
  { title: 'Generate 2D preview', desc: 'Doubao API creates a 2D character illustration.' },
  { title: 'Generate 3D model', desc: 'Hunyuan 3D converts the 2D preview into a 3D OBJ model.' },
  { title: 'NFC interaction', desc: 'Tap NFC cards to adjust expression parameters in real-time.' }
]

const onScroll = () => {
  navScrolled.value = window.scrollY > 50
}

// ===== 局域网快速访问二维码 =====
async function generateQuickQr() {
  if (typeof window === 'undefined') return
  const url = `${window.location.origin}${route.fullPath}`
  quickQrUrl.value = url
  try {
    quickQrDataUrl.value = await QRCode.toDataURL(url, {
      width: 220,
      margin: 2,
      errorCorrectionLevel: 'M',
      color: {
        dark: '#111918',
        light: '#ffffff'
      }
    })
  } catch (e) {
    console.warn('[Quick QR] generate failed:', e.message || e)
    quickQrDataUrl.value = ''
  }
}

async function copyQuickQrUrl() {
  if (!quickQrUrl.value) return
  try {
    await navigator.clipboard.writeText(quickQrUrl.value)
    nfcToast.value = '✅ URL copied'
  } catch (e) {
    nfcToast.value = quickQrUrl.value
  }
  setTimeout(() => { nfcToast.value = '' }, 1800)
}

watch(() => route.fullPath, () => {
  generateQuickQr()
})

// ===== 全局 NFC 版本卡轮询 =====
// 在任意页面（包括平板端）刷 V 卡都能跳转到对应编辑界面
let globalNfcTimer = null
let globalNfcLastTs = 0
let globalNfcPolling = false
let globalNfcJumping = false  // 防止重复跳转
const GLOBAL_NFC_POLL_INTERVAL = 1500

function startGlobalNfcPolling() {
  stopGlobalNfcPolling()
  // 用当前时间戳初始化，忽略后端残留的旧指令，避免刷新页面时重复跳转
  globalNfcLastTs = Date.now()
  globalNfcJumping = false
  globalNfcTimer = setInterval(pollGlobalNfc, GLOBAL_NFC_POLL_INTERVAL)
}

function stopGlobalNfcPolling() {
  if (globalNfcTimer) {
    clearInterval(globalNfcTimer)
    globalNfcTimer = null
  }
}

async function pollGlobalNfc() {
  // 如果正在跳转中或正在轮询中，跳过
  if (globalNfcPolling || globalNfcJumping) return
  globalNfcPolling = true
  try {
    const res = await getNfcPresetCommand(globalNfcLastTs || undefined)
    const data = res.data
    if (!data || data.status !== 'ok') return

    if (data.presetCommand && data.presetTimestamp && data.presetTimestamp > globalNfcLastTs) {
      globalNfcLastTs = data.presetTimestamp
      const cmd = data.presetCommand

      // 处理 V 类型版本卡/社区卡跳转
      if (cmd.presetType === 'V' && cmd.targetUrl) {
        // 如果当前已经在目标页面，不重复跳转
        const currentPath = route.fullPath
        if (currentPath === cmd.targetUrl) return

        globalNfcJumping = true
        nfcToast.value = `📡 Model card detected: ${cmd.presetCode}, redirecting...`
        setTimeout(() => { nfcToast.value = '' }, 3000)
        setTimeout(() => {
          router.push(cmd.targetUrl)
          // 跳转完成后重置，允许后续跳转
          setTimeout(() => { globalNfcJumping = false }, 2000)
        }, 400)
      }
    }
  } catch (e) {
    // 轮询失败时静默处理，不影响后续轮询
    console.warn('[NFC Global Poll] error:', e.message || e)
  } finally {
    globalNfcPolling = false
  }
}

onMounted(() => {
  window.addEventListener('scroll', onScroll)
  generateQuickQr()
  startGlobalNfcPolling()
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  stopGlobalNfcPolling()
})
</script>

<style>
/* ===== 主题变量 ===== */
:root,
[data-theme="dark"] {
  --bg-primary: #040000;
  --bg-secondary: #171B22;
  --bg-card: rgba(255,255,255,0.05);
  --border-card: rgba(255,255,255,0.1);
  --text-primary: #FFFFFF;
  --text-secondary: rgba(255,255,255,0.6);
  --text-muted: rgba(255,255,255,0.3);
  --accent: #114548;
  --accent-hover: #1a6b6f;
  --nav-bg: rgba(4, 0, 0, 0.85);
  --nav-border: rgba(255,255,255,0.06);
  --input-bg: rgba(255,255,255,0.05);
  /* Gallery section */
  --gallery-bg: #0a0e0e;
  --gallery-card-bg: #111918;
  --gallery-text: #d0e8e4;
  --gallery-text-muted: rgba(208, 232, 228, 0.5);
  --gallery-accent: rgba(26, 107, 111, 0.6);
  --gallery-border: rgba(26, 107, 111, 0.45);
  /* Tablet scene */
  --video-layer-bg: #000;
  --desk-bg:
    radial-gradient(ellipse 60% 50% at 25% 18%, rgba(255, 210, 160, 0.32), transparent),
    radial-gradient(ellipse 40% 35% at 80% 22%, rgba(255, 240, 220, 0.12), transparent),
    linear-gradient(180deg, #3a2a1e 0%, #2c1d14 30%, #1e120b 60%, #120906 100%);
  --tablet-body-bg: linear-gradient(145deg, #2a2c2f 0%, #111214 22%, #050607 58%, #1b1d21 100%);
  --tablet-body-shadow:
    0 42px 60px rgba(0,0,0,0.52),
    0 26px 24px rgba(0,0,0,0.22),
    inset 1px 1px 0 rgba(255,255,255,0.18),
    inset -1px -2px 0 rgba(0,0,0,0.55);
  --tablet-inner-border: rgba(255,255,255,0.08);
}

[data-theme="light"] {
  --bg-primary: #F5F5F7;
  --bg-secondary: #f6f4ef;
  --bg-card: rgba(0,0,0,0.03);
  --border-card: rgba(0,0,0,0.1);
  --text-primary: #1C1D1E;
  --text-secondary: rgba(0,0,0,0.5);
  --text-muted: rgba(0,0,0,0.3);
  --accent: #2B3F7B;
  --accent-hover: #114548;
  --nav-bg: rgba(245, 245, 247, 0.9);
  --nav-border: rgba(0,0,0,0.08);
  --input-bg: rgba(0,0,0,0.04);
  /* Gallery section */
  --gallery-bg: #f0f0f2;
  --gallery-card-bg: #e4e4e8;
  --gallery-text: #1C1D1E;
  --gallery-text-muted: rgba(28, 29, 30, 0.42);
  --gallery-accent: rgba(43, 63, 123, 0.28);
  --gallery-border: rgba(43, 63, 123, 0.18);
  /* Tablet scene — light */
  --video-layer-bg: #000;
  --desk-bg:
    radial-gradient(ellipse 60% 50% at 25% 18%, rgba(255, 240, 220, 0.5), transparent),
    radial-gradient(ellipse 40% 35% at 80% 22%, rgba(255, 255, 255, 0.3), transparent),
    linear-gradient(180deg, #c4b8a8 0%, #b5a48e 30%, #a89478 60%, #9a8768 100%);
  --tablet-body-bg: linear-gradient(145deg, #d4d6da 0%, #b8babe 22%, #a0a2a6 58%, #c8cacd 100%);
  --tablet-body-shadow:
    0 42px 60px rgba(0,0,0,0.18),
    0 26px 24px rgba(0,0,0,0.08),
    inset 1px 1px 0 rgba(255,255,255,0.5),
    inset -1px -2px 0 rgba(0,0,0,0.12);
  --tablet-inner-border: rgba(0,0,0,0.06);
}

/* ===== 全局 reset ===== */
*, *::before, *::after {
  box-sizing: border-box;
}

body {
  margin: 0;
  padding: 0;
  background: var(--bg-primary);
  color: var(--text-primary);
  font-family: 'Microsoft Sans Serif', system-ui, -apple-system, sans-serif;
  transition: background 0.3s, color 0.3s;
}

#app {
  width: 100%;
  max-width: 100%;
  border: none;
  text-align: left;
}

a {
  text-decoration: none;
  color: inherit;
}

/* ===== 导航栏 — 几何杂志风 ===== */
.top-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 999;
  padding: 16px 0;
  transition: background 0.3s, padding 0.3s, backdrop-filter 0.3s;
}

.top-nav.scrolled {
  background: var(--nav-bg);
  backdrop-filter: blur(12px);
  padding: 10px 0;
  border-bottom: 1px solid var(--nav-border);
}

.nav-inner {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font: 700 15px/1 'Inter', 'Helvetica Neue', sans-serif;
  color: #fff;
  letter-spacing: 0.08em;
}

.brand-icon {
  color: var(--accent-hover);
  font-size: 16px;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.nav-links {
  display: flex;
  gap: 24px;
}

.nav-links a {
  font: 700 11px/1 'Inter', sans-serif;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  color: #fff;
  transition: color 0.2s, opacity 0.2s;
  padding: 4px 0;
}

.nav-links a:hover,
.nav-links a.router-link-active {
  color: #fff;
  opacity: 0.8;
}

/* 白天模式下，视频暂停（页面展开）后导航栏文字改为黑色 */
[data-theme="light"] body.video-paused .nav-brand,
[data-theme="light"] body:not(.video-playing):not(.video-paused) .nav-brand {
  color: #111;
}

[data-theme="light"] body.video-paused .nav-links a,
[data-theme="light"] body:not(.video-playing):not(.video-paused) .nav-links a {
  color: #333;
}

[data-theme="light"] body.video-paused .nav-links a:hover,
[data-theme="light"] body.video-paused .nav-links a.router-link-active,
[data-theme="light"] body:not(.video-playing):not(.video-paused) .nav-links a:hover,
[data-theme="light"] body:not(.video-playing):not(.video-paused) .nav-links a.router-link-active {
  color: #111;
  opacity: 1;
}

.theme-toggle {
  background: transparent;
  border: 1px solid var(--border-card);
  border-radius: 2px;
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.2s, border-color 0.2s;
}

.theme-toggle:hover {
  border-color: var(--text-primary);
}

/* 全局 NFC 刷卡提示 */
.global-nfc-toast {
  position: fixed;
  top: 70px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(80, 120, 255, 0.92);
  color: #fff;
  padding: 12px 28px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  z-index: 10000;
  backdrop-filter: blur(10px);
  animation: nfcToastIn 0.3s ease;
  white-space: nowrap;
  box-shadow: 0 4px 20px rgba(0,0,0,0.3);
}

@keyframes nfcToastIn {
  from { opacity: 0; transform: translateX(-50%) translateY(-12px); }
  to { opacity: 1; transform: translateX(-50%) translateY(0); }
}
/* 局域网快速访问二维码 */
.quick-qr {
  position: fixed;
  right: -30px;
  bottom: 22px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
  pointer-events: none;
  transition: right 0.3s ease;
}
.quick-qr:hover,
.quick-qr.open {
  right: 22px;
}

.quick-qr-toggle,
.quick-qr-panel {
  pointer-events: auto;
}

.quick-qr-toggle {
  height: 44px;
  padding: 0 14px;
  border: 1.5px solid var(--text-primary);
  border-radius: 4px 0 0 4px;
  background: color-mix(in srgb, var(--bg-secondary) 92%, transparent);
  color: var(--text-primary);
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  box-shadow: -4px 4px 16px rgba(0,0,0,0.18);
  backdrop-filter: blur(14px);
  transition: transform 0.18s, border-color 0.18s, background 0.18s;
}

.quick-qr-toggle:hover {
  transform: translateY(-2px);
  border-color: var(--accent-hover);
  background: var(--bg-secondary);
}

.quick-qr-icon {
  width: 22px;
  height: 22px;
  border: 1px solid var(--text-primary);
  border-radius: 2px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--text-primary);
  font-size: 15px;
  line-height: 1;
}

.quick-qr-text {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.8px;
}

.quick-qr-panel {
  width: min(280px, calc(100vw - 32px));
  padding: 14px;
  border: 1px solid var(--border-card);
  border-radius: 18px;
  background: color-mix(in srgb, var(--bg-secondary) 96%, transparent);
  box-shadow: 0 16px 48px rgba(0,0,0,0.32);
  backdrop-filter: blur(18px);
  animation: quickQrIn 0.18s ease;
}

.quick-qr-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.quick-qr-title {
  margin: 0 0 3px;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}

.quick-qr-sub {
  margin: 0;
  font-size: 11px;
  color: var(--text-secondary);
}

.quick-qr-close {
  width: 26px;
  height: 26px;
  border: none;
  border-radius: 50%;
  background: var(--input-bg);
  color: var(--text-secondary);
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
}

.quick-qr-close:hover {
  color: var(--text-primary);
  background: var(--bg-card);
}

.quick-qr-code {
  width: 220px;
  height: 220px;
  max-width: 100%;
  margin: 0 auto 10px;
  padding: 10px;
  border-radius: 14px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #111918;
  font-size: 12px;
}

.quick-qr-code img {
  width: 100%;
  height: 100%;
  display: block;
}

.quick-qr-url {
  margin: 0 0 10px;
  padding: 8px 10px;
  border-radius: 10px;
  background: var(--input-bg);
  color: var(--text-secondary);
  font-size: 11px;
  line-height: 1.45;
  word-break: break-all;
}

.quick-qr-actions {
  display: flex;
  gap: 8px;
}

.quick-qr-actions button {
  flex: 1;
  padding: 8px 10px;
  border: 1px solid var(--border-card);
  border-radius: 10px;
  background: var(--input-bg);
  color: var(--text-primary);
  font-size: 12px;
  cursor: pointer;
  transition: border-color 0.18s, color 0.18s, background 0.18s;
}

.quick-qr-actions button:hover {
  border-color: var(--accent-hover);
  color: var(--accent-hover);
  background: var(--bg-card);
}

@keyframes quickQrIn {
  from { opacity: 0; transform: translateY(8px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

/* ===== 路由过渡动画 ===== */
/* 默认：向左平滑推入 + 物理顿挫感 */
.page-slide-enter-active {
  animation: pageSlideIn 0.42s cubic-bezier(0.22, 0.68, 0.35, 1.2) forwards;
}

.page-slide-leave-active {
  animation: pageSlideOut 0.28s cubic-bezier(0.55, 0, 0.75, 0.2) forwards;
}

@keyframes pageSlideIn {
  0% {
    opacity: 0;
    transform: translateX(60px) scale(0.97);
  }
  65% {
    opacity: 1;
    transform: translateX(-4px) scale(1.005);
  }
  100% {
    opacity: 1;
    transform: translateX(0) scale(1);
  }
}

@keyframes pageSlideOut {
  0% {
    opacity: 1;
    transform: translateX(0) scale(1);
  }
  100% {
    opacity: 0;
    transform: translateX(-40px) scale(0.97);
  }
}

/* 翻页效果 — 用于 Gallery → Detail */
.page-flip-enter-active {
  animation: pageFlipIn 0.5s cubic-bezier(0.22, 0.68, 0.35, 1.15) forwards;
}

.page-flip-leave-active {
  animation: pageFlipOut 0.32s cubic-bezier(0.55, 0, 0.75, 0.2) forwards;
}

@keyframes pageFlipIn {
  0% {
    opacity: 0;
    transform: perspective(1200px) rotateY(-12deg) translateX(80px) scale(0.94);
  }
  60% {
    opacity: 1;
    transform: perspective(1200px) rotateY(2deg) translateX(-6px) scale(1.01);
  }
  100% {
    opacity: 1;
    transform: perspective(1200px) rotateY(0deg) translateX(0) scale(1);
  }
}

@keyframes pageFlipOut {
  0% {
    opacity: 1;
    transform: perspective(1200px) rotateY(0deg) translateX(0) scale(1);
  }
  100% {
    opacity: 0;
    transform: perspective(1200px) rotateY(8deg) translateX(-50px) scale(0.95);
  }
}
@media (max-width: 640px) {
  .quick-qr {
    right: 14px;
    bottom: 14px;
  }

  .quick-qr-toggle {
    height: 40px;
    padding: 0 12px;
  }

  .quick-qr-panel {
    width: min(260px, calc(100vw - 28px));
  }

  .quick-qr-code {
    width: 200px;
    height: 200px;
  }
}

/* ===== 全局 Guide 浮窗 ===== */
.guide-float { position:fixed; right:-30px; bottom:76px; z-index:9998; display:flex; flex-direction:column; align-items:flex-end; gap:10px; pointer-events:none; transition:right 0.3s ease; }
.guide-float:hover, .guide-float.open { right:22px; }
.guide-float-toggle, .guide-float-panel { pointer-events:auto; }
.guide-float-toggle { height:40px; padding:0 12px; border:1.5px solid var(--text-primary); border-radius:4px 0 0 4px; background:color-mix(in srgb, var(--bg-secondary) 92%, transparent); color:var(--text-primary); display:inline-flex; align-items:center; gap:8px; cursor:pointer; box-shadow:-4px 4px 16px rgba(0,0,0,0.18); backdrop-filter:blur(14px); transition:transform 0.18s,border-color 0.18s,background 0.18s; }
.guide-float-toggle:hover { transform:translateY(-2px); border-color:var(--accent-hover); background:var(--bg-secondary); }
.guide-float-icon { width:20px; height:20px; border:1px solid var(--text-primary); border-radius:2px; display:inline-flex; align-items:center; justify-content:center; font:700 12px/1 'Georgia',serif; }
.guide-float-text { font:700 11px/1 'Inter',sans-serif; letter-spacing:0.8px; text-transform:uppercase; }
.guide-float-panel { width:min(300px, calc(100vw - 32px)); max-height:calc(100vh - 160px); overflow-y:auto; padding:12px; border:1px solid var(--border-card); border-radius:4px; background:color-mix(in srgb, var(--bg-secondary) 96%, transparent); box-shadow:0 16px 48px rgba(0,0,0,0.32); backdrop-filter:blur(18px); animation:guideFloatIn 0.18s ease; }
@keyframes guideFloatIn { from{opacity:0;transform:translateY(8px) scale(0.98)} to{opacity:1;transform:translateY(0) scale(1)} }
.guide-float-head { display:flex; align-items:flex-start; justify-content:space-between; gap:12px; margin-bottom:10px; }
.guide-float-title { margin:0 0 3px; font:700 13px/1 'Inter',sans-serif; color:var(--text-primary); letter-spacing:0.04em; text-transform:uppercase; }
.guide-float-sub { margin:0; font:400 10px/1 'Georgia',serif; color:var(--text-secondary); font-variant-numeric:tabular-nums; letter-spacing:0.1em; }
.guide-float-close { width:24px; height:24px; border:none; border-radius:2px; background:var(--input-bg); color:var(--text-secondary); font-size:16px; line-height:1; cursor:pointer; }
.guide-float-close:hover { color:var(--text-primary); background:var(--bg-card); }
.guide-float-steps { border-left:2px solid var(--border-card); margin-left:6px; margin-bottom:10px; }
.gf-step { display:flex; align-items:flex-start; gap:10px; padding:7px 0 7px 12px; border-bottom:1px solid var(--border-card); cursor:pointer; transition:background 0.2s; position:relative; }
.gf-step::before { content:''; position:absolute; left:-5px; top:12px; width:8px; height:8px; border:1.5px solid var(--border-card); background:var(--bg-secondary); transition:all 0.2s; }
.gf-step.active::before { border-color:var(--accent-hover); background:var(--accent-hover); }
.gf-step:hover { background:var(--input-bg); }
.gf-step:last-child { border-bottom:none; }
.gf-step-num { font:700 13px/1 'Georgia',serif; color:var(--text-muted); font-variant-numeric:tabular-nums; flex-shrink:0; width:20px; transition:color 0.2s; }
.gf-step.active .gf-step-num { color:var(--text-primary); }
.gf-step-body { flex:1; min-width:0; }
.gf-step-title { font:500 10px/1.3 'Inter',sans-serif; color:var(--text-secondary); letter-spacing:0.04em; text-transform:uppercase; transition:color 0.2s; }
.gf-step.active .gf-step-title { color:var(--text-primary); }
.gf-step-desc { font:300 10px/1.6 'Georgia',serif; color:var(--text-secondary); margin:4px 0 0; letter-spacing:0.01em; animation:gfDescIn 0.25s ease; }
@keyframes gfDescIn { from{opacity:0;transform:translateY(-3px)} to{opacity:1;transform:translateY(0)} }
.guide-float-actions { display:flex; gap:8px; margin-bottom:8px; }
.guide-float-actions button { flex:1; padding:6px 10px; border:1px solid var(--border-card); border-radius:2px; background:var(--input-bg); color:var(--text-primary); font:500 10px/1 'Inter',sans-serif; letter-spacing:0.1em; text-transform:uppercase; cursor:pointer; transition:border-color 0.18s,color 0.18s,background 0.18s; }
.guide-float-actions button:hover:not(:disabled) { border-color:var(--accent-hover); color:var(--accent-hover); }
.guide-float-actions button:disabled { opacity:0.25; cursor:not-allowed; }

@media (max-width:768px) {
  .guide-float { right:-30px; bottom:64px; }
  .guide-float:hover, .guide-float.open { right:14px; }
  .guide-float-toggle { height:36px; padding:0 10px; }
  .guide-float-panel { width:min(260px, calc(100vw - 28px)); }
}


</style>