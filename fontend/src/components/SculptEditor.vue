
<template>
  <div class="sculpt-root" ref="editorRoot">
    <div class="top-bar">
      <div class="top-bar-brand">
        <span class="tb-vol">SCULPT</span>
        <span class="tb-sub">EDITOR</span>
        <span v-if="autoBindStatus" class="tb-status" :class="{ error: autoBindStatus.includes('❌') }">{{ autoBindStatus }}</span>
      </div>
      <div class="top-bar-actions">
        <button class="ch-btn" @click="handleSave" :disabled="saving">
          <span class="ch-no">§1</span>
          <div class="ch-copy"><span class="ch-title">{{ saving ? 'Saving…' : (saveSuccess ? 'Saved' : 'Save') }}</span><span class="ch-desc">Store current sculpt</span></div>
        </button>
        <button class="ch-btn" @click="enterCalibration">
          <span class="ch-no">§2</span>
          <div class="ch-copy"><span class="ch-title">Calibrate</span><span class="ch-desc">Mark face anchors</span></div>
        </button>
        <button class="ch-btn" @click="resetAllParams">
          <span class="ch-no">§3</span>
          <div class="ch-copy"><span class="ch-title">Reset</span><span class="ch-desc">Neutral face</span></div>
        </button>
        <button class="ch-btn" @click="handleSaveToNfc" :disabled="nfcSaving">
          <span class="ch-no">§4</span>
          <div class="ch-copy"><span class="ch-title">{{ nfcSaving ? 'Writing…' : 'Save to NFC' }}</span><span class="ch-desc">Write params to card</span></div>
        </button>
        <button class="ch-btn" @click="handleBack">
          <span class="ch-no">§5</span>
          <div class="ch-copy"><span class="ch-title">Exit</span><span class="ch-desc">Back to viewer</span></div>
        </button>
      </div>
    </div>

    <div class="bottom-stage">
      <transition name="fold-left">
        <div v-if="leftPanelOpen && !calibrationMode" class="fold-page fold-left">
          <div class="fold-page-scroll">
            <div class="fp-head">
              <div class="fp-kicker">§ PRESETS</div>
              <div class="fp-title">Presets</div>
              <div class="fp-desc">Expressions &amp; face templates</div>
            </div>
            <div class="fp-section">
              <div class="fp-label">EXPRESSIONS</div>
              <div class="fp-preset-list">
                <button v-for="preset in expressionPresets" :key="preset.code" class="fp-preset" :class="{ active: activePresetCode === preset.code }" @click="applyPreset(preset)">
                  <div class="fp-preset-img"><img :src="preset.image" :alt="preset.name" /></div>
                  <div class="fp-preset-info"><span class="fp-preset-code">{{ preset.code }}</span><span class="fp-preset-name">{{ preset.name }}</span></div>
                </button>
              </div>
            </div>
            <div class="fp-section">
              <div class="fp-label">FACE TYPES</div>
              <div class="fp-preset-list">
                <button v-for="preset in facePresetList" :key="preset.code" class="fp-preset" :class="{ active: activePresetCode === preset.code }" @click="applyPreset(preset)">
                  <div class="fp-preset-img"><img :src="preset.image" :alt="preset.name" /></div>
                  <div class="fp-preset-info"><span class="fp-preset-code">{{ preset.code }}</span><span class="fp-preset-name">{{ preset.name }}</span></div>
                </button>
              </div>
            </div>
            <div class="fp-footer">
              <button class="fp-mini" @click="randomize">Random</button>
              <button class="fp-mini" @click="resetAllParams(false)">Neutral</button>
            </div>
          </div>
        </div>
      </transition>

      <div class="model-frame" ref="modelFrame">
        <button v-if="!calibrationMode" class="fold-handle fold-handle-l" @click="togglePanel('left')">
          <span class="fh-arrow">{{ leftPanelOpen ? '‹' : '›' }}</span>
          <span class="fh-text">PRESETS</span>
        </button>

        <div class="spec-vertical">F<br/>A<br/>C<br/>E<br/><br/>D<br/>E<br/>T<br/>A<br/>I<br/>L</div>

        <div class="mf-corner mf-tl">┌</div>
        <div class="mf-corner mf-tr">┐</div>
        <div class="mf-corner mf-bl">└</div>
        <div class="mf-corner mf-br">┘</div>
        <div class="mf-meta">
          <div class="mf-date">NFC · FACE EDITION</div>
          <div class="mf-big">CHARACTER.</div>
        </div>
        <div ref="threeContainer" class="three-box"></div>

        <!-- NFC 笔呼吸点 -->
        <svg v-if="!calibrationMode && breathingDots.length > 0" class="breathing-dot-svg">
          <defs>
            <filter id="breathing-glow">
              <feGaussianBlur stdDeviation="3" result="blur" />
              <feMerge><feMergeNode in="blur" /><feMergeNode in="SourceGraphic" /></feMerge>
            </filter>
          </defs>
          <g v-for="(dot, idx) in breathingDots" :key="'bd-' + idx">
            <circle :cx="dot.x" :cy="dot.y" r="18"
              fill="none" stroke="rgba(0,120,255,0.35)" stroke-width="1.5"
              filter="url(#breathing-glow)" class="breathing-ring-outer" />
            <circle :cx="dot.x" :cy="dot.y" r="10"
              fill="none" stroke="rgba(0,120,255,0.55)" stroke-width="1"
              class="breathing-ring-inner" />
            <circle :cx="dot.x" :cy="dot.y" r="5"
              fill="#0078FF" filter="url(#breathing-glow)"
              class="breathing-core" />
          </g>
        </svg>

        <svg v-if="!calibrationMode && calloutLines.length > 0" class="callout-svg">
          <defs>
            <filter id="callout-glow">
              <feGaussianBlur stdDeviation="2" result="blur" />
              <feMerge><feMergeNode in="blur" /><feMergeNode in="SourceGraphic" /></feMerge>
            </filter>
          </defs>
          <g v-for="(line, idx) in calloutLines" :key="'cl-' + idx">
            <line :x1="line.x1" :y1="line.y1" :x2="line.x2" :y2="line.y2"
              stroke="#002FA7"
              stroke-width="1"
              filter="url(#callout-glow)"
              stroke-dasharray="6 4" />
            <circle :cx="line.x1" :cy="line.y1" r="6"
              fill="#002FA7" filter="url(#callout-glow)" class="callout-dot-pulse" />
            <circle :cx="line.x1" :cy="line.y1" r="12"
              fill="none" stroke="rgba(0,47,167,0.5)" stroke-width="1"
              class="callout-ring-pulse" />
          </g>
        </svg>

        <div class="intro-guide" v-if="showIntroGuide && !calibrationMode">
          <div class="intro-ring"></div>
          <div class="intro-card">
            <div class="intro-kicker">FACE TRACKING</div>
            <div class="intro-headline">{{ introStepTitle }}</div>
            <div class="intro-body">{{ introStepText }}</div>
          </div>
        </div>
        <div v-if="loading" class="mf-loading">
          <div class="mf-spinner"></div>
          <span>{{ loadingText }}</span>
        </div>
      </div>

      <transition name="fold-right">
      <div v-if="!calibrationMode && rightPanelOpen" class="fold-page fold-right" :class="{ 'nfc-sensing': nfcAnimPhase >= 1 }">
        <div class="fold-crease"></div>
        <!-- 物理插槽缝隙 — 折页左边缘 -->
        <div class="nfc-slot-slit" :class="{ active: nfcAnimPhase >= 1 }">
          <span class="nfc-slot-arrow">▸</span>
        </div>
        <!-- NFC 卡片 — 从折页左边缘外侧插入，大部分藏在折页下方 -->
        <transition name="nfc-card-fly">
          <div v-if="nfcAnimPhase >= 2" class="nfc-card-bookmark" :class="[`nfc-phase-${nfcAnimPhase}`]">
            <div class="nfc-card-body">
              <img v-if="nfcCardAnimImage" :src="nfcCardAnimImage" :alt="nfcCardAnimName" class="nfc-card-img" />
              <div v-else class="nfc-card-blank"></div>
            </div>
          </div>
        </transition>
        <div class="fold-page-scroll">
          <div class="fp-watermark" aria-hidden="true">{{ activeParamDisplay }}</div>
          <div class="fp-head">
            <div class="fp-head-top">
              <div>
                <div class="fp-kicker">§ PARAMETERS</div>
                <div class="fp-title">Params</div>
                <div class="fp-desc">Facial structure controls</div>
              </div>
              <button class="fp-close" @click="togglePanel('right')" title="Close panel">✕</button>
            </div>
          </div>
          <transition name="value-fade">
          <div class="fp-value-display" :key="selectedParam || 'none'" :class="{ 'nfc-value-hidden': nfcAnimPhase >= 1 }">
            <div class="fp-value-label">{{ selectedParamDef ? selectedParamDef.name.toUpperCase() : 'PARAMETERS' }}</div>
            <div class="fp-value-num" :class="valueClass(activeParamNumber)">{{ activeParamDisplay }}</div>
          </div>
          </transition>
          <div class="fp-cat-tabs" :class="{ 'nfc-value-pushed': nfcAnimPhase >= 1 }">
            <button v-for="(cat, idx) in categories" :key="cat.id" class="fp-cat" :class="{ active: activeCategory === cat.id }" @click="activeCategory = cat.id">
              <span class="fp-cat-idx">0{{ idx + 1 }}</span>
              <span class="fp-cat-name">{{ cat.name }}</span>
            </button>
          </div>
          <div class="fp-callouts" :class="{ 'nfc-value-pushed': nfcAnimPhase >= 1 }">
            <div v-for="param in filteredParams" :key="param.id"
              class="fp-callout-row"
              :class="{ active: selectedParam === param.id, modified: Math.abs(param.value) > 0.05 }"
              @click="toggleSelectParam(param.id)"
              @mouseenter="hoveredParam = param.id"
              @mouseleave="hoveredParam = null">
              <div class="fp-callout-main">
                <div class="fp-callout-left">
                  <span class="fp-callout-dot" :class="valueClass(param.value)"></span>
                  <span class="fp-callout-name">{{ param.name }}</span>
                </div>
                <div class="fp-callout-right">
                  <span v-if="selectedParam === param.id" class="fp-callout-val" :class="valueClass(param.value)">{{ param.value > 0 ? '+' : '' }}{{ param.value.toFixed(2) }}</span>
                  <span v-else class="fp-callout-shape">{{ getShapeLabel(param) }}</span>
                  <button v-if="selectedParam === param.id" class="fp-callout-reset" @click.stop="resetParam(param)" :disabled="param.value === 0" title="Reset">↺</button>
                </div>
              </div>
              <div v-if="selectedParam === param.id" class="fp-slider-wrap">
                <input type="range" :min="param.min" :max="param.max" :step="0.1" :value="param.value"
                  class="fp-slider"
                  @input="onParamInput(param, $event)" />
              </div>
            </div>
          </div>
          <!-- 页脚 — 杂志页脚 -->
          <div class="fp-footer-mag">
            <span class="fp-footer-left">NFC × AI</span>
            <span class="fp-footer-center">— FACE EDITION —</span>
            <span class="fp-footer-right">SCULPT</span>
          </div>
        </div>
      </div>
      </transition>

      <button v-if="!calibrationMode && !rightPanelOpen" class="fold-handle fold-handle-r" @click="togglePanel('right')">
        <span class="fh-arrow">‹</span>
        <span class="fh-text">PARAMS</span>
      </button>
    </div>

    <div v-if="calibrationMode" class="calib-overlay">
      <!-- Scanning effect on enter -->
      <div class="calib-scanline" :class="{ active: calibScanActive }"></div>
      <div class="calib-crosshair" :class="{ active: !calibScanActive }">
        <div class="ch-h"></div><div class="ch-v"></div>
      </div>

      <!-- Top bar: compact -->
      <div class="calib-bar">
        <div class="calib-brand">
          <span class="calib-vol">CALIBRATION</span>
          <span class="calib-step">{{ currentAnchorIdx + (anchorSteps[currentAnchorIdx]?.position ? 1 : 0) }} / {{ anchorSteps.length }}</span>
        </div>
        <div class="calib-dots">
          <div v-for="(step, idx) in anchorSteps" :key="step.id" class="calib-dot" :class="{ done: step.position !== null, current: idx === currentAnchorIdx, pending: idx > currentAnchorIdx && step.position === null }">{{ step.icon }}</div>
        </div>
        <div class="calib-progress-bg"><div class="calib-progress-fill" :style="{ width: calibrationProgress + '%' }"></div></div>
      </div>

      <!-- Floating PiP prompt card (left or right based on step) -->
      <transition name="pip-slide">
      <div class="calib-pip" :class="'pip-' + calibPromptSide" v-if="currentAnchorIdx < anchorSteps.length && !allAnchorsPlaced" :key="currentAnchorIdx">
        <div class="pip-accent" :style="{ background: '#' + anchorSteps[currentAnchorIdx].color.toString(16).padStart(6, '0') }"></div>
        <div class="pip-body">
          <div class="pip-step">STEP {{ currentAnchorIdx + 1 }}/{{ anchorSteps.length }}</div>
          <div class="pip-icon">{{ anchorSteps[currentAnchorIdx].icon }}</div>
          <div class="pip-title">{{ anchorSteps[currentAnchorIdx].name }}</div>
          <div class="pip-hint">{{ anchorSteps[currentAnchorIdx].hint }}</div>
          <div class="pip-sub">Click on model to mark</div>
        </div>
      </div>
      </transition>

      <!-- Completion card (center, subtle) -->
      <transition name="pip-slide">
      <div class="calib-complete-card" v-if="allAnchorsPlaced">
        <div class="cc-icon">✓</div>
        <div class="cc-title">All {{ anchorSteps.length }} anchors marked</div>
        <div class="cc-hint">Press Apply to bind skeleton</div>
      </div>
      </transition>

      <!-- Bottom actions -->
      <div class="calib-actions">
        <button class="calib-btn" @click="exitCalibration">✕ Cancel</button>
        <button class="calib-btn" @click="undoLastAnchor" :disabled="currentAnchorIdx === 0 && !anchorSteps[0].position">↩ Undo</button>
        <button class="calib-btn" @click="resetAnchors" :disabled="currentAnchorIdx === 0 && !anchorSteps[0].position">↺ Reset</button>
        <button class="calib-btn calib-confirm" @click="confirmAnchors" :disabled="!allAnchorsPlaced">Apply</button>
      </div>

    </div>

    <!-- NFC 写入底部提示 -->
    <Transition name="nfc-toast-slide">
      <div v-if="nfcToastMsg" class="nfc-write-toast" :class="nfcToastType" @click="nfcToastMsg = ''">
        <span class="nfc-toast-icon">{{ nfcToastIcon }}</span>
        <span class="nfc-toast-text">{{ nfcToastMsg }}</span>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls'
import { OBJLoader } from 'three/examples/jsm/loaders/OBJLoader'
import { OBJExporter } from 'three/examples/jsm/exporters/OBJExporter'
import { FaceRigSystem, FACE_PARAMS_DEF, EXPRESSION_PRESETS, FACE_PRESETS, ANCHOR_DEFS, PRESETS } from './faceRigSystem'
import { saveFaceCard, getNfcWriteStatus } from '../api/api.js'

const props = defineProps({
  modelUrl: { type: String, default: '' },
  modelText: { type: String, default: '' },
  expressions: { type: Array, default: () => [] },
  initialFaceParams: { type: Object, default: null }
})
const emit = defineEmits(['update:expressions', 'anchorsChanged', 'save', 'reset', 'back'])

const editorRoot = ref(null)
const threeContainer = ref(null)
const modelFrame = ref(null)
const loading = ref(true)
const loadingText = ref('Loading model...')
const autoBindStatus = ref('')
const saving = ref(false)
const saveSuccess = ref(false)
const nfcSaving = ref(false)
const nfcToastMsg = ref('')
const nfcToastType = ref('')
const nfcToastIcon = ref('')
const showBones = ref(false)
const wireframeMode = ref(false)
const leftPanelOpen = ref(false)
const rightPanelOpen = ref(true)
const activeCategory = ref('face')
const activePresetCode = ref('')
const selectedParam = ref(null)
const showIntroGuide = ref(false)
const introStepTitle = ref('Locating face')
const introStepText = ref('Camera zooming to face area.')
const calibrationMode = ref(false)
const hasCalibrated = ref(false)
const currentAnchorIdx = ref(0)
const anchorSteps = reactive(ANCHOR_DEFS.map(a => ({ ...a, position: null })))
const allAnchorsPlaced = computed(() => anchorSteps.every(s => s.position !== null))
const calibrationProgress = computed(() => (anchorSteps.filter(s => s.position !== null).length / anchorSteps.length) * 100)
const calibPromptSide = computed(() => 'left')
const calibScanActive = ref(false)
const hoveredParam = ref(null)
const calloutLines = ref([])

// NFC 卡片插入动画状态
const nfcAnimPhase = ref(0) // 0=idle, 1=sensing(参数下移), 2=arrive(大卡滑入并停顿), 3=insert(卡槽出现并插入)
const nfcCardAnimName = ref('Face Preset')
const nfcCardAnimImage = ref('') // 预设卡图片路径，空字符串=白卡
let nfcAnimTimer = null

// NFC 笔呼吸点状态
const breathingDots = ref([]) // [{x, y, readerIndex}]
let breathingDotTimers = [] // 每个呼吸点的自动消失定时器

// NFC 读卡器索引 → anchorSteps 索引映射（与校准时标记的7个锚点重合）
// anchorSteps: 0=forehead, 1=eyeLeft, 2=eyeRight, 3=noseTip, 4=mouthCenter, 5=cheekLeft, 6=cheekRight
const READER_TO_ANCHOR = [
  6,  // reader 0 → 右脸颊 → anchorSteps[6] cheekRight
  4,  // reader 1 → 下巴   → anchorSteps[4] mouthCenter（最近的锚点）
  5,  // reader 2 → 左脸颊 → anchorSteps[5] cheekLeft
  1,  // reader 3 → 左眼   → anchorSteps[1] eyeLeft
  0,  // reader 4 → 额头   → anchorSteps[0] forehead
  2,  // reader 5 → 右眼   → anchorSteps[2] eyeRight
]

let anchorMarkers = []
const raycaster = new THREE.Raycaster()
const mouse = new THREE.Vector2()
const faceParams = reactive(FACE_PARAMS_DEF.map(p => ({ ...p, value: 0 })))
const categories = [
  { id: 'face', name: 'Face' },
  { id: 'eyes', name: 'Eyes' },
  { id: 'nose', name: 'Nose' },
  { id: 'mouth', name: 'Chin' },
]
const filteredParams = computed(() => faceParams.filter(p => p.category === activeCategory.value))
const expressionPresets = EXPRESSION_PRESETS
const facePresetList = FACE_PRESETS

const selectedParamDef = computed(() => selectedParam.value ? faceParams.find(fp => fp.id === selectedParam.value) : null)
const activeParamNumber = computed(() => selectedParamDef.value?.value ?? 0)
const activeParamDisplay = computed(() => {
  const p = selectedParamDef.value
  return p ? `${p.value > 0 ? '+' : ''}${p.value.toFixed(2)}` : '—'
})
function showNfcToast(msg, type = 'info', icon = '📡') {
  nfcToastMsg.value = msg
  nfcToastType.value = type
  nfcToastIcon.value = icon
}
function clearNfcToast(delay = 3000) {
  setTimeout(() => { nfcToastMsg.value = '' }, delay)
}

// 7级形态描述: [very-neg, neg, slight-neg, zero, slight-pos, pos, very-pos]
const SHAPE_LABELS = {
  faceWidth:       ['Very Narrow','Narrow','Sl. Narrow','Normal','Sl. Wide','Wide','Very Wide'],
  jawWidth:        ['Very Slim','Slim','Sl. Slim','Normal','Sl. Sharp','Sharp','Very Sharp'],
  chinLength:      ['Very Short','Short','Sl. Short','Normal','Sl. Long','Long','Very Long'],
  cheekBone:       ['Very Flat','Flat','Sl. Flat','Normal','Sl. Defined','Defined','Very Defined'],
  eyeSize:         ['Very Small','Small','Sl. Small','Normal','Sl. Large','Large','Very Large'],
  eyeDistance:     ['Very Close','Close','Sl. Close','Normal','Sl. Apart','Apart','Very Apart'],
  noseHeight:      ['Very Flat','Flat','Sl. Flat','Normal','Sl. High','High','Very High'],
  noseWidth:       ['Very Thin','Thin','Sl. Thin','Normal','Sl. Wide','Wide','Very Wide'],
  mouthWidth:      ['Very Narrow','Narrow','Sl. Narrow','Normal','Sl. Wide','Wide','Very Wide'],
  lipThickness:    ['Very Thin','Thin','Sl. Thin','Normal','Sl. Full','Full','Very Full'],
  foreheadHeight:  ['Very Low','Low','Sl. Low','Normal','Sl. High','High','Very High'],
  foreheadDepth:   ['Very Flat','Flat','Sl. Flat','Normal','Sl. Round','Round','Very Round'],
  eyeSizeLeft:     ['Very Small','Small','Sl. Small','Normal','Sl. Large','Large','Very Large'],
  eyeSizeRight:    ['Very Small','Small','Sl. Small','Normal','Sl. Large','Large','Very Large'],
  cheekLeft:       ['Very Flat','Flat','Sl. Flat','Normal','Sl. Defined','Defined','Very Defined'],
  cheekRight:      ['Very Flat','Flat','Sl. Flat','Normal','Sl. Defined','Defined','Very Defined'],
}

function getShapeLabel(param) {
  const labels = SHAPE_LABELS[param.id]
  if (!labels) return 'Normal'
  const v = param.value
  if (v <= -0.7) return labels[0]
  if (v <= -0.35) return labels[1]
  if (v <= -0.05) return labels[2]
  if (v < 0.05) return labels[3]
  if (v < 0.35) return labels[4]
  if (v < 0.7) return labels[5]
  return labels[6]
}

const PARAM_VISUAL_BONE_MAP = {
  faceWidth: 'face_center',
  jawWidth: 'chin',
  chinLength: 'chin',
  cheekBone: 'cheek_right',
  eyeSize: 'eye_left',
  eyeDistance: 'nose_bridge',
  noseHeight: 'nose_bridge',
  noseWidth: 'nose_tip',
  mouthWidth: 'mouth_center',
  lipThickness: 'lip_upper',
  foreheadHeight: 'forehead_center',
  foreheadDepth: 'forehead_center',
  eyeSizeLeft: 'eye_left',
  eyeSizeRight: 'eye_right',
  cheekLeft: 'cheek_left',
  cheekRight: 'cheek_right',
}

let scene, camera, renderer, controls, currentModel = null, animationId = null, resizeObserver = null
let modelFaceY = 0.18 // 动态计算的面部 Y 坐标，用于相机聚焦
const rigSystem = new FaceRigSystem()

function togglePanel(side) {
  if (side === 'left') leftPanelOpen.value = !leftPanelOpen.value
  if (side === 'right') rightPanelOpen.value = !rightPanelOpen.value
}

function valueClass(val) {
  if (val > 0.05) return 'positive'
  if (val < -0.05) return 'negative'
  return 'zero'
}

function initThree() {
  const el = threeContainer.value
  if (!el) return
  const w = el.clientWidth || 800, h = el.clientHeight || 600
  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x090909)
  camera = new THREE.PerspectiveCamera(40, w / h, 0.01, 1000)
  camera.position.set(0, 0.22, 3.4)
  const isTablet = /iPad|Android|Tablet/i.test(navigator.userAgent) || (navigator.maxTouchPoints > 1 && /Macintosh/i.test(navigator.userAgent))
  try {
    renderer = new THREE.WebGLRenderer({ antialias: !isTablet, alpha: false, powerPreference: 'default', failIfMajorPerformanceCaveat: false })
  } catch (e) { loading.value = false; autoBindStatus.value = '❌ WebGL unavailable'; return }
  renderer.setSize(w, h)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, isTablet ? 1.5 : 2))
  renderer.toneMapping = THREE.ACESFilmicToneMapping
  renderer.toneMappingExposure = 1.18
  el.appendChild(renderer.domElement)
  renderer.domElement.addEventListener('webglcontextlost', (e) => { e.preventDefault(); if (animationId) { cancelAnimationFrame(animationId); animationId = null }; autoBindStatus.value = '⚠️ GPU lost' })
  renderer.domElement.addEventListener('webglcontextrestored', () => { autoBindStatus.value = '✅ GPU restored'; animate() })
  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true; controls.dampingFactor = 0.08; controls.enablePan = true; controls.target.set(0, 0.18, 0)
  scene.add(new THREE.AmbientLight(0xffffff, 0.62))
  scene.add(new THREE.HemisphereLight(0xffffff, 0x111111, 0.88))
  const key = new THREE.DirectionalLight(0xffffff, 1.35); key.position.set(3, 5, 5); scene.add(key)
  const fill = new THREE.DirectionalLight(0x8aa4ff, 0.42); fill.position.set(-4, 1, -3); scene.add(fill)
  const rim = new THREE.DirectionalLight(0xffffff, 0.32); rim.position.set(0, 4, -5); scene.add(rim)
  const grid = new THREE.GridHelper(8, 18, 0x202020, 0x131313); grid.position.y = -1.25; grid.material.transparent = true; grid.material.opacity = 0.55; scene.add(grid)
  resizeObserver = new ResizeObserver(() => {
    if (!camera || !renderer || !el) return
    const nw = el.clientWidth, nh = el.clientHeight
    if (nw > 0 && nh > 0) { camera.aspect = nw / nh; camera.updateProjectionMatrix(); renderer.setSize(nw, nh) }
  })
  resizeObserver.observe(el)
  animate()
}

function animate() {
    if (!renderer || !scene || !camera) return
    animationId = requestAnimationFrame(animate)
    if (controls) controls.update()
    if (showBones.value) rigSystem.updateBoneHelpers()
    renderer.render(scene, camera)
    if (!calibrationMode.value) {
      updateCalloutLines()
      updateBreathingDotPositions()
    }
  }

function boneToScreen(boneId) {
  const bone = rigSystem.boneMap[boneId]
  if (!bone || !camera || !renderer) return null
  const wp = new THREE.Vector3()
  bone.getWorldPosition(wp)
  return worldToScreen(wp)
}

// 将世界坐标投影到屏幕坐标（相对于 modelFrame）
function worldToScreen(worldPos) {
  if (!camera || !renderer) return null
  const projected = worldPos.clone().project(camera)
  const el = threeContainer.value
  if (!el) return null
  const rect = el.getBoundingClientRect()
  const frameRect = modelFrame.value?.getBoundingClientRect()
  if (!frameRect) return null
  const x = ((projected.x + 1) / 2) * rect.width + (rect.left - frameRect.left)
  const y = ((-projected.y + 1) / 2) * rect.height + (rect.top - frameRect.top)
  if (projected.z > 1) return null
  return { x, y }
}

function toggleSelectParam(paramId) {
  selectedParam.value = selectedParam.value === paramId ? null : paramId
}

function updateCalloutLines() {
  if (!modelFrame.value || calibrationMode.value || !selectedParamDef.value) { calloutLines.value = []; return }
  const frameRect = modelFrame.value.getBoundingClientRect()
  const boneId = PARAM_VISUAL_BONE_MAP[selectedParamDef.value.id]
  if (!boneId) { calloutLines.value = []; return }
  const screenPos = boneToScreen(boneId)
  if (!screenPos) { calloutLines.value = []; return }
  const targetX = frameRect.width
  const targetY = Math.max(40, Math.min(frameRect.height - 40, screenPos.y))
  calloutLines.value = [{
    x1: screenPos.x, y1: screenPos.y,
    x2: targetX, y2: targetY,
    active: true,
    paramId: selectedParamDef.value.id,
  }]
}

function runIntroSequence() {
  if (!camera || !controls || !currentModel) return
  showIntroGuide.value = true
  introStepTitle.value = 'Locating face'
  introStepText.value = 'Camera moving to face position.'
  const fromPos = camera.position.clone(), fromTarget = controls.target.clone()
  const toPos = new THREE.Vector3(0, modelFaceY + 0.04, 1.45), toTarget = new THREE.Vector3(0, modelFaceY, 0)
  const duration = 1800, start = performance.now()
  const tick = (now) => {
    const t = Math.min((now - start) / duration, 1)
    const e = t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2
    camera.position.lerpVectors(fromPos, toPos, e)
    controls.target.lerpVectors(fromTarget, toTarget, e)
    if (t < 1) { requestAnimationFrame(tick); return }
    introStepTitle.value = 'Ready'
    introStepText.value = 'Open fold pages to edit.'
    setTimeout(() => { showIntroGuide.value = false }, 1200)
  }
  requestAnimationFrame(tick)
}

function enterCalibration() {
  if (!renderer || !controls) return
  calibrationMode.value = true; showIntroGuide.value = false; leftPanelOpen.value = false; rightPanelOpen.value = false
  calibScanActive.value = true
  resetAnchors()
  // Scan animation: 1.2s scan, then fade out
  setTimeout(() => { calibScanActive.value = false }, 1400)
  renderer.domElement.addEventListener('click', onCalibrationClick)
  controls.mouseButtons = { LEFT: null, MIDDLE: THREE.MOUSE.DOLLY, RIGHT: THREE.MOUSE.ROTATE }
  controls.enablePan = false
}

function exitCalibration() {
  calibrationMode.value = false
  calibScanActive.value = false
  rightPanelOpen.value = true
  if (renderer?.domElement) renderer.domElement.removeEventListener('click', onCalibrationClick)
  clearAnchorMarkers()
  if (controls) {
    controls.mouseButtons = { LEFT: THREE.MOUSE.ROTATE, MIDDLE: THREE.MOUSE.DOLLY, RIGHT: THREE.MOUSE.PAN }
    controls.enablePan = true
  }
}

function onCalibrationClick(event) {
  if (!calibrationMode.value || !currentModel || !renderer) return
  if (allAnchorsPlaced.value || currentAnchorIdx.value >= anchorSteps.length) return
  const rect = renderer.domElement.getBoundingClientRect()
  mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
  mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1
  raycaster.setFromCamera(mouse, camera)
  const meshes = []; currentModel.traverse(child => { if (child.isMesh) meshes.push(child) })
  const intersects = raycaster.intersectObjects(meshes, false)
  if (intersects.length === 0) return
  const point = intersects[0].point.clone()
  const step = anchorSteps[currentAnchorIdx.value]
  step.position = point; addAnchorMarker(point, step.color)
  emit('anchorsChanged', { id: step.id, position: { x: point.x, y: point.y, z: point.z } })
  if (currentAnchorIdx.value < anchorSteps.length - 1) currentAnchorIdx.value++
}

function undoLastAnchor() {
  if (anchorSteps[currentAnchorIdx.value]?.position !== null) { anchorSteps[currentAnchorIdx.value].position = null; removeLastMarkerPair(); return }
  if (currentAnchorIdx.value > 0) { currentAnchorIdx.value--; anchorSteps[currentAnchorIdx.value].position = null; removeLastMarkerPair() }
}

function removeLastMarkerPair() {
  for (let i = 0; i < 2 && anchorMarkers.length > 0; i++) { const m = anchorMarkers.pop(); scene?.remove(m); m.geometry?.dispose?.(); m.material?.dispose?.() }
}

function jumpToStep(idx) {
  if (idx <= currentAnchorIdx.value || anchorSteps[idx].position !== null) {
    for (let i = anchorSteps.length - 1; i >= idx; i--) { if (anchorSteps[i].position !== null) { anchorSteps[i].position = null; removeLastMarkerPair() } }
    currentAnchorIdx.value = idx
  }
}

function addAnchorMarker(position, color) {
  const r = 0.02
  const geo = new THREE.SphereGeometry(r, 12, 12)
  const mat = new THREE.MeshBasicMaterial({ color, transparent: true, opacity: 0.9, depthTest: false })
  const sphere = new THREE.Mesh(geo, mat); sphere.position.copy(position); sphere.renderOrder = 1000; scene?.add(sphere); anchorMarkers.push(sphere)
  const ringGeo = new THREE.RingGeometry(r * 1.5, r * 2.2, 24)
  const ringMat = new THREE.MeshBasicMaterial({ color, transparent: true, opacity: 0.5, side: THREE.DoubleSide, depthTest: false })
  const ring = new THREE.Mesh(ringGeo, ringMat); ring.position.copy(position); ring.lookAt(camera.position); ring.renderOrder = 1000; scene?.add(ring); anchorMarkers.push(ring)
}

function clearAnchorMarkers() { for (const m of anchorMarkers) { scene?.remove(m); m.geometry?.dispose?.(); m.material?.dispose?.() }; anchorMarkers = [] }
function resetAnchors() { currentAnchorIdx.value = 0; anchorSteps.forEach(s => { s.position = null }); clearAnchorMarkers() }

function confirmAnchors() {
  if (!allAnchorsPlaced.value || !currentModel) return
  const anchors = {}; for (const step of anchorSteps) anchors[step.id] = step.position.clone()
  loading.value = true; loadingText.value = 'Rebinding...'; autoBindStatus.value = '⏳ Rebinding'
  if (showBones.value) rigSystem.removeBoneHelpers(scene)
  setTimeout(() => {
    try {
      rigSystem.autoBindWithAnchors(currentModel, new THREE.Box3().setFromObject(currentModel), new THREE.Box3().setFromObject(currentModel).getSize(new THREE.Vector3()), anchors)
      rigSystem.buildShapeKeys(); loading.value = false; autoBindStatus.value = '✅ Bound'; hasCalibrated.value = true; exitCalibration(); applyDrivers()
      if (showBones.value) rigSystem.createBoneHelpers(scene)
      // Smooth transition: camera eases to edit position
      smoothCameraToEdit()
      setTimeout(() => { autoBindStatus.value = '' }, 3000)
    } catch (err) { loading.value = false; autoBindStatus.value = '❌ Failed' }
  }, 120)
}

function smoothCameraToEdit() {
  if (!camera || !controls) return
  const fromPos = camera.position.clone(), fromTarget = controls.target.clone()
  const toPos = new THREE.Vector3(0, modelFaceY + 0.04, 1.45), toTarget = new THREE.Vector3(0, modelFaceY, 0)
  const duration = 800, start = performance.now()
  const tick = (now) => {
    const t = Math.min((now - start) / duration, 1)
    const e = t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2
    camera.position.lerpVectors(fromPos, toPos, e)
    controls.target.lerpVectors(fromTarget, toTarget, e)
    if (t < 1) requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
}

function loadModel(url) {
  if (!url) return; loading.value = true; loadingText.value = 'Loading model...'; autoBindStatus.value = ''
  const loader = new OBJLoader()
  if (props.modelText) { onModelLoaded(loader.parse(props.modelText)); return }
  loader.load(url, (obj) => { onModelLoaded(obj) }, undefined, () => { loading.value = false; autoBindStatus.value = '❌ Load failed' })
}

function onModelLoaded(object) {
  if (currentModel) { rigSystem.destroy(); scene?.remove(currentModel) }
  object.traverse((child) => { if (child.isMesh) { child.material = new THREE.MeshStandardMaterial({ color: 0xcecbc4, roughness: 0.58, metalness: 0.05, side: THREE.DoubleSide }); child.castShadow = false; child.receiveShadow = false } })
  const box = new THREE.Box3().setFromObject(object); const center = box.getCenter(new THREE.Vector3()); object.position.sub(center)
  const size = box.getSize(new THREE.Vector3()).length(); object.scale.setScalar(2.18 / size); object.position.y = -0.25
  scene?.add(object); currentModel = object; object.updateMatrixWorld(true)
  const bbox = new THREE.Box3().setFromObject(object); const mSize = bbox.getSize(new THREE.Vector3())
  // 将 controls.target 设置到模型上部（面部大致区域），确保缩放以面部为中心
  modelFaceY = bbox.min.y + mSize.y * 0.82
  if (controls) { controls.target.set(0, modelFaceY, 0); controls.update() }
  if (camera) camera.position.set(0, modelFaceY + 0.04, 3.4)
  loadingText.value = 'Binding skeleton...'; autoBindStatus.value = '⏳ Binding'
  setTimeout(() => {
    try {
      rigSystem.autoBind(object, bbox, mSize); rigSystem.buildShapeKeys(); loading.value = false; autoBindStatus.value = '✅ Bound'; applyDrivers(); runIntroSequence()
      if (!hasCalibrated.value) setTimeout(() => { if (!calibrationMode.value) enterCalibration() }, 3400)
      setTimeout(() => { autoBindStatus.value = '' }, 5000)
    } catch (err) { loading.value = false; autoBindStatus.value = '❌ Binding failed' }
  }, 120)
}

function getParamValues() { const v = {}; faceParams.forEach(p => { v[p.id] = p.value }); return v }
function setParamValues(values) {
  if (!values || typeof values !== 'object') return
  faceParams.forEach(p => { if (values[p.id] !== undefined) p.value = values[p.id] })
  applyDrivers()
}
function applyNfcParamValues(values, cardName = 'NFC Card') {
  if (!values || typeof values !== 'object') return
  // 匹配预设：遍历所有预设，找到参数值完全匹配的预设以获取其图片
  const matchedPreset = findMatchingPreset(values)
  const image = matchedPreset ? matchedPreset.image : ''
  const name = matchedPreset ? matchedPreset.name : (cardName || 'NFC Card')
  triggerNfcCardAnimation(name, image)
  faceParams.forEach(p => { if (values[p.id] !== undefined) p.value = values[p.id] })
  applyDrivers()
}

// 根据参数值匹配预设，返回匹配的预设对象或 null
function findMatchingPreset(values) {
  if (!values) return null
  for (const preset of PRESETS) {
    if (!preset.values) continue
    let match = true
    for (const key of Object.keys(preset.values)) {
      const pv = preset.values[key]
      const nv = values[key]
      if (nv === undefined || Math.abs(pv - nv) > 0.05) { match = false; break }
    }
    if (match) return preset
  }
  return null
}

// NFC 卡片插入动画：缝隙亮起 → 飞入数值区停顿 → 插入折页
function triggerNfcCardAnimation(cardName, cardImage = '') {
  clearNfcCardAnimation()
  nfcCardAnimName.value = cardName || 'Face Preset'
  nfcCardAnimImage.value = cardImage || ''

  // 第一步：插槽缝隙亮起
  nfcAnimPhase.value = 1
  nfcAnimTimer = setTimeout(() => {
    // 第二步：卡片从右侧快速飞入数值区域，停住让用户看清
    nfcAnimPhase.value = 2
    nfcAnimTimer = setTimeout(() => {
      // 第三步：卡片继续向左插入折页，只露出书签角
      nfcAnimPhase.value = 3
      playClickSound()
      nfcAnimTimer = setTimeout(() => {
        nfcAnimPhase.value = 0
        nfcAnimTimer = null
      }, 2000)
    }, 1200)
  }, 200)
}

function clearNfcCardAnimation() {
  if (nfcAnimTimer) { clearTimeout(nfcAnimTimer); nfcAnimTimer = null }
  nfcAnimPhase.value = 0
}

// NFC 笔呼吸点：在校准锚点位置显示蓝色呼吸闪烁点（与定位时标记的点重合）
function triggerBreathingDot(readerIndex) {
  if (readerIndex < 0 || readerIndex >= READER_TO_ANCHOR.length) return
  const anchorIdx = READER_TO_ANCHOR[readerIndex]
  const anchor = anchorSteps[anchorIdx]
  if (!anchor || !anchor.position) return  // 锚点未标记则不显示
  const screenPos = worldToScreen(anchor.position)
  if (!screenPos) return

  // 移除同一 readerIndex 的旧呼吸点（避免重复叠加）
  breathingDots.value = breathingDots.value.filter(d => d.readerIndex !== readerIndex)
  // 清除对应的旧定时器
  breathingDotTimers = breathingDotTimers.filter(t => {
    if (t.readerIndex === readerIndex) { clearTimeout(t.timer); return false }
    return true
  })

  // 添加新呼吸点
  breathingDots.value.push({ x: screenPos.x, y: screenPos.y, readerIndex })

  // 3秒后自动消失
  const timer = setTimeout(() => {
    breathingDots.value = breathingDots.value.filter(d => d.readerIndex !== readerIndex)
    breathingDotTimers = breathingDotTimers.filter(t => t.readerIndex !== readerIndex)
  }, 3000)
  breathingDotTimers.push({ readerIndex, timer })
}

// 更新呼吸点位置（锚点是固定的3D坐标，但相机可能旋转所以需要重新投影）
function updateBreathingDotPositions() {
  if (breathingDots.value.length === 0) return
  breathingDots.value = breathingDots.value.map(dot => {
    const anchorIdx = READER_TO_ANCHOR[dot.readerIndex]
    const anchor = anchorSteps[anchorIdx]
    if (!anchor || !anchor.position) return dot
    const screenPos = worldToScreen(anchor.position)
    if (!screenPos) return dot
    return { ...dot, x: screenPos.x, y: screenPos.y }
  })
}

function clearBreathingDots() {
  breathingDots.value = []
  breathingDotTimers.forEach(t => clearTimeout(t.timer))
  breathingDotTimers = []
}

// NFC 笔操作：只触发呼吸点 + 应用参数，不播放卡片动画
function applyNfcPenValues(values, readerIndex) {
  if (!values || typeof values !== 'object') return
  // 触发呼吸点
  triggerBreathingDot(readerIndex)
  // 静默应用参数（不触发卡片动画）
  faceParams.forEach(p => { if (values[p.id] !== undefined) p.value = values[p.id] })
  applyDrivers()
}

function playClickSound() {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)()
    // 机械咔嗒声：短促的方波脉冲
    const osc = ctx.createOscillator()
    const gain = ctx.createGain()
    osc.type = 'square'
    osc.frequency.setValueAtTime(800, ctx.currentTime)
    osc.frequency.exponentialRampToValueAtTime(200, ctx.currentTime + 0.04)
    gain.gain.setValueAtTime(0.15, ctx.currentTime)
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.06)
    osc.connect(gain)
    gain.connect(ctx.destination)
    osc.start(ctx.currentTime)
    osc.stop(ctx.currentTime + 0.06)
    // 第二声轻响
    const osc2 = ctx.createOscillator()
    const gain2 = ctx.createGain()
    osc2.type = 'square'
    osc2.frequency.setValueAtTime(1200, ctx.currentTime + 0.07)
    osc2.frequency.exponentialRampToValueAtTime(400, ctx.currentTime + 0.1)
    gain2.gain.setValueAtTime(0.08, ctx.currentTime + 0.07)
    gain2.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.12)
    osc2.connect(gain2)
    gain2.connect(ctx.destination)
    osc2.start(ctx.currentTime + 0.07)
    osc2.stop(ctx.currentTime + 0.12)
    setTimeout(() => ctx.close(), 300)
  } catch (e) { /* 静默失败 */ }
}

function applyDrivers() {
  rigSystem.applyDrivers(getParamValues())
  const v = getParamValues()
  emit('update:expressions', [v.faceWidth, v.eyeSize, v.noseHeight, v.mouthWidth, v.chinLength, v.cheekBone, v.foreheadHeight || 0])
}

async function exportModel() {
  if (!currentModel) return null
  if (animationId) { cancelAnimationFrame(animationId); animationId = null }
  await new Promise(r => setTimeout(r, 50))
  try {
    const exporter = new OBJExporter(); const tempScene = new THREE.Scene()
    let baked = null; try { baked = rigSystem.bakeDeformed() } catch (e) {}
    if (baked && baked.length > 0) {
      for (let i = 0; i < baked.length; i++) {
        const r = baked[i]; const g = new THREE.BufferGeometry()
        g.setAttribute('position', new THREE.BufferAttribute(r.positions, 3))
        if (r.normals) g.setAttribute('normal', new THREE.BufferAttribute(r.normals, 3))
        if (r.uvs) g.setAttribute('uv', new THREE.BufferAttribute(r.uvs, 2))
        if (r.indices) g.setIndex(new THREE.BufferAttribute(r.indices, 1))
        if (!r.normals) g.computeVertexNormals()
        const m = new THREE.Mesh(g, new THREE.MeshBasicMaterial({ color: 0xcccccc })); m.name = r.mesh.name || `mesh_${i}`
        if (r.mesh.parent && r.mesh.parent !== scene) { m.position.copy(r.mesh.position); m.rotation.copy(r.mesh.rotation); m.scale.copy(r.mesh.scale) }
        tempScene.add(m)
      }
    } else {
      const meshes = []; currentModel.traverse(c => { if (c.isMesh && c.geometry) meshes.push(c) })
      for (const mesh of meshes) {
        const geo = mesh.geometry; if (!geo?.attributes?.position) continue
        const g = new THREE.BufferGeometry()
        g.setAttribute('position', new THREE.BufferAttribute(new Float32Array(geo.attributes.position.array), 3))
        if (geo.attributes.normal) g.setAttribute('normal', new THREE.BufferAttribute(new Float32Array(geo.attributes.normal.array), 3))
        if (geo.attributes.uv) g.setAttribute('uv', new THREE.BufferAttribute(new Float32Array(geo.attributes.uv.array), 2))
        if (geo.index) g.setIndex(new THREE.BufferAttribute(new Uint32Array(geo.index.array), 1))
        const m = new THREE.Mesh(g, new THREE.MeshBasicMaterial({ color: 0xcccccc })); m.name = mesh.name
        m.position.copy(mesh.position); m.rotation.copy(mesh.rotation); m.scale.copy(mesh.scale)
        tempScene.add(m)
      }
    }
    await new Promise(r => setTimeout(r, 30))
    const result = exporter.parse(tempScene)
    animate(); return result
  } catch (err) { animate(); return null }
}

async function handleSave() {
  saving.value = true; saveSuccess.value = false
  try { emit('save', { params: getParamValues(), obj: await exportModel() }); saveSuccess.value = true; setTimeout(() => { saveSuccess.value = false }, 2000) }
  catch (e) { /* ignore */ }
  finally { saving.value = false }
}

async function handleSaveToNfc() {
  if (nfcSaving.value) return
  nfcSaving.value = true
  showNfcToast('Saving face params…', 'info', '💾')
  try {
    const res = await saveFaceCard(getParamValues(), null, 'Custom Face')
    if (!res.data || res.data.status === 'error') {
      showNfcToast('❌ ' + (res.data?.message || 'Save failed'), 'error', '❌')
      clearNfcToast(4000)
      return
    }
    const code = res.data?.code || ''
    showNfcToast('Place NFC card near the reader…', 'pending', '📡')
    // Poll write status
    let attempts = 0
    const maxAttempts = 40 // max 20 seconds
    const pollWrite = () => {
      return new Promise((resolve) => {
        const timer = setInterval(async () => {
          attempts++
          try {
            const sr = await getNfcWriteStatus()
            const ws = sr.data?.writeStatus
            if (ws === 'done') {
              clearInterval(timer)
              resolve('done')
            } else if (ws === 'failed') {
              clearInterval(timer)
              resolve('failed')
            } else if (attempts >= maxAttempts) {
              clearInterval(timer)
              resolve('timeout')
            }
          } catch (e) {
            if (attempts >= maxAttempts) {
              clearInterval(timer)
              resolve('timeout')
            }
          }
        }, 500)
      })
    }
    const writeResult = await pollWrite()
    if (writeResult === 'done') {
      showNfcToast('✅ Write success! Card code: ' + code, 'success', '✅')
    } else if (writeResult === 'failed') {
      showNfcToast('❌ Write failed, please retry', 'error', '❌')
    } else {
      showNfcToast('⏱ Timeout, please try again', 'warn', '⏱')
    }
    clearNfcToast(4000)
  } catch (e) {
    console.error('Save to NFC failed:', e)
    const status = e.response?.status
    let msg = 'Save failed'
    if (status === 404) {
      msg = 'NFC service unavailable (404)'
    } else if (status === 500) {
      msg = 'Server error, please check backend'
    } else if (e.code === 'ECONNABORTED' || e.message?.includes('timeout')) {
      msg = 'Request timeout'
    } else if (e.response?.data?.message) {
      msg = e.response.data.message
    } else if (e.message) {
      msg = e.message
    }
    showNfcToast('❌ ' + msg, 'error', '❌')
    clearNfcToast(4000)
  } finally {
    nfcSaving.value = false
  }
}

function handleBack() {
  emit('back')
}

function onParamInput(param, event) { param.value = parseFloat(event.target.value); selectedParam.value = param.id; activePresetCode.value = ''; applyDrivers() }
function resetParam(param) { param.value = 0; selectedParam.value = param.id; applyDrivers() }
function resetAllParams(emitReset = true) { faceParams.forEach(p => { p.value = 0 }); activePresetCode.value = ''; applyDrivers(); if (emitReset) emit('reset') }

function applyPreset(preset) {
  activePresetCode.value = preset.code
  if (preset.values) { faceParams.forEach(p => { if (preset.values[p.id] !== undefined) p.value = preset.values[p.id] }) }
  applyDrivers()
}

function randomize() {
  faceParams.forEach(p => { p.value = Math.round((Math.random() * (p.max - p.min) + p.min) * 10) / 10 })
  activePresetCode.value = ''; applyDrivers()
}

function toggleBoneViz() {
  showBones.value = !showBones.value
  if (showBones.value) rigSystem.createBoneHelpers(scene)
  else rigSystem.removeBoneHelpers(scene)
}

function toggleWireframe() {
  wireframeMode.value = !wireframeMode.value
  if (currentModel) currentModel.traverse(c => { if (c.isMesh && c.material) c.material.wireframe = wireframeMode.value })
}

defineExpose({ getParamValues, setParamValues, applyNfcParamValues, applyNfcPenValues, triggerBreathingDot, exportModel, clearNfcCardAnimation, clearBreathingDots })

onMounted(() => {
  initThree()
  if (props.modelUrl || props.modelText) loadModel(props.modelUrl)
  if (props.initialFaceParams) setParamValues(props.initialFaceParams)
})

watch(() => props.modelUrl, (v) => { if (v) loadModel(v) })
watch(() => props.modelText, (v) => { if (v) { const loader = new OBJLoader(); onModelLoaded(loader.parse(v)) } })
watch(activeCategory, () => {
  selectedParam.value = null
})

onBeforeUnmount(() => {
  if (animationId) { cancelAnimationFrame(animationId); animationId = null }
  if (resizeObserver) resizeObserver.disconnect()
  if (renderer) { renderer.dispose(); renderer.forceContextLoss?.() }
  if (controls) controls.dispose()
  rigSystem.destroy()
  clearAnchorMarkers()
  clearNfcCardAnimation()
  clearBreathingDots()
})
</script>

<style scoped>
.sculpt-root{display:flex;flex-direction:column;width:100%;height:100vh;overflow:hidden;background:#090909;font-family:'Inter','Helvetica Neue',Arial,sans-serif}
.top-bar{display:flex;align-items:center;justify-content:space-between;padding:10px 24px;background:#f6f4ef;border-bottom:1px solid #e0e0e0;flex-shrink:0;z-index:10}
.top-bar-brand{display:flex;align-items:baseline;gap:8px}
.tb-vol{font:900 18px/1 'Georgia',serif;color:#111;letter-spacing:2px;text-transform:uppercase}
.tb-sub{font:300 12px/1 'Inter',sans-serif;color:#888;letter-spacing:3px;text-transform:uppercase}
.tb-status{font:500 11px/1 'Inter',sans-serif;color:#555;margin-left:12px}
.tb-status.error{color:#c0392b}
.top-bar-actions{display:flex;gap:6px}
.ch-btn{display:flex;align-items:flex-start;gap:8px;padding:8px 14px;background:transparent;border:1px solid #ddd;border-radius:0;cursor:pointer;transition:all .2s}
.ch-btn:hover{background:#edeae3;border-color:#111}
.ch-btn:disabled{opacity:.4;cursor:default}
.ch-no{font:700 12px/1 'Georgia',serif;color:#999}
.ch-copy{display:flex;flex-direction:column;gap:2px}
.ch-title{font:500 12px/1.2 'Inter',sans-serif;color:#111}
.ch-desc{font:300 9px/1.3 'Georgia',serif;color:#999;font-style:italic}
.bottom-stage{display:flex;flex:1;overflow:hidden;position:relative;min-height:0}
.fold-page{flex-shrink:0;background:#f6f4ef;overflow:hidden;position:relative;z-index:5;min-height:0}
.fold-left{width:38%;max-width:420px;min-width:260px;border-right:1px solid #e0e0e0;transform-origin:left center}
.fold-right{width:38.2%;max-width:380px;min-width:220px;border-left:none;transform-origin:right center;position:relative;overflow:hidden;min-height:0}
.fold-page-scroll{height:100%;min-height:0;overflow-y:auto;overflow-x:hidden;-webkit-overflow-scrolling:touch;overscroll-behavior:contain;touch-action:pan-y;padding:24px 20px 100px;position:relative;z-index:2;box-sizing:border-box}
.fold-page-scroll::-webkit-scrollbar{width:4px}
.fold-page-scroll::-webkit-scrollbar-track{background:transparent}
.fold-page-scroll::-webkit-scrollbar-thumb{background:#ccc;border-radius:2px}
.fold-crease{position:absolute;left:0;top:0;bottom:0;width:18px;z-index:3;pointer-events:none;background:linear-gradient(to right,rgba(0,0,0,.12) 0%,rgba(0,0,0,.04) 40%,transparent 100%)}
.fold-left-enter-active,.fold-left-leave-active{transition:transform .45s cubic-bezier(.4,0,.2,1),opacity .35s ease}
.fold-left-enter-from{transform:perspective(1200px) rotateY(75deg);opacity:0}
.fold-left-leave-to{transform:perspective(1200px) rotateY(75deg);opacity:0}
.fold-right-enter-active,.fold-right-leave-active{transition:transform .45s cubic-bezier(.4,0,.2,1),opacity .35s ease}
.fold-right-enter-from{transform:perspective(1200px) rotateY(-75deg);opacity:0}
.fold-right-leave-to{transform:perspective(1200px) rotateY(-75deg);opacity:0}
.fp-head{margin-bottom:20px;padding-bottom:14px;border-bottom:1px solid #e8e8e8}
.fp-head-top{display:flex;justify-content:space-between;align-items:flex-start}
.fp-close{width:26px;height:26px;background:#f6f4ef;border:1px solid #ddd;color:#111;font-size:11px;cursor:pointer;display:flex;align-items:center;justify-content:center;transition:all .2s;flex-shrink:0;border-radius:0;margin-top:18px}
.fp-close:hover{border-color:#111;color:#111;background:#edeae3}
.fp-kicker{font:700 10px/1 'Georgia',serif;color:#bbb;letter-spacing:3px;margin-bottom:6px}
.fp-title{font:700 22px/1.1 'Georgia',serif;color:#111}
.fp-desc{font:300 11px/1.3 'Inter',sans-serif;color:#999;margin-top:4px}
.fp-section{margin-bottom:18px}
.fp-label{font:700 9px/1 'Inter',sans-serif;color:#bbb;letter-spacing:2.5px;text-transform:uppercase;margin-bottom:10px}
.fp-preset-list{display:flex;flex-wrap:wrap;gap:8px}
.fp-preset{display:flex;align-items:center;gap:8px;padding:6px 10px;background:#f0ede6;border:1px solid #e8e8e8;cursor:pointer;transition:all .2s;width:calc(50% - 4px);box-sizing:border-box}
.fp-preset:hover{border-color:#111;background:#edeae3}
.fp-preset.active{border-color:#111;background:#111}
.fp-preset.active .fp-preset-name,.fp-preset.active .fp-preset-code{color:#fff}
.fp-preset-img{width:32px;height:32px;border-radius:4px;overflow:hidden;flex-shrink:0;background:#eee}
.fp-preset-img img{width:100%;height:100%;object-fit:cover}
.fp-preset-info{display:flex;flex-direction:column;gap:1px;overflow:hidden}
.fp-preset-code{font:700 9px/1 'Georgia',serif;color:#bbb}
.fp-preset-name{font:500 11px/1.2 'Inter',sans-serif;color:#333;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.fp-footer{display:flex;gap:8px;margin-top:16px;padding-top:14px;border-top:1px solid #e8e8e8;flex-wrap:wrap}
.fp-mini{flex:1;padding:8px 0;background:transparent;border:1px solid #ddd;font:500 11px/1 'Inter',sans-serif;color:#555;cursor:pointer;transition:all .2s}
.fp-mini:hover{border-color:#111;color:#111}
.fp-ch-btn{flex:1;min-width:calc(50% - 4px);justify-content:flex-start;padding:10px 12px}
.fp-footer-mag{display:flex;justify-content:space-between;align-items:center;margin-top:28px;padding-top:14px;border-top:1px solid #e8e8e8;font:300 8px/1 'Inter',sans-serif;letter-spacing:.2em;color:#bbb}
.fp-footer-mag .fp-footer-left,.fp-footer-mag .fp-footer-right{font:400 8px/1 'Inter',sans-serif;letter-spacing:.2em;color:#bbb}
.fp-footer-mag .fp-footer-center{font:300 7px/1 'Georgia',serif;letter-spacing:.15em;color:#ccc}
.fp-watermark{position:absolute;top:50%;right:10px;transform:translateY(-50%);font:900 120px/1 'Georgia',serif;color:rgba(0,0,0,.04);pointer-events:none;z-index:0;letter-spacing:-6px;white-space:nowrap;user-select:none}
.fp-value-display{margin:-6px 0 16px}
.fp-value-label{font:700 10px/1 'Inter',sans-serif;color:#bbb;letter-spacing:2px;text-transform:uppercase;margin-bottom:4px}
.fp-value-num{font:900 38px/1 'Georgia',serif;color:#111;letter-spacing:-1px;transition:all .3s}
.value-fade-enter-active{animation:vfade-in .35s ease}
.value-fade-leave-active{animation:vfade-out .25s ease}
@keyframes vfade-in{from{opacity:0;transform:translateY(6px)}to{opacity:1;transform:translateY(0)}}
@keyframes vfade-out{from{opacity:1;transform:translateY(0)}to{opacity:0;transform:translateY(-6px)}}
.fp-value-num.positive{color:#2ecc71}
.fp-value-num.negative{color:#e74c3c}
.fp-value-num.zero{color:#999}
.fp-cat-tabs{display:flex;gap:4px;margin-bottom:16px;flex-wrap:wrap}
.fp-cat{display:flex;align-items:center;gap:4px;padding:6px 10px;background:transparent;border:1px solid #e0e0e0;cursor:pointer;transition:all .2s}
.fp-cat:hover{border-color:#111}
.fp-cat.active{background:#111;border-color:#111}
.fp-cat.active .fp-cat-idx,.fp-cat.active .fp-cat-name{color:#fff}
.fp-cat-idx{font:700 9px/1 'Georgia',serif;color:#bbb}
.fp-cat-name{font:500 11px/1 'Inter',sans-serif;color:#333}
.fp-callouts{display:flex;flex-direction:column;gap:2px}
.fp-callout-row{display:flex;flex-direction:column;gap:10px;padding:12px 12px;position:relative;border-left:2px solid transparent;transition:all .2s;cursor:pointer;background:#f6f4ef}
.fp-callout-row:hover{background:#edeae3;border-left-color:#111}
.fp-callout-row.active{background:#111;border-left-color:#111;transform:scale(1.02)}
.fp-callout-row.modified{border-left-color:#2ecc71}
.fp-callout-row.active.modified{border-left-color:#fff;background:#111}
.fp-callout-row.active .fp-callout-name,.fp-callout-row.active .fp-callout-val{color:#fff}
.fp-callout-row.active .fp-callout-dot{background:#fff}
.fp-callout-main{width:100%;display:flex;align-items:center;justify-content:space-between}
.fp-callout-left{display:flex;align-items:center;gap:8px}
.fp-callout-dot{width:6px;height:6px;border-radius:50%;background:#ddd;flex-shrink:0;transition:background .2s}
.fp-callout-dot.positive{background:#2ecc71}
.fp-callout-dot.negative{background:#e74c3c}
.fp-callout-name{font:500 11px/1 'Inter',sans-serif;color:#333;text-transform:uppercase;letter-spacing:1px}
.fp-callout-right{display:flex;align-items:center;gap:6px}
.fp-callout-val{font:700 13px/1 'Georgia',serif;color:#999;min-width:48px;text-align:right}
.fp-callout-shape{font:400 11px/1 'Georgia',serif;color:#bbb;font-style:italic;text-align:right}
.fp-callout-val.positive{color:#2ecc71}
.fp-callout-val.negative{color:#e74c3c}
.fp-callout-reset{width:22px;height:22px;background:transparent;border:1px solid #ddd;color:#999;font-size:12px;cursor:pointer;display:flex;align-items:center;justify-content:center;transition:all .2s;flex-shrink:0;border-radius:0}
.fp-callout-reset:hover{border-color:#111;color:#111}
.fp-callout-reset:disabled{opacity:.2;cursor:default}
.fp-slider-wrap{width:100%;padding-top:2px}
.fp-slider{width:100%;height:3px;accent-color:#111;cursor:pointer}
.fp-callout-row.active .fp-slider{accent-color:#fff}
.model-frame{flex:1;position:relative;background:#090909;overflow:hidden;min-width:200px;margin-left:-3%}
.three-box{width:100%;height:100%}
/* NFC 笔呼吸点 SVG 层 */
.breathing-dot-svg{position:absolute;inset:0;width:100%;height:100%;pointer-events:none;z-index:7}
.breathing-core{animation:breathing-pulse 2s ease-in-out infinite;transform-origin:center}
.breathing-ring-inner{animation:breathing-ring 2s ease-in-out infinite;transform-origin:center}
.breathing-ring-outer{animation:breathing-ring-outer 2s ease-in-out infinite;transform-origin:center}
@keyframes breathing-pulse{0%,100%{opacity:.5;r:4}50%{opacity:1;r:6}}
@keyframes breathing-ring{0%,100%{opacity:.3;r:8;stroke-width:1.5}50%{opacity:.7;r:12;stroke-width:.8}}
@keyframes breathing-ring-outer{0%,100%{opacity:.15;r:14;stroke-width:2}50%{opacity:.4;r:22;stroke-width:.5}}

.callout-svg{position:absolute;inset:0;width:100%;height:100%;pointer-events:none;z-index:6}
.callout-dot-pulse{animation:dot-breathe 2.8s ease-in-out infinite}
.callout-ring-pulse{animation:ring-breathe 2.8s ease-in-out infinite}
@keyframes dot-breathe{0%,100%{opacity:.45;r:4}50%{opacity:.95;r:7}}
@keyframes ring-breathe{0%,100%{opacity:.35;r:10;stroke-width:1.2}50%{opacity:.08;r:20;stroke-width:0.3}}
.spec-vertical{position:absolute;right:18px;top:50%;transform:translateY(-50%);font:900 20px/1.6 'Georgia',serif;color:rgba(255,255,255,.18);letter-spacing:6px;text-align:center;z-index:2;pointer-events:none;user-select:none;text-shadow:0 0 10px rgba(255,255,255,.08)}
.fold-handle{position:absolute;top:50%;transform:translateY(-50%);z-index:8;display:flex;align-items:center;gap:4px;padding:10px 6px;background:rgba(255,255,255,.08);border:1px solid rgba(255,255,255,.15);color:rgba(255,255,255,.6);cursor:pointer;transition:all .25s;backdrop-filter:blur(4px)}
.fold-handle:hover{background:rgba(255,255,255,.15);color:#fff}
.fold-handle-l{left:0;border-left:none;border-radius:0 4px 4px 0;flex-direction:column}
.fold-handle-r{position:absolute;right:0;top:50%;transform:translateY(-50%);border-right:none;border-radius:4px 0 0 4px;flex-direction:column;z-index:8;display:flex;align-items:center;gap:4px;padding:10px 6px;background:rgba(255,255,255,.08);border:1px solid rgba(255,255,255,.15);color:rgba(255,255,255,.6);cursor:pointer;transition:all .25s;backdrop-filter:blur(4px)}
.fold-handle-r:hover{background:rgba(255,255,255,.15);color:#fff}
.fh-arrow{font-size:16px;line-height:1}
.fh-text{writing-mode:vertical-lr;font:700 9px/1 'Inter',sans-serif;letter-spacing:2px;text-transform:uppercase}
.mf-corner{position:absolute;font:300 18px/1 monospace;color:rgba(255,255,255,.12);pointer-events:none;z-index:2}
.mf-tl{top:12px;left:14px}
.mf-tr{top:12px;right:14px}
.mf-bl{bottom:12px;left:14px}
.mf-br{bottom:12px;right:14px}
.mf-meta{position:absolute;bottom:24px;left:24px;z-index:2;pointer-events:none}
.mf-date{font:300 10px/1 'Inter',sans-serif;color:rgba(255,255,255,.18);letter-spacing:3px;text-transform:uppercase;margin-bottom:4px}
.mf-big{font:900 clamp(28px,5vw,52px)/1 'Georgia',serif;color:rgba(255,255,255,.035);letter-spacing:4px;text-transform:uppercase}
.mf-loading{position:absolute;inset:0;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:12px;background:rgba(9,9,9,.85);z-index:20;color:rgba(255,255,255,.6);font:400 12px/1 'Inter',sans-serif;letter-spacing:1px}
.mf-spinner{width:28px;height:28px;border:2px solid rgba(255,255,255,.15);border-top-color:#fff;border-radius:50%;animation:spin .8s linear infinite}
@keyframes spin{to{transform:rotate(360deg)}}
.intro-guide{position:absolute;inset:0;display:flex;flex-direction:column;align-items:center;justify-content:center;z-index:15;pointer-events:none}
.intro-ring{width:80px;height:80px;border:2px solid rgba(0,47,167,.4);border-radius:50%;animation:pulse-ring 1.6s ease-in-out infinite}
@keyframes pulse-ring{0%,100%{transform:scale(1);opacity:.6}50%{transform:scale(1.15);opacity:1}}
.intro-card{margin-top:16px;text-align:center;padding:14px 22px;background:rgba(0,0,0,.75);backdrop-filter:blur(12px);border:1px solid rgba(255,255,255,.12);border-radius:8px}
.intro-kicker{font:700 9px/1 'Inter',sans-serif;color:rgba(255,255,255,.45);letter-spacing:3px;margin-bottom:6px}
.intro-headline{font:700 16px/1.2 'Georgia',serif;color:#fff;margin-bottom:4px}
.intro-body{font:300 11px/1.4 'Inter',sans-serif;color:rgba(255,255,255,.6)}
.calib-overlay{position:absolute;inset:0;z-index:50;display:flex;flex-direction:column;pointer-events:none}
.calib-overlay>*{pointer-events:auto}

/* Scan line effect */
.calib-scanline{position:absolute;left:0;right:0;height:2px;background:linear-gradient(90deg,transparent 0%,rgba(46,204,113,.6) 30%,rgba(255,255,255,.9) 50%,rgba(46,204,113,.6) 70%,transparent 100%);z-index:55;pointer-events:none;opacity:0;top:0;box-shadow:0 0 20px rgba(46,204,113,.4),0 0 60px rgba(46,204,113,.15)}
.calib-scanline.active{animation:scan-down 1.2s ease-in-out forwards}
@keyframes scan-down{0%{top:0;opacity:0}5%{opacity:1}90%{opacity:.8}100%{top:100%;opacity:0}}

/* Crosshair */
.calib-crosshair{position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);width:60px;height:60px;z-index:54;pointer-events:none;opacity:0;transition:opacity .6s ease}
.calib-crosshair.active{opacity:1}
.ch-h,.ch-v{position:absolute;background:rgba(255,255,255,.2)}
.ch-h{left:0;right:0;top:50%;height:1px;transform:translateY(-50%)}
.ch-v{top:0;bottom:0;left:50%;width:1px;transform:translateX(-50%)}

/* Top bar */
.calib-bar{display:flex;align-items:center;gap:16px;padding:10px 24px;background:rgba(0,0,0,.85);backdrop-filter:blur(8px);border-bottom:1px solid rgba(255,255,255,.1)}
.calib-brand{display:flex;align-items:baseline;gap:8px}
.calib-vol{font:900 14px/1 'Georgia',serif;color:#fff;letter-spacing:2px}
.calib-step{font:300 11px/1 'Inter',sans-serif;color:rgba(255,255,255,.5)}
.calib-dots{display:flex;gap:6px;margin-left:auto}
.calib-dot{width:28px;height:28px;display:flex;align-items:center;justify-content:center;font-size:14px;border-radius:50%;border:1px solid rgba(255,255,255,.2);background:transparent;color:rgba(255,255,255,.4);transition:all .3s}
.calib-dot.done{background:rgba(46,204,113,.2);border-color:#2ecc71;color:#2ecc71}
.calib-dot.current{background:rgba(255,255,255,.1);border-color:#fff;color:#fff;animation:pulse-dot 1.2s ease-in-out infinite}
@keyframes pulse-dot{0%,100%{box-shadow:0 0 0 0 rgba(255,255,255,.3)}50%{box-shadow:0 0 0 6px rgba(255,255,255,0)}}
.calib-progress-bg{flex:1;height:3px;background:rgba(255,255,255,.1);border-radius:2px;overflow:hidden;margin-left:12px}
.calib-progress-fill{height:100%;background:#2ecc71;transition:width .4s ease}

/* PiP floating prompt card */
.calib-pip{position:absolute;bottom:110px;z-index:53;display:flex;overflow:hidden;border-radius:6px;background:rgba(0,0,0,.75);backdrop-filter:blur(12px);border:1px solid rgba(255,255,255,.12);box-shadow:0 8px 32px rgba(0,0,0,.4);max-width:220px;animation:pip-enter .4s ease;transition:left .4s cubic-bezier(.4,0,.2,1),right .4s cubic-bezier(.4,0,.2,1)}
.calib-pip.pip-left{left:24px;right:auto}
.calib-pip.pip-right{right:24px;left:auto;flex-direction:row-reverse}
.calib-pip.pip-right .pip-body{text-align:right;align-items:flex-end}
.pip-accent{width:4px;flex-shrink:0}
.pip-body{padding:12px 14px;display:flex;flex-direction:column;gap:3px}
.pip-step{font:700 8px/1 'Inter',sans-serif;color:rgba(255,255,255,.35);letter-spacing:2px}
.pip-icon{font-size:22px;margin:2px 0}
.pip-title{font:700 14px/1.2 'Georgia',serif;color:#fff}
.pip-hint{font:300 10px/1.4 'Inter',sans-serif;color:rgba(255,255,255,.55)}
.pip-sub{font:300 9px/1 'Inter',sans-serif;color:rgba(255,255,255,.25);letter-spacing:.5px;margin-top:2px}

/* PiP transition */
.pip-slide-enter-active{animation:pip-enter .35s ease}
.pip-slide-leave-active{animation:pip-exit .25s ease}
@keyframes pip-enter{from{opacity:0;transform:translateY(12px) scale(.95)}to{opacity:1;transform:translateY(0) scale(1)}}
@keyframes pip-exit{from{opacity:1;transform:translateY(0) scale(1)}to{opacity:0;transform:translateY(-8px) scale(.95)}}

/* Completion card (center) */
.calib-complete-card{position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);z-index:53;text-align:center;padding:20px 28px;background:rgba(0,0,0,.7);backdrop-filter:blur(12px);border:1px solid rgba(46,204,113,.3);border-radius:8px;box-shadow:0 8px 32px rgba(0,0,0,.4)}
.cc-icon{font-size:36px;color:#2ecc71;margin-bottom:6px}
.cc-title{font:700 16px/1.2 'Georgia',serif;color:#fff;margin-bottom:4px}
.cc-hint{font:300 11px/1.4 'Inter',sans-serif;color:rgba(255,255,255,.5)}

/* Bottom actions */
.calib-actions{position:absolute;bottom:60px;left:50%;transform:translateX(-50%);display:flex;gap:8px;z-index:52;padding:8px 14px;background:rgba(0,0,0,.72);backdrop-filter:blur(12px);border:1px solid rgba(255,255,255,.1);border-radius:6px}
.calib-btn{padding:8px 16px;background:rgba(255,255,255,.1);border:1px solid rgba(255,255,255,.22);color:#fff;font:500 11px/1 'Inter',sans-serif;cursor:pointer;transition:all .2s;border-radius:4px}
.calib-btn:hover{background:rgba(255,255,255,.15);border-color:#fff;color:#fff}
.calib-btn:disabled{opacity:.3;cursor:default}
.calib-confirm{background:rgba(46,204,113,.15);border-color:#2ecc71;color:#2ecc71}
.calib-confirm:hover{background:rgba(46,204,113,.3)}

/* Chip strip */
.calib-chips{position:absolute;bottom:16px;left:50%;transform:translateX(-50%);display:flex;gap:6px;z-index:52;flex-wrap:wrap;justify-content:center}
.calib-chip{display:flex;align-items:center;gap:4px;padding:4px 10px;background:rgba(0,0,0,.6);border:1px solid rgba(255,255,255,.15);color:rgba(255,255,255,.5);font:400 10px/1 'Inter',sans-serif;cursor:pointer;transition:all .2s}
.calib-chip.done{border-color:#2ecc71;color:#2ecc71}
.calib-chip.current{border-color:#fff;color:#fff}
/* NFC write toast */
.nfc-write-toast{position:fixed;bottom:32px;left:50%;transform:translateX(-50%);z-index:200;display:flex;align-items:center;gap:10px;padding:14px 28px;border-radius:8px;font:500 14px/1.4 'Inter',sans-serif;color:#fff;cursor:pointer;box-shadow:0 8px 32px rgba(0,0,0,.35);backdrop-filter:blur(12px);max-width:90vw;white-space:nowrap}
.nfc-write-toast.info{background:rgba(52,73,94,.92);border:1px solid rgba(255,255,255,.15)}
.nfc-write-toast.pending{background:rgba(41,128,185,.92);border:1px solid rgba(255,255,255,.2)}
.nfc-write-toast.success{background:rgba(39,174,96,.92);border:1px solid rgba(46,204,113,.4)}
.nfc-write-toast.error{background:rgba(192,57,43,.92);border:1px solid rgba(231,76,60,.4)}
.nfc-write-toast.warn{background:rgba(211,84,0,.88);border:1px solid rgba(243,156,18,.4)}
.nfc-toast-icon{font-size:20px;flex-shrink:0}
.nfc-toast-text{font:500 13px/1.3 'Inter',sans-serif;letter-spacing:.3px}
.nfc-toast-slide-enter-active{transition:all .35s cubic-bezier(.4,0,.2,1)}
.nfc-toast-slide-leave-active{transition:all .25s ease}
.nfc-toast-slide-enter-from{opacity:0;transform:translateX(-50%) translateY(20px)}
.nfc-toast-slide-leave-to{opacity:0;transform:translateX(-50%) translateY(20px)}

/* ═══ NFC 卡片插入动画 — 物理插槽 + 书签式 ═══ */

/* 第一步：PARAMETER OVERVIEW 区域向下位移腾出空间 */
.nfc-value-pushed {
  transition: transform 0.42s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.28s ease;
}
.nfc-sensing .nfc-value-pushed {
  transform: translateY(80px);
}
/* 数值区在动画期间隐藏 */
.nfc-value-hidden {
  transition: opacity 0.25s ease;
}
.nfc-sensing .nfc-value-hidden {
  opacity: 0;
  pointer-events: none;
}

/* 折页左边缘的物理插槽缝隙 */
.nfc-slot-slit {
  position: absolute;
  left: 0;
  top: 100px;
  width: 6px;
  height: 130px;
  z-index: 12;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.35s ease;
  background: linear-gradient(to right,
    rgba(0,0,0,0.5) 0%,
    rgba(0,0,0,0.25) 40%,
    rgba(0,0,0,0.08) 100%
  );
  box-shadow:
    inset -1px 0 4px rgba(0,0,0,0.5),
    2px 0 8px rgba(0,0,0,0.12);
}
.nfc-slot-slit.active {
  opacity: 1;
}
/* 槽口上下边缘的微小括号暗示 */
.nfc-slot-slit::before,
.nfc-slot-slit::after {
  content: '';
  position: absolute;
  left: 1px;
  width: 4px;
  height: 4px;
  border-left: 1.5px solid rgba(0,0,0,0.35);
  opacity: 0;
  transition: opacity 0.3s ease 0.1s;
}
.nfc-slot-slit::before {
  top: -1px;
  border-top: 1.5px solid rgba(0,0,0,0.35);
}
.nfc-slot-slit::after {
  bottom: -1px;
  border-bottom: 1.5px solid rgba(0,0,0,0.35);
}
.nfc-slot-slit.active::before,
.nfc-slot-slit.active::after {
  opacity: 1;
}
/* 槽口小三角箭头 */
.nfc-slot-arrow {
  position: absolute;
  left: 6px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 10px;
  color: rgba(0,0,0,0.3);
  line-height: 1;
  transition: opacity 0.3s ease, color 0.3s ease;
  opacity: 0;
}
.nfc-slot-slit.active .nfc-slot-arrow {
  opacity: 1;
  color: rgba(0,0,0,0.45);
}

/* 卡片书签 — 定位在折页内部，phase-2 停在数值区，phase-3 插入左边 */
.nfc-card-bookmark {
  position: absolute;
  left: 30px;
  top: 105px;
  width: 120px;
  height: 120px;
  z-index: 10;
  pointer-events: none;
  /* 卡片插入面板边缘时的内阴影效果 */
  filter: drop-shadow(-2px 0 3px rgba(0,0,0,0.12));
}

/* 卡片主体 */
.nfc-card-body {
  width: 120px;
  height: 120px;
  border-radius: 0 10px 10px 0; /* 左侧平齐对齐槽口，右侧圆角 */
  overflow: hidden;
  box-shadow:
    0 6px 24px rgba(0,0,0,0.25),
    0 2px 8px rgba(0,0,0,0.15),
    -3px 0 8px rgba(0,0,0,0.18); /* 左侧插入阴影 */
}
.nfc-card-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  border-radius: 0 10px 10px 0;
}
.nfc-card-blank {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #f5f5f5 0%, #e0e0e0 100%);
  border-radius: 0 10px 10px 0;
}
/* 卡片插入后，在面板边缘留下的缝隙阴影 */
.nfc-card-bookmark.nfc-phase-3 .nfc-card-body::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: linear-gradient(180deg,
    transparent 0%,
    rgba(0,0,0,0.2) 15%,
    rgba(0,0,0,0.35) 50%,
    rgba(0,0,0,0.2) 85%,
    transparent 100%
  );
  z-index: 2;
  pointer-events: none;
}

/* 飞入/飞出过渡 */
.nfc-card-fly-enter-active {
  animation: nfc-fly-in 0.38s cubic-bezier(0.2, 0.9, 0.3, 1.15) forwards;
}
.nfc-card-fly-leave-active {
  /* 无离场动画，瞬间消失 */
  display: none;
}

/* 第二步：快速飞入数值区 → 撞击回弹 → 停住让用户看清 */
@keyframes nfc-fly-in {
  0% {
    transform: translateX(400px);
    opacity: 0;
  }
  50% {
    transform: translateX(-8px);
    opacity: 1;
  }
  65% {
    transform: translateX(4px);
    opacity: 1;
  }
  80% {
    transform: translateX(-2px);
    opacity: 1;
  }
  100% {
    transform: translateX(0);
    opacity: 1;
  }
}
@keyframes nfc-fade-out {
  0% {
    opacity: 1;
    transform: translateX(0);
  }
  100% {
    opacity: 0;
    transform: translateX(0);
  }
}

/* 第三步：继续向左插入折页边缘，只露出 1/4 书签角 */
.nfc-card-bookmark.nfc-phase-3 {
  animation: nfc-insert-lock 0.45s cubic-bezier(0.25, 0.8, 0.25, 1.02) forwards;
}
@keyframes nfc-insert-lock {
  0% {
    left: 30px;
  }
  50% {
    left: -96px;
  }
  72% {
    left: -90px;
  }
  100% {
    left: -92px;
  }
}

@media(max-width:1100px){.fold-page{min-width:220px}.fold-left{width:35%}.fold-right{width:35%}.fp-preset{width:100%}}
@media(max-width:768px){.top-bar{padding:8px 12px;flex-wrap:wrap;gap:8px}.ch-desc{display:none}.fold-page{width:70%;position:absolute;top:0;bottom:0;z-index:20;box-shadow:0 0 24px rgba(0,0,0,.3)}.fold-left{left:0}.fold-right{right:0}.fold-page-scroll{padding:20px 16px 96px}.calib-dots{display:none}.model-frame{margin-left:0}.fp-value-display{font-size:30px}}
@media(max-width:560px){.top-bar-actions{gap:4px}.ch-btn{padding:6px 8px}.ch-no{display:none}.fold-page{width:85%;min-width:unset}.fold-page-scroll{padding:18px 14px 110px}}
</style>