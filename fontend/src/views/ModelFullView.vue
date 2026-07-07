
<template>
  <div class="model-full-view" :class="{ 'gesture-fullscreen': gestureFullscreen, 'exit-animating': exitAnimating }">

    <!-- 雕刻模式覆盖层 -->
    <div v-if="sculptMode" class="sculpt-overlay">
      <SculptEditor
        ref="sculptEditorRef"
        :modelUrl="currentModelUrl"
        :modelText="cachedModelText"
        :initialFaceParams="savedFaceParams"
        @update:expressions="onSculptExpressions"
        @anchorsChanged="onAnchorsChanged"
        @save="onSaveFaceParams"
        @reset="onResetFaceParams"
        @back="exitSculptMode"
      />
    </div>

    <!-- 手势全屏海报模式 -->
    <div v-if="gestureFullscreen && !sculptMode" ref="posterModeRef" class="poster-mode" :class="{ 'poster-exit': gestureExiting }">
      <div ref="posterThreeContainer" class="poster-three"></div>

      <!-- 装饰线 -->
      <div class="poster-deco-tl"></div>
      <div class="poster-deco-tr"></div>
      <div class="poster-deco-bl"></div>
      <div class="poster-deco-br"></div>
      <div class="poster-rule-top"></div>
      <div class="poster-rule-bottom"></div>

      <div class="poster-texts" :class="'poster-texts-' + currentView" :key="'poster-' + currentView">
        <div class="poster-big-title" :class="'anim-' + currentView">{{ posterTitle }}</div>
        <div class="poster-subtitle" :class="'anim-' + currentView">NFC CHARACTER SYSTEM</div>
        <div class="poster-view-label" :class="'anim-' + currentView">{{ currentViewLabel }}</div>
        <div class="poster-date" :class="'anim-' + currentView">{{ currentDate }}</div>
      </div>

      <!-- 右上角期刊标 + 操作线组 -->
      <div class="poster-top-tools">
        <div class="poster-issue-tag">VOL.{{ versionId }}</div>
        <button class="poster-tool poster-share-btn" :disabled="sharing || postcardMode" @click="sharePoster">
          <span class="poster-tool-line"></span>
          <span class="poster-tool-text">{{ sharing ? 'Sharing…' : 'Share' }}</span>
        </button>
        <button class="poster-tool poster-exit-btn-inline" @click="exitGestureFullscreen">
          <span class="poster-tool-line"></span>
          <span class="poster-tool-text">Exit</span>
        </button>
      </div>

      <div class="camera-preview active">
        <video ref="videoEl" autoplay playsinline></video>
        <canvas ref="canvasEl"></canvas>
      </div>
      <div class="gesture-hint-poster">
        <div class="hint-item">✊ Front (0 fingers)</div>
        <div class="hint-item">☝ Right (1 finger)</div>
        <div class="hint-item">✌ Left (2 fingers)</div>
        <div class="hint-item">🤟 Back (3 fingers)</div>
        <div class="hint-item">🖐 Top (4 fingers)</div>
        <div class="hint-item">👐 Bottom (both hands)</div>
        <div v-if="gestureDebug" class="hint-debug">{{ gestureDebug }}</div>
      </div>

      <!-- ═══ 明信片模式覆盖层 ═══ -->
      <Transition name="postcard-transition">
        <div v-if="postcardMode" class="postcard-overlay" @click.self="cancelPostcard">
          <!-- 入场动画容器 -->
          <div class="postcard-entrance">
            <!-- 翻转容器 -->
            <div class="postcard-flipper" :class="{ flipped: postcardFlipped }">
              <!-- 正面：海报截图 -->
              <div class="postcard-face postcard-front" @click="flipPostcard">
              <img v-if="postcardSnapshot" :src="postcardSnapshot" class="postcard-front-img" alt="poster snapshot" />
              <div class="postcard-front-hint">TAP TO FLIP</div>
            </div>
              <!-- 反面：签名 + 邮戳 + 操作 -->
              <div ref="postcardBackRef" class="postcard-face postcard-back" @click="flipPostcardBack">
              <!-- 操作按钮：右上角 -->
              <div class="postcard-back-actions">
                <button class="pc-action-btn pc-send-btn" :disabled="sharing" @click.stop="sendPostcard" @touchend.stop.prevent="sendPostcard" aria-label="Send to community">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M22 2L11 13"/><path d="M22 2L15 22L11 13L2 9L22 2Z"/></svg>
                </button>
                <button class="pc-action-btn pc-close-btn" @click.stop="cancelPostcard" @touchend.stop.prevent="cancelPostcard" aria-label="Cancel">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                </button>
              </div>
              <div class="pc-stamp" :class="{ 'stamp-animate': postcardStamped }">
                <div class="stamp-circle">
                  <div class="stamp-text-top">NFC CHARACTER</div>
                  <div class="stamp-date">{{ postcardStampDate }}</div>
                  <div class="stamp-text-bottom">VOL. {{ versionId }}</div>
                </div>
              </div>

              <div class="pc-postage-box"></div>

              <div class="pc-center-divider"></div>

              <div class="pc-code-zone pc-code-zone-left">
                <span class="pc-code-box"></span>
                <span class="pc-code-box"></span>
                <span class="pc-code-box"></span>
                <span class="pc-code-box"></span>
                <span class="pc-code-box"></span>
                <span class="pc-code-box"></span>
              </div>

              <div class="pc-address-write">
                <div class="pc-address-line long"></div>
                <div class="pc-address-line"></div>
              </div>

              <div class="pc-code-zone pc-code-zone-right">
                <span class="pc-code-box"></span>
                <span class="pc-code-box"></span>
                <span class="pc-code-box"></span>
                <span class="pc-code-box"></span>
                <span class="pc-code-box"></span>
              </div>

              <div class="pc-brand-mark">
                <div class="pc-brand-en">NFC CHARACTER</div>
              </div>

              <div class="pc-creation-note">
                <div class="pc-note-row"><span>Created</span><strong>{{ postcardStampDate }}</strong></div>
                <div class="pc-note-row"><span>View</span><strong>{{ currentViewLabel }}</strong></div>
              </div>

              <div class="pc-flip-back-hint">TAP TO FLIP</div>
              </div>
            </div>
          </div>
        </div>
      </Transition>

    </div>

    <!-- 主界面：黄金比例全屏布局 -->
    <div v-show="!sculptMode && !gestureFullscreen" class="golden-layout" :class="{ 'golden-enter': goldenEntering }">
      <!-- 全屏3D画布 -->
      <div ref="threeContainer" class="three-fullscreen"></div>

      <!-- 返回 — 左上角极简 -->
      <button class="g-back" @click="goBack">‹ Back</button>

      <!-- 杂志大标题层 — 与模型重叠 -->
      <div class="g-titles">
        <div class="g-date">{{ currentDate }}</div>
        <div class="g-brand">
          <span class="g-brand-thin">N F C</span>
          <span class="g-brand-bold">CHARACTER.</span>
        </div>
        <div class="g-issue">ISSUE {{ versionId }}</div>
      </div>


      <!-- 模型切换 — 极简标签 -->
      <div class="g-model-tag" v-if="sculptedModelUrlRef">
        <span :class="{ active: !viewingSculpted }" @click="switchToOriginal">Original</span>
        <span class="g-tag-sep">|</span>
        <span :class="{ active: viewingSculpted }" @click="switchToSculpted">Sculpted</span>
      </div>

      <!-- 右侧专栏浮层 — 杂志编辑排版 -->
      <div class="g-sidebar">
        <!-- 角标装饰 -->
        <div class="gs-corner gs-corner-tl">┌</div>
        <div class="gs-corner gs-corner-tr">┐</div>

        <!-- 刊头 -->
        <div class="gs-masthead">
          <div class="gs-masthead-left">
            <span class="gs-vol">VOL.</span>
            <span class="gs-no">{{ versionId }}</span>
          </div>
          <div class="gs-masthead-right">
            <span class="gs-folio">CHARACTER</span>
            <span class="gs-folio-sub">PROFILE</span>
          </div>
        </div>

        <div class="gs-rule gs-rule-double"></div>

        <!-- 角色名 — 大标题 -->
        <div class="gs-character-name" v-if="characterName">{{ characterName }}</div>
        <div class="gs-character-name gs-character-name-placeholder" v-else>Character</div>

        <div class="gs-rule"></div>

        <!-- 角色描述 — 杂志引言 -->
        <div class="gs-lead-block" v-if="versionData.characterPrompt">
          <span class="gs-dropcap">{{ (versionData.characterPrompt || '').charAt(0).toUpperCase() }}</span>
          <p class="gs-lead">{{ versionData.characterPrompt }}</p>
        </div>

        <!-- 场景 -->
        <div class="gs-prompt-block" v-if="versionData.scenePrompt">
          <div class="gs-prompt-label">SCENE</div>
          <p class="gs-prompt-text">{{ versionData.scenePrompt }}</p>
        </div>

        <!-- 动作 -->
        <div class="gs-prompt-block" v-if="versionData.actionPrompt">
          <div class="gs-prompt-label">ACTION</div>
          <p class="gs-prompt-text">{{ versionData.actionPrompt }}</p>
        </div>

        <!-- 无描述时的默认导读 -->
        <div class="gs-lead-block" v-if="!versionData.characterPrompt && !versionData.scenePrompt && !versionData.actionPrompt">
          <span class="gs-dropcap">N</span>
          <p class="gs-lead">FC-powered character with real-time face sculpting, gesture control, and physical card presets.</p>
        </div>

        <!-- 统计数据 — 紧凑行 -->
        <div class="gs-stats-inline">
          <span class="gs-stat-chip"><em>{{ faceParamCount }}</em> params</span>
          <span class="gs-stat-chip"><em>{{ presetCount }}</em> presets</span>
          <span class="gs-stat-chip"><em>6</em> views</span>
          <span class="gs-stat-chip" v-if="versionData.status"><em>{{ versionData.status }}</em></span>
        </div>

        <div class="gs-rule"></div>

        <!-- 月相弧形视角切换 -->
        <div class="gs-moon-arc">
          <svg
            ref="arcSvgRef"
            viewBox="0 0 280 120"
            class="moon-svg"
            @pointerdown="onArcPointerDown"
            @pointermove="onArcPointerMove"
            @pointerup="onArcPointerUp"
            @pointerleave="onArcPointerUp"
          >
            <!-- 底部弧线 -->
            <path d="M 15,105 Q 140,50 265,105" fill="none" stroke="rgba(0,0,0,0.10)" stroke-width="1"/>
            <path d="M 15,105 Q 140,50 265,105" fill="none" stroke="rgba(0,0,0,0.01)" stroke-width="28" class="moon-arc-hit"/>
            <path d="M 15,105 Q 140,50 265,105" fill="none" stroke="rgba(0,0,0,0.18)" stroke-width="2" stroke-linecap="round" class="moon-arc-active"/>
            <!-- 视角点 -->
            <g v-for="(v, idx) in views" :key="v.name" class="moon-point" :class="{ active: currentView === v.name }">
              <text :x="moonPoints[idx].x" :y="moonPoints[idx].y - 16" text-anchor="middle" class="moon-label">{{ arcDegrees[idx] }}</text>
              <circle :cx="moonPoints[idx].x" :cy="moonPoints[idx].y" :r="currentView === v.name ? 8 : 4" :class="currentView === v.name ? 'moon-dot-active' : 'moon-dot'" />
            </g>
            <circle :cx="arcSliderPos.x" :cy="arcSliderPos.y" r="8" class="moon-slider-ring" />
            <circle :cx="arcSliderPos.x" :cy="arcSliderPos.y" r="3" class="moon-slider-core" />
          </svg>
          <div class="moon-arc-hint">DRAG ARC TO ROTATE VIEW</div>
        </div>

        <div class="gs-rule gs-rule-thick"></div>

        <!-- 操作入口 — 章节导读 -->
        <div class="gs-chapters">
          <div class="gs-section-mark">FEATURES</div>
          <div class="gs-chapter" @click="enterSculptMode">
            <span class="gs-ch-num">§1</span>
            <div class="gs-ch-body">
              <span class="gs-ch-title">Sculpt Mode</span>
              <span class="gs-ch-desc">Real-time face parameter editing</span>
            </div>
          </div>
          <div class="gs-chapter" @click="enterGestureFullscreen">
            <span class="gs-ch-num">§2</span>
            <div class="gs-ch-body">
              <span class="gs-ch-title">Gesture Poster</span>
              <span class="gs-ch-desc">Hand-controlled 3D viewing</span>
            </div>
          </div>
          <div class="gs-chapter" :class="{ disabled: printing }" @click="openBambuStudio">
            <span class="gs-ch-num">§3</span>
            <div class="gs-ch-body">
              <span class="gs-ch-title">{{ printing ? 'Launching…' : '3D Print' }}</span>
              <span class="gs-ch-desc">Export to Bambu Studio</span>
            </div>
          </div>
        </div>

        <!-- 页脚 — 杂志页脚 -->
        <div class="gs-footer">
          <span class="gs-footer-left">NFC × AI</span>
          <span class="gs-footer-center">—  {{ currentDate }}  —</span>
          <span class="gs-footer-right">P. {{ versionId }}</span>
        </div>

        <!-- 角标装饰 -->
        <div class="gs-corner gs-corner-bl">└</div>
        <div class="gs-corner gs-corner-br">┘</div>
      </div>
    </div>

    <!-- NFC 刷卡弹出动画 -->
    <Transition name="nfc-pop">
      <div v-if="nfcCardVisible" class="nfc-card-popup" @click="applyNfcCard">
        <div class="nfc-card-inner">
          <div class="nfc-card-glow"></div>
          <div class="nfc-card-icon">{{ nfcCardIcon }}</div>
          <div class="nfc-card-title">{{ nfcCardTitle }}</div>
          <div class="nfc-card-hint">Tap to apply</div>
        </div>
      </div>
    </Transition>

    <!-- Toast -->
    <div v-if="saveToast" class="save-toast" :class="saveToastType" @click="saveToast = ''">{{ saveToast }}</div>
    <div v-if="printMsg" class="print-toast" :class="{ success: printSuccess }" @click="printMsg = ''">{{ printMsg }}</div>
    <div v-if="errorMsg" class="error-toast" @click="errorMsg = ''">{{ errorMsg }}</div>

    <!-- 加载 -->
    <div v-if="loadingModel" class="loading-overlay">
      <div class="spinner"></div>
      <span>{{ loadingProgress > 0 ? 'Loading model.. ' + loadingProgress + '%' : 'Loading model..' }}</span>
    </div>
    <div v-if="gestureLoading" class="loading-overlay gesture-loading">
      <div class="spinner"></div>
      <span>{{ gestureStatus }}</span>
    </div>

    <video ref="videoElHidden" autoplay playsinline style="display:none"></video>
    <canvas ref="canvasElHidden" style="display:none"></canvas>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import html2canvas from 'html2canvas'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls'
import { OBJLoader } from 'three/examples/jsm/loaders/OBJLoader'
import { print3D, saveFaceParams, uploadSculptedModel, getNfcFaceParams, getNfcPresetCommand, getVersion } from '../api/api.js'
import SculptEditor from '../components/SculptEditor.vue'
import { EXPRESSION_PRESETS, FACE_PRESETS, FACE_PARAMS_DEF } from '../components/faceRigSystem.js'

let Hands = null, HAND_CONNECTIONS = null, MediaPipeCamera = null, drawingUtils = null
function loadScriptOnce(src, validate) {
  if (validate()) return Promise.resolve()
  return new Promise((resolve, reject) => {
    const existing = Array.from(document.scripts).find(s => s.src === src)
    const onLoad = () => validate() ? resolve() : reject(new Error('Global missing: ' + src))
    const onErr = () => reject(new Error('Load failed: ' + src))
    if (existing) { existing.addEventListener('load', onLoad, { once: true }); existing.addEventListener('error', onErr, { once: true }); setTimeout(() => { if (validate()) resolve() }, 0); return }
    const s = document.createElement('script'); s.src = src; s.crossOrigin = 'anonymous'; s.async = true; s.onload = onLoad; s.onerror = onErr; document.head.appendChild(s)
  })
}

const route = useRoute()
const router = useRouter()
const versionId = route.params.versionId

const threeContainer = ref(null)
const posterThreeContainer = ref(null)
const posterModeRef = ref(null)
const videoEl = ref(null)
const canvasEl = ref(null)
const videoElHidden = ref(null)
const canvasElHidden = ref(null)
const arcSvgRef = ref(null)

const cameraOn = ref(false)
const currentView = ref('front')
const loadingModel = ref(true)
const gestureLoading = ref(false)
const gestureStatus = ref('')
const errorMsg = ref('')
const gestureDebug = ref('')
const loadingProgress = ref(0)
const printing = ref(false)
const printMsg = ref('')
const printSuccess = ref(false)
const gestureFullscreen = ref(false)
const gestureExiting = ref(false)
const goldenEntering = ref(false)
const sharing = ref(false)
const postcardMode = ref(false)
const postcardSnapshot = ref('')
const postcardStamped = ref(false)
const postcardFlipped = ref(false)
const postcardBackRef = ref(null)

const sculptMode = ref(false)
const sculptEditorRef = ref(null)
const currentModelUrl = ref('')
const sculptedModelUrlRef = ref(null)
const viewingSculpted = ref(false)
const sculptExpressions = ref([0, 0, 0, 0, 0, 0, 0])
const savedFaceParams = ref(null)
const characterName = ref('')
const versionData = ref({})

let nfcPollingTimer = null
const NFC_POLL_INTERVAL_FAST = 150
const NFC_POLL_INTERVAL_IDLE = 300
let currentPollInterval = NFC_POLL_INTERVAL_FAST
let nfcPolling = false
let lastNfcParams = null
let nfcIdleCount = 0
const NFC_IDLE_THRESHOLD = 10
let lastPresetTimestamp = 0
let lastPenTimestamp = 0

const nfcCardVisible = ref(false)
const nfcCardIcon = ref('🎴')
const nfcCardTitle = ref('')
let pendingNfcPreset = null

let versionCardPollTimer = null
let lastVersionCardTs = 0
let versionCardPolling = false

const allPresets = [...EXPRESSION_PRESETS, ...FACE_PRESETS]
const presetCount = allPresets.length
const faceParamCount = FACE_PARAMS_DEF.length

const paramCategories = [
  { id: 'face', name: 'Face Shape', icon: '😊' },
  { id: 'eyes', name: 'Eyes', icon: '👁️' },
  { id: 'nose', name: 'Nose', icon: '👃' },
  { id: 'mouth', name: 'Chin & Mouth', icon: '🫦' },
]

const now = new Date()
const months = ['JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC']
const currentDate = months[now.getMonth()] + '. ' + now.getDate() + ' ' + now.getFullYear()
const postcardStampDate = `${now.getFullYear()}.${String(now.getMonth()+1).padStart(2,'0')}.${String(now.getDate()).padStart(2,'0')}`
const datelineMonths = computed(() => {
  const cur = now.getMonth()
  return months.map((m, i) => ({ label: m, active: i === cur }))
})

const posterTitles = { front:'FRONT.', back:'BACK.', left:'LEFT.', right:'RIGHT.', top:'TOP.', bottom:'BOTTOM.' }
const posterTitle = computed(() => posterTitles[currentView.value] || 'FRONT.')

const saveToast = ref('')
const saveToastType = ref('')
function showSaveToast(msg, type = 'success', dur = 3000) { saveToast.value = msg; saveToastType.value = type; setTimeout(() => { saveToast.value = '' }, dur) }

const views = [
  { name:'front', label:'Front', pos:[0,0,2.35] },
  { name:'back', label:'Back', pos:[0,0,-2.35] },
  { name:'left', label:'Left', pos:[-2.35,0,0] },
  { name:'right', label:'Right', pos:[2.35,0,0] },
  { name:'top', label:'Top', pos:[0,2.35,0] },
  { name:'bottom', label:'Bottom', pos:[0,-2.35,0] }
]
const currentViewLabel = computed(() => { const v = views.find(x => x.name === currentView.value); return v ? v.label : '' })

const arcDegrees = ['0°', '180°', '270°', '90°', 'Top', 'Btm']
// 月相弧线上6个点的坐标，viewBox 0 0 280 120
const moonPoints = [
  { x: 25,  y: 72 },   // 0° — front (左端)
  { x: 70,  y: 42 },   // 180° — back
  { x: 118, y: 24 },   // 270° — left
  { x: 162, y: 24 },   // 90° — right
  { x: 210, y: 42 },   // Top
  { x: 255, y: 72 },   // Btm (右端)
]
const arcDragging = ref(false)
const currentMoonPoint = computed(() => {
  const idx = Math.max(0, views.findIndex(v => v.name === currentView.value))
  return moonPoints[idx] || moonPoints[0]
})
// 二次贝塞尔曲线 M15,105 Q140,50 265,105
function arcBezierY(x) {
  const t = Math.max(0, Math.min(1, (x - 15) / 250))
  const p0y = 105, p1y = 50, p2y = 105
  return (1-t)*(1-t)*p0y + 2*(1-t)*t*p1y + t*t*p2y
}
// 滑块精确落在弧线上
const arcSliderPos = computed(() => {
  const mp = currentMoonPoint.value
  return { x: mp.x, y: arcBezierY(mp.x) }
})
function setViewFromArcPointer(event) {
  const svg = arcSvgRef.value
  if (!svg) return
  const rect = svg.getBoundingClientRect()
  const x = ((event.clientX - rect.left) / rect.width) * 280
  const y = ((event.clientY - rect.top) / rect.height) * 160
  if (y < 50) return
  let nearestIdx = 0
  let minDist = Infinity
  moonPoints.forEach((p, idx) => {
    const trackY = p.y + 24
    const dist = Math.hypot(x - p.x, y - trackY)
    if (dist < minDist) {
      minDist = dist
      nearestIdx = idx
    }
  })
  switchView(views[nearestIdx].name)
}
function onArcPointerDown(event) {
  arcDragging.value = true
  setViewFromArcPointer(event)
}
function onArcPointerMove(event) {
  if (!arcDragging.value) return
  setViewFromArcPointer(event)
}
function onArcPointerUp() {
  arcDragging.value = false
}

const exitAnimating = ref(false)
function goBack() {
  exitAnimating.value = true
  setTimeout(() => { router.back() }, 650)
}

let scene, camera, renderer, controls, currentModel = null, mediapipeCamera = null, hands = null, animationId = null
const objTextCache = new Map()
const cachedModelText = computed(() => {
  const url = currentModelUrl.value
  return url && objTextCache.has(url) ? objTextCache.get(url) : ''
})

function switchView(name) {
  const v = views.find(x => x.name === name)
  if (!v || !camera || !controls) return
  currentView.value = name
  const [tx,ty,tz] = v.pos
  const start = { x:camera.position.x, y:camera.position.y, z:camera.position.z }
  const duration = 600, startTime = Date.now()
  function tween() {
    const t = Math.min((Date.now()-startTime)/duration, 1)
    const e = t<0.5 ? 2*t*t : -1+(4-2*t)*t
    camera.position.set(start.x+(tx-start.x)*e, start.y+(ty-start.y)*e, start.z+(tz-start.z)*e)
    camera.lookAt(0,0,0); controls.update()
    if (t<1) requestAnimationFrame(tween)
  }
  tween()
}

function initThree(container) {
  if (!container) return
  const w = container.clientWidth || window.innerWidth, h = container.clientHeight || window.innerHeight
  scene = new THREE.Scene(); scene.background = new THREE.Color(0x0a0a0a)
  camera = new THREE.PerspectiveCamera(45, w/h, 0.1, 1000); camera.position.set(0,0,3)
  const isTablet = /iPad|Android|Tablet/i.test(navigator.userAgent) || (navigator.maxTouchPoints>1 && /Macintosh/i.test(navigator.userAgent))
  renderer = new THREE.WebGLRenderer({ antialias:!isTablet, powerPreference:'default', alpha:true })
  renderer.setSize(w,h); renderer.setPixelRatio(Math.min(window.devicePixelRatio, isTablet?1.5:2))
  renderer.toneMapping = THREE.ACESFilmicToneMapping; renderer.toneMappingExposure = 1.2
  container.appendChild(renderer.domElement)
  controls = new OrbitControls(camera, renderer.domElement); controls.enableDamping = true; controls.dampingFactor = 0.08
  scene.add(new THREE.AmbientLight(0xffffff, 0.5))
  const d = new THREE.DirectionalLight(0xffffff, 1); d.position.set(5,10,7); scene.add(d)
  const b = new THREE.DirectionalLight(0x4488ff, 0.3); b.position.set(-5,-3,-5); scene.add(b)
  scene.add(new THREE.GridHelper(10, 20, 0x1a1a1a, 0x111111))
  window.addEventListener('resize', onResize); animate()
}

function destroyThree() {
  if (animationId) { cancelAnimationFrame(animationId); animationId = null }
  if (renderer) { renderer.dispose(); renderer.forceContextLoss(); if (renderer.domElement?.parentNode) renderer.domElement.parentNode.removeChild(renderer.domElement); renderer = null }
  if (controls) { controls.dispose(); controls = null }
  if (currentModel && scene) { scene.remove(currentModel); currentModel = null }
  if (scene) { scene.traverse(c => { if(c.geometry)c.geometry.dispose(); if(c.material){if(Array.isArray(c.material))c.material.forEach(m=>m.dispose());else c.material.dispose()} }); scene = null }
  camera = null
}

function onResize() {
  if (!camera || !renderer) return
  const c = renderer.domElement.parentNode; if (!c) return
  camera.aspect = c.clientWidth/c.clientHeight; camera.updateProjectionMatrix(); renderer.setSize(c.clientWidth, c.clientHeight)
}

function animate() { if (!renderer||!scene||!camera) return; animationId = requestAnimationFrame(animate); if(controls)controls.update(); renderer.render(scene,camera) }

function setupModel(object) {
  if (!scene) return; if (currentModel) scene.remove(currentModel)
  const box = new THREE.Box3().setFromObject(object); const center = box.getCenter(new THREE.Vector3()); object.position.sub(center)
  const size = box.getSize(new THREE.Vector3()).length(); object.scale.setScalar(2/size)
  object.traverse(c => { if(c.isMesh) c.material = new THREE.MeshStandardMaterial({color:0xcccccc,metalness:0.1,roughness:0.6}) })
  scene.add(object); currentModel = object
}

async function loadModel(url) {
  if (!scene||!url) { loadingModel.value=false; return }
  loadingModel.value=true; loadingProgress.value=0
  if (objTextCache.has(url)) { setupModel(new OBJLoader().parse(objTextCache.get(url))); loadingModel.value=false; loadingProgress.value=100; return }
  try {
    const resp = await fetch(url); if(!resp.ok) throw new Error('HTTP '+resp.status)
    const total = parseInt(resp.headers.get('content-length')||'0',10)
    const reader = resp.body.getReader(); const chunks=[]; let loaded=0
    while(true) { const {done,value}=await reader.read(); if(done)break; chunks.push(value); loaded+=value.length; if(total>0) loadingProgress.value=Math.round(loaded/total*100) }
    const text = await new Blob(chunks).text(); objTextCache.set(url, text)
    if(!scene){loadingModel.value=false;return}
    setupModel(new OBJLoader().parse(text)); loadingModel.value=false; loadingProgress.value=100
  } catch(e) { console.error('模型加载失败:',e); loadingModel.value=false }
}

async function enterSculptMode() {
  if(cameraOn.value) await toggleCamera()
  destroyThree(); await new Promise(r=>setTimeout(r,100)); sculptMode.value=true
  // 每次进入 sculpt 模式时清除刷卡缓存，避免残留上一轮未读完的卡
  lastNfcParams = null
  nfcIdleCount = 0
  lastPresetTimestamp = Date.now()
  currentPollInterval = NFC_POLL_INTERVAL_FAST
  try { await axios.get('/api/nfc/setCurrent?versionId='+versionId) } catch(e) {}
  // 清除 SculptEditor 中可能残留的卡片动画
  nextTick(() => {
    if (sculptEditorRef.value && typeof sculptEditorRef.value.clearNfcCardAnimation === 'function') {
      sculptEditorRef.value.clearNfcCardAnimation()
    }
  })
  startNfcPolling()
}
function enterSculptWithCategory(catId) { enterSculptMode() }
function exitSculptMode() {
  sculptMode.value=false; stopNfcPolling()
  if(sculptedModelUrlRef.value) viewingSculpted.value=true
  setTimeout(()=>{ initThree(threeContainer.value); const url=viewingSculpted.value&&sculptedModelUrlRef.value?sculptedModelUrlRef.value:currentModelUrl.value; if(url)loadModel(url) },100)
}
function switchToOriginal() { viewingSculpted.value=false; loadModel(currentModelUrl.value) }
function switchToSculpted() { if(sculptedModelUrlRef.value){viewingSculpted.value=true;loadModel(sculptedModelUrlRef.value)} }
function onSculptExpressions(e) { sculptExpressions.value=e }
function onAnchorsChanged(d) { console.log('锚点更新:',d) }
async function onResetFaceParams(fp) { try { savedFaceParams.value={...(fp||{})}; lastNfcParams=JSON.stringify(savedFaceParams.value); nfcIdleCount=0; currentPollInterval=NFC_POLL_INTERVAL_FAST; await saveFaceParams(versionId,savedFaceParams.value,null) } catch(e){} }
async function onSaveFaceParams(sd) {
  try {
    let fp,mb; if(sd&&sd.params){fp=sd.params;mb=sd.modelBlob}else{fp=sd;mb=null}
    try{await saveFaceParams(versionId,fp,null);savedFaceParams.value={...fp}}catch(e){showSaveToast('❌ Save failed','error',4000);return}
    if(mb&&mb.size>0){try{const f=new File([mb],'sculpted_v'+versionId+'.obj',{type:'text/plain'});const r=await uploadSculptedModel(f,'sculpted_v'+versionId);await saveFaceParams(versionId,fp,r.data.modelUrl);sculptedModelUrlRef.value=r.data.modelUrl;viewingSculpted.value=true;showSaveToast('✅ Saved','success')}catch(e){showSaveToast('⚠️ Params saved, model failed','warn',4000)}}
    else showSaveToast('✅ Saved','success')
  } catch(e) { showSaveToast('❌ Error','error',4000) }
}

async function sharePoster() {
  if (sharing.value || postcardMode.value || !posterModeRef.value) return

  // 确保 3D 渲染最新帧
  if (posterRenderer && posterScene && posterCamera) {
    posterRenderer.render(posterScene, posterCamera)
  }

  try {
    // 直接用 3D canvas 作为底图，再单独叠加文字/装饰层
    const threeCanvas = posterRenderer ? posterRenderer.domElement : null
    if (!threeCanvas) throw new Error('No 3D canvas')

    const rect = posterModeRef.value.getBoundingClientRect()
    const w = threeCanvas.width
    const h = threeCanvas.height

    const compositeCanvas = document.createElement('canvas')
    compositeCanvas.width = w
    compositeCanvas.height = h
    const ctx = compositeCanvas.getContext('2d')

    // 1) 先画模型底图
    ctx.drawImage(threeCanvas, 0, 0, w, h)

    // 2) 只截海报文字和装饰，避免整层黑底把模型盖掉
    const overlayRoot = document.createElement('div')
    overlayRoot.style.position = 'fixed'
    overlayRoot.style.left = '0'
    overlayRoot.style.top = '0'
    overlayRoot.style.width = rect.width + 'px'
    overlayRoot.style.height = rect.height + 'px'
    overlayRoot.style.pointerEvents = 'none'
    overlayRoot.style.background = 'transparent'
    overlayRoot.style.zIndex = '-1'
    document.body.appendChild(overlayRoot)

    const overlaySelectors = [
      '.poster-deco-tl',
      '.poster-deco-tr',
      '.poster-deco-bl',
      '.poster-deco-br',
      '.poster-rule-top',
      '.poster-rule-bottom',
      '.poster-texts'
    ]

    overlaySelectors.forEach(selector => {
      posterModeRef.value.querySelectorAll(selector).forEach(node => {
        overlayRoot.appendChild(node.cloneNode(true))
      })
    })

    try {
      const overlayCanvas = await html2canvas(overlayRoot, {
        backgroundColor: null,
        scale: w / rect.width,
        useCORS: true,
        allowTaint: true,
        logging: false,
      })
      ctx.drawImage(overlayCanvas, 0, 0, w, h)
    } finally {
      document.body.removeChild(overlayRoot)
    }

    postcardSnapshot.value = compositeCanvas.toDataURL('image/png')
  } catch (e) {
    // fallback: 只截 3D canvas
    postcardSnapshot.value = posterRenderer
      ? posterRenderer.domElement.toDataURL('image/png')
      : ''
  }

  // 进入明信片模式，显示正面
  postcardMode.value = true
  postcardFlipped.value = false
  postcardStamped.value = false

  // 延迟触发邮戳动画（翻到反面时可见）
  await nextTick()
  setTimeout(() => { postcardStamped.value = true }, 200)
}

function flipPostcard() {
  if (!postcardFlipped.value) postcardFlipped.value = true
}
function flipPostcardBack(e) {
  // 不拦截按钮点击
  if (e.target.closest('.pc-action-btn')) return
  if (postcardFlipped.value) postcardFlipped.value = false
}

function cancelPostcard() {
  postcardMode.value = false
  postcardFlipped.value = false
  postcardStamped.value = false
  postcardSnapshot.value = ''
}

async function sendPostcard() {
  if (sharing.value) return
  sharing.value = true
  try {
    // 截取正面海报图作为社区封面
    const blob = await (await fetch(postcardSnapshot.value)).blob()
    if (!blob) throw new Error('Failed to create blob')

    const presignRes = await axios.get('/api/upload/presign', {
      params: {
        fileName: `postcard_${versionId}_${currentView.value}_${Date.now()}.png`,
        contentType: 'image/png'
      }
    })

    const { uploadUrl, fileUrl } = presignRes.data || {}
    if (!uploadUrl) throw new Error('Missing upload url')

    const uploadRes = await fetch(uploadUrl, {
      method: 'PUT',
      headers: { 'Content-Type': 'image/png' },
      body: blob
    })
    if (!uploadRes.ok) throw new Error('Upload failed')

    const shareRes = await axios.post('/api/community/share', null, {
      params: {
        versionId,
        userId: 1,
        authorName: 'NFC User',
        description: `Postcard · ${currentViewLabel.value} view · ${currentDate}`,
        tags: 'postcard,gesture,' + currentView.value,
        posterImageUrl: fileUrl || ''
      }
    })

    showSaveToast('✅ Postcard shared!', 'success')
    if (shareRes.data && shareRes.data.id) {
      setTimeout(() => {
        cancelPostcard()
        exitGestureFullscreen()
        router.push('/community/' + shareRes.data.id)
      }, 800)
    }
  } catch (e) {
    console.error('明信片分享失败:', e)
    showSaveToast('❌ Share failed', 'error', 4000)
  } finally {
    sharing.value = false
  }
}

async function openBambuStudio() {
  printing.value=true;printMsg.value=''
  try{const r=await print3D(versionId);printSuccess.value=r.data.success;printMsg.value=(r.data.success?'✅ ':'❌ ')+r.data.message}
  catch(e){printSuccess.value=false;printMsg.value='❌ '+(e.response?.data?.message||e.message)}
  finally{printing.value=false;setTimeout(()=>{printMsg.value=''},5000)}
}

function startVersionCardPolling() { stopVersionCardPolling(); lastVersionCardTs=0; versionCardPollTimer=setInterval(pollVersionCard,1500) }
function stopVersionCardPolling() { if(versionCardPollTimer){clearInterval(versionCardPollTimer);versionCardPollTimer=null} }
async function pollVersionCard() {
  if(versionCardPolling)return; versionCardPolling=true
  try{const r=await getNfcPresetCommand(lastVersionCardTs||undefined);const d=r.data;if(!d||d.status!=='ok')return;if(d.presetCommand&&d.presetTimestamp&&d.presetTimestamp>lastVersionCardTs){lastVersionCardTs=d.presetTimestamp;const cmd=d.presetCommand;if(cmd.presetType==='V'&&cmd.targetUrl){console.log('V卡跳转交由全局处理')}}}
  catch(e){}finally{versionCardPolling=false}
}

function startNfcPolling() { stopNfcPolling(); lastNfcParams=savedFaceParams.value?JSON.stringify(savedFaceParams.value):null; nfcIdleCount=0; lastPresetTimestamp=Date.now(); lastPenTimestamp=Date.now(); currentPollInterval=NFC_POLL_INTERVAL_FAST; scheduleNextPoll() }
function scheduleNextPoll() { if(nfcPollingTimer)clearTimeout(nfcPollingTimer); nfcPollingTimer=setTimeout(async()=>{await pollNfcChanges();if(sculptMode.value)scheduleNextPoll()},currentPollInterval) }
function stopNfcPolling() { if(nfcPollingTimer){clearTimeout(nfcPollingTimer);nfcPollingTimer=null;lastNfcParams=null;nfcIdleCount=0;lastPresetTimestamp=0;lastPenTimestamp=0} }

function showNfcCardPopup(icon, title, preset) {
  nfcCardIcon.value = icon; nfcCardTitle.value = title; pendingNfcPreset = preset; nfcCardVisible.value = true
  setTimeout(() => { if(nfcCardVisible.value) nfcCardVisible.value = false }, 5000)
}

function applyNfcCard() {
  nfcCardVisible.value = false
  if (pendingNfcPreset && sculptEditorRef.value && typeof sculptEditorRef.value.setParamValues === 'function') {
    sculptEditorRef.value.setParamValues(pendingNfcPreset)
  }
  pendingNfcPreset = null
}

async function pollNfcChanges() {
  if(!sculptMode.value||nfcPolling)return; nfcPolling=true
  try {
    const r=await getNfcFaceParams(lastPresetTimestamp||undefined); const d=r.data
    if(!d||d.status!=='ok'){nfcIdleCount++;return}

    // ★ 检测 NFC 笔操作（penCommand）— 笔只触发呼吸点，不播卡片动画
    let isPenOperation = false
    if(d.penCommand&&d.penTimestamp&&d.penTimestamp>lastPenTimestamp){
      lastPenTimestamp=d.penTimestamp
      isPenOperation = true
      const penCmd=d.penCommand
      nfcIdleCount=0; currentPollInterval=NFC_POLL_INTERVAL_FAST
      // 笔操作：触发呼吸点
      if(sculptEditorRef.value&&typeof sculptEditorRef.value.triggerBreathingDot==='function'){
        sculptEditorRef.value.triggerBreathingDot(penCmd.readerIndex)
      }
    }

    // ★ 检测 NFC 卡片预设指令 — 卡片走原有卡片插入动画
    if(d.presetCommand&&d.presetTimestamp&&d.presetTimestamp>lastPresetTimestamp){
      lastPresetTimestamp=d.presetTimestamp; const cmd=d.presetCommand; nfcIdleCount=0; currentPollInterval=NFC_POLL_INTERVAL_FAST
      if(cmd.presetType==='V')return
      // 卡片预设：播放卡片弹出动画
      if(cmd.presetType==='A') showNfcCardPopup('🎬', cmd.presetName||'Action', null)
      if(cmd.presetType==='E') showNfcCardPopup('😊', cmd.presetName||'Expression', null)
      if(cmd.presetType==='F') showNfcCardPopup('👤', cmd.presetName||'Face', null)
    }

    if(!d.faceParams){nfcIdleCount++;return}
    let np; try{np=typeof d.faceParams==='string'?JSON.parse(d.faceParams):d.faceParams}catch(e){nfcIdleCount++;return}
    const ns=JSON.stringify(np)
    if(ns!==lastNfcParams){
      lastNfcParams=ns;nfcIdleCount=0;currentPollInterval=NFC_POLL_INTERVAL_FAST
      savedFaceParams.value={...np}

      if(isPenOperation){
        // ★ 笔操作：静默应用参数，不播放卡片动画
        if(sculptEditorRef.value&&typeof sculptEditorRef.value.applyNfcPenValues==='function'){
          const penCmd=d.penCommand
          sculptEditorRef.value.applyNfcPenValues(np, penCmd?.readerIndex ?? -1)
        } else if(sculptEditorRef.value&&typeof sculptEditorRef.value.setParamValues==='function'){
          sculptEditorRef.value.setParamValues(np)
        }
      } else {
        // ★ 卡片操作：播放卡片插入动画
        if(sculptEditorRef.value&&typeof sculptEditorRef.value.applyNfcParamValues==='function') sculptEditorRef.value.applyNfcParamValues(np, d.presetCommand?.presetName || 'NFC Card')
        else if(sculptEditorRef.value&&typeof sculptEditorRef.value.setParamValues==='function') sculptEditorRef.value.setParamValues(np)
      }
    } else { nfcIdleCount++ }
    if(nfcIdleCount>NFC_IDLE_THRESHOLD) currentPollInterval=NFC_POLL_INTERVAL_IDLE
  } catch(e){nfcIdleCount++} finally{nfcPolling=false}
}

async function toggleCamera() {
  if(cameraOn.value){cameraOn.value=false;if(mediapipeCamera){mediapipeCamera.stop();mediapipeCamera=null};return}
  cameraOn.value=true
}

async function enterGestureFullscreen() {
  gestureFullscreen.value=true; gestureLoading.value=true; gestureStatus.value='Initializing camera...'
  destroyThree()
  await nextTick()
  try {
    await loadScriptOnce('/mediapipe/hands.js',()=>!!window.Hands)
    await loadScriptOnce('/mediapipe/camera_utils.js',()=>!!window.Camera)
    await loadScriptOnce('/mediapipe/drawing_utils.js',()=>!!window.drawConnectors)
    Hands=window.Hands;MediaPipeCamera=window.Camera;drawingUtils={drawConnectors:window.drawConnectors,drawLandmarks:window.drawLandmarks}
    HAND_CONNECTIONS=window.HAND_CONNECTIONS||[[0,1],[1,2],[2,3],[3,4],[0,5],[5,6],[6,7],[7,8],[5,9],[9,10],[10,11],[11,12],[9,13],[13,14],[14,15],[15,16],[13,17],[17,18],[18,19],[19,20],[0,17]]
  } catch(e){gestureLoading.value=false;gestureStatus.value='';errorMsg.value='Failed to load MediaPipe';return}
  gestureStatus.value='Starting camera...'
  await nextTick()
  setTimeout(()=>{initPosterThree();startPosterGesture()},300)
}

function exitGestureFullscreen() {
  gestureLoading.value = false
  gestureExiting.value = true

  if (mediapipeCamera) { mediapipeCamera.stop(); mediapipeCamera = null }
  if (hands) { hands.close(); hands = null }

  // 等海报缩小动画完成后再切换
  setTimeout(() => {
    destroyPosterThree()
    gestureExiting.value = false
    gestureFullscreen.value = false
    goldenEntering.value = true

    setTimeout(() => {
      initThree(threeContainer.value)
      const url = viewingSculpted.value && sculptedModelUrlRef.value ? sculptedModelUrlRef.value : currentModelUrl.value
      if (url) loadModel(url)
    }, 50)

    // sidebar 入场动画结束后清除 class
    setTimeout(() => { goldenEntering.value = false }, 700)
  }, 600)
}

let posterScene,posterCamera,posterRenderer,posterControls,posterModel=null,posterAnimId=null,posterViewTweening=false
function initPosterThree(){
  const c=posterThreeContainer.value;if(!c)return
  const w=c.clientWidth||window.innerWidth,h=c.clientHeight||window.innerHeight
  posterScene=new THREE.Scene();posterScene.background=new THREE.Color(0x0a0a0a)
  posterCamera=new THREE.PerspectiveCamera(45,w/h,0.1,1000);posterCamera.position.set(0,0,2.35)
  const isTablet=/iPad|Android|Tablet/i.test(navigator.userAgent)||(navigator.maxTouchPoints>1&&/Macintosh/i.test(navigator.userAgent))
  posterRenderer=new THREE.WebGLRenderer({antialias:!isTablet,alpha:true,powerPreference:'default',preserveDrawingBuffer:true})
  posterRenderer.setSize(w,h);posterRenderer.setPixelRatio(Math.min(window.devicePixelRatio,isTablet?1.5:2))
  posterRenderer.toneMapping=THREE.ACESFilmicToneMapping;posterRenderer.toneMappingExposure=1.2
  c.appendChild(posterRenderer.domElement)
  posterControls=new OrbitControls(posterCamera,posterRenderer.domElement);posterControls.enableDamping=false;posterControls.enableZoom=false
  posterScene.add(new THREE.AmbientLight(0xffffff,0.5))
  const d=new THREE.DirectionalLight(0xffffff,1);d.position.set(5,10,7);posterScene.add(d)
  const b=new THREE.DirectionalLight(0x4488ff,0.3);b.position.set(-5,-3,-5);posterScene.add(b)
  const url=viewingSculpted.value&&sculptedModelUrlRef.value?sculptedModelUrlRef.value:currentModelUrl.value
  if(url&&objTextCache.has(url)){const obj=new OBJLoader().parse(objTextCache.get(url));const box=new THREE.Box3().setFromObject(obj);const center=box.getCenter(new THREE.Vector3());obj.position.sub(center);const size=box.getSize(new THREE.Vector3()).length();obj.scale.setScalar(2.45/size);obj.traverse(ch=>{if(ch.isMesh)ch.material=new THREE.MeshStandardMaterial({color:0xcccccc,metalness:0.1,roughness:0.6})});posterScene.add(obj);posterModel=obj}
  function anim(){if(!posterRenderer)return;posterAnimId=requestAnimationFrame(anim);posterRenderer.render(posterScene,posterCamera)}
  anim()
}
function destroyPosterThree(){
  if(posterAnimId){cancelAnimationFrame(posterAnimId);posterAnimId=null}
  if(posterRenderer){posterRenderer.dispose();posterRenderer.forceContextLoss();if(posterRenderer.domElement?.parentNode)posterRenderer.domElement.parentNode.removeChild(posterRenderer.domElement);posterRenderer=null}
  if(posterControls){posterControls.dispose();posterControls=null}
  posterScene=null;posterCamera=null;posterModel=null
}
function setPosterView(name){
  const v=views.find(x=>x.name===name);if(!v||!posterCamera)return
  currentView.value=name
  posterViewTweening=true
  const[tx,ty,tz]=v.pos
  const sx=posterCamera.position.x,sy=posterCamera.position.y,sz=posterCamera.position.z
  const duration=600,startTime=Date.now()
  function tween(){
    const t=Math.min((Date.now()-startTime)/duration,1)
    const e=t<0.5?2*t*t:-1+(4-2*t)*t
    posterCamera.position.set(sx+(tx-sx)*e,sy+(ty-sy)*e,sz+(tz-sz)*e)
    posterCamera.lookAt(0,0,0)
    if(t<1)requestAnimationFrame(tween)
    else posterViewTweening=false
  }
  tween()
}

function startPosterGesture(){
  const vid=videoEl.value;const cvs=canvasEl.value;if(!vid||!cvs)return
  hands=new Hands({locateFile:f=>'/mediapipe/'+f})
  hands.setOptions({maxNumHands:2,modelComplexity:0,minDetectionConfidence:0.6,minTrackingConfidence:0.4})
  let lastGesture='',gestureCount=0,lastSwitchTime=0
  hands.onResults(results=>{
    const ctx=cvs.getContext('2d');cvs.width=vid.videoWidth;cvs.height=vid.videoHeight;ctx.clearRect(0,0,cvs.width,cvs.height)
    if(results.multiHandLandmarks){for(const lm of results.multiHandLandmarks){if(drawingUtils){drawingUtils.drawConnectors(ctx,lm,HAND_CONNECTIONS,{color:'rgba(0,255,0,0.4)',lineWidth:1});drawingUtils.drawLandmarks(ctx,lm,{color:'rgba(255,0,0,0.6)',lineWidth:1,radius:2})}}}
    gestureLoading.value=false;gestureStatus.value=''
    if(!results.multiHandLandmarks||results.multiHandLandmarks.length===0){gestureDebug.value='No hand';lastGesture='';gestureCount=0;return}
    const g=recognizeGesture(results.multiHandLandmarks,results.multiHandedness)
    gestureDebug.value=g||'?'
    if(g&&g===lastGesture){gestureCount++;if(gestureCount>=2){const now=Date.now();if(now-lastSwitchTime>500){const map={f0:'front',f1:'right',f2:'left',f3:'back',f4:'top',f5:'bottom'};if(map[g]&&currentView.value!==map[g]){setPosterView(map[g]);lastSwitchTime=now}}gestureCount=0}}else{lastGesture=g;gestureCount=1}
  })
  mediapipeCamera=new MediaPipeCamera(vid,{onFrame:async()=>{if(hands&&!posterViewTweening)await hands.send({image:vid})},width:320,height:240})
  mediapipeCamera.start()
}

function recognizeGesture(handsLm,handedness){
  if(!handsLm||handsLm.length===0)return null
  // 双手出现 → bottom (f5)
  if(handsLm.length>=2){
    const t=countFourFingers(handsLm[0])+countFourFingers(handsLm[1])
    if(t>=4)return'f5'
  }
  const lm=handsLm[0]
  const four=countFourFingers(lm)
  // 4根手指全张开 + 拇指也张开 → 5指全张 → bottom (f5)
  if(four===4&&isThumbOpen(lm,handedness?.[0]?.label))return'f5'
  // 否则按4根手指数量映射 f0~f4
  return 'f'+four
}
// 只数4根手指（不含拇指），保持原有映射稳定
function countFourFingers(lm){let c=0;if(lm[8].y<lm[6].y)c++;if(lm[12].y<lm[10].y)c++;if(lm[16].y<lm[14].y)c++;if(lm[20].y<lm[18].y)c++;return c}
// 拇指张开判断，兼容前摄镜像
function isThumbOpen(lm,handLabel){
  const dx=Math.abs(lm[4].x-lm[3].x)
  if(dx<0.03)return false
  if(handLabel==='Left')return lm[4].x<lm[3].x
  if(handLabel==='Right')return lm[4].x>lm[3].x
  return dx>0.04
}

onMounted(async()=>{
  initThree(threeContainer.value)
  try{
    const r=await getVersion(versionId)
    const v=r.data
    versionData.value = v
    currentModelUrl.value=v.modelUrl||''
    if(v.sculptedModelUrl)sculptedModelUrlRef.value=v.sculptedModelUrl
    if(v.faceParams){try{savedFaceParams.value=typeof v.faceParams==='string'?JSON.parse(v.faceParams):v.faceParams}catch(e){}}
    if(currentModelUrl.value)await loadModel(currentModelUrl.value)
    else loadingModel.value=false
    // 获取角色名
    if(v.characterId){try{const cr=await axios.get('/api/character/list',{params:{userId:1}});const ch=cr.data.find(c=>c.id===v.characterId);if(ch)characterName.value=ch.name}catch(e){}}
  }catch(e){console.error(e);loadingModel.value=false;errorMsg.value='Failed to load version data'}
  startVersionCardPolling()
})

onBeforeUnmount(()=>{
  destroyThree();destroyPosterThree();stopNfcPolling();stopVersionCardPolling()
  if(mediapipeCamera){mediapipeCamera.stop();mediapipeCamera=null}
  if(hands){hands.close();hands=null}
  window.removeEventListener('resize',onResize)
})
</script>

<style scoped>
/* ═══════════════════════════════════════════
   BASE & ROOT
   ═══════════════════════════════════════════ */
.model-full-view {
  position: fixed; inset: 0;
  background: #0a0a0a;
  overflow: hidden;
  font-family: 'Inter', 'Helvetica Neue', Arial, sans-serif;
  color: #e8e8e8;
  -webkit-font-smoothing: antialiased;
}

/* ═══════════════════════════════════════════
   GOLDEN LAYOUT — full-screen 3D + overlays
   ═══════════════════════════════════════════ */
.golden-layout {
  position: absolute; inset: 0;
  overflow: hidden;
}

/* ── Golden layout enter animation ── */
.golden-enter .three-fullscreen {
  animation: golden-3d-enter 0.6s cubic-bezier(0.22, 1, 0.36, 1) both;
}
.golden-enter .g-sidebar {
  animation: golden-sidebar-enter 0.55s cubic-bezier(0.22, 1, 0.36, 1) 0.1s both;
}
.golden-enter .g-titles {
  animation: golden-titles-enter 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.15s both;
}
.golden-enter .g-back {
  animation: golden-titles-enter 0.4s cubic-bezier(0.22, 1, 0.36, 1) 0.2s both;
}

@keyframes golden-3d-enter {
  0% { opacity: 0; transform: scale(0.92); }
  100% { opacity: 1; transform: scale(1); }
}
@keyframes golden-sidebar-enter {
  0% { opacity: 0; transform: translateY(60px); }
  100% { opacity: 1; transform: translateY(0); }
}
@keyframes golden-titles-enter {
  0% { opacity: 0; transform: translateY(20px); }
  100% { opacity: 1; transform: translateY(0); }
}
.three-fullscreen {
  position: absolute; top: 0; left: 0; bottom: 0;
  right: 38.2%;
  z-index: 0;
}

/* ── Back button ── */
.g-back {
  position: absolute; top: 16px; left: 16px; z-index: 20;
  background: none; border: none; color: rgba(255,255,255,0.6);
  font: 400 14px/1 'Inter', sans-serif; letter-spacing: 0.08em;
  cursor: pointer; padding: 6px 10px;
  transition: color 0.2s;
}
.g-back:hover { color: #fff; }

/* ── Magazine titles — bottom-left, overlapping model ── */
.g-titles {
  position: absolute; bottom: 80px; left: 28px; z-index: 10;
  pointer-events: none;
  max-width: 55%;
}
.g-date {
  font: 300 11px/1 'Inter', sans-serif;
  letter-spacing: 0.25em; text-transform: uppercase;
  color: rgba(255,255,255,0.35);
  margin-bottom: 8px;
}
.g-brand {
  display: flex; flex-direction: column; gap: 2px;
}
.g-brand-thin {
  font: 300 13px/1 'Inter', sans-serif;
  letter-spacing: 0.5em;
  color: rgba(255,255,255,0.4);
}
.g-brand-bold {
  font: 800 clamp(42px, 10vw, 80px)/0.9 'Georgia', 'Times New Roman', serif;
  color: #fff;
  letter-spacing: -0.02em;
  text-shadow: 0 2px 40px rgba(0,0,0,0.6);
}
.g-issue {
  margin-top: 10px;
  font: 400 10px/1 'Inter', sans-serif;
  letter-spacing: 0.3em;
  color: rgba(255,255,255,0.25);
  border: 1px solid rgba(255,255,255,0.15);
  display: inline-block; padding: 4px 12px;
  border-radius: 2px;
}


/* ── Model tag (Original / Sculpted) ── */
.g-model-tag {
  position: absolute; top: 16px; left: 50%;
  transform: translateX(-50%); z-index: 15;
  font: 400 11px/1 'Inter', sans-serif;
  letter-spacing: 0.15em; color: rgba(255,255,255,0.35);
  display: flex; gap: 8px; align-items: center;
}
.g-model-tag span { cursor: pointer; transition: color 0.2s; padding: 4px 0; }
.g-model-tag span.active { color: #fff; }
.g-model-tag span:hover { color: rgba(255,255,255,0.7); }
.g-tag-sep { color: rgba(255,255,255,0.15); cursor: default; }

/* ═══════════════════════════════════════════
   SIDEBAR — magazine editorial column
   ═══════════════════════════════════════════ */
.g-sidebar {
  position: absolute; top: 0; right: 0; bottom: 0;
  width: 38.2%;
  z-index: 10;
  padding: 32px 28px 80px 28px;
  overflow-y: auto;
  overflow-x: hidden;
  background: #f6f4ef;
  border-left: 1px solid #e0e0e0;
  scrollbar-width: none;
  color: #171717;
}
.g-sidebar::-webkit-scrollbar { display: none; }

/* ── Corner marks ── */
.gs-corner {
  position: absolute;
  font: 300 16px/1 'Georgia', serif;
  color: rgba(0,0,0,0.12);
  pointer-events: none;
  z-index: 1;
}
.gs-corner-tl { top: 12px; left: 12px; }
.gs-corner-tr { top: 12px; right: 12px; }
.gs-corner-bl { bottom: 12px; left: 12px; }
.gs-corner-br { bottom: 12px; right: 12px; }

/* ── Masthead ── */
.gs-masthead {
  display: flex; justify-content: space-between; align-items: baseline;
  padding-bottom: 14px;
  margin-bottom: 20px;
}
.gs-masthead-left {
  display: flex; align-items: baseline; gap: 4px;
}
.gs-vol {
  font: 400 9px/1 'Inter', sans-serif;
  letter-spacing: 0.2em;
  color: rgba(0,0,0,0.4);
}
.gs-no {
  font: 700 28px/1 'Georgia', serif;
  color: #111;
  letter-spacing: -0.02em;
}
.gs-masthead-right {
  display: flex; flex-direction: column; align-items: flex-end; gap: 1px;
}
.gs-folio {
  font: 700 9px/1 'Inter', sans-serif;
  letter-spacing: 0.35em;
  color: rgba(0,0,0,0.5);
}
.gs-folio-sub {
  font: 300 8px/1 'Inter', sans-serif;
  letter-spacing: 0.25em;
  color: rgba(0,0,0,0.3);
}

/* ── Rules ── */
.gs-rule {
  height: 1px;
  background: rgba(0,0,0,0.1);
  margin: 16px 0;
}
.gs-rule-double {
  height: 0;
  border-top: 2px solid rgba(0,0,0,0.25);
  border-bottom: 1px solid rgba(0,0,0,0.1);
  padding-top: 3px;
  background: none;
}
.gs-rule-thick {
  height: 2px;
  background: rgba(0,0,0,0.2);
}

/* ── Character name ── */
.gs-character-name {
  font: 700 clamp(22px, 4vw, 32px)/1.1 'Georgia', serif;
  color: #111;
  letter-spacing: -0.01em;
  margin-bottom: 4px;
  word-break: break-word;
}
.gs-character-name-placeholder {
  color: rgba(0,0,0,0.25);
  font-style: italic;
}

/* ── Lead block with dropcap ── */
.gs-lead-block {
  display: flex; gap: 2px;
  margin-bottom: 4px;
}
.gs-dropcap {
  font: 700 42px/0.85 'Georgia', serif;
  color: #111;
  float: left;
  margin-right: 4px;
  margin-top: 2px;
}
.gs-lead {
  font: 300 12.5px/1.75 'Georgia', serif;
  color: rgba(0,0,0,0.5);
  margin: 0;
  letter-spacing: 0.01em;
}

/* ── Prompt blocks ── */
.gs-prompt-block {
  margin-bottom: 8px;
}
.gs-prompt-label {
  font: 700 8px/1 'Inter', sans-serif;
  letter-spacing: 0.3em;
  color: rgba(0,0,0,0.35);
  margin-bottom: 6px;
}
.gs-prompt-text {
  font: 300 12px/1.7 'Georgia', serif;
  color: rgba(0,0,0,0.45);
  margin: 0;
  letter-spacing: 0.01em;
  font-style: italic;
}

/* ── Stats inline ── */
.gs-stats-inline {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 4px;
}
.gs-stat-chip {
  font: 400 9px/1 'Inter', sans-serif;
  letter-spacing: 0.1em;
  color: rgba(0,0,0,0.35);
  padding: 4px 10px;
  border: 1px solid rgba(0,0,0,0.08);
  border-radius: 12px;
}
.gs-stat-chip em {
  font-style: normal;
  font-weight: 700;
  color: #111;
  margin-right: 3px;
}

/* ── Section mark ── */
.gs-section-mark {
  font: 700 9px/1 'Inter', sans-serif;
  letter-spacing: 0.35em;
  color: rgba(0,0,0,0.4);
  margin-bottom: 12px;
  padding-bottom: 6px;
  border-bottom: 1px solid rgba(0,0,0,0.08);
}

/* ── Moon Arc — 月相弧形视角切换 ── */
.gs-moon-arc {
  margin: 8px 0 4px;
  width: 100%;
}
.moon-svg {
  width: 100%;
  height: auto;
  display: block;
  touch-action: none;
  user-select: none;
}
.moon-arc-hit {
  cursor: ew-resize;
}
.moon-arc-active {
  opacity: 0.9;
}
.moon-label {
  font: 400 9px/1 'Inter', sans-serif;
  fill: rgba(0,0,0,0.42);
  letter-spacing: 0.05em;
  transition: fill 0.2s;
}
.moon-dot {
  fill: rgba(0,0,0,0.28);
  transition: fill 0.25s, r 0.25s, opacity 0.25s;
}
.moon-dot-active {
  fill: #111;
  filter: drop-shadow(0 0 4px rgba(0,0,0,0.12));
  transition: fill 0.25s, r 0.25s;
}
.moon-point.active .moon-label {
  fill: #111;
}
.moon-slider-ring {
  fill: rgba(255,255,255,0.86);
  stroke: rgba(0,0,0,0.24);
  stroke-width: 1;
  filter: drop-shadow(0 2px 6px rgba(0,0,0,0.12));
}
.moon-slider-core {
  fill: #111;
}
.moon-arc-hint {
  margin-top: 4px;
  text-align: center;
  font: 500 8px/1 'Inter', sans-serif;
  letter-spacing: 0.24em;
  color: rgba(0,0,0,0.28);
}

/* ── Chapters (features) ── */
.gs-chapters { margin-top: 4px; }
.gs-chapter {
  display: flex; align-items: flex-start; gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid rgba(0,0,0,0.06);
  cursor: pointer;
  transition: background 0.15s;
}
.gs-chapter:hover {
  background: rgba(0,0,0,0.04);
}
.gs-chapter.disabled { opacity: 0.3; pointer-events: none; }
.gs-ch-num {
  font: 700 13px/1 'Georgia', serif;
  color: rgba(0,0,0,0.35);
  min-width: 24px;
  padding-top: 2px;
}
.gs-ch-body {
  display: flex; flex-direction: column; gap: 3px;
}
.gs-ch-title {
  font: 500 13px/1.2 'Inter', sans-serif;
  color: #111;
  letter-spacing: 0.02em;
}
.gs-ch-desc {
  font: 300 10px/1.3 'Georgia', serif;
  color: rgba(0,0,0,0.35);
  letter-spacing: 0.02em;
  font-style: italic;
}

/* ── Footer ── */
.gs-footer {
  display: flex; justify-content: space-between; align-items: center;
  margin-top: 28px; padding-top: 14px;
  border-top: 1px solid rgba(0,0,0,0.08);
  font: 300 8px/1 'Inter', sans-serif;
  letter-spacing: 0.2em;
  color: rgba(0,0,0,0.25);
}
.gs-footer-left, .gs-footer-right {
  font: 400 8px/1 'Inter', sans-serif;
  letter-spacing: 0.2em;
}
.gs-footer-center {
  font: 300 7px/1 'Georgia', serif;
  letter-spacing: 0.15em;
  color: rgba(0,0,0,0.18);
}

/* ═══════════════════════════════════════════
   SCULPT OVERLAY
   ═══════════════════════════════════════════ */
.sculpt-overlay {
  position: fixed; inset: 0; z-index: 100;
  background: #0a0a0a;
}
.sculpt-exit-btn {
  position: fixed; top: 14px; left: 14px; z-index: 10001;
  width: 32px; height: 32px;
  display: flex; align-items: center; justify-content: center;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 50%;
  color: rgba(255, 255, 255, 0.7);
  font: 400 14px/1 'Inter', sans-serif;
  padding: 0; cursor: pointer;
  backdrop-filter: blur(10px);
  transition: all 0.25s ease;
}
.sculpt-exit-btn:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.16);
  border-color: rgba(255, 255, 255, 0.4);
  transform: scale(1.1);
}

/* ═══════════════════════════════════════════
   POSTER MODE (gesture fullscreen)
   ═══════════════════════════════════════════ */
.poster-mode {
  position: fixed; inset: 0; z-index: 90;
  background: #0a0a0a; overflow: hidden;
  transition: none;
}

/* ── Poster exit animation: shrink into left 3D area ── */
.poster-exit {
  animation: poster-shrink-exit 0.6s cubic-bezier(0.4, 0, 0.2, 1) forwards;
  pointer-events: none;
}

@keyframes poster-shrink-exit {
  0% {
    opacity: 1;
    transform: scale(1);
    border-radius: 0;
  }
  60% {
    opacity: 0.8;
    transform: scale(0.55) translate(-28%, 0);
    border-radius: 18px;
  }
  100% {
    opacity: 0;
    transform: scale(0.45) translate(-35%, 0);
    border-radius: 24px;
  }
}
.poster-three { position: absolute; inset: 0; }
.poster-texts {
  position: absolute;
  z-index: 2;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  pointer-events: none;
  transition: top 0.6s cubic-bezier(0.22,1,0.36,1), left 0.6s cubic-bezier(0.22,1,0.36,1), right 0.6s cubic-bezier(0.22,1,0.36,1), bottom 0.6s cubic-bezier(0.22,1,0.36,1), transform 0.6s cubic-bezier(0.22,1,0.36,1), text-align 0.6s cubic-bezier(0.22,1,0.36,1);
}
.poster-texts-front {
  bottom: 120px;
  left: 50%;
  transform: translateX(-50%);
  align-items: center;
  text-align: center;
}
.poster-texts-back,
.poster-texts-top,
.poster-texts-bottom {
  top: 50%;
  left: 50%;
  transform: translate(-50%, -52%);
  align-items: center;
  text-align: center;
}
.poster-texts-left {
  top: 56px;
  left: 56px;
  transform: none;
  align-items: flex-start;
  text-align: left;
}
.poster-texts-right {
  bottom: 120px;
  right: 56px;
  transform: none;
  align-items: flex-end;
  text-align: right;
}
.poster-big-title {
  font: 800 clamp(60px, 15vw, 140px)/0.85 'Georgia', serif;
  color: #fff; letter-spacing: -0.03em;
  text-shadow: 0 4px 60px rgba(0,0,0,0.7);
}
.poster-subtitle {
  font: 300 11px/1 'Inter', sans-serif;
  letter-spacing: 0.4em; color: rgba(255,255,255,0.3);
  margin-top: 12px;
}
.poster-view-label {
  font: 400 10px/1 'Inter', sans-serif;
  letter-spacing: 0.2em; color: rgba(255,255,255,0.25);
  margin-top: 8px;
}
.poster-date {
  font: 300 10px/1 'Inter', sans-serif;
  letter-spacing: 0.2em; color: rgba(255,255,255,0.2);
  margin-top: 4px;
}
.poster-deco-tl,
.poster-deco-tr,
.poster-deco-bl,
.poster-deco-br {
  position: absolute;
  width: 56px;
  height: 56px;
  z-index: 3;
  pointer-events: none;
  opacity: 0.72;
}
.poster-deco-tl {
  top: 20px; left: 20px;
  border-top: 2px solid rgba(255,255,255,0.28);
  border-left: 2px solid rgba(255,255,255,0.28);
}
.poster-deco-tr {
  top: 20px; right: 20px;
  border-top: 2px solid rgba(255,255,255,0.28);
  border-right: 2px solid rgba(255,255,255,0.28);
}
.poster-deco-bl {
  bottom: 20px; left: 20px;
  border-bottom: 2px solid rgba(255,255,255,0.22);
  border-left: 2px solid rgba(255,255,255,0.22);
}
.poster-deco-br {
  bottom: 20px; right: 20px;
  border-bottom: 2px solid rgba(255,255,255,0.22);
  border-right: 2px solid rgba(255,255,255,0.22);
}
.poster-rule-top,
.poster-rule-bottom {
  position: absolute;
  left: 72px;
  right: 72px;
  height: 2px;
  z-index: 3;
  pointer-events: none;
}
.poster-rule-top {
  top: 48px;
  background: linear-gradient(90deg, rgba(255,255,255,0), rgba(255,255,255,0.36), rgba(255,255,255,0));
}
.poster-rule-bottom {
  bottom: 104px;
  background: linear-gradient(90deg, rgba(255,255,255,0), rgba(255,255,255,0.18), rgba(255,255,255,0));
}
.poster-top-tools {
  position: absolute;
  top: 32px;
  right: 56px;
  z-index: 6;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}
.poster-issue-tag {
  font: 500 10px/1 'Inter', sans-serif;
  letter-spacing: 0.38em;
  color: rgba(255,255,255,0.46);
  text-transform: uppercase;
  margin-bottom: 6px;
}
.poster-tool {
  appearance: none;
  border: none;
  background: transparent;
  color: rgba(255,255,255,0.86);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 2px 0;
  min-width: 180px;
  text-transform: uppercase;
}
.poster-tool-line {
  width: 2px;
  height: 26px;
  background: rgba(255,255,255,0.34);
  transform-origin: center;
  transition: transform 0.28s cubic-bezier(0.22,1,0.36,1), width 0.28s cubic-bezier(0.22,1,0.36,1), height 0.28s cubic-bezier(0.22,1,0.36,1), background 0.2s;
}
.poster-tool-text {
  font: 500 11px/1 'Inter', sans-serif;
  letter-spacing: 0.26em;
  color: rgba(255,255,255,0.68);
  transition: color 0.2s, transform 0.2s;
}
.poster-tool:hover .poster-tool-line,
.poster-tool:focus-visible .poster-tool-line {
  width: 28px;
  height: 2px;
  background: rgba(255,255,255,0.82);
}
.poster-tool:hover .poster-tool-text,
.poster-tool:focus-visible .poster-tool-text {
  color: #fff;
  transform: translateX(-2px);
}
.poster-share-btn:disabled {
  opacity: 0.45;
  cursor: wait;
}
.poster-share-btn:disabled .poster-tool-line {
  width: 18px;
  height: 2px;
  background: rgba(255,255,255,0.36);
}
.poster-exit-btn-inline .poster-tool-text {
  color: rgba(255,255,255,0.5);
}

/* Camera preview — bottom-left like fig4 */
.camera-preview {
  position: fixed; bottom: 20px; left: 20px; z-index: 96;
  width: 200px; height: 150px; border-radius: 8px;
  overflow: hidden; border: 1px solid rgba(255,255,255,0.1);
  background: #000;
}
.camera-preview video, .camera-preview canvas {
  position: absolute; inset: 0; width: 100%; height: 100%;
  object-fit: cover;
}
.camera-preview canvas { z-index: 1; }

/* Gesture hints — bottom-right like fig4 */
.gesture-hint-poster {
  position: fixed; bottom: 20px; right: 20px; z-index: 96;
  display: flex; flex-direction: column; gap: 4px;
  background: rgba(0,0,0,0.6);
  padding: 12px 16px;
  border-radius: 10px;
  border: 1px solid rgba(255,255,255,0.08);
}
.hint-item {
  font: 400 10px/1.4 'Inter', sans-serif;
  color: rgba(255,255,255,0.35);
  letter-spacing: 0.05em;
}
.hint-debug {
  font: 400 10px/1 monospace;
  color: rgba(255,200,0,0.5); margin-top: 4px;
}

/* ═══════════════════════════════════════════
   POSTCARD MODE — 3D flip card
   ═══════════════════════════════════════════ */
.postcard-overlay {
  position: absolute;
  inset: 0;
  z-index: 120;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(10,10,10,0.78);
  backdrop-filter: blur(12px);
  perspective: 1400px;
}
.postcard-transition-enter-active,
.postcard-transition-leave-active {
  transition: opacity 0.35s ease;
}
.postcard-transition-enter-from,
.postcard-transition-leave-to {
  opacity: 0;
}

/* ── Entrance + flipper container ── */
.postcard-entrance {
  animation: postcard-enter 0.6s cubic-bezier(0.22,1,0.36,1) both;
}
.postcard-flipper {
  width: min(86vw, 680px);
  aspect-ratio: 1.5 / 1;
  position: relative;
  transform-style: preserve-3d;
  -webkit-transform-style: preserve-3d;
  transition: transform 0.7s cubic-bezier(0.4, 0.0, 0.2, 1);
  cursor: pointer;
  will-change: transform;
}
.postcard-flipper.flipped {
  transform: rotateY(180deg);
}
@keyframes postcard-enter {
  0% { opacity: 0; transform: scale(0.88) translateY(30px); }
  100% { opacity: 1; transform: scale(1) translateY(0); }
}

/* ── Shared face styles ── */
.postcard-face {
  position: absolute;
  inset: 0;
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
  border-radius: 14px;
  overflow: hidden;
  box-shadow:
    0 20px 60px rgba(0,0,0,0.28),
    0 0 0 1px rgba(255,255,255,0.9),
    0 0 26px rgba(255,255,255,0.75),
    0 0 54px rgba(255,255,255,0.45);
}

/* ── FRONT: poster screenshot ── */
.postcard-front {
  background: #0a0a0a;
  cursor: pointer;
  touch-action: manipulation;
}
.postcard-front-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
  background: #0a0a0a;
  filter: saturate(0.92) contrast(1.02);
  pointer-events: none;
  user-select: none;
  -webkit-user-drag: none;
}
.postcard-front-hint {
  position: absolute;
  bottom: 18px;
  left: 50%;
  transform: translateX(-50%);
  font: 500 10px/1 'Inter', sans-serif;
  letter-spacing: 0.35em;
  color: rgba(255,255,255,0.45);
  background: rgba(0,0,0,0.5);
  padding: 6px 18px;
  border-radius: 20px;
  backdrop-filter: blur(4px);
  pointer-events: none;
  animation: hint-pulse 2.4s ease-in-out infinite;
}
@keyframes hint-pulse {
  0%, 100% { opacity: 0.45; }
  50% { opacity: 0.85; }
}

/* ── BACK: postal side ── */
.postcard-back {
  transform: rotateY(180deg);
  background:
    radial-gradient(circle at 50% 50%, rgba(255,255,255,0.98), rgba(246,246,246,0.98)),
    linear-gradient(175deg, #ffffff 0%, #f6f6f6 100%);
  color: #222;
  cursor: pointer;
  touch-action: manipulation;
  padding: 28px 30px;
  display: block;
}

/* ── Back: action buttons ── */
.postcard-back-actions {
  position: absolute;
  top: 16px;
  right: 16px;
  display: flex;
  gap: 10px;
  z-index: 5;
}
.pc-action-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 1.5px solid rgba(80,60,40,0.25);
  background: rgba(255,255,255,0.6);
  color: rgba(60,45,30,0.75);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  backdrop-filter: blur(4px);
}
.pc-action-btn svg {
  width: 18px;
  height: 18px;
}
.pc-action-btn:hover {
  background: rgba(255,255,255,0.9);
  border-color: rgba(80,60,40,0.45);
  color: rgba(40,28,16,0.95);
  transform: scale(1.08);
}
.pc-send-btn:disabled {
  opacity: 0.4;
  cursor: wait;
}

/* ── Back: stamp ── */
.pc-stamp {
  position: absolute;
  left: 34px;
  bottom: 78px;
  width: 86px;
  height: 86px;
  opacity: 0;
  transform: scale(1.5) rotate(-18deg);
  pointer-events: none;
}
.stamp-circle {
  width: 100%;
  height: 100%;
  border: 2px double rgba(70,70,70,0.7);
  border-radius: 50%;
  color: rgba(40,40,40,0.85);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  text-align: center;
  background: rgba(255,255,255,0.5);
}
.stamp-text-top,
.stamp-text-bottom {
  font: 700 6px/1.1 'Inter', sans-serif;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}
.stamp-date {
  font: 700 12px/1 'Georgia', serif;
  letter-spacing: 0.03em;
}
.stamp-animate {
  animation: stamp-hit 0.78s cubic-bezier(0.2,0.9,0.24,1) forwards;
}
@keyframes stamp-hit {
  0% { opacity: 0; transform: scale(1.8) rotate(-22deg); filter: blur(3px); }
  55% { opacity: 1; transform: scale(0.88) rotate(-12deg); filter: blur(0); }
  75% { transform: scale(1.06) rotate(-10deg); }
  100% { opacity: 1; transform: scale(1) rotate(-11deg); }
}

.pc-postage-box {
  position: absolute;
  top: 34px;
  right: 34px;
  width: 56px;
  height: 56px;
  border: 1.5px solid rgba(50,50,50,0.45);
}

.pc-center-divider {
  position: absolute;
  top: 116px;
  bottom: 72px;
  left: 50%;
  width: 1px;
  background: rgba(40,40,40,0.18);
}

.pc-code-zone {
  position: absolute;
  display: flex;
  gap: 6px;
}
.pc-code-zone-left {
  left: 34px;
  top: 132px;
}
.pc-code-zone-right {
  right: 34px;
  bottom: 84px;
}
.pc-code-box {
  width: 18px;
  height: 18px;
  border: 1px solid rgba(50,50,50,0.42);
  display: inline-block;
}

.pc-address-write {
  position: absolute;
  top: 138px;
  right: 46px;
  width: 34%;
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.pc-address-line {
  height: 1px;
  background: rgba(40,40,40,0.28);
}
.pc-address-line.long {
  width: 100%;
}

.pc-brand-mark {
  position: absolute;
  left: 34px;
  bottom: 26px;
  color: rgba(20,20,20,0.92);
}
.pc-brand-cn {
  font: 700 16px/1 'STKaiti', 'KaiTi', 'Microsoft YaHei', serif;
  letter-spacing: 0.04em;
}
.pc-brand-en {
  margin-top: 4px;
  font: 600 7px/1 'Inter', sans-serif;
  letter-spacing: 0.18em;
}

.pc-creation-note {
  position: absolute;
  left: 34px;
  top: 182px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  color: rgba(40,40,40,0.68);
}
.pc-note-row {
  display: flex;
  gap: 10px;
  align-items: baseline;
}
.pc-note-row span {
  font: 500 8px/1 'Inter', sans-serif;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}
.pc-note-row strong {
  font: 500 11px/1 'Georgia', serif;
  color: rgba(24,24,24,0.86);
  font-weight: 500;
}

/* ── Back: flip hint ── */
.pc-flip-back-hint {
  position: absolute;
  bottom: 14px;
  right: 24px;
  font: 500 9px/1 'Inter', sans-serif;
  letter-spacing: 0.2em;
  color: rgba(80,80,80,0.42);
  cursor: pointer;
  transition: color 0.2s;
}
.pc-flip-back-hint:hover {
  color: rgba(50,50,50,0.75);
}

/* ═══════════════════════════════════════════
   NFC CARD POPUP
   ═══════════════════════════════════════════ */
.nfc-card-popup {
  position: fixed; inset: 0; z-index: 200;
  display: flex; align-items: center; justify-content: center;
  background: rgba(0,0,0,0.6);
  cursor: pointer;
}
.nfc-card-inner {
  position: relative; width: 200px; padding: 32px 24px;
  background: rgba(20,20,20,0.95);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 12px; text-align: center;
}
.nfc-card-glow {
  position: absolute; inset: -2px; border-radius: 14px;
  background: conic-gradient(from 0deg, #ff6b6b, #ffd93d, #6bcb77, #4d96ff, #ff6b6b);
  opacity: 0.3; filter: blur(8px); z-index: -1;
  animation: nfc-glow-spin 3s linear infinite;
}
@keyframes nfc-glow-spin { to { transform: rotate(360deg); } }
.nfc-card-icon { font-size: 40px; margin-bottom: 12px; }
.nfc-card-title {
  font: 600 16px/1.3 'Inter', sans-serif;
  color: #fff; margin-bottom: 8px;
}
.nfc-card-hint {
  font: 300 10px/1 'Inter', sans-serif;
  color: rgba(255,255,255,0.3); letter-spacing: 0.15em;
}
.nfc-pop-enter-active { animation: nfc-pop-in 0.4s cubic-bezier(0.34,1.56,0.64,1); }
.nfc-pop-leave-active { animation: nfc-pop-in 0.25s ease-in reverse; }
@keyframes nfc-pop-in {
  0% { opacity: 0; transform: scale(0.5) translateY(40px); }
  100% { opacity: 1; transform: scale(1) translateY(0); }
}

/* ═══════════════════════════════════════════
   TOASTS & LOADING
   ═══════════════════════════════════════════ */
.save-toast, .print-toast, .error-toast {
  position: fixed; top: 20px; left: 50%; transform: translateX(-50%);
  z-index: 300; padding: 10px 24px; border-radius: 6px;
  font: 500 13px/1.4 'Inter', sans-serif;
  cursor: pointer; animation: toast-in 0.3s ease-out;
}
.save-toast {
  background: rgba(16,185,129,0.9); color: #fff;
}
.save-toast.error { background: rgba(239,68,68,0.9); }
.save-toast.warn { background: rgba(245,158,11,0.9); }
.print-toast {
  background: rgba(59,130,246,0.9); color: #fff; top: 60px;
}
.print-toast.success { background: rgba(16,185,129,0.9); }
.error-toast {
  background: rgba(239,68,68,0.9); color: #fff; top: 100px;
}
@keyframes toast-in {
  0% { opacity: 0; transform: translateX(-50%) translateY(-10px); }
  100% { opacity: 1; transform: translateX(-50%) translateY(0); }
}

.loading-overlay {
  position: fixed; inset: 0; z-index: 250;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  background: rgba(10,10,10,0.85);
  gap: 16px;
  font: 300 13px/1 'Inter', sans-serif;
  color: rgba(255,255,255,0.5);
  letter-spacing: 0.1em;
}
.spinner {
  width: 32px; height: 32px;
  border: 2px solid rgba(255,255,255,0.1);
  border-top-color: rgba(255,255,255,0.6);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ═══════════════════════════════════════════
   EXIT ANIMATION
   ═══════════════════════════════════════════ */
.exit-animating {
  animation: page-exit 0.65s cubic-bezier(0.4,0,0.2,1) forwards;
}
@keyframes page-exit {
  0% { opacity: 1; transform: scale(1); }
  100% { opacity: 0; transform: scale(0.95); }
}

/* ═══════════════════════════════════════════
   POSTER ANIMATIONS
   ═══════════════════════════════════════════ */
.poster-big-title, .poster-subtitle, .poster-view-label, .poster-date {
  animation: poster-text-in 0.6s cubic-bezier(0.16,1,0.3,1) both;
}
.poster-subtitle { animation-delay: 0.08s; }
.poster-view-label { animation-delay: 0.14s; }
.poster-date { animation-delay: 0.2s; }
@keyframes poster-text-in {
  0% { opacity: 0; transform: translateY(20px); }
  100% { opacity: 1; transform: translateY(0); }
}

/* ═══════════════════════════════════════════
   MOBILE RESPONSIVE
   ═══════════════════════════════════════════ */
@media (max-width: 768px) {
  .postcard-flipper {
    width: min(92vw, 520px);
  }
  .postcard-back {
    padding: 22px 20px;
  }
  .pc-postage-box {
    top: 22px;
    right: 22px;
    width: 46px;
    height: 46px;
  }
  .pc-center-divider {
    top: 92px;
    bottom: 58px;
  }
  .pc-code-zone-left {
    left: 22px;
    top: 106px;
  }
  .pc-code-zone-right {
    right: 22px;
    bottom: 66px;
  }
  .pc-code-box {
    width: 14px;
    height: 14px;
    gap: 4px;
  }
  .pc-address-write {
    top: 112px;
    right: 28px;
    width: 36%;
    gap: 14px;
  }
  .pc-creation-note {
    left: 22px;
    top: 148px;
  }
  .pc-stamp {
    left: 22px;
    bottom: 62px;
    width: 72px;
    height: 72px;
  }
  .pc-brand-mark {
    left: 22px;
    bottom: 20px;
  }
  .three-fullscreen {
    right: 0;
  }
  .g-sidebar {
    width: 100%;
    top: auto; bottom: 0; left: 0; right: 0;
    height: auto; max-height: 50vh;
    padding: 20px 20px 72px;
    border-left: none;
    border-top: 1px solid #e0e0e0;
    border-radius: 16px 16px 0 0;
    background: #f6f4ef;
  }
  .g-titles {
    bottom: auto; top: 60px; left: 20px;
    max-width: 60%;
  }
  .g-brand-bold {
    font-size: clamp(32px, 12vw, 52px);
  }
  .g-model-tag {
    top: 16px; left: 16px; transform: none;
    font-size: 10px;
  }
  .gs-stat em { font-size: 16px; }
  .gs-lead { font-size: 12px; }
  .camera-preview { width: 140px; height: 105px; bottom: 12px; left: 12px; }
  .poster-top-tools {
    top: 24px;
    right: 28px;
    gap: 8px;
  }
  .poster-tool {
    min-width: 132px;
  }
  .poster-tool-line {
    height: 20px;
  }
  .poster-rule-top,
  .poster-rule-bottom {
    left: 40px;
    right: 40px;
  }
  .poster-texts-front,
  .poster-texts-back,
  .poster-texts-top,
  .poster-texts-bottom {
    width: calc(100% - 80px);
  }
  .poster-texts-left {
    top: 44px;
    left: 24px;
    max-width: 58%;
  }
  .poster-texts-right {
    top: 44px;
    right: 24px;
    max-width: 58%;
  }
}

@media (max-width: 480px) {
  .postcard-flipper {
    width: calc(100vw - 24px);
  }
  .postcard-back {
    padding: 16px 12px;
  }
  .pc-postage-box {
    top: 18px;
    right: 18px;
    width: 38px;
    height: 38px;
  }
  .pc-center-divider {
    top: 82px;
    bottom: 48px;
  }
  .pc-code-zone-left {
    left: 14px;
    top: 94px;
    gap: 4px;
  }
  .pc-code-zone-right {
    right: 14px;
    bottom: 56px;
    gap: 4px;
  }
  .pc-code-box {
    width: 11px;
    height: 11px;
  }
  .pc-address-write {
    top: 98px;
    right: 18px;
    width: 38%;
    gap: 10px;
  }
  .pc-creation-note {
    left: 14px;
    top: 128px;
    gap: 8px;
  }
  .pc-note-row span {
    font-size: 7px;
  }
  .pc-note-row strong {
    font-size: 9px;
  }
  .pc-stamp {
    left: 14px;
    bottom: 48px;
    width: 60px;
    height: 60px;
  }
  .pc-brand-mark {
    left: 14px;
    bottom: 16px;
  }
  .pc-brand-cn {
    font-size: 12px;
  }
  .pc-brand-en {
    font-size: 6px;
  }
  .g-sidebar {
    max-height: 45vh;
    padding: 16px 16px 68px;
  }
  .g-titles { top: 50px; left: 16px; }
  .g-brand-bold { font-size: clamp(28px, 14vw, 42px); }
  .gs-masthead { margin-bottom: 12px; }
  .gs-no { font-size: 18px; }
  .gs-idx-row { padding: 8px 0; }
  .gs-act { padding: 10px 0; font-size: 12px; }
  .poster-top-tools {
    top: 18px;
    right: 18px;
  }
  .poster-tool {
    min-width: auto;
    gap: 8px;
  }
  .poster-tool-text {
    font-size: 10px;
    letter-spacing: 0.2em;
  }
  .poster-texts-front,
  .poster-texts-back,
  .poster-texts-top,
  .poster-texts-bottom {
    width: calc(100% - 40px);
  }
  .poster-texts-left {
    top: 56px;
    left: 16px;
    max-width: 64%;
  }
  .poster-texts-right {
    top: 56px;
    right: 16px;
    max-width: 64%;
  }
  .poster-deco-tl,
  .poster-deco-tr,
  .poster-deco-bl,
  .poster-deco-br {
    width: 40px;
    height: 40px;
  }
}
</style>