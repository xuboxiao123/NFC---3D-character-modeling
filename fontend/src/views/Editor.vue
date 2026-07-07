<template>
  <div class="em-root">
    <header class="em-header">
      <div class="em-header-left">
        <button class="em-back" @click="$router.push(`/versions/${characterId}`)">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M19 12H5"/><path d="M12 19l-7-7 7-7"/></svg>
          <span>Back</span>
        </button>
        <div class="em-brand">
          <span class="em-brand-thin">E D I T O R</span>
          <span class="em-brand-bold">{{ currentVersionId ? '#' + currentVersionId : 'New' }}</span>
        </div>
      </div>
      <div class="em-header-center">
        <div class="em-ver-switcher">
          <button class="em-ver-chip" :class="{ active: !currentVersionId }" @click="selectNew">+ New</button>
          <button v-for="v in versions" :key="v.id" class="em-ver-chip" :class="{ active: currentVersionId === v.id }" @click="selectVersion(v)">
            <img v-if="v.previewImageUrl" :src="v.previewImageUrl" class="em-ver-chip-img" />
            <span v-else class="em-ver-chip-dot">◆</span>
            #{{ v.id }}
          </button>
        </div>
      </div>
      <div class="em-header-right">
        <span class="em-page-indicator">{{ String(currentPage + 1).padStart(2, '0') }} / {{ String(totalPages).padStart(2, '0') }}</span>
      </div>
    </header>
    <div class="em-progress">
      <div class="em-progress-track"><div class="em-progress-fill" :style="{ width: ((currentPage + 1) / totalPages * 100) + '%' }"></div></div>
      <div class="em-progress-steps">
        <button v-for="(pg, idx) in pages" :key="idx" class="em-step" :class="{ active: currentPage === idx, done: currentPage > idx }" @click="goToPage(idx)">
          <span class="em-step-num">{{ String(idx + 1).padStart(2, '0') }}</span>
          <span class="em-step-label">{{ pg.short }}</span>
        </button>
      </div>
    </div>
    <div class="em-viewport" @touchstart="onTouchStart" @touchmove="onTouchMove" @touchend="onTouchEnd">
      <div class="em-track" :style="trackStyle">
        <!-- P1: Images -->
        <section class="em-page"><div class="em-page-inner">
          <div class="em-page-head"><span class="em-page-num">01</span><div><h2 class="em-page-title">Reference Images</h2><p class="em-page-sub">Upload design references to guide AI generation</p></div></div>
          <div class="em-divider"></div>
          <div class="em-upload-grid">
            <div class="em-upload-card" v-for="(item, i) in uploadSlots" :key="i" @click="triggerUpload(item.type)">
              <input :ref="el => setUploadRef(item.type, el)" type="file" accept="image/*" hidden @change="e => handleUpload(e, item.type)" />
              <img v-if="uploadImages[item.type]" :src="uploadImages[item.type]" class="em-upload-img" />
              <div v-else class="em-upload-empty"><svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg><span>{{ item.label }}</span></div>
              <button v-if="uploadImages[item.type]" class="em-upload-remove" @click.stop="uploadImages[item.type] = ''">×</button>
              <div class="em-upload-tag">{{ item.label.toUpperCase() }}</div>
            </div>
          </div>
          <div class="em-page-nav"><div></div><button class="em-nav-btn" @click="goToPage(1)">Prompts →</button></div>
        </div></section>
        <!-- P2: Prompts -->
        <section class="em-page"><div class="em-page-inner">
          <div class="em-page-head"><span class="em-page-num">02</span><div><h2 class="em-page-title">Prompts</h2><p class="em-page-sub">Describe your character for AI</p></div></div>
          <div class="em-divider"></div>
          <div class="em-fields">
            <div class="em-field"><div class="em-field-head"><span class="em-field-label">CHARACTER</span><span class="em-field-count">{{ prompt.length }}</span></div><textarea v-model="prompt" placeholder="Describe the character's appearance and traits..." class="em-textarea" rows="4"></textarea></div>
            <div class="em-field"><div class="em-field-head"><span class="em-field-label">SCENE</span><span class="em-field-count">{{ scene.length }}</span></div><textarea v-model="scene" placeholder="Describe the environment or background..." class="em-textarea" rows="3"></textarea></div>
            <div class="em-field"><div class="em-field-head"><span class="em-field-label">ACTION</span><span class="em-field-count">{{ action.length }}</span></div><textarea v-model="action" placeholder="Describe the pose or action..." class="em-textarea" rows="3"></textarea></div>
          </div>
          <div class="em-page-nav"><button class="em-nav-btn" @click="goToPage(0)">← Images</button><button class="em-nav-btn" @click="goToPage(2)">Result →</button></div>
        </div></section>
        <!-- P3: Result (with generate actions) -->
        <section class="em-page"><div class="em-page-inner">
          <div class="em-page-head"><span class="em-page-num">03</span><div><h2 class="em-page-title">Result</h2><p class="em-page-sub">Generate and preview assets</p></div></div>
          <div class="em-divider"></div>
          <div class="em-toolbar">
            <div class="em-toolbar-label">ACTIONS</div>
            <div class="em-toolbar-btns">
              <button class="em-toolbar-btn" :class="{ disabled: loading }" @click="currentVersionId ? saveVersion() : createVersion()">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M19 21H5a2 2 0 01-2-2V5a2 2 0 012-2h11l5 5v11a2 2 0 01-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>
                <span>{{ currentVersionId ? 'Save' : 'Create' }}</span>
              </button>
              <span class="em-toolbar-sep"></span>
              <button class="em-toolbar-btn" :class="{ disabled: !currentVersionId || loading }" @click="gen2D">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
                <span>Generate 2D</span>
              </button>
              <span class="em-toolbar-sep"></span>
              <button class="em-toolbar-btn" :class="{ disabled: !currentVersionId || loading }" @click="gen3D">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>
                <span>Generate 3D</span>
              </button>
            </div>
          </div>
          <div v-if="loading" class="em-status"><span class="em-spinner"></span><span>{{ statusText }}</span></div>
          <div class="em-result-layout" v-if="imageUrl || modelUrl">
            <div v-if="imageUrl" class="em-result-card"><div class="em-result-label">2D PREVIEW</div><img :src="imageUrl" class="em-result-img" /></div>
            <div v-if="modelUrl" class="em-result-card">
              <div class="em-result-label">3D MODEL {{ sculptedModelUrl && viewingSculpted ? '(Sculpted)' : '(Original)' }}</div>
              <div v-if="modelUrl && sculptedModelUrl" class="em-model-toggle"><button :class="{ active: !viewingSculpted }" @click="viewingSculpted = false">Original</button><button :class="{ active: viewingSculpted }" @click="viewingSculpted = true">Sculpted</button></div>
              <div class="em-model-wrap" @click="openModelView"><ModelViewer :modelUrl="displayModelUrl" @loadError="onModelLoadError" /><div class="em-model-overlay"><span>Click for fullscreen →</span></div></div>
              <button class="em-print-btn" :disabled="printing" @click.stop="handlePrint3D">{{ printing ? '🖨️ Launching...' : '🖨️ Bambu Studio' }}</button>
              <p v-if="printMsg" class="em-msg" :class="{ success: printSuccess }">{{ printMsg }}</p>
            </div>
          </div>
          <div v-else class="em-empty"><span>◇</span><p>No results yet. Generate assets above.</p></div>
          <div class="em-page-nav"><button class="em-nav-btn" @click="goToPage(1)">← Prompts</button><button class="em-nav-btn" @click="goToPage(3)" v-if="currentVersionId && modelUrl">Sculpt →</button></div>
        </div></section>
        <!-- P4: Sculpt -->
        <section class="em-page"><div class="em-page-inner">
          <div class="em-page-head"><span class="em-page-num">04</span><div><h2 class="em-page-title">Face Sculpt</h2><p class="em-page-sub">Customize facial details</p></div></div>
          <div class="em-divider"></div>
          <div v-if="currentVersionId && modelUrl" class="em-sculpt-hero" @click="openModelView"><div class="em-sculpt-icon">🎭</div><h3>Open Sculpt Editor</h3><p>Fine-tune facial expressions in the interactive 3D editor</p><span class="em-sculpt-arrow">→</span></div>
          <div v-else class="em-empty"><span>🎭</span><p>Generate a 3D model first.</p></div>
          <div class="em-page-nav"><button class="em-nav-btn" @click="goToPage(2)">← Result</button><button class="em-nav-btn" @click="goToPage(4)" v-if="currentVersionId">Share →</button></div>
        </div></section>
        <!-- P5: Share -->
        <section class="em-page"><div class="em-page-inner">
          <div class="em-page-head"><span class="em-page-num">05</span><div><h2 class="em-page-title">Community Share</h2><p class="em-page-sub">Publish your creation to the gallery</p></div></div>
          <div class="em-divider"></div>
          <div v-if="currentVersionId" class="em-share-content">

            <!-- 分享预览卡片 -->
            <div class="em-share-preview">
              <div class="em-share-card">
                <div class="em-share-card-visual">
                  <img v-if="imageUrl" :src="imageUrl" class="em-share-card-img" />
                  <div v-else class="em-share-card-placeholder"><span>◆</span></div>
                  <!-- 邮戳 -->
                  <div class="em-share-stamp">
                    <div class="em-share-stamp-inner">
                      <span class="em-share-stamp-ring"></span>
                      <span class="em-share-stamp-text">VOL.</span>
                      <span class="em-share-stamp-num">{{ String(currentVersionId).padStart(2, '0') }}</span>
                    </div>
                  </div>
                  <!-- 大页码 -->
                  <div class="em-share-big-num">{{ String(currentVersionId).padStart(2, '0') }}</div>
                </div>
                <div class="em-share-card-info">
                  <div class="em-share-kicker">COMMUNITY FEATURE</div>
                  <h3 class="em-share-headline">{{ shareDesc || 'Your Creation' }}</h3>
                  <div class="em-share-desc-divider"><span class="em-share-desc-line"></span><span class="em-share-desc-dot">◆</span><span class="em-share-desc-line"></span></div>
                  <div class="em-share-meta">
                    <span>V{{ currentVersionId }}</span>
                    <span class="em-share-meta-sep">·</span>
                    <span>{{ shareTags || 'No tags' }}</span>
                  </div>
                  <!-- 标签预览 -->
                  <div class="em-share-tags-preview" v-if="shareTags">
                    <span v-for="tag in parsedShareTags" :key="tag" class="em-share-tag">{{ tag }}</span>
                  </div>
                </div>
              </div>

              <!-- 状态徽章 -->
              <div v-if="isShared" class="em-share-status-badge">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
                <span>PUBLISHED TO GALLERY</span>
              </div>
            </div>

            <!-- 编辑区域 -->
            <div class="em-share-editor">
              <div class="em-share-editor-label">EDITORIAL DETAILS</div>
              <div class="em-share-editor-divider"></div>

              <div class="em-field">
                <div class="em-field-head"><span class="em-field-label">DESCRIPTION</span><span class="em-field-count">{{ shareDesc.length }}</span></div>
                <textarea v-model="shareDesc" placeholder="Write a compelling description for the gallery..." class="em-textarea" rows="3"></textarea>
              </div>
              <div class="em-field">
                <div class="em-field-head"><span class="em-field-label">TAGS</span></div>
                <input v-model="shareTags" placeholder="character, cyberpunk, fantasy..." class="em-input" />
                <span class="em-field-hint">Separate with commas</span>
              </div>

              <!-- 操作按钮 -->
              <div class="em-share-toolbar">
                <button class="em-share-pub-btn" @click="shareToComm" :disabled="shareLoading">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z"/></svg>
                  <span>{{ isShared ? 'Update Share' : 'Publish to Gallery' }}</span>
                </button>
                <button v-if="isShared" class="em-share-unshr-btn" @click="unshareFromComm" :disabled="shareLoading">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                  <span>Remove</span>
                </button>
              </div>
              <p v-if="shareMsg" class="em-msg" :class="{ success: shareSuccess }">{{ shareMsg }}</p>
            </div>

          </div>
          <div v-else class="em-empty"><span>↗</span><p>Create a version first to share your work.</p></div>
          <div class="em-page-nav"><button class="em-nav-btn" @click="goToPage(3)">← Sculpt</button><button class="em-nav-btn" @click="goToPage(5)" v-if="currentVersionId">NFC →</button></div>
        </div></section>
        <!-- P6: NFC -->
        <section class="em-page"><div class="em-page-inner">
          <div class="em-page-head"><span class="em-page-num">06</span><div><h2 class="em-page-title">NFC Card Writer</h2><p class="em-page-sub">Write model data to NFC cards</p></div></div>
          <div class="em-divider"></div>
          <div v-if="currentVersionId" class="em-nfc-split">
            <!-- 左侧：演示动画 -->
            <div class="em-nfc-demo">
              <div class="em-nfc-demo-scene">
                <!-- 读卡器底座 -->
                <div class="em-nfc-reader">
                  <div class="em-nfc-reader-body">
                    <div class="em-nfc-reader-screen">
                      <span class="em-nfc-reader-icon">📡</span>
                      <span class="em-nfc-reader-label">RC522</span>
                    </div>
                    <div class="em-nfc-reader-led" :class="{ active: nfcWriteLoading, done: nfcWriteSuccess }"></div>
                  </div>
                  <div class="em-nfc-reader-base"></div>
                </div>
                <!-- NFC 卡片动画 -->
                <div class="em-nfc-card-anim" :class="{ writing: nfcWriteLoading, done: nfcWriteSuccess }">
                  <div class="em-nfc-card-face">
                    <div class="em-nfc-card-chip"></div>
                    <span class="em-nfc-card-ver">V{{ currentVersionId }}</span>
                    <div class="em-nfc-card-waves">
                      <span></span><span></span><span></span>
                    </div>
                  </div>
                </div>
                <!-- 信号波纹 -->
                <div class="em-nfc-signal" :class="{ active: nfcWriteLoading }">
                  <span></span><span></span><span></span>
                </div>
              </div>
              <!-- 步骤提示 -->
              <div class="em-nfc-steps">
                <div class="em-nfc-step" :class="{ active: !nfcWriteLoading && !nfcWriteSuccess }">
                  <span class="em-nfc-step-num">1</span>
                  <span>Take out NFC card</span>
                </div>
                <div class="em-nfc-step-arrow">→</div>
                <div class="em-nfc-step" :class="{ active: nfcWriteLoading }">
                  <span class="em-nfc-step-num">2</span>
                  <span>Place on reader</span>
                </div>
                <div class="em-nfc-step-arrow">→</div>
                <div class="em-nfc-step" :class="{ active: nfcWriteSuccess }">
                  <span class="em-nfc-step-num">3</span>
                  <span>Character written</span>
                </div>
              </div>
            </div>
            <!-- 右侧：操作区 -->
            <div class="em-nfc-action">
              <div class="em-nfc-action-header">
                <span class="em-nfc-action-kicker">WRITE TO CARD</span>
                <h3 class="em-nfc-action-title">Current Model</h3>
              </div>
              <div class="em-nfc-action-divider"></div>
              <div class="em-nfc-display"><div><p class="em-nfc-title">Version Card</p><p class="em-nfc-desc">Write version id into a physical NFC sticker.</p></div><span class="em-nfc-chip">V{{ currentVersionId }}</span></div>
              <button class="em-nfc-write-btn" @click="writeCurrentVersionCard" :disabled="nfcWriteLoading">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z"/><path d="M8 12l3 3 5-5"/></svg>
                <span>{{ nfcWriteLoading ? 'Writing...' : 'Write Card' }}</span>
              </button>
              <button v-if="nfcWriteLoading" class="em-nfc-cancel-btn" @click="cancelNfcWrite">
                Cancel
              </button>
              <button v-if="!nfcWriteLoading && nfcWriteMsg && !nfcWriteSuccess" class="em-nfc-retry-btn" @click="retryNfcWrite">
                Retry
              </button>
              <p v-if="nfcWriteMsg" class="em-msg" :class="{ success: nfcWriteSuccess }">{{ nfcWriteMsg }}</p>
              <p class="em-nfc-tip">Place the NFC card on RC522 reader after clicking write.</p>
            </div>
          </div>
          <div v-else class="em-empty"><span>📡</span><p>Create a version first.</p></div>
          <div class="em-page-nav"><button class="em-nav-btn" @click="goToPage(4)">← Share</button><div></div></div>
        </div></section>
      </div>
    </div>
    <div class="em-footer">
      <div class="em-footer-cassette" aria-hidden="true">
        <div class="em-cassette-shell">
          <div class="em-cassette-label"></div>
          <div class="em-cassette-window">
            <span class="em-cassette-reel"></span>
            <span class="em-cassette-reel"></span>
          </div>
          <div class="em-cassette-bottom">
            <span></span>
            <span></span>
          </div>
        </div>
      </div>
      <div class="em-footer-info"><span class="em-footer-page">PAGE {{ String(currentPage + 1).padStart(2, '0') }}</span><span class="em-footer-sep">—</span><span class="em-footer-title">{{ pages[currentPage]?.title || '' }}</span></div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, watch, computed } from 'vue'
import axios from 'axios'
import { useRoute, useRouter } from 'vue-router'
import ModelViewer from '../components/ModelViewer.vue'
import { print3D, requestNfcWriteCard, getNfcWriteStatus, getNfcPresetCommand } from '../api/api.js'

const route = useRoute()
const router = useRouter()
const characterId = route.params.characterId
const versions = ref([])
const currentVersionId = ref(null)
const currentPage = ref(0)
const pages = [
  { title: 'Reference Images', short: 'Images' },
  { title: 'Prompts', short: 'Prompts' },
  { title: 'Result Preview', short: 'Result' },
  { title: 'Face Sculpt', short: 'Sculpt' },
  { title: 'Community Share', short: 'Share' },
  { title: 'NFC Card Writer', short: 'NFC' }
]
const totalPages = pages.length

let touchStartX = 0, touchStartY = 0, isSwiping = false
const onTouchStart = (e) => { touchStartX = e.touches[0].clientX; touchStartY = e.touches[0].clientY; isSwiping = false }
const onTouchMove = (e) => { if (Math.abs(e.touches[0].clientX - touchStartX) > Math.abs(e.touches[0].clientY - touchStartY) && Math.abs(e.touches[0].clientX - touchStartX) > 10) isSwiping = true }
const onTouchEnd = (e) => { if (!isSwiping) return; const dx = e.changedTouches[0].clientX - touchStartX; if (dx < -50 && currentPage.value < totalPages - 1) currentPage.value++; else if (dx > 50 && currentPage.value > 0) currentPage.value-- }
const goToPage = (idx) => { if (idx >= 0 && idx < totalPages) currentPage.value = idx }
const trackStyle = computed(() => ({ transform: `translateX(-${currentPage.value * 100}%)`, transition: 'transform 0.55s cubic-bezier(0.22,0.61,0.36,1)' }))

const uploadSlots = [{ type: 'design', label: 'Design' }, { type: 'pose', label: 'Pose' }, { type: 'extra', label: 'Extra' }]
const uploadRefMap = {}
const setUploadRef = (type, el) => { if (el) uploadRefMap[type] = el }
const uploadImages = reactive({ design: '', pose: '', extra: '' })
const triggerUpload = (type) => { if (uploadRefMap[type]) uploadRefMap[type].click() }
const uploadFile = async (file) => {
  if (!file) return null
  try { const ct = file.type || 'image/png'; const r = await axios.get('/api/upload/presign', { params: { fileName: file.name, contentType: ct } }); await axios.put(r.data.uploadUrl, file, { headers: { 'Content-Type': ct } }); return r.data.fileUrl } catch (e) { return null }
}
const handleUpload = async (e, type) => { const url = await uploadFile(e.target.files[0]); if (url) uploadImages[type] = url }

const prompt = ref(''), scene = ref(''), action = ref('')
const imageUrl = ref(''), modelUrl = ref(''), sculptedModelUrl = ref(''), viewingSculpted = ref(false)
const loading = ref(false), statusText = ref('')
const printing = ref(false), printMsg = ref(''), printSuccess = ref(false)
const displayModelUrl = computed(() => viewingSculpted.value && sculptedModelUrl.value ? sculptedModelUrl.value : modelUrl.value)
const onModelLoadError = ({ url }) => { if (viewingSculpted.value && url === sculptedModelUrl.value && modelUrl.value) viewingSculpted.value = false }

const isShared = ref(false), shareDesc = ref(''), shareTags = ref(''), shareLoading = ref(false), shareMsg = ref(''), shareSuccess = ref(false)
const parsedShareTags = computed(() => shareTags.value.split(',').map(t => t.trim()).filter(Boolean).slice(0, 5))
const nfcWriteLoading = ref(false), nfcWriteMsg = ref(''), nfcWriteSuccess = ref(false)
let nfcWriteTimer = null, nfcPresetTimer = null, lastPresetCommandTs = 0, nfcPresetPolling = false

const syncNfc = async (vid) => { try { if (vid) await axios.get(`/api/nfc/setCurrent?versionId=${vid}`) } catch (e) { /* */ } }
const loadVersions = async () => { try { versions.value = (await axios.get(`/api/version/list?characterId=${characterId}`)).data } catch (e) { console.error(e) } }
const loadVersionData = async (vid) => {
  try {
    const v = (await axios.get(`/api/version/get?versionId=${vid}`)).data
    prompt.value = v.characterPrompt || ''; scene.value = v.scenePrompt || ''; action.value = v.actionPrompt || ''
    uploadImages.design = v.designImageUrl || ''; uploadImages.pose = v.poseImageUrl || ''; uploadImages.extra = v.extraImageUrl || ''
    imageUrl.value = v.previewImageUrl || ''; modelUrl.value = v.modelUrl || ''; sculptedModelUrl.value = v.sculptedModelUrl || ''
    viewingSculpted.value = false; currentVersionId.value = v.id; await syncNfc(v.id)
  } catch (e) { console.error(e) }
}
const selectVersion = (v) => loadVersionData(v.id)
const selectNew = () => { currentVersionId.value = null; prompt.value = ''; scene.value = ''; action.value = ''; uploadImages.design = ''; uploadImages.pose = ''; uploadImages.extra = ''; imageUrl.value = ''; modelUrl.value = ''; sculptedModelUrl.value = ''; viewingSculpted.value = false }

const createVersion = async () => {
  loading.value = true; statusText.value = 'Creating version...'
  try { const r = await axios.post('/api/version/create', null, { params: { characterId, characterPrompt: prompt.value, scenePrompt: scene.value, actionPrompt: action.value, designImageUrl: uploadImages.design, poseImageUrl: uploadImages.pose, extraImageUrl: uploadImages.extra } }); currentVersionId.value = r.data.id; await syncNfc(r.data.id); await loadVersions() } catch (e) { console.error(e) }
  loading.value = false; statusText.value = ''
}
const saveVersion = async () => {
  if (!currentVersionId.value) return; loading.value = true; statusText.value = 'Saving...'
  try { await axios.put('/api/version/edit', null, { params: { versionId: currentVersionId.value, characterPrompt: prompt.value, scenePrompt: scene.value, actionPrompt: action.value, designImageUrl: uploadImages.design, poseImageUrl: uploadImages.pose, extraImageUrl: uploadImages.extra } }); await loadVersions() } catch (e) { console.error(e) }
  loading.value = false; statusText.value = ''
}
const gen2D = async () => {
  if (!currentVersionId.value || loading.value) return; loading.value = true; statusText.value = 'Generating 2D...'
  try { imageUrl.value = (await axios.post(`/api/version/generate2D?versionId=${currentVersionId.value}`)).data.previewImageUrl; await loadVersions() } catch (e) { console.error(e) }
  loading.value = false; statusText.value = ''
}
const gen3D = async () => {
  if (!currentVersionId.value || loading.value) return; loading.value = true; statusText.value = 'Submitting 3D...'
  try { await axios.post(`/api/version/generate3D?versionId=${currentVersionId.value}`); statusText.value = 'Waiting for 3D model...'; for (let i = 0; i < 30; i++) { await new Promise(r => setTimeout(r, 2000)); const r = await axios.get(`/api/version/get?versionId=${currentVersionId.value}`); if (r.data.modelUrl) { modelUrl.value = r.data.modelUrl; break } statusText.value = `Polling... (${i+1}/30)` } await loadVersions() } catch (e) { console.error(e) }
  loading.value = false; statusText.value = ''
}
const openModelView = () => { if (currentVersionId.value) router.push(`/model-view/${currentVersionId.value}`) }
const handlePrint3D = async () => {
  if (!currentVersionId.value) return; printing.value = true; printMsg.value = ''
  try { const r = await print3D(currentVersionId.value); printSuccess.value = r.data.success; printMsg.value = (r.data.success ? '✅ ' : '❌ ') + r.data.message } catch (e) { printSuccess.value = false; printMsg.value = '❌ ' + (e.response?.data?.message || e.message) }
  printing.value = false; setTimeout(() => { printMsg.value = '' }, 5000)
}
const checkShareStatus = async () => { if (!currentVersionId.value) return; try { isShared.value = (await axios.get(`/api/community/isShared?versionId=${currentVersionId.value}`)).data.shared } catch (e) { /* */ } }
const shareToComm = async () => {
  if (!currentVersionId.value) return; shareLoading.value = true; shareMsg.value = ''
  try { const r = await axios.post('/api/community/share', null, { params: { versionId: currentVersionId.value, userId: localStorage.getItem('userId') || 1, authorName: 'User', description: shareDesc.value, tags: shareTags.value } }); isShared.value = true; shareSuccess.value = true; shareMsg.value = '✅ Shared'; if (r.data?.id) setTimeout(() => router.push(`/community/${r.data.id}`), 600) } catch (e) { shareSuccess.value = false; shareMsg.value = '❌ Failed' }
  shareLoading.value = false; setTimeout(() => { shareMsg.value = '' }, 3000)
}
const unshareFromComm = async () => {
  if (!currentVersionId.value) return; shareLoading.value = true; shareMsg.value = ''
  try { await axios.delete(`/api/community/unshare?versionId=${currentVersionId.value}`); isShared.value = false; shareSuccess.value = true; shareMsg.value = '✅ Removed' } catch (e) { shareSuccess.value = false; shareMsg.value = '❌ Failed' }
  shareLoading.value = false; setTimeout(() => { shareMsg.value = '' }, 3000)
}
const nfcLastWriteCode = ref('')
const writeCurrentVersionCard = () => { if (!currentVersionId.value) return; doNfcWrite(`V${currentVersionId.value}`, 'version') }
const retryNfcWrite = () => { if (nfcLastWriteCode.value) doNfcWrite(nfcLastWriteCode.value, 'version') }
const cancelNfcWrite = () => {
  if (nfcWriteTimer) { clearInterval(nfcWriteTimer); nfcWriteTimer = null }
  nfcWriteLoading.value = false
  nfcWriteSuccess.value = false
  nfcWriteMsg.value = '⏹️ Write cancelled'
}
const doNfcWrite = async (code, type) => {
  nfcWriteLoading.value = true; nfcWriteSuccess.value = false; nfcWriteMsg.value = `📝 Writing ${code}...`; nfcLastWriteCode.value = code
  try { await requestNfcWriteCard({ code, type, versionId: currentVersionId.value }); startNfcPoll(code) } catch (e) { nfcWriteLoading.value = false; nfcWriteMsg.value = '❌ Write request failed. Tap Retry.' }
}
const startNfcPoll = (code) => {
  if (nfcWriteTimer) clearInterval(nfcWriteTimer); let c = 0
  nfcWriteTimer = setInterval(async () => {
    c++
    try {
      const s = (await getNfcWriteStatus()).data.writeStatus
      if (s === 'done') {
        nfcWriteSuccess.value = true; nfcWriteLoading.value = false
        nfcWriteMsg.value = `✅ Written: ${code}`
        clearInterval(nfcWriteTimer); nfcWriteTimer = null
        setTimeout(() => { nfcWriteMsg.value = '' }, 5000)
      } else if (s === 'failed') {
        nfcWriteLoading.value = false
        nfcWriteMsg.value = '❌ Write failed. Tap Retry.'
        clearInterval(nfcWriteTimer); nfcWriteTimer = null
      }
    } catch (e) { /* network error, keep polling */ }
    if (c >= 30) {
      nfcWriteLoading.value = false
      nfcWriteMsg.value = '⚠️ Timed out. Tap Retry.'
      clearInterval(nfcWriteTimer); nfcWriteTimer = null
    }
  }, 2000)
}
const startPresetPoll = () => { if (nfcPresetTimer) clearInterval(nfcPresetTimer); lastPresetCommandTs = Date.now(); nfcPresetTimer = setInterval(pollPreset, 1500) }
const pollPreset = async () => {
  if (nfcPresetPolling) return; nfcPresetPolling = true
  try { const d = (await getNfcPresetCommand(lastPresetCommandTs)).data; if (d?.status === 'ok' && d.presetCommand && d.presetTimestamp > lastPresetCommandTs) { lastPresetCommandTs = d.presetTimestamp; if (d.presetCommand.presetType === 'A' && d.presetCommand.referenceImageUrl) { uploadImages.pose = d.presetCommand.referenceImageUrl; nfcWriteMsg.value = `🎬 Action card applied`; setTimeout(() => { nfcWriteMsg.value = '' }, 3000) } } } catch (e) { /* */ }
  nfcPresetPolling = false
}

watch(currentVersionId, () => { checkShareStatus(); shareDesc.value = ''; shareTags.value = ''; shareMsg.value = ''; if (nfcWriteTimer) { clearInterval(nfcWriteTimer); nfcWriteTimer = null }; nfcWriteLoading.value = false; nfcWriteMsg.value = '' })
onMounted(async () => { startPresetPoll(); await loadVersions(); const vid = route.query.versionId; if (vid) await loadVersionData(Number(vid)) })
onBeforeUnmount(() => { if (nfcPresetTimer) clearInterval(nfcPresetTimer); if (nfcWriteTimer) clearInterval(nfcWriteTimer) })
</script>

<style scoped>
@import './EditorMag.css';
</style>