
<template>
  <div class="cd-page">
    <!-- Loading -->
    <div v-if="loading" class="cd-loading">
      <span class="cd-spinner"></span>
      <span>Loading...</span>
    </div>

    <template v-else>
      <div class="cd-layout">

        <!-- ====== 左侧导航栏 ====== -->
        <aside class="cd-sidebar">
          <button class="cd-back" @click="$router.back()" aria-label="go back">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
          </button>

          <div class="cd-brand">
            <span class="cd-brand-sub">NFC</span>
            <span class="cd-brand-main">STORY</span>
          </div>

          <nav class="cd-tabs">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              class="cd-tab"
              :class="{ active: activeTab === tab.key }"
              @click="activeTab = tab.key"
            >
              <span class="cd-tab-icon">{{ tab.icon }}</span>
              <span class="cd-tab-label">{{ tab.label }}</span>
            </button>
          </nav>

          <!-- 底部作者信息 -->
          <div class="cd-sidebar-footer">
            <div class="cd-author-avatar">{{ (share.authorName || 'U').charAt(0) }}</div>
            <span class="cd-author-label">{{ share.authorName || 'Anonymous' }}</span>
          </div>
        </aside>

        <!-- ====== 右侧内容面板 ====== -->
        <main class="cd-main">

          <!-- Tab: OVERVIEW -->
          <section v-if="activeTab === 'overview'" class="cd-panel cd-overview">
            <div class="cd-panel-header">
              <span class="cd-panel-kicker">OVERVIEW</span>
              <div class="cd-panel-divider"></div>
            </div>

            <!-- Poster 翻转卡 -->
            <div class="cd-poster-area" v-if="share.posterImageUrl">
              <div class="cd-flip-card" :class="{ flipped: posterFlipped }" @click="posterFlipped = !posterFlipped">
                <div class="cd-flip-face cd-flip-front">
                  <img :src="share.posterImageUrl" alt="Poster" class="cd-poster-img" />
                  <div class="cd-poster-badge">POSTER</div>
                  <div class="cd-flip-hint">TAP TO FLIP</div>
                </div>
                <div class="cd-flip-face cd-flip-back">
                  <div class="cd-back-stamp">
                    <div class="cd-back-stamp-circle">
                      <span class="cd-stamp-top">NFC CHARACTER</span>
                      <span class="cd-stamp-date">{{ posterStampDate }}</span>
                      <span class="cd-stamp-bottom">VOL. {{ share.versionId || share.id }}</span>
                    </div>
                  </div>
                  <div class="cd-back-notes">
                    <div class="cd-back-note"><span>Created</span><strong>{{ posterStampDate }}</strong></div>
                    <div class="cd-back-note"><span>View</span><strong>{{ posterViewLabel }}</strong></div>
                  </div>
                  <div class="cd-back-brand">NFC CHARACTER</div>
                  <div class="cd-flip-hint cd-flip-hint-back">TAP TO FLIP</div>
                </div>
              </div>
            </div>

            <!-- 描述 + 数据 -->
            <div class="cd-overview-info">
              <h1 class="cd-title">{{ share.description || 'No description' }}</h1>
              <div class="cd-desc-divider">
                <span class="cd-desc-line"></span>
                <span class="cd-desc-dot">◆</span>
                <span class="cd-desc-line"></span>
              </div>
              <p class="cd-summary">A magazine-style selection from {{ share.authorName || 'Anonymous' }}. Presented in the community gallery with a clean editorial layout and a focused visual story.</p>

              <div class="cd-tags" v-if="parsedTags.length > 0">
                <span v-for="tag in parsedTags" :key="tag" class="cd-tag">{{ tag }}</span>
              </div>

              <!-- 条形码数据 -->
              <div class="cd-barcode-row">
                <div class="cd-barcode-stat">
                  <div class="cd-barcode-bars">
                    <span v-for="n in 14" :key="'l'+n" class="cd-bar" :style="{ height: barH(n, share.likesCount || 0) + 'px' }"></span>
                  </div>
                  <div class="cd-barcode-info">
                    <span class="cd-barcode-label">LIKES</span>
                    <span class="cd-barcode-value">{{ share.likesCount || 0 }}</span>
                  </div>
                </div>
                <div class="cd-barcode-stat">
                  <div class="cd-barcode-bars">
                    <span v-for="n in 14" :key="'r'+n" class="cd-bar" :style="{ height: barH(n, (share.rating || 0) * 20) + 'px' }"></span>
                  </div>
                  <div class="cd-barcode-info">
                    <span class="cd-barcode-label">RATING</span>
                    <span class="cd-barcode-value">{{ share.rating || 0 }}/5</span>
                  </div>
                </div>
              </div>

              <!-- 评分 + 点赞 -->
              <div class="cd-interact-row">
                <div class="cd-stars">
                  <span v-for="s in 5" :key="s" class="cd-star" :class="{ filled: s <= (share.rating || 0) }" @click="setRating(s)">★</span>
                </div>
                <button class="cd-like-btn" :class="{ liked: isLiked }" @click="toggleLike">
                  <svg width="15" height="15" viewBox="0 0 24 24" :fill="isLiked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                  <span>{{ share.likesCount || 0 }}</span>
                </button>
              </div>

              <!-- 编辑按钮 -->
              <button v-if="share.characterId && share.versionId" class="cd-edit-btn" @click="goToEditor">
                ✏️ 进入编辑界面
              </button>
            </div>
          </section>

          <!-- Tab: GALLERY -->
          <section v-if="activeTab === 'gallery'" class="cd-panel cd-gallery">
            <div class="cd-panel-header">
              <span class="cd-panel-kicker">GALLERY</span>
              <div class="cd-panel-divider"></div>
            </div>

            <div class="cd-gallery-grid">
              <div
                v-for="(img, i) in carouselImages"
                :key="i"
                class="cd-gallery-item"
                :class="{ large: i === 0 }"
                @click="lightboxIdx = i"
              >
                <img v-if="img" :src="img" alt="" />
                <div v-else class="cd-gallery-placeholder"></div>
                <span class="cd-gallery-num">{{ String(i + 1).padStart(2, '0') }}</span>
              </div>
            </div>

            <!-- 3D Model -->
            <div class="cd-model-area" v-if="share.modelUrl">
              <div class="cd-model-label">3D MODEL</div>
              <div class="cd-model-viewer" @click="openModelView">
                <ModelViewer :modelUrl="share.modelUrl" />
              </div>
              <span class="cd-model-hint">Click for fullscreen</span>
            </div>

            <!-- Lightbox -->
            <Teleport to="body">
              <div v-if="lightboxIdx !== null" class="cd-lightbox" @click="lightboxIdx = null">
                <img v-if="carouselImages[lightboxIdx]" :src="carouselImages[lightboxIdx]" alt="" class="cd-lightbox-img" @click.stop />
                <button class="cd-lightbox-close" @click="lightboxIdx = null">✕</button>
              </div>
            </Teleport>
          </section>

          <!-- Tab: PROMPTS -->
          <section v-if="activeTab === 'prompts'" class="cd-panel cd-prompts-panel">
            <div class="cd-panel-header">
              <span class="cd-panel-kicker">PROMPTS</span>
              <div class="cd-panel-divider"></div>
            </div>

            <div class="cd-prompts-list">
              <div class="cd-prompt-card" v-if="share.characterPrompt">
                <div class="cd-prompt-head"><span>👤</span><span>Character Prompt</span></div>
                <p class="cd-prompt-body">{{ share.characterPrompt }}</p>
              </div>
              <div class="cd-prompt-card" v-if="share.scenePrompt">
                <div class="cd-prompt-head"><span>🌄</span><span>Scene Prompt</span></div>
                <p class="cd-prompt-body">{{ share.scenePrompt }}</p>
              </div>
              <div class="cd-prompt-card" v-if="share.actionPrompt">
                <div class="cd-prompt-head"><span>🎬</span><span>Action Prompt</span></div>
                <p class="cd-prompt-body">{{ share.actionPrompt }}</p>
              </div>
              <div class="cd-prompt-card cd-prompt-empty" v-if="!share.characterPrompt && !share.scenePrompt && !share.actionPrompt">
                <p class="cd-prompt-body muted">No prompts available for this creation.</p>
              </div>
            </div>
          </section>

          <!-- Tab: PARAMETERS -->
          <section v-if="activeTab === 'params'" class="cd-panel cd-params-panel">
            <div class="cd-panel-header">
              <span class="cd-panel-kicker">PARAMETERS</span>
              <div class="cd-panel-divider"></div>
            </div>

            <!-- Info chips -->
            <div class="cd-chips">
              <div class="cd-chip">
                <span class="cd-chip-label">Version</span>
                <span class="cd-chip-val">#{{ share.versionId || '—' }}</span>
              </div>
              <div class="cd-chip" v-if="share.versionStatus">
                <span class="cd-chip-label">Status</span>
                <span class="cd-chip-val accent">{{ share.versionStatus }}</span>
              </div>
              <div class="cd-chip" v-if="share.has3D">
                <span class="cd-chip-label">3D</span>
                <span class="cd-chip-val accent">✓ Available</span>
              </div>
              <div class="cd-chip">
                <span class="cd-chip-label">Author</span>
                <span class="cd-chip-val">{{ share.authorName || 'Anonymous' }}</span>
              </div>
            </div>

            <!-- Expression grid -->
            <div class="cd-exp-section">
              <div class="cd-section-label">Expression Parameters</div>
              <div class="cd-exp-grid">
                <div class="cd-exp-card" v-for="(val, i) in expressionValues" :key="i">
                  <span class="cd-exp-label">E{{ i + 1 }}</span>
                  <span class="cd-exp-value">{{ val ?? 0 }}</span>
                </div>
              </div>
            </div>

            <!-- Reference images -->
            <div class="cd-ref-section" v-if="share.designImageUrl || share.poseImageUrl || share.extraImageUrl">
              <div class="cd-section-label">Reference Images</div>
              <div class="cd-ref-grid">
                <div class="cd-ref-thumb" v-if="share.designImageUrl">
                  <img :src="share.designImageUrl" alt="Design" />
                  <span class="cd-ref-tag">Design</span>
                </div>
                <div class="cd-ref-thumb" v-if="share.poseImageUrl">
                  <img :src="share.poseImageUrl" alt="Pose" />
                  <span class="cd-ref-tag">Pose</span>
                </div>
                <div class="cd-ref-thumb" v-if="share.extraImageUrl">
                  <img :src="share.extraImageUrl" alt="Extra" />
                  <span class="cd-ref-tag">Extra</span>
                </div>
              </div>
            </div>
          </section>

          <!-- Tab: COMMENTS -->
          <section v-if="activeTab === 'comments'" class="cd-panel cd-comments-panel">
            <div class="cd-panel-header">
              <span class="cd-panel-kicker">COMMENTS</span>
              <span class="cd-panel-count">{{ comments.length }}</span>
              <div class="cd-panel-divider"></div>
            </div>

            <div v-if="comments.length === 0" class="cd-empty-comments">
              No comments yet. Be the first to share your thoughts.
            </div>

            <div class="cd-comment-list">
              <div v-for="comment in comments" :key="comment.id" class="cd-comment-item">
                <div class="cd-comment-row">
                  <div class="cd-comment-avatar">{{ (comment.authorName || 'U').charAt(0) }}</div>
                  <div class="cd-comment-body">
                    <span class="cd-comment-author">{{ comment.authorName }}</span>
                    <p class="cd-comment-text">{{ comment.content }}</p>

                    <!-- Replies -->
                    <div v-if="comment.replies && comment.replies.length" class="cd-replies">
                      <div v-for="reply in comment.replies" :key="reply.id" class="cd-reply-item">
                        <div class="cd-comment-avatar small">{{ (reply.authorName || 'U').charAt(0) }}</div>
                        <div class="cd-comment-body">
                          <span class="cd-comment-author">{{ reply.authorName }}</span>
                          <p class="cd-comment-text">{{ reply.content }}</p>
                        </div>
                        <button class="cd-comment-like" @click="likeComment(reply.id)">❤ {{ reply.likesCount || 0 }}</button>
                      </div>
                    </div>
                  </div>
                  <div class="cd-comment-actions">
                    <button class="cd-comment-like" @click="likeComment(comment.id)">❤ {{ comment.likesCount || 0 }}</button>
                    <button class="cd-reply-trigger" @click="replyTo = comment.id">Reply</button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 评论输入 -->
            <div class="cd-comment-input-area">
              <input
                v-model="newComment"
                :placeholder="replyTo ? 'Reply to comment...' : 'Write a comment...'"
                class="cd-comment-input"
                @keyup.enter="submitComment"
              />
              <button v-if="replyTo" class="cd-cancel-reply" @click="replyTo = null">✕</button>
              <button class="cd-send-btn" @click="submitComment" :disabled="!newComment.trim()">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z"/></svg>
              </button>
            </div>
          </section>

        </main>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import ModelViewer from '../components/ModelViewer.vue'

const route = useRoute()
const router = useRouter()
const shareId = route.params.id
const userId = localStorage.getItem('userId') || 1

const loading = ref(true)
const activeTab = ref('overview')
const replyTo = ref(null)
const newComment = ref('')
const isLiked = ref(false)
const posterFlipped = ref(false)
const lightboxIdx = ref(null)

const share = ref({})
const comments = ref([])

const tabs = [
  { key: 'overview', label: 'OVERVIEW', icon: '◎' },
  { key: 'gallery', label: 'GALLERY', icon: '◫' },
  { key: 'prompts', label: 'PROMPTS', icon: '✎' },
  { key: 'params', label: 'PARAMS', icon: '⚙' },
  { key: 'comments', label: 'COMMENTS', icon: '💬' },
]

const parsedTags = computed(() => {
  if (!share.value.tags) return []
  return share.value.tags.split(',').map(t => t.trim()).filter(t => t)
})

const carouselImages = computed(() => {
  const imgs = []
  if (share.value.posterImageUrl) imgs.push(share.value.posterImageUrl)
  if (share.value.previewImageUrl) imgs.push(share.value.previewImageUrl)
  if (share.value.designImageUrl) imgs.push(share.value.designImageUrl)
  if (share.value.poseImageUrl) imgs.push(share.value.poseImageUrl)
  if (share.value.extraImageUrl) imgs.push(share.value.extraImageUrl)
  if (imgs.length === 0) imgs.push(null)
  return imgs
})

const expressionValues = computed(() => {
  const s = share.value
  return [s.expression1, s.expression2, s.expression3, s.expression4, s.expression5, s.expression6, s.expression7]
})

const posterStampDate = computed(() => {
  const raw = share.value.createdAt || share.value.createTime || share.value.createdTime || share.value.updatedAt
  const date = raw ? new Date(raw) : new Date()
  if (Number.isNaN(date.getTime())) return '----.--.--'
  return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`
})

const posterViewLabel = computed(() => {
  const tags = parsedTags.value.map(tag => tag.toLowerCase())
  const viewTag = ['front', 'back', 'left', 'right', 'top', 'bottom'].find(view => tags.includes(view))
  return viewTag ? viewTag.charAt(0).toUpperCase() + viewTag.slice(1) : 'Poster'
})

function barH(n, value) {
  const seed = (n * 7 + (value || 0) * 3) % 17
  return 12 + (seed / 17) * 28
}

const loadDetail = async () => {
  loading.value = true
  try {
    const res = await axios.get(`/api/community/detail?shareId=${shareId}&userId=${userId}`)
    share.value = res.data
    isLiked.value = res.data.liked || false
  } catch (e) { console.error(e) }
  loading.value = false
}

const loadComments = async () => {
  try {
    const res = await axios.get(`/api/community/comments?shareId=${shareId}`)
    comments.value = res.data
  } catch (e) { console.error(e) }
}

const toggleLike = async () => {
  try {
    const res = await axios.post(`/api/community/like?shareId=${shareId}&userId=${userId}`)
    isLiked.value = res.data.liked
    share.value.likesCount = res.data.likesCount
  } catch (e) { console.error(e) }
}

const setRating = async (val) => {
  try {
    await axios.post(`/api/community/rate?shareId=${shareId}&rating=${val}`)
    share.value.rating = val
  } catch (e) { console.error(e) }
}

const submitComment = async () => {
  if (!newComment.value.trim()) return
  try {
    const params = { shareId, userId, authorName: 'User', content: newComment.value }
    if (replyTo.value) params.parentId = replyTo.value
    await axios.post('/api/community/comment', null, { params })
    newComment.value = ''
    replyTo.value = null
    await loadComments()
  } catch (e) { console.error(e) }
}

const openModelView = () => {
  if (share.value.versionId) router.push(`/model-view/${share.value.versionId}`)
}

const goToEditor = () => {
  if (share.value.characterId && share.value.versionId) {
    router.push(`/editor/${share.value.characterId}?versionId=${share.value.versionId}`)
  }
}

const likeComment = async (commentId) => {
  try {
    await axios.post(`/api/community/comment/like?commentId=${commentId}`)
    await loadComments()
  } catch (e) { console.error(e) }
}

onMounted(() => {
  loadDetail()
  loadComments()
})
</script>

<style scoped>
/* ===== 全局 ===== */
.cd-page {
  min-height: 100vh;
  background: var(--bg-primary);
  color: var(--text-primary);
  font-family: 'Inter', 'Helvetica Neue', Arial, sans-serif;
}

.cd-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  gap: 12px;
  color: var(--text-secondary);
  font-size: 13px;
}

.cd-spinner {
  width: 20px; height: 20px;
  border: 2px solid var(--border-card);
  border-top-color: var(--text-secondary);
  border-radius: 50%;
  animation: cdspin 0.75s linear infinite;
}
@keyframes cdspin { to { transform: rotate(360deg); } }

/* ===== 布局 ===== */
.cd-layout {
  display: flex;
  min-height: 100vh;
}

/* ===== 左侧边栏 ===== */
.cd-sidebar {
  width: clamp(180px, 18vw, 260px);
  min-height: 100vh;
  background: var(--bg-secondary);
  border-right: 1px solid var(--border-card);
  display: flex;
  flex-direction: column;
  padding: 60px 20px 24px;
  flex-shrink: 0;
  position: sticky;
  top: 0;
  height: 100vh;
  overflow-y: auto;
}

.cd-back {
  width: 32px; height: 32px;
  border: 1px solid var(--border-card);
  border-radius: 2px;
  background: var(--bg-card);
  color: var(--text-secondary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: 0.2s;
  margin-bottom: 28px;
}
.cd-back:hover { color: var(--text-primary); border-color: var(--text-muted); background: var(--bg-card); }

.cd-brand {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: 36px;
}
.cd-brand-sub {
  font: 300 9px/1 'Inter', sans-serif;
  letter-spacing: 0.5em;
  color: var(--text-muted);
}
.cd-brand-main {
  font: 800 clamp(20px, 2.5vw, 28px)/0.94 'Georgia', 'Times New Roman', serif;
  letter-spacing: -0.02em;
  color: var(--text-primary);
}

/* Tabs */
.cd-tabs {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}

.cd-tab {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border: none;
  border-radius: 0;
  background: transparent;
  color: var(--text-muted);
  font: 600 10px/1 'Inter', sans-serif;
  letter-spacing: 0.2em;
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
  position: relative;
}

.cd-tab::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 2px;
  height: 0;
  background: var(--text-primary);
  transition: height 0.25s ease;
}

.cd-tab:hover {
  color: var(--text-secondary);
  background: var(--bg-card);
}

.cd-tab.active {
  color: var(--text-primary);
  background: var(--bg-card);
}

.cd-tab.active::before {
  height: 60%;
}

.cd-tab-icon {
  font-size: 13px;
  width: 18px;
  text-align: center;
}

.cd-tab-label {
  white-space: nowrap;
}

/* Sidebar footer */
.cd-sidebar-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-top: 20px;
  border-top: 1px solid var(--border-card);
  margin-top: auto;
}

.cd-author-avatar {
  width: 28px; height: 28px;
  border-radius: 2px;
  background: var(--text-primary);
  color: var(--bg-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font: 600 11px/1 'Inter', sans-serif;
}

.cd-author-label {
  font: 500 11px/1 'Inter', sans-serif;
  color: var(--text-secondary);
  letter-spacing: 0.04em;
}

/* ===== 右侧主面板 ===== */
.cd-main {
  flex: 1;
  min-height: 100vh;
  overflow-y: auto;
  padding: 60px 40px 40px;
}

.cd-panel-header {
  margin-bottom: 24px;
}

.cd-panel-kicker {
  font: 700 10px/1 'Inter', sans-serif;
  letter-spacing: 0.35em;
  color: var(--text-muted);
}

.cd-panel-count {
  font: 700 10px/1 'Inter', sans-serif;
  letter-spacing: 0.1em;
  color: var(--text-muted);
  margin-left: 8px;
}

.cd-panel-divider {
  height: 0.5px;
  background: var(--border-card);
  margin-top: 10px;
}

/* ===== OVERVIEW ===== */
.cd-overview {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.cd-poster-area {
  perspective: 1200px;
  max-width: 560px;
}

.cd-flip-card {
  position: relative;
  width: 100%;
  aspect-ratio: 1.5/1;
  transform-style: preserve-3d;
  transition: transform 0.7s cubic-bezier(0.4,0,0.2,1);
  cursor: pointer;
}
.cd-flip-card.flipped { transform: rotateY(180deg); }

.cd-flip-face {
  position: absolute;
  inset: 0;
  backface-visibility: hidden;
  overflow: hidden;
  box-shadow: 0 16px 48px rgba(0,0,0,0.12);
}

.cd-flip-front { background: var(--bg-card); }
.cd-flip-back {
  transform: rotateY(180deg);
  background: var(--bg-secondary);
  color: var(--text-primary);
  padding: 24px;
}

.cd-poster-img {
  width: 100%; height: 100%;
  object-fit: contain;
  display: block;
  background: var(--bg-card);
}

.cd-poster-badge {
  position: absolute;
  top: 12px; left: 12px;
  padding: 4px 10px;
  background: var(--text-primary);
  color: var(--bg-primary);
  font: 700 8px/1 'Inter', sans-serif;
  letter-spacing: 0.2em;
}

.cd-flip-hint {
  position: absolute;
  bottom: 14px; left: 50%;
  transform: translateX(-50%);
  font: 500 8px/1 'Inter', sans-serif;
  letter-spacing: 0.3em;
  color: var(--text-muted);
  background: var(--bg-card);
  padding: 5px 12px;
  pointer-events: none;
}

.cd-flip-hint-back {
  left: auto; right: 16px; bottom: 10px;
  transform: none;
  color: var(--text-muted);
  background: transparent;
}

/* Poster back */
.cd-back-stamp {
  position: absolute;
  left: 24px; bottom: 60px;
  width: 72px; height: 72px;
}

.cd-back-stamp-circle {
  width: 100%; height: 100%;
  border: 2px double var(--text-secondary);
  border-radius: 50%;
  color: var(--text-secondary);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  background: var(--bg-card);
  transform: rotate(-11deg);
}

.cd-stamp-top, .cd-stamp-bottom {
  font: 700 5px/1 'Inter', sans-serif;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.cd-stamp-date {
  font: 700 10px/1 'Georgia', serif;
}

.cd-back-notes {
  position: absolute;
  left: 24px; top: 140px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.cd-back-note {
  display: flex; gap: 8px; align-items: baseline;
}

.cd-back-note span {
  font: 500 7px/1 'Inter', sans-serif;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.cd-back-note strong {
  font: 500 10px/1 'Inter', sans-serif;
  color: var(--text-secondary);
}

.cd-back-brand {
  position: absolute;
  left: 24px; bottom: 20px;
  font: 600 7px/1 'Inter', sans-serif;
  letter-spacing: 0.18em;
  color: var(--text-primary);
}

/* Overview info */
.cd-overview-info {
  max-width: 600px;
}

.cd-title {
  margin: 0 0 14px;
  font: 700 clamp(22px, 3.5vw, 38px)/1.08 'Georgia', 'Times New Roman', serif;
  letter-spacing: -0.03em;
  color: var(--text-primary);
  word-break: break-word;
}

.cd-desc-divider {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.cd-desc-line {
  flex: 1;
  height: 0.5px;
  background: var(--border-card);
}

.cd-desc-dot {
  font-size: 5px;
  color: var(--text-muted);
}

.cd-summary {
  margin: 0 0 16px;
  font: 400 13px/1.75 'Inter', sans-serif;
  color: var(--text-secondary);
  max-width: 42ch;
}

.cd-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 16px;
}

.cd-tag {
  padding: 5px 10px;
  border: 1px solid var(--border-card);
  border-radius: 2px;
  font: 500 10px/1 'Inter', sans-serif;
  color: var(--text-secondary);
  background: var(--bg-card);
}

/* Barcode */
.cd-barcode-row {
  display: flex;
  gap: 24px;
  padding: 14px 0;
  border-top: 0.5px solid var(--border-card);
  border-bottom: 0.5px solid var(--border-card);
  margin-bottom: 16px;
}

.cd-barcode-stat {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.cd-barcode-bars {
  display: flex;
  align-items: flex-end;
  gap: 1.5px;
  height: 36px;
}

.cd-bar {
  display: block;
  width: 2px;
  background: var(--text-primary);
  border-radius: 0.5px;
  min-height: 6px;
}

.cd-barcode-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.cd-barcode-label {
  font: 700 7px/1 'Inter', sans-serif;
  letter-spacing: 0.22em;
  color: var(--text-muted);
}

.cd-barcode-value {
  font: 700 16px/1 'Georgia', serif;
  color: var(--text-primary);
}

/* Interact */
.cd-interact-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.cd-stars {
  display: flex;
  gap: 3px;
}

.cd-star {
  font-size: 20px;
  color: var(--border-card);
  cursor: pointer;
  transition: 0.15s;
}

.cd-star.filled { color: #c8a84e; }
.cd-star:hover { transform: scale(1.15); }

.cd-like-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border: 1px solid var(--border-card);
  border-radius: 2px;
  background: var(--bg-card);
  color: var(--text-secondary);
  font: 500 12px/1 'Inter', sans-serif;
  cursor: pointer;
  transition: 0.2s;
}

.cd-like-btn.liked {
  color: #c44;
  border-color: rgba(200,50,50,0.25);
  background: rgba(200,50,50,0.06);
}

.cd-like-btn:hover { border-color: var(--text-muted); }

.cd-edit-btn {
  padding: 10px 20px;
  border: 1.5px solid var(--text-primary);
  border-radius: 2px;
  background: var(--text-primary);
  color: var(--bg-primary);
  font: 700 10px/1 'Inter', sans-serif;
  letter-spacing: 0.18em;
  cursor: pointer;
  transition: 0.2s;
}

.cd-edit-btn:hover {
  background: transparent;
  color: var(--text-primary);
}

/* ===== GALLERY ===== */
.cd-gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 24px;
}

.cd-gallery-item {
  position: relative;
  aspect-ratio: 3/4;
  overflow: hidden;
  background: var(--bg-card);
  box-shadow: 0 4px 16px rgba(0,0,0,0.06);
  border: 1px solid var(--border-card);
  cursor: pointer;
  transition: transform 0.2s;
}

.cd-gallery-item:hover { transform: translateY(-3px); }

.cd-gallery-item.large {
  grid-column: span 2;
  grid-row: span 2;
}

.cd-gallery-item img {
  width: 100%; height: 100%;
  object-fit: cover;
  display: block;
}

.cd-gallery-placeholder {
  width: 100%; height: 100%;
  background: linear-gradient(135deg, #ebe7e0, #f3efe9);
}

.cd-gallery-num {
  position: absolute;
  bottom: 8px; left: 10px;
  font: 700 11px/1 'Georgia', serif;
  color: var(--text-muted);
}

/* 3D Model */
.cd-model-area {
  margin-top: 8px;
}

.cd-model-label {
  font: 700 9px/1 'Inter', sans-serif;
  letter-spacing: 0.2em;
  color: var(--text-muted);
  margin-bottom: 10px;
}

.cd-model-viewer {
  width: 100%;
  height: 300px;
  background: var(--bg-card);
  cursor: pointer;
  overflow: hidden;
  border: 1px solid var(--border-card);
}

.cd-model-hint {
  font: 400 10px/1 'Inter', sans-serif;
  color: var(--text-muted);
  margin-top: 6px;
  display: block;
}

/* Lightbox */
.cd-lightbox {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0,0,0,0.88);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.cd-lightbox-img {
  max-width: 90vw;
  max-height: 90vh;
  object-fit: contain;
  cursor: default;
}

.cd-lightbox-close {
  position: absolute;
  top: 20px; right: 24px;
  width: 36px; height: 36px;
  border: 1px solid rgba(255,255,255,0.3);
  border-radius: 2px;
  background: rgba(0,0,0,0.4);
  color: #fff;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ===== PROMPTS ===== */
.cd-prompts-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cd-prompt-card {
  border: 1px solid var(--border-card);
  padding: 16px;
  background: var(--bg-card);
  transition: border-color 0.2s;
}

.cd-prompt-card:hover { border-color: var(--text-muted); }

.cd-prompt-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  font: 600 10px/1 'Inter', sans-serif;
  letter-spacing: 0.2em;
  color: var(--text-muted);
  text-transform: uppercase;
}

.cd-prompt-body {
  font: 400 13px/1.7 'Inter', sans-serif;
  color: var(--text-primary);
  margin: 0;
  word-break: break-word;
}

.cd-prompt-body.muted {
  color: var(--text-muted);
  font-style: italic;
}

.cd-prompt-empty { border-style: dashed; }

/* ===== PARAMETERS ===== */
.cd-chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.cd-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1px solid var(--border-card);
  border-radius: 2px;
  background: var(--bg-card);
  font: 400 11px/1 'Inter', sans-serif;
}

.cd-chip-label { color: var(--text-muted); }
.cd-chip-val { color: var(--text-primary); font-weight: 600; }
.cd-chip-val.accent { color: var(--text-secondary); }

.cd-section-label {
  font: 600 9px/1 'Inter', sans-serif;
  letter-spacing: 0.25em;
  color: var(--text-muted);
  text-transform: uppercase;
  margin-bottom: 10px;
}

.cd-exp-section { margin-bottom: 20px; }

.cd-exp-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
}

.cd-exp-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 4px;
  border: 1px solid var(--border-card);
  background: var(--bg-card);
}

.cd-exp-card:hover { border-color: var(--text-muted); }

.cd-exp-label {
  font: 600 8px/1 'Inter', sans-serif;
  letter-spacing: 0.3em;
  color: var(--text-muted);
}

.cd-exp-value {
  font: 700 16px/1 'Georgia', serif;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}

/* Reference images */
.cd-ref-section { margin-bottom: 16px; }

.cd-ref-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.cd-ref-thumb {
  position: relative;
  aspect-ratio: 1;
  overflow: hidden;
  border: 1px solid var(--border-card);
  cursor: pointer;
  transition: 0.2s;
}

.cd-ref-thumb:hover { border-color: var(--text-muted); transform: scale(1.02); }

.cd-ref-thumb img {
  width: 100%; height: 100%;
  object-fit: cover;
}

.cd-ref-tag {
  position: absolute;
  bottom: 6px; left: 6px;
  padding: 2px 8px;
  background: var(--text-primary);
  color: var(--bg-primary);
  font: 600 8px/1 'Inter', sans-serif;
  letter-spacing: 0.15em;
}

/* ===== COMMENTS ===== */
.cd-empty-comments {
  font: 400 12px/1.5 'Inter', sans-serif;
  color: var(--text-muted);
  text-align: center;
  padding: 32px 0;
}

.cd-comment-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 20px;
}

.cd-comment-row {
  display: flex;
  gap: 10px;
}

.cd-comment-avatar {
  width: 28px; height: 28px;
  border-radius: 2px;
  background: var(--text-primary);
  color: var(--bg-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font: 600 10px/1 'Inter', sans-serif;
  flex-shrink: 0;
}

.cd-comment-avatar.small {
  width: 22px; height: 22px;
  font-size: 8px;
}

.cd-comment-body {
  flex: 1;
  min-width: 0;
}

.cd-comment-author {
  font: 600 11px/1 'Inter', sans-serif;
  color: var(--text-primary);
  display: block;
  margin-bottom: 3px;
}

.cd-comment-text {
  font: 400 12px/1.6 'Inter', sans-serif;
  color: var(--text-secondary);
  margin: 0;
}

.cd-comment-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  flex-shrink: 0;
}

.cd-comment-like {
  background: none;
  border: none;
  color: var(--text-muted);
  font: 400 10px/1 'Inter', sans-serif;
  cursor: pointer;
  padding: 2px 4px;
}

.cd-comment-like:hover { color: #c44; }

.cd-reply-trigger {
  background: none;
  border: none;
  color: var(--text-muted);
  font: 400 9px/1 'Inter', sans-serif;
  cursor: pointer;
}

.cd-reply-trigger:hover { color: var(--text-primary); }

.cd-replies {
  margin-top: 10px;
  padding-left: 4px;
  border-left: 1.5px solid var(--border-card);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.cd-reply-item {
  display: flex;
  gap: 8px;
  padding-left: 8px;
}

/* Comment input */
.cd-comment-input-area {
  display: flex;
  gap: 8px;
  padding-top: 16px;
  border-top: 0.5px solid var(--border-card);
}

.cd-comment-input {
  flex: 1;
  padding: 9px 14px;
  border: 1px solid var(--border-card);
  border-radius: 2px;
  background: var(--bg-card);
  color: var(--text-primary);
  font: 400 12px/1 'Inter', sans-serif;
  outline: none;
  transition: border-color 0.2s;
}

.cd-comment-input:focus { border-color: var(--text-muted); }
.cd-comment-input::placeholder { color: var(--text-muted); }

.cd-cancel-reply {
  width: 32px; height: 36px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cd-send-btn {
  width: 36px; height: 36px;
  border: 1.5px solid var(--text-primary);
  border-radius: 2px;
  background: var(--text-primary);
  color: var(--bg-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: 0.2s;
  flex-shrink: 0;
}

.cd-send-btn:hover:not(:disabled) {
  background: transparent;
  color: var(--text-primary);
}

.cd-send-btn:disabled {
  opacity: 0.25;
  cursor: not-allowed;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .cd-layout {
    flex-direction: column;
  }

  .cd-sidebar {
    width: 100%;
    min-height: auto;
    height: auto;
    position: relative;
    flex-direction: row;
    flex-wrap: wrap;
    align-items: center;
    padding: 56px 16px 12px;
    gap: 12px;
    background: var(--bg-secondary);
  }

  .cd-back { margin-bottom: 0; }
  .cd-brand { margin-bottom: 0; }

  .cd-tabs {
    flex-direction: row;
    flex-wrap: wrap;
    gap: 0;
    width: 100%;
    order: 10;
    margin-top: 8px;
  }

  .cd-tab {
    padding: 8px 10px;
    font-size: 9px;
    letter-spacing: 0.12em;
  }

  .cd-tab::before { display: none; }

  .cd-tab.active {
    border-bottom: 2px solid var(--text-primary);
  }

  .cd-sidebar-footer {
    margin-left: auto;
    margin-top: 0;
    padding-top: 0;
    border-top: none;
  }

  .cd-main {
    padding: 20px 16px 32px;
  }

  .cd-gallery-item.large {
    grid-column: span 1;
    grid-row: span 1;
  }

  .cd-exp-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 480px) {
  .cd-sidebar {
    padding: 52px 12px 8px;
  }

  .cd-main {
    padding: 16px 12px 28px;
  }

  .cd-title {
    font-size: clamp(20px, 6vw, 28px);
  }

  .cd-gallery-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
  }

  .cd-exp-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}
</style>