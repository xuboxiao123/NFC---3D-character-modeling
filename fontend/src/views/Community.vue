
<template>
  <div class="community-magazine" ref="pageRef" tabindex="0" @keydown="onKeydown">

    <!-- 顶部刊头 -->
    <header class="mag-header">
      <div class="mag-brand">
        <span class="mag-brand-thin">N F C</span>
        <span class="mag-brand-bold">GALLERY.</span>
      </div>
      <div class="mag-date">{{ currentDate }}</div>
      <button class="mag-search-btn" @click="showSearch = !showSearch" aria-label="toggle search">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <circle cx="11" cy="11" r="7"></circle>
          <path d="m20 20-3.5-3.5"></path>
        </svg>
      </button>
    </header>

    <Transition name="search-fade">
      <div v-if="showSearch" class="mag-search-bar">
        <input
          v-model="searchQuery"
          class="mag-search-input"
          placeholder="Search author / tag / story..."
          @input="onSearch"
        />
      </div>
    </Transition>

    <!-- Loading -->
    <div v-if="loading" class="mag-state">
      <span class="mag-spinner"></span>
      <span>Loading gallery...</span>
    </div>

    <!-- Empty -->
    <div v-else-if="displayItems.length === 0" class="mag-state">
      <span class="mag-empty-icon">◇</span>
      <p>No shared works yet</p>
      <p class="mag-empty-sub">Share your first creation from the editor.</p>
    </div>

    <!-- 主体 -->
    <template v-else>

      <!-- ====== 总览卡片条 ====== -->
      <section
        class="gallery-hero"
        @wheel.prevent="onWheel"
        @touchstart="onTouchStart"
        @touchmove.prevent="onTouchMove"
        @touchend="onTouchEnd"
        @mousedown="onMouseDown"
      >
        <div class="gallery-hero-inner">
          <div
            v-for="(item, idx) in displayItems"
            :key="'s'+item.id"
            class="gallery-strip-card"
            :class="stripCardClass(idx)"
            :style="stripCardStyle(idx)"
            @click="selectFromStrip(idx)"
          >
            <div class="gallery-strip-image">
              <img v-if="getDisplayImage(item)" :src="getDisplayImage(item)" :alt="item.description || ''" draggable="false" />
              <div v-else class="gallery-strip-placeholder" :style="{ background: gradients[idx % gradients.length] }">
                <span>◆</span>
              </div>
            </div>

            <!-- 邮戳标签 — 仅主卡片 -->
            <div v-if="idx === currentIndex" class="gallery-stamp-label">
              <div class="stamp-inner">
                <span class="stamp-border-ring"></span>
                <span class="stamp-text">{{ (item.authorName || 'USER').toUpperCase() }}</span>
                <span class="stamp-sub">VOL.{{ String(item.id).padStart(2, '0') }}</span>
              </div>
            </div>

            <!-- 超大半透明页码 -->
            <div v-if="idx === currentIndex" class="gallery-big-index">
              {{ String(currentIndex + 1).padStart(2, '0') }}
            </div>
          </div>
        </div>

        <!-- 导航箭头 -->
        <button v-if="currentIndex > 0" class="gallery-nav gallery-nav-left" @click.stop="prev" aria-label="previous">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M15 18l-6-6 6-6"/></svg>
        </button>
        <button v-if="currentIndex < displayItems.length - 1" class="gallery-nav gallery-nav-right" @click.stop="next" aria-label="next">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M9 18l6-6-6-6"/></svg>
        </button>

        <!-- 进度条 -->
        <div class="gallery-hero-footer">
          <div class="hero-progress-line">
            <div class="hero-progress-fill" :style="{ width: progressPercent + '%' }"></div>
          </div>
          <div class="hero-counter">
            <span class="hero-counter-current">{{ String(currentIndex + 1).padStart(2, '0') }}</span>
            <span class="hero-counter-sep">/</span>
            <span class="hero-counter-total">{{ String(displayItems.length).padStart(2, '0') }}</span>
          </div>
        </div>
      </section>

      <!-- ====== 衔接过渡区 ====== -->
      <div class="section-bridge">
        <div class="bridge-line"></div>
        <span class="bridge-label">EDITORIAL VIEW</span>
        <div class="bridge-line"></div>
      </div>

      <!-- ====== 杂志风格详情区 ====== -->
      <section class="mag-spread" ref="magSpreadRef" :class="{ 'mag-visible': magVisible }">
        <!-- 左侧：大标题 + 描述 -->
        <div class="mag-left mag-anim mag-anim-left">
          <div class="mag-kicker">COMMUNITY FEATURE</div>
          <h1 class="mag-title" :key="'t'+magIndex">{{ magActiveHeadline }}</h1>

          <div class="mag-divider">
            <span class="mag-divider-line"></span>
            <span class="mag-divider-dot">◆</span>
            <span class="mag-divider-line"></span>
          </div>

          <div class="mag-desc-wrap">
            <p class="mag-desc">{{ magActiveSummary }}</p>
          </div>

          <div class="mag-meta-row">
            <span class="mag-meta-item">VOL. {{ String(magActiveItem.id || 0).padStart(2, '0') }}</span>
            <span class="mag-meta-sep">·</span>
            <span class="mag-meta-item">{{ magActiveItem.authorName || 'Anonymous' }}</span>
            <span class="mag-meta-sep">·</span>
            <span class="mag-meta-item">{{ magActiveTagsText }}</span>
          </div>

          <div class="mag-tags" v-if="magActiveTags.length > 0">
            <span v-for="tag in magActiveTags" :key="tag" class="mag-tag">{{ tag }}</span>
          </div>

          <!-- 条形码数据 -->
          <div class="mag-barcode-row">
            <div class="mag-barcode-stat">
              <div class="mag-barcode-bars">
                <span v-for="n in 12" :key="'l'+n" class="mag-bar" :style="{ height: barH(n, magActiveItem.likesCount || 0) + 'px' }"></span>
              </div>
              <div class="mag-barcode-info">
                <span class="mag-barcode-label">LIKES</span>
                <span class="mag-barcode-value">{{ magActiveItem.likesCount || 0 }}</span>
              </div>
            </div>
            <div class="mag-barcode-stat">
              <div class="mag-barcode-bars">
                <span v-for="n in 12" :key="'r'+n" class="mag-bar" :style="{ height: barH(n, (magActiveItem.rating || 0) * 20) + 'px' }"></span>
              </div>
              <div class="mag-barcode-info">
                <span class="mag-barcode-label">RATING</span>
                <span class="mag-barcode-value">{{ magActiveItem.rating || 0 }}/5</span>
              </div>
            </div>
          </div>

          <button class="mag-view-btn" @click="goDetail(magActiveItem.id)">
            View Story →
          </button>
        </div>

        <!-- 中间：主视觉大图 -->
        <div class="mag-center mag-anim mag-anim-center" @click="goDetail(magActiveItem.id)">
          <div class="mag-hero-card">
            <img
              v-if="getDisplayImage(magActiveItem)"
              :src="getDisplayImage(magActiveItem)"
              :alt="magActiveItem.description || 'gallery'"
              class="mag-hero-img"
              draggable="false"
            />
            <div v-else class="mag-hero-placeholder">
              <span>◆</span>
            </div>

            <!-- 邮戳标签 -->
            <div class="mag-stamp">
              <div class="mag-stamp-inner">
                <span class="mag-stamp-ring"></span>
                <span class="mag-stamp-text">{{ (magActiveItem.authorName || 'USER').toUpperCase() }}</span>
                <span class="mag-stamp-sub">VOL.{{ String(magActiveItem.id || 0).padStart(2, '0') }}</span>
              </div>
            </div>

            <!-- 超大半透明页码 -->
            <div class="mag-big-num">{{ String(magIndex + 1).padStart(2, '0') }}</div>
          </div>

          <!-- 导航箭头 -->
          <button v-if="magIndex > 0" class="mag-nav mag-nav-left" @click.stop="magPrev" aria-label="previous">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M15 18l-6-6 6-6"/></svg>
          </button>
          <button v-if="magIndex < displayItems.length - 1" class="mag-nav mag-nav-right" @click.stop="magNext" aria-label="next">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M9 18l6-6-6-6"/></svg>
          </button>
        </div>

        <!-- 右侧：缩略图预览列 -->
        <div class="mag-right mag-anim mag-anim-right">
          <div class="mag-thumb-list">
            <div
              v-for="(item, idx) in nearbyItems"
              :key="item.id"
              class="mag-thumb"
              :class="{ active: item._idx === magIndex }"
              @click="magIndex = item._idx"
            >
              <img v-if="getDisplayImage(item)" :src="getDisplayImage(item)" alt="" draggable="false" />
              <div v-else class="mag-thumb-placeholder" :style="{ background: gradients[idx % gradients.length] }"></div>
              <div class="mag-thumb-overlay">
                <span class="mag-thumb-num">{{ String(item._idx + 1).padStart(2, '0') }}</span>
              </div>
            </div>
          </div>

          <!-- 进度 -->
          <div class="mag-progress">
            <div class="mag-progress-bar">
              <div class="mag-progress-fill" :style="{ width: magProgressPercent + '%' }"></div>
            </div>
            <div class="mag-counter">
              <span class="mag-counter-cur">{{ String(magIndex + 1).padStart(2, '0') }}</span>
              <span class="mag-counter-sep">/</span>
              <span class="mag-counter-total">{{ String(displayItems.length).padStart(2, '0') }}</span>
            </div>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()

const loading = ref(true)
const showSearch = ref(false)
const searchQuery = ref('')
const currentIndex = ref(0)   // 总览区索引
const magIndex = ref(0)       // 杂志区独立索引
const allModels = ref([])
const pageRef = ref(null)
const magSpreadRef = ref(null)
const magVisible = ref(false) // 杂志区是否进入视口

let touchStartX = 0
let touchStartY = 0
let isDragging = false
let dragStartX = 0
let wheelLock = false
let magObserver = null

const now = new Date()
const months = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC']
const currentDate = `${months[now.getMonth()]}. ${now.getDate()} ${now.getFullYear()}`

const gradients = [
  'linear-gradient(135deg, #cfc8bf 0%, #e9e5df 100%)',
  'linear-gradient(135deg, #d8d1c8 0%, #f3efe9 100%)',
  'linear-gradient(135deg, #d8dbde 0%, #eef1f4 100%)',
  'linear-gradient(135deg, #ddd5ce 0%, #f6f1eb 100%)',
]

const displayItems = computed(() => {
  const source = allModels.value || []
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return source
  return source.filter((m) =>
    (m.description || '').toLowerCase().includes(q) ||
    (m.authorName || '').toLowerCase().includes(q) ||
    (m.tags || '').toLowerCase().includes(q)
  )
})

// ===== 总览区 computed =====
const activeItem = computed(() => displayItems.value[currentIndex.value] || {})

const progressPercent = computed(() => {
  if (displayItems.value.length <= 1) return 100
  return ((currentIndex.value + 1) / displayItems.value.length) * 100
})

// ===== 杂志区 computed（独立索引 magIndex）=====
const magActiveItem = computed(() => displayItems.value[magIndex.value] || {})

const magActiveTags = computed(() => parseTags(magActiveItem.value.tags).slice(0, 3))

const magActiveTagsText = computed(() => {
  if (magActiveTags.value.length === 0) return 'CURATED SHARE'
  return magActiveTags.value.join(' · ')
})

const magActiveHeadline = computed(() => {
  const desc = (magActiveItem.value.description || '').trim()
  if (!desc) return 'Community Selection'
  return desc.length > 48 ? `${desc.slice(0, 48)}…` : desc
})

const magActiveSummary = computed(() => {
  const author = magActiveItem.value.authorName || 'Anonymous'
  const tagText = magActiveTags.value.length ? magActiveTags.value.join(', ') : 'community'
  return `A magazine-style selection from ${author}. Presented in the community gallery with a clean editorial layout and a focused visual story built around ${tagText}.`
})

const magProgressPercent = computed(() => {
  if (displayItems.value.length <= 1) return 100
  return ((magIndex.value + 1) / displayItems.value.length) * 100
})

// 杂志区右侧缩略图：magIndex 附近的几个
const nearbyItems = computed(() => {
  const items = displayItems.value
  if (items.length === 0) return []
  const ci = magIndex.value
  const result = []
  for (let i = ci - 1; i <= ci + 3; i++) {
    if (i >= 0 && i < items.length && i !== ci) {
      result.push({ ...items[i], _idx: i })
    }
  }
  return result
})

function parseTags(tags) {
  if (!tags) return []
  return tags.split(',').map(t => t.trim()).filter(Boolean)
}

function getDisplayImage(item) {
  return item?.posterImageUrl || item?.previewImageUrl || item?.designImageUrl || item?.poseImageUrl || item?.extraImageUrl || ''
}

function barH(n, value) {
  const seed = (n * 7 + (value || 0) * 3) % 17
  return 14 + (seed / 17) * 26
}

function stripCardClass(idx) {
  return {
    active: idx === currentIndex.value,
    left: idx < currentIndex.value,
    right: idx > currentIndex.value,
  }
}

function stripCardStyle(idx) {
  const distance = idx - currentIndex.value
  const abs = Math.abs(distance)
  const isActive = idx === currentIndex.value
  const activeHalfW = 150
  const inactiveHalfW = 55
  const gap = 6
  let translateX = 0
  if (distance === 0) {
    translateX = 0
  } else if (distance > 0) {
    translateX = activeHalfW + gap + (distance - 1) * (inactiveHalfW * 2 + gap) + inactiveHalfW
  } else {
    translateX = -(activeHalfW + gap + (-distance - 1) * (inactiveHalfW * 2 + gap) + inactiveHalfW)
  }
  const scale = isActive ? 1 : Math.max(0.72, 0.94 - abs * 0.06)
  const opacity = isActive ? 1 : Math.max(0.25, 0.7 - abs * 0.12)
  const zIndex = 100 - abs
  return {
    transform: `translate(-50%, -50%) translateX(${translateX}px) scale(${scale})`,
    opacity,
    zIndex,
  }
}

function selectFromStrip(idx) {
  if (idx === currentIndex.value) {
    // 点击当前主卡片 → 跳转详情
    goDetail(displayItems.value[idx]?.id)
    return
  }
  currentIndex.value = idx
}

// 总览区导航
function prev() {
  if (currentIndex.value > 0) currentIndex.value--
}
function next() {
  if (currentIndex.value < displayItems.value.length - 1) currentIndex.value++
}

// 杂志区导航
function magPrev() {
  if (magIndex.value > 0) magIndex.value--
}
function magNext() {
  if (magIndex.value < displayItems.value.length - 1) magIndex.value++
}

function goDetail(id) {
  if (id) router.push(`/community/${id}`)
}

function onSearch() {
  currentIndex.value = 0
}

function onKeydown(e) {
  if (e.key === 'ArrowLeft') prev()
  if (e.key === 'ArrowRight') next()
  if (e.key === 'Enter' && activeItem.value?.id) goDetail(activeItem.value.id)
}

function onWheel(e) {
  if (wheelLock) return
  wheelLock = true
  if (e.deltaY > 5 || e.deltaX > 5) next()
  if (e.deltaY < -5 || e.deltaX < -5) prev()
  setTimeout(() => { wheelLock = false }, 320)
}

function onTouchStart(e) {
  touchStartX = e.touches[0].clientX
  touchStartY = e.touches[0].clientY
}

function onTouchMove(e) {
  // prevent default handled by .prevent modifier
}

function onTouchEnd(e) {
  const dx = e.changedTouches[0].clientX - touchStartX
  if (Math.abs(dx) > 50) {
    if (dx > 0) prev()
    else next()
  }
}

function onMouseDown(e) {
  isDragging = true
  dragStartX = e.clientX
  const onMove = (ev) => {
    if (!isDragging) return
    const delta = ev.clientX - dragStartX
    if (Math.abs(delta) > 80) {
      if (delta > 0) prev()
      else next()
      cleanup()
    }
  }
  const onUp = () => cleanup()
  function cleanup() {
    isDragging = false
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', onUp)
  }
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
}

async function loadShares() {
  loading.value = true
  try {
    const res = await axios.get('/api/community/list')
    allModels.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    console.error(e)
    allModels.value = []
  }
  loading.value = false
}

function setupMagObserver() {
  if (magObserver) return // 已绑定
  if (!magSpreadRef.value) return
  magObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting && !magVisible.value) {
          magVisible.value = true
        }
      })
    },
    { threshold: 0.15 }
  )
  magObserver.observe(magSpreadRef.value)
}

onMounted(async () => {
  await loadShares()
  await nextTick()
  if (pageRef.value) pageRef.value.focus()
  setupMagObserver()
})

// 数据加载完后 magSpreadRef 才会渲染，watch 确保绑定
watch(magSpreadRef, (el) => {
  if (el) {
    nextTick(() => setupMagObserver())
  }
})

onUnmounted(() => {
  if (magObserver) {
    magObserver.disconnect()
    magObserver = null
  }
})
</script>

<style scoped>
/* ===== 全局容器 ===== */
.community-magazine {
  min-height: 100vh;
  background: var(--bg-primary);
  color: var(--text-primary);
  font-family: 'Inter', 'Helvetica Neue', Arial, sans-serif;
  outline: none;
}

/* ===== 顶部刊头 ===== */
.mag-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 68px 32px 12px;
  flex-shrink: 0;
}

.mag-brand {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.mag-brand-thin {
  font: 300 10px/1 'Inter', sans-serif;
  letter-spacing: 0.45em;
  color: var(--text-muted);
}

.mag-brand-bold {
  font: 800 clamp(24px, 3.5vw, 36px)/0.94 'Georgia', 'Times New Roman', serif;
  letter-spacing: -0.025em;
  color: var(--text-primary);
}

.mag-date {
  font: 400 10px/1 'Inter', sans-serif;
  letter-spacing: 0.26em;
  color: var(--text-muted);
}

.mag-search-btn {
  width: 36px;
  height: 36px;
  border: 1px solid var(--border-card);
  border-radius: 2px;
  background: var(--bg-card);
  color: var(--text-secondary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: 0.2s ease;
}

.mag-search-btn:hover {
  color: var(--text-primary);
  border-color: var(--text-muted);
  background: var(--bg-card);
}

/* ===== 搜索 ===== */
.mag-search-bar {
  padding: 0 32px 8px;
}

.mag-search-input {
  width: min(420px, 100%);
  height: 40px;
  padding: 0 16px;
  border: 1px solid var(--border-card);
  border-radius: 2px;
  background: var(--bg-card);
  color: var(--text-primary);
  font: 400 13px/1 'Inter', sans-serif;
  outline: none;
}

.mag-search-input::placeholder { color: var(--text-muted); }
.mag-search-input:focus { border-color: var(--text-muted); }

.search-fade-enter-active, .search-fade-leave-active { transition: all 0.2s ease; }
.search-fade-enter-from, .search-fade-leave-to { opacity: 0; transform: translateY(-6px); }

/* ===== 状态面板 ===== */
.mag-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--text-secondary);
  font: 400 13px/1.5 'Inter', sans-serif;
}

.mag-spinner {
  width: 22px; height: 22px;
  border: 2px solid var(--border-card);
  border-top-color: var(--text-secondary);
  border-radius: 50%;
  animation: spin 0.75s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.mag-empty-icon { font-size: 44px; color: var(--border-card); }
.mag-empty-sub { margin: 0; font-size: 12px; color: var(--text-muted); }

/* ===== 总览卡片条 ===== */
.gallery-hero {
  position: relative;
  height: min(58vh, 560px);
  margin-bottom: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.gallery-hero-inner {
  position: relative;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.gallery-strip-card {
  position: absolute;
  top: 50%;
  left: 50%;
  width: clamp(78px, 10vw, 110px);
  height: clamp(320px, 48vh, 470px);
  transform-origin: center center;
  transition: transform 0.45s cubic-bezier(0.22, 1, 0.36, 1),
              opacity 0.35s ease,
              filter 0.35s ease,
              width 0.45s cubic-bezier(0.22, 1, 0.36, 1),
              height 0.45s cubic-bezier(0.22, 1, 0.36, 1);
  cursor: pointer;
}

.gallery-strip-card.active {
  width: clamp(180px, 22vw, 300px);
  height: clamp(360px, 54vh, 520px);
}

.gallery-strip-card.left,
.gallery-strip-card.right {
  filter: grayscale(0.35);
}

.gallery-strip-image {
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: var(--bg-secondary);
  box-shadow: 0 16px 38px rgba(20,20,20,0.08);
}

.gallery-strip-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  user-select: none;
  -webkit-user-drag: none;
}

.gallery-strip-card.active .gallery-strip-image img {
  object-fit: contain;
  background: var(--bg-card);
}

.gallery-strip-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.gallery-strip-placeholder span {
  font-size: 42px;
  color: var(--border-card);
}

/* 邮戳标签 */
.gallery-stamp-label {
  position: absolute;
  right: -8px;
  top: 18px;
  transform: rotate(12deg);
  z-index: 10;
  pointer-events: none;
}

.stamp-inner {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border: 2.5px double var(--text-primary);
  border-radius: 50%;
  background: var(--bg-card);
  box-shadow: 0 2px 12px rgba(0,0,0,0.12);
}

.stamp-border-ring {
  position: absolute;
  inset: 3px;
  border: 1px dashed var(--text-muted);
  border-radius: 50%;
}

.stamp-text {
  font: 800 8px/1 'Inter', sans-serif;
  letter-spacing: 0.18em;
  color: var(--text-primary);
  text-transform: uppercase;
  max-width: 52px;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stamp-sub {
  font: 600 7px/1 'Inter', sans-serif;
  letter-spacing: 0.12em;
  color: var(--text-secondary);
  margin-top: 2px;
}

/* 超大半透明页码 */
.gallery-big-index {
  position: absolute;
  left: -18px;
  bottom: -10px;
  font: 900 96px/1 'Georgia', 'Times New Roman', serif;
  color: var(--border-card);
  letter-spacing: -0.04em;
  pointer-events: none;
  z-index: 1;
  user-select: none;
}

/* 总览导航箭头 */
.gallery-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 42px;
  height: 42px;
  border: 1px solid var(--border-card);
  border-radius: 2px;
  background: var(--bg-card);
  color: var(--text-secondary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: 0.2s ease;
  z-index: 200;
}

.gallery-nav:hover {
  background: var(--bg-card);
  color: var(--text-primary);
  border-color: var(--text-muted);
}

.gallery-nav-left { left: 4px; }
.gallery-nav-right { right: 4px; }

/* 总览进度 */
.gallery-hero-footer {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 16px;
  padding: 0 32px;
}

.hero-progress-line {
  flex: 1;
  height: 1px;
  background: var(--border-card);
  overflow: hidden;
}

.hero-progress-fill {
  height: 100%;
  background: var(--text-secondary);
  transition: width 0.35s ease;
}

.hero-counter {
  display: flex;
  align-items: baseline;
  gap: 3px;
}

.hero-counter-current {
  font: 700 18px/1 'Georgia', 'Times New Roman', serif;
  color: var(--text-primary);
}

.hero-counter-sep,
.hero-counter-total {
  font: 400 12px/1 'Inter', sans-serif;
  color: var(--text-muted);
}

/* ===== 衔接过渡区 ===== */
.section-bridge {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 28px 32px;
}

.bridge-line {
  flex: 1;
  height: 0.5px;
  background: var(--border-card);
}

.bridge-label {
  font: 700 9px/1 'Inter', sans-serif;
  letter-spacing: 0.35em;
  color: var(--text-muted);
}

/* ===== 杂志详情布局 ===== */
.mag-spread {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) minmax(300px, 1.4fr) minmax(160px, 0.6fr);
  gap: 0;
  padding: 12px 32px 40px;
  min-height: 50vh;
}

/* ===== 左侧文字区 ===== */
.mag-left {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding-right: 28px;
  overflow: hidden;
}

.mag-kicker {
  font: 700 9px/1 'Inter', sans-serif;
  letter-spacing: 0.3em;
  color: var(--text-muted);
  margin-bottom: 12px;
}

.mag-title {
  margin: 0 0 14px;
  font: 700 clamp(24px, 3.2vw, 48px)/1.02 'Georgia', 'Times New Roman', serif;
  letter-spacing: -0.03em;
  color: var(--text-primary);
  max-width: 12ch;
  word-break: break-word;
}

.mag-divider {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.mag-divider-line {
  flex: 1;
  height: 0.5px;
  background: var(--border-card);
}

.mag-divider-dot {
  font-size: 5px;
  color: var(--text-muted);
}

.mag-desc-wrap {
  border-left: 2px solid var(--border-card);
  padding-left: 14px;
  margin-bottom: 16px;
}

.mag-desc {
  margin: 0;
  font: 400 12px/1.75 'Inter', sans-serif;
  color: var(--text-secondary);
  max-width: 36ch;
}

.mag-meta-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
}

.mag-meta-item {
  font: 500 9px/1 'Inter', sans-serif;
  letter-spacing: 0.2em;
  color: var(--text-muted);
  text-transform: uppercase;
}

.mag-meta-sep {
  color: var(--border-card);
  font-size: 10px;
}

.mag-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 14px;
}

.mag-tag {
  padding: 5px 10px;
  border: 1px solid var(--border-card);
  border-radius: 2px;
  background: var(--bg-card);
  font: 500 10px/1 'Inter', sans-serif;
  color: var(--text-secondary);
}

/* 条形码 */
.mag-barcode-row {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
}

.mag-barcode-stat {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.mag-barcode-bars {
  display: flex;
  align-items: flex-end;
  gap: 1.5px;
  height: 36px;
}

.mag-bar {
  display: block;
  width: 2px;
  background: var(--text-primary);
  border-radius: 0.5px;
  min-height: 6px;
}

.mag-barcode-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.mag-barcode-label {
  font: 700 7px/1 'Inter', sans-serif;
  letter-spacing: 0.22em;
  color: var(--text-muted);
}

.mag-barcode-value {
  font: 700 16px/1 'Georgia', 'Times New Roman', serif;
  color: var(--text-primary);
}

.mag-view-btn {
  align-self: flex-start;
  min-width: 130px;
  height: 40px;
  padding: 0 20px;
  border: 1.5px solid var(--text-primary);
  border-radius: 2px;
  background: var(--text-primary);
  color: var(--bg-primary);
  font: 700 11px/1 'Inter', sans-serif;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  cursor: pointer;
  transition: 0.2s ease;
}

.mag-view-btn:hover {
  background: transparent;
  color: var(--text-primary);
}

/* ===== 中间主视觉 ===== */
.mag-center {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.mag-hero-card {
  position: relative;
  width: 100%;
  max-width: 480px;
  aspect-ratio: 3/4;
  background: var(--bg-card);
  box-shadow: 0 20px 60px rgba(0,0,0,0.1), 0 0 0 1px var(--border-card);
  overflow: visible;
  transition: transform 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}

.mag-hero-card:hover {
  transform: translateY(-4px);
}

.mag-hero-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
  background: var(--bg-card);
  user-select: none;
  -webkit-user-drag: none;
}

.mag-hero-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ebe7e0, #f3efe9);
}

.mag-hero-placeholder span {
  font-size: 48px;
  color: var(--border-card);
}

/* 邮戳 */
.mag-stamp {
  position: absolute;
  right: -16px;
  top: -16px;
  transform: rotate(12deg);
  z-index: 10;
  pointer-events: none;
}

.mag-stamp-inner {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 76px;
  height: 76px;
  border: 2.5px double var(--text-primary);
  border-radius: 50%;
  background: var(--bg-card);
  box-shadow: 0 2px 14px rgba(0,0,0,0.1);
}

.mag-stamp-ring {
  position: absolute;
  inset: 3px;
  border: 1px dashed var(--text-muted);
  border-radius: 50%;
}

.mag-stamp-text {
  font: 800 8px/1 'Inter', sans-serif;
  letter-spacing: 0.16em;
  color: var(--text-primary);
  text-transform: uppercase;
  max-width: 54px;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mag-stamp-sub {
  font: 600 7px/1 'Inter', sans-serif;
  letter-spacing: 0.1em;
  color: var(--text-secondary);
  margin-top: 2px;
}

/* 超大页码 */
.mag-big-num {
  position: absolute;
  left: -20px;
  bottom: -16px;
  font: 900 100px/1 'Georgia', 'Times New Roman', serif;
  color: var(--border-card);
  letter-spacing: -0.04em;
  pointer-events: none;
  user-select: none;
  z-index: 1;
}

/* 导航箭头 */
.mag-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  border: 1px solid var(--border-card);
  border-radius: 2px;
  background: var(--bg-card);
  color: var(--text-secondary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: 0.2s ease;
  z-index: 20;
}

.mag-nav:hover {
  background: var(--bg-card);
  color: var(--text-primary);
  border-color: var(--text-muted);
}

.mag-nav-left { left: -20px; }
.mag-nav-right { right: -20px; }

/* ===== 右侧缩略图 ===== */
.mag-right {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding-left: 24px;
  gap: 16px;
  overflow: hidden;
}

.mag-thumb-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.mag-thumb {
  position: relative;
  width: 100%;
  aspect-ratio: 3/4;
  max-height: 140px;
  overflow: hidden;
  cursor: pointer;
  opacity: 0.6;
  transition: opacity 0.25s, transform 0.25s, box-shadow 0.25s;
  box-shadow: 0 4px 16px rgba(0,0,0,0.06);
  border: 1px solid var(--border-card);
}

.mag-thumb:hover {
  opacity: 0.85;
  transform: translateY(-2px);
}

.mag-thumb.active {
  opacity: 1;
  box-shadow: 0 4px 20px rgba(0,0,0,0.12);
  outline: 2px solid var(--text-primary);
  outline-offset: 2px;
}

.mag-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  user-select: none;
  -webkit-user-drag: none;
}

.mag-thumb-placeholder {
  width: 100%;
  height: 100%;
}

.mag-thumb-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: flex-end;
  justify-content: flex-start;
  padding: 6px 8px;
  background: linear-gradient(to top, rgba(0,0,0,0.25) 0%, transparent 50%);
  pointer-events: none;
}

.mag-thumb-num {
  font: 700 11px/1 'Georgia', 'Times New Roman', serif;
  color: rgba(255,255,255,0.85);
}

/* 进度条 */
.mag-progress {
  display: flex;
  align-items: center;
  gap: 10px;
}

.mag-progress-bar {
  flex: 1;
  height: 1px;
  background: var(--border-card);
  overflow: hidden;
}

.mag-progress-fill {
  height: 100%;
  background: var(--text-secondary);
  transition: width 0.35s ease;
}

.mag-counter {
  display: flex;
  align-items: baseline;
  gap: 3px;
}

.mag-counter-cur {
  font: 700 16px/1 'Georgia', 'Times New Roman', serif;
  color: var(--text-primary);
}

.mag-counter-sep,
.mag-counter-total {
  font: 400 11px/1 'Inter', sans-serif;
  color: var(--text-muted);
}

/* ===== 杂志区入场动画 ===== */
.mag-anim {
  opacity: 0;
  will-change: transform, opacity;
}

/* 中间图片：从下方滑入 */
.mag-anim-center {
  transform: translateY(60px);
}

/* 左侧文字：从左侧滑入 */
.mag-anim-left {
  transform: translateX(-40px);
}

/* 右侧缩略图：从右侧滑入 */
.mag-anim-right {
  transform: translateX(40px);
}

/* 当 mag-spread 进入视口时触发动画 */
.mag-visible .mag-anim-center {
  animation: magSlideUp 0.7s cubic-bezier(0.22, 1, 0.36, 1) 0.05s forwards;
}

.mag-visible .mag-anim-left {
  animation: magSlideFromLeft 0.65s cubic-bezier(0.22, 1, 0.36, 1) 0.45s forwards;
}

.mag-visible .mag-anim-right {
  animation: magSlideFromRight 0.6s cubic-bezier(0.22, 1, 0.36, 1) 0.7s forwards;
}

@keyframes magSlideUp {
  from {
    opacity: 0;
    transform: translateY(60px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes magSlideFromLeft {
  from {
    opacity: 0;
    transform: translateX(-40px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes magSlideFromRight {
  from {
    opacity: 0;
    transform: translateX(40px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* ===== 响应式 ===== */
@media (max-width: 1024px) {
  .mag-spread {
    grid-template-columns: minmax(200px, 1fr) minmax(260px, 1.3fr) minmax(120px, 0.5fr);
    padding: 12px 20px 24px;
  }

  .mag-left { padding-right: 18px; }
  .mag-title { font-size: clamp(22px, 3vw, 36px); }
  .mag-stamp-inner { width: 60px; height: 60px; }
  .mag-stamp-text { font-size: 7px; max-width: 42px; }
  .mag-big-num { font-size: 72px; }
}

@media (max-width: 768px) {
  .mag-header { padding: 60px 16px 10px; }

  .gallery-hero { height: min(52vh, 500px); }
  .gallery-strip-card { width: 66px; height: clamp(250px, 42vh, 360px); }
  .gallery-strip-card.active { width: min(42vw, 220px); height: clamp(300px, 48vh, 420px); }
  .gallery-stamp-label { right: -4px; top: 12px; }
  .stamp-inner { width: 56px; height: 56px; }
  .stamp-text { font-size: 7px; max-width: 40px; }
  .stamp-sub { font-size: 6px; }
  .gallery-big-index { font-size: 64px; left: -10px; bottom: -6px; }
  .gallery-hero-footer { padding: 0 16px; }
  .section-bridge { padding: 20px 16px; }

  .mag-spread {
    grid-template-columns: 1fr;
    grid-template-rows: auto 1fr auto;
    padding: 8px 16px 20px;
    gap: 16px;
  }

  .mag-left { padding-right: 0; order: 2; }
  .mag-center { order: 1; }
  .mag-right { order: 3; padding-left: 0; flex-direction: row; align-items: center; gap: 12px; }
  .mag-thumb-list { flex-direction: row; gap: 8px; overflow-x: auto; }
  .mag-thumb { min-width: 80px; max-height: 110px; }
  .mag-hero-card { max-width: 100%; aspect-ratio: 4/5; }
  .mag-nav-left { left: 8px; }
  .mag-nav-right { right: 8px; }
  .mag-stamp { right: -8px; top: -10px; }
  .mag-stamp-inner { width: 56px; height: 56px; }
  .mag-stamp-text { font-size: 6px; max-width: 38px; }
  .mag-stamp-sub { font-size: 5px; }
  .mag-big-num { font-size: 60px; left: -10px; bottom: -10px; }
  .mag-title { max-width: none; font-size: clamp(24px, 7vw, 38px); }
  .mag-barcode-row { gap: 16px; }
  .mag-view-btn { width: 100%; }
}

@media (max-width: 480px) {
  .mag-header { padding: 56px 12px 8px; }
  .mag-brand-bold { font-size: 26px; }
  .gallery-hero { height: 46vh; }
  .gallery-strip-card { width: 56px; height: 250px; }
  .gallery-strip-card.active { width: 42vw; max-width: 180px; height: 300px; }
  .section-bridge { padding: 16px 12px; }
  .mag-spread { padding: 6px 12px 16px; }
  .mag-thumb { min-width: 64px; max-height: 90px; }
  .mag-hero-card { aspect-ratio: 3/4; }
}
</style>