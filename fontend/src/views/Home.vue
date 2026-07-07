
<template>
  <div class="home" :class="{ 'phase-shrunk': phase >= 1, 'phase-content': phase >= 2 }">

    <!-- ===== 全屏视频 + Hero 文字（Phase 0） ===== -->
    <div class="hero-section" :class="{ shrunk: phase >= 1 }" v-show="phase < 2">
      <video ref="bgVideo" class="bg-video" src="/source/hero-video.mp4" muted autoplay loop playsinline @click="toggleVideo"></video>
      <div class="video-pause-indicator" v-if="videoPaused && phase < 1">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="white"><polygon points="5,3 19,12 5,21"/></svg>
      </div>
      <div class="hero-overlay" :class="{ hidden: phase >= 1 }">
        <div class="hero-text">
          <h1>Future of 3D.</h1>
          <p class="tagline">AI-driven character modeling with multimodal interaction</p>
          <button class="enter-btn" @click.stop="enterDesk">Explore &#8594;</button>
        </div>
      </div>
    </div>

    <!-- ===== Phase 1: 电子杂志排版 ===== -->
    <div class="split-panel" :class="{ visible: showSplitPanel }" v-show="showSplitPanel && phase < 2">
      <div class="mag-page" :class="{ 'input-focused': isInputFocused }">
        <!-- 介绍区 -->
        <div class="mag-intro" v-show="!isInputFocused">
          <span class="mag-kicker">ABOUT</span>
          <h2 class="mag-title">NFC 3D Studio</h2>
          <div class="mag-divider"></div>
          <p class="mag-desc">An AI-driven multimodal 3D character modeling system. Generate 2D character illustrations from text descriptions and reference images, then convert them into 3D models with real-time NFC card interaction for expression control.</p>
        </div>

        <!-- 两个区块 -->
        <div class="mag-blocks" :class="{ 'blocks-expanded': isInputFocused }">
          <!-- 01 创建角色 -->
          <div class="mag-block mag-block--create">
            <div class="mag-block-header">
              <span class="mag-block-num">01</span>
              <h3 class="mag-block-title">Create</h3>
            </div>
            <p class="mag-block-desc">Enter a character name to start your AI modeling journey</p>
            <div class="create-row">
              <input v-model="charName" placeholder="Character name" class="text-input" @keyup.enter="createCharacter" @focus="isInputFocused = true" @blur="isInputFocused = false" />
              <button class="action-btn" @click="createCharacter">+</button>
            </div>
          </div>

          <!-- 02 查看角色 -->
          <div class="mag-block mag-block--nav" @click="enterContent" v-show="!isInputFocused">
            <div class="mag-block-header">
              <span class="mag-block-num">02</span>
              <h3 class="mag-block-title">Characters</h3>
            </div>
            <p class="mag-block-desc">Browse and manage your character collection</p>
            <span class="mag-block-arrow">&#8594;</span>
          </div>
        </div>
      </div>
    </div>

    <!-- ===== Phase 2: 角色列表（全屏固定，从角色模块放大展开） ===== -->
    <div class="gallery-fullscreen" :class="{ visible: phase >= 2, expanding: galleryAnimating === 'expand', collapsing: galleryAnimating === 'collapse' }">

      <!-- 左上角返回按钮 -->
      <button class="back-btn" @click="goBack">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 12H5"/><path d="M12 19l-7-7 7-7"/></svg>
        <span>Back</span>
      </button>

      <!-- 标题行 -->
      <div class="gallery-header-row">
        <div class="gallery-header-left">
          <span class="gallery-kicker">YOUR COLLECTION</span>
          <h2 class="gallery-headline">Characters</h2>
        </div>
        <div class="gallery-header-right">
          <span class="gallery-counter">{{ String(activeCardIndex + 1).padStart(2, '0') }} / {{ String(cardList.length).padStart(2, '0') }}</span>
          <button class="edit-toggle-btn" @click="editMode = !editMode">{{ editMode ? '&#10003; Done' : '&#9998; Edit' }}</button>
        </div>
      </div>

      <!-- 卡片轮播 -->
      <div class="gallery-main" ref="carouselRef" @touchstart="onTouchStart" @touchmove="onTouchMove" @touchend="onTouchEnd" @mousedown="onMouseDown">
        <div v-if="cardList.length === 0" class="empty gallery-empty">No characters yet</div>
        <div v-else class="gallery-track">
          <div v-for="item in visibleCards" :key="item.card.id" class="gallery-card" :class="{ active: item.index === activeCardIndex, near: Math.abs(item.relative) === 1, far: Math.abs(item.relative) >= 2 }" :style="getCarouselCardStyle(item.index)" @click="onCardClick(item.card, item.index)">
            <img v-if="item.card.coverImage" :src="item.card.coverImage" class="gcard-img" draggable="false" />
            <div v-else-if="item.card.isNew" class="gcard-new">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
              <span>NEW</span>
            </div>
            <div v-else class="gcard-empty"><span>&#127912;</span></div>
            <div class="gcard-overlay" v-if="item.index === activeCardIndex">
              <div class="gcard-overlay-text"><span class="gcard-name">{{ item.card.name }}</span></div>
            </div>
            <div class="gcard-edit-overlay" v-if="editMode && !item.card.isCreateCard && item.index === activeCardIndex">
              <button class="gcard-delete-btn" @click.stop="confirmDeleteCharacter(item.card)">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 6h18M8 6V4a2 2 0 012-2h4a2 2 0 012 2v2m3 0v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6h14"/></svg>
              </button>
            </div>
          </div>
          <div class="gallery-focus-frame"></div>
        </div>

        <div class="gallery-edit-controls" v-if="editMode && activeCard && !activeCard.isCreateCard">
          <button class="move-btn" @click="moveCharacter(-1)" :disabled="activeCardIndex === 0">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 18l-6-6 6-6"/></svg> Left
          </button>
          <span class="move-label">{{ activeCard.name }}</span>
          <button class="move-btn" @click="moveCharacter(1)" :disabled="activeCardIndex >= list.length - 1">
            Right <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>
          </button>
        </div>
      </div>

      <!-- 底部缩略图 + 编号 -->
      <div class="gallery-footer">
        <div class="gallery-thumbs-bar" v-if="cardList.length > 1">
          <div v-for="(card, idx) in cardList.slice(0, 8)" :key="'thumb-' + card.id" class="gallery-thumb" :class="{ active: idx === activeCardIndex }" @click="snapTo(idx)">
            <img v-if="card.coverImage" :src="card.coverImage" draggable="false" />
            <div v-else class="thumb-empty">{{ idx + 1 }}</div>
          </div>
        </div>
        <span class="gallery-footer-num">{{ String(activeCardIndex + 1).padStart(2, '0') }} — {{ String(cardList.length).padStart(2, '0') }}</span>
      </div>

      <!-- 删除确认弹窗 -->
      <div class="modal-overlay" v-if="showDeleteConfirm" @click.self="showDeleteConfirm = false">
        <div class="confirm-box">
          <p class="confirm-text">Delete character <strong>{{ deleteTarget?.name }}</strong>?</p>
          <p class="confirm-sub">All versions and community shares will also be deleted.</p>
          <div class="confirm-actions">
            <button class="confirm-btn cancel" @click="showDeleteConfirm = false">Cancel</button>
            <button class="confirm-btn danger" @click="doDeleteCharacter">Delete</button>
          </div>
        </div>
      </div>
    </div>


    <GuidedTutorial :visible="showTutorial" @close="closeTutorial" @finish="finishTutorial" @step-change="onTutorialStep" />
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed, watch } from 'vue'
import axios from 'axios'
import { useRouter, useRoute } from 'vue-router'
import GuidedTutorial from '../components/GuidedTutorial.vue'

const router = useRouter()
const route = useRoute()
const phase = ref(0)
const activeStep = ref(0)
const showTutorial = ref(false)

const list = ref([])
const charName = ref('')
const userId = localStorage.getItem('userId') || 1
const isInputFocused = ref(false)
const showSplitPanel = ref(false)
const editMode = ref(false)
const showDeleteConfirm = ref(false)
const deleteTarget = ref(null)
const bgVideo = ref(null)
const videoPaused = ref(false)
const communityPreview = ref([])
const carouselRef = ref(null)
const activeCardIndex = ref(0)
const isDragging = ref(false)
const dragOffset = ref(0)
const isPointerDragging = ref(false)
let dragStartX = 0
let dragStartOffset = 0
const CARD_WIDTH = 180
const CARD_GAP = 16
const STEP_WIDTH = CARD_WIDTH + CARD_GAP

const cardList = computed(() => {
  const baseCards = list.value.map((item) => ({ ...item, coverImage: item.coverImage || '', firstVersionId: item.firstVersionId || null, isNew: false, isCreateCard: false }))
  return [...baseCards, { id: 'create-new-card', name: 'Create New', coverImage: '', firstVersionId: null, isNew: true, isCreateCard: true }]
})

const activeCard = computed(() => cardList.value[activeCardIndex.value] || null)

const getRelativeOffset = (index) => {
  const total = cardList.value.length
  if (!total) return 0
  let diff = index - activeCardIndex.value
  if (diff > total / 2) diff -= total
  if (diff < -total / 2) diff += total
  return diff
}

const visibleCards = computed(() => {
  return cardList.value.map((card, index) => ({ card, index, relative: getRelativeOffset(index) })).filter(item => Math.abs(item.relative) <= 3).sort((a, b) => a.relative - b.relative)
})

const steps = [
  { title: 'Create a character', desc: 'Give your character a name to create a workspace for AI generation.' },
  { title: 'Upload reference images', desc: 'Upload design, pose, and extra reference images.' },
  { title: 'Write prompts', desc: 'Describe traits, scene, and desired pose for AI guidance.' },
  { title: 'Generate 2D preview', desc: 'Doubao API creates a 2D character illustration.' },
  { title: 'Generate 3D model', desc: 'Hunyuan 3D converts the 2D preview into a 3D OBJ model.' },
  { title: 'NFC interaction', desc: 'Tap NFC cards to adjust expression parameters in real-time.' }
] // steps kept for GuidedTutorial step-change sync

const toggleVideo = () => {
  if (!bgVideo.value) return
  if (bgVideo.value.paused) {
    bgVideo.value.play()
    videoPaused.value = false
  } else {
    bgVideo.value.pause()
    videoPaused.value = true
  }
}

const galleryAnimating = ref('')

let splitPanelTimer = null
const enterDesk = () => {
  phase.value = 1
  // 等视频缩小动画完成后再显示右侧内容
  splitPanelTimer = setTimeout(() => {
    showSplitPanel.value = true
  }, 1100)
}

const enterContent = () => {
  galleryAnimating.value = 'expand'
  phase.value = 2
  // 进入角色界面时暂停视频
  if (bgVideo.value) {
    bgVideo.value.pause()
    videoPaused.value = true
  }
  setTimeout(() => { galleryAnimating.value = '' }, 600)
}

const goBack = () => {
  if (phase.value === 2) {
    galleryAnimating.value = 'collapse'
    setTimeout(() => {
      phase.value = 1
      galleryAnimating.value = ''
      // 恢复视频播放
      if (bgVideo.value) {
        bgVideo.value.play().catch(() => {})
        videoPaused.value = false
      }
    }, 400)
  }
  else if (phase.value === 1) { showSplitPanel.value = false; if (splitPanelTimer) clearTimeout(splitPanelTimer); phase.value = 0; if (bgVideo.value) bgVideo.value.play() }
}

const goToCommunity = () => router.push('/community')
const goToCommunityDetail = (id) => router.push('/community/' + id)

watch(phase, (val) => {
  if (val >= 2) {
    // Phase 2: 暂停并隐藏视频
    document.body.classList.add('video-paused')
    document.body.classList.remove('video-playing')
    if (bgVideo.value) { bgVideo.value.pause(); videoPaused.value = true }
  } else if (val >= 1) {
    document.body.classList.add('video-paused')
    document.body.classList.remove('video-playing')
    // Phase 1: 视频继续播放（缩小显示在左侧）
    if (bgVideo.value) { bgVideo.value.play().catch(() => {}); videoPaused.value = false }
  } else {
    // Phase 0: 恢复播放
    document.body.classList.add('video-playing')
    document.body.classList.remove('video-paused')
    if (bgVideo.value) { bgVideo.value.play().catch(() => {}); videoPaused.value = false }
  }
}, { immediate: true })

const startTutorial = () => {
  if (phase.value < 2) phase.value = 2
  setTimeout(() => { window.scrollTo({ top: 0, behavior: 'smooth' }); setTimeout(() => { showTutorial.value = true }, 500) }, 0)
}

const closeTutorial = () => { showTutorial.value = false }
const finishTutorial = () => { showTutorial.value = false; const input = document.querySelector('.create-row .text-input'); if (input) { input.scrollIntoView({ behavior: 'smooth', block: 'center' }); setTimeout(() => input.focus(), 400) } }
const onTutorialStep = (step) => { if (step < steps.length) activeStep.value = step }
const clampIndex = (index) => Math.max(0, Math.min(index, cardList.value.length - 1))
const snapTo = (index) => { activeCardIndex.value = clampIndex(index); dragOffset.value = 0 }

const getCarouselCardStyle = (idx) => {
  const relative = getRelativeOffset(idx)
  const dragProgress = dragOffset.value / STEP_WIDTH
  const offset = relative + dragProgress
  const distance = Math.abs(offset)
  const translateX = offset * 140
  const translateZ = -distance * 50
  const rotateY = offset * -3.5
  const scale = Math.max(0.65, 1 - distance * 0.15)
  const blur = Math.min(3, distance * 1.2)
  const opacity = Math.max(0.2, 1 - distance * 0.28)
  const brightness = Math.max(0.65, 1 - distance * 0.14)
  const zIndex = Math.round(100 - distance * 10)
  return { transform: `translate3d(calc(-50% + ${translateX}px), -50%, ${translateZ}px) scale(${scale}) rotateY(${rotateY}deg)`, filter: `blur(${blur}px) brightness(${brightness})`, opacity, zIndex: String(zIndex) }
}

const onCardClick = (card, idx) => { if (idx !== activeCardIndex.value) { snapTo(idx); return } if (card.isCreateCard) { createCharacter(); return } goEdit(card.id) }
const handleDragStart = (clientX) => { if (!cardList.value.length) return; isDragging.value = true; isPointerDragging.value = false; dragStartX = clientX; dragStartOffset = dragOffset.value }
const handleDragMove = (clientX) => { if (!isDragging.value) return; const deltaX = clientX - dragStartX; if (Math.abs(deltaX) > 4) isPointerDragging.value = true; dragOffset.value = dragStartOffset + deltaX }
const handleDragEnd = () => { if (!isDragging.value) return; const movedSteps = Math.round(-dragOffset.value / STEP_WIDTH); const nextIndex = clampIndex(activeCardIndex.value + movedSteps); isDragging.value = false; dragOffset.value = 0; activeCardIndex.value = nextIndex; setTimeout(() => { isPointerDragging.value = false }, 0) }
const onTouchStart = (e) => { handleDragStart(e.touches[0].clientX) }
const onTouchMove = (e) => { handleDragMove(e.touches[0].clientX) }
const onTouchEnd = () => { handleDragEnd() }
const onMouseMove = (e) => { handleDragMove(e.clientX) }
const onMouseUp = () => { window.removeEventListener('mousemove', onMouseMove); window.removeEventListener('mouseup', onMouseUp); handleDragEnd() }
const onMouseDown = (e) => { handleDragStart(e.clientX); window.addEventListener('mousemove', onMouseMove); window.addEventListener('mouseup', onMouseUp) }

const loadCharacterCovers = async (characters) => {
  const cards = await Promise.all(characters.map(async (character) => {
    try {
      const versionRes = await axios.get('/api/version/list?characterId=' + character.id)
      const versions = Array.isArray(versionRes.data) ? versionRes.data : []
      const first = versions.find(v => v.previewImageUrl)
      return { ...character, coverImage: first?.previewImageUrl || '', firstVersionId: first?.id || versions[0]?.id || null }
    } catch (e) { return { ...character, coverImage: '', firstVersionId: null } }
  }))
  list.value = cards
  activeCardIndex.value = clampIndex(activeCardIndex.value)
}

const load = async () => { try { const res = await axios.get('/api/character/list?userId=' + userId); await loadCharacterCovers(Array.isArray(res.data) ? res.data : []) } catch (e) { console.error(e) } }

const createCharacter = async () => {
  const trimmedName = charName.value.trim()
  if (!trimmedName) return

  try {
    const res = await axios.post('/api/character/create?userId=' + userId + '&name=' + encodeURIComponent(trimmedName))
    const createdCharacterId = res?.data?.id || null

    charName.value = ''
    await load()

    const createdIndex = createdCharacterId
      ? list.value.findIndex(item => item.id === createdCharacterId)
      : list.value.length - 1

    enterContent()
    snapTo(createdIndex >= 0 ? createdIndex : Math.max(0, list.value.length - 1))
  } catch (e) {
    console.error(e)
  }
}

const goEdit = (id) => { if (isPointerDragging.value) return; router.push('/versions/' + id) }
const confirmDeleteCharacter = (card) => { deleteTarget.value = card; showDeleteConfirm.value = true }

const doDeleteCharacter = async () => {
  if (!deleteTarget.value) return
  try { await axios.delete('/api/character/delete?characterId=' + deleteTarget.value.id); showDeleteConfirm.value = false; deleteTarget.value = null; await load(); activeCardIndex.value = clampIndex(activeCardIndex.value) } catch (e) { console.error(e) }
}

const moveCharacter = async (direction) => {
  const idx = activeCardIndex.value; const realList = list.value; const newIdx = idx + direction
  if (newIdx < 0 || newIdx >= realList.length) return
  const temp = realList[idx]; realList[idx] = realList[newIdx]; realList[newIdx] = temp
  const orderList = realList.map((item, i) => ({ id: item.id, sortOrder: i }))
  try { await axios.put('/api/character/reorder', orderList); activeCardIndex.value = newIdx } catch (e) { console.error(e); await load() }
}

onMounted(() => {
  load()
  // 如果带 phase 参数（从 Versions 页面返回），直接跳到对应阶段
  const initPhase = parseInt(route.query.phase)
  if (initPhase >= 2) {
    phase.value = 2
    showSplitPanel.value = true
  } else if (initPhase >= 1) {
    phase.value = 1
    setTimeout(() => { showSplitPanel.value = true }, 1100)
  } else {
    document.body.classList.add('video-playing')
    if (bgVideo.value) {
      bgVideo.value.play().catch(() => {})
      videoPaused.value = false
    }
  }
})
onBeforeUnmount(() => { window.removeEventListener('mousemove', onMouseMove); window.removeEventListener('mouseup', onMouseUp); document.body.classList.remove('video-playing', 'video-paused') })
</script>

<style scoped>
@import './HomeStyle.css';
@import './HomeStyle2.css';
</style>