
<template>
  <div class="versions-magazine" ref="pageRef">

    <!-- 顶部刊头 -->
    <header class="vm-header">
      <div class="vm-brand">
        <span class="vm-brand-thin">V E R S I O N S</span>
        <span class="vm-brand-bold">{{ characterName || 'Character' }}.</span>
      </div>
      <div class="vm-header-right">
        <span class="vm-date">{{ currentDate }}</span>
        <span class="vm-counter">{{ String(versions.length).padStart(2, '0') }} ITEMS</span>
      </div>
    </header>

    <!-- 返回 + 操作栏 -->
    <div class="vm-toolbar">
      <button class="vm-back-btn" @click="$router.push({ path: '/home', query: { phase: '2' } })">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M19 12H5"/><path d="M12 19l-7-7 7-7"/></svg>
        <span>Back</span>
      </button>
      <div class="vm-toolbar-actions">
        <button class="vm-action-btn danger-outline" @click="showDeleteCharacterConfirm = true">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 6h18M8 6V4a2 2 0 012-2h4a2 2 0 012 2v2m3 0v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6h14"/></svg>
          Delete
        </button>
        <button class="vm-action-btn" @click="editMode = !editMode">
          {{ editMode ? '✓ Done' : '✎ Edit' }}
        </button>
      </div>
    </div>

    <!-- 分隔线 -->
    <div class="vm-bridge">
      <div class="vm-bridge-line"></div>
      <span class="vm-bridge-label">VERSION ARCHIVE</span>
      <div class="vm-bridge-line"></div>
    </div>

    <!-- 版本网格 -->
    <section class="vm-content">
      <div class="vm-grid">
        <!-- 新建版本卡片 -->
        <div class="vm-card vm-card-new" @click="createNew">
          <div class="vm-card-new-inner">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round">
              <line x1="12" y1="5" x2="12" y2="19"/>
              <line x1="5" y1="12" x2="19" y2="12"/>
            </svg>
            <span class="vm-card-new-text">New Version</span>
          </div>
        </div>

        <!-- 已有版本卡片 -->
        <div
          v-for="(v, idx) in versions"
          :key="v.id"
          class="vm-card"
          :class="{ 'vm-card-editing': editMode }"
        >
          <!-- 编辑模式操作栏 -->
          <div class="vm-card-edit-bar" v-if="editMode">
            <button class="vm-card-edit-btn" @click.stop="moveVersion(idx, -1)" :disabled="idx === 0" title="Move left">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 18l-6-6 6-6"/></svg>
            </button>
            <button class="vm-card-edit-btn vm-card-edit-btn--danger" @click.stop="confirmDeleteVersion(v)" title="Delete">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 6h18M8 6V4a2 2 0 012-2h4a2 2 0 012 2v2m3 0v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6h14"/></svg>
            </button>
            <button class="vm-card-edit-btn" @click.stop="moveVersion(idx, 1)" :disabled="idx === versions.length - 1" title="Move right">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>
            </button>
          </div>

          <!-- 卡片图片 -->
          <div class="vm-card-visual" @click="openVersion(v.id)">
            <img v-if="v.previewImageUrl" :src="v.previewImageUrl" class="vm-card-img" />
            <div v-else class="vm-card-placeholder">
              <span>◆</span>
            </div>

            <!-- 邮戳标签 -->
            <div class="vm-stamp">
              <div class="vm-stamp-inner">
                <span class="vm-stamp-ring"></span>
                <span class="vm-stamp-text">#{{ v.id }}</span>
                <span class="vm-stamp-sub">{{ v.status || 'DRAFT' }}</span>
              </div>
            </div>

            <!-- 超大半透明编号 -->
            <div class="vm-big-num">{{ String(idx + 1).padStart(2, '0') }}</div>
          </div>

          <!-- 卡片信息 -->
          <div class="vm-card-body">
            <div class="vm-card-meta-row">
              <span class="vm-card-id">VOL. {{ String(v.id).padStart(2, '0') }}</span>
              <span class="vm-card-status" :class="v.status">{{ v.status }}</span>
            </div>

            <p class="vm-card-prompt" v-if="v.characterPrompt">{{ truncate(v.characterPrompt, 50) }}</p>
            <p class="vm-card-prompt vm-card-prompt--empty" v-else>No prompt</p>

            <!-- 条形码装饰 -->
            <div class="vm-card-barcode">
              <span v-for="n in 8" :key="n" class="vm-bar" :style="{ height: barH(n, v.id) + 'px' }"></span>
            </div>

            <!-- 操作按钮 -->
            <div class="vm-card-actions">
              <button class="vm-share-btn" :class="{ shared: sharedMap[v.id] }" @click.stop="openShareModal(v)">
                {{ sharedMap[v.id] ? '✓ Shared' : '↗ Share' }}
              </button>
              <button v-if="sharedMap[v.id] && shareIdMap[v.id]" class="vm-view-btn" @click.stop="goShareDetail(v.id)">
                View →
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部进度 -->
      <div class="vm-footer" v-if="versions.length > 0">
        <div class="vm-progress-line">
          <div class="vm-progress-fill" :style="{ width: '100%' }"></div>
        </div>
        <div class="vm-footer-counter">
          <span class="vm-footer-cur">{{ String(versions.length).padStart(2, '0') }}</span>
          <span class="vm-footer-sep">/</span>
          <span class="vm-footer-total">{{ String(versions.length).padStart(2, '0') }}</span>
          <span class="vm-footer-label">VERSIONS</span>
        </div>
      </div>
    </section>

    <!-- 删除角色确认弹窗 -->
    <div class="vm-modal-overlay" v-if="showDeleteCharacterConfirm" @click.self="showDeleteCharacterConfirm = false">
      <div class="vm-modal-box">
        <div class="vm-modal-corner vm-modal-corner--tl"></div>
        <div class="vm-modal-corner vm-modal-corner--br"></div>
        <p class="vm-modal-title">Delete character <strong>{{ characterName || 'Character' }}</strong>?</p>
        <p class="vm-modal-desc">All versions and community shares under this character will also be deleted. This action cannot be undone.</p>
        <div class="vm-modal-actions">
          <button class="vm-modal-btn vm-modal-btn--cancel" @click="showDeleteCharacterConfirm = false">Cancel</button>
          <button class="vm-modal-btn vm-modal-btn--danger" @click="doDeleteCharacter">Delete</button>
        </div>
      </div>
    </div>

    <!-- 删除版本确认弹窗 -->
    <div class="vm-modal-overlay" v-if="showDeleteConfirm" @click.self="showDeleteConfirm = false">
      <div class="vm-modal-box">
        <div class="vm-modal-corner vm-modal-corner--tl"></div>
        <div class="vm-modal-corner vm-modal-corner--br"></div>
        <p class="vm-modal-title">Delete version <strong>#{{ deleteTarget?.id }}</strong>?</p>
        <p class="vm-modal-desc">All data for this version (images, models, parameters) will be permanently deleted. This action cannot be undone.</p>
        <div class="vm-modal-actions">
          <button class="vm-modal-btn vm-modal-btn--cancel" @click="showDeleteConfirm = false">Cancel</button>
          <button class="vm-modal-btn vm-modal-btn--danger" @click="doDeleteVersion">Delete</button>
        </div>
      </div>
    </div>

    <!-- 分享弹窗 -->
    <div class="vm-modal-overlay" v-if="showShareModal" @click.self="showShareModal = false">
      <div class="vm-share-modal">
        <div class="vm-modal-corner vm-modal-corner--tl"></div>
        <div class="vm-modal-corner vm-modal-corner--br"></div>

        <div class="vm-share-header">
          <h3>Share to Community</h3>
          <button class="vm-share-close" @click="showShareModal = false">×</button>
        </div>

        <div class="vm-share-body">
          <!-- 版本预览 -->
          <div class="vm-share-preview" v-if="shareTarget">
            <img v-if="shareTarget.previewImageUrl" :src="shareTarget.previewImageUrl" class="vm-share-preview-img" />
            <div v-else class="vm-share-preview-placeholder">◆</div>
            <div class="vm-share-preview-info">
              <span class="vm-share-preview-id">#{{ shareTarget.id }}</span>
              <span class="vm-share-preview-status" :class="shareTarget.status">{{ shareTarget.status }}</span>
            </div>
          </div>

          <!-- 版本参数摘要 -->
          <div class="vm-share-params" v-if="shareTarget">
            <p class="vm-share-params-title">Version Parameters</p>
            <div class="vm-share-param" v-if="shareTarget.characterPrompt">
              <span class="vm-share-param-key">Character</span>
              <span class="vm-share-param-val">{{ truncate(shareTarget.characterPrompt, 80) }}</span>
            </div>
            <div class="vm-share-param" v-if="shareTarget.scenePrompt">
              <span class="vm-share-param-key">Scene</span>
              <span class="vm-share-param-val">{{ truncate(shareTarget.scenePrompt, 80) }}</span>
            </div>
            <div class="vm-share-param" v-if="shareTarget.actionPrompt">
              <span class="vm-share-param-key">Action</span>
              <span class="vm-share-param-val">{{ truncate(shareTarget.actionPrompt, 80) }}</span>
            </div>
            <div class="vm-share-param" v-if="hasExpressions(shareTarget)">
              <span class="vm-share-param-key">Expressions</span>
              <span class="vm-share-param-val">{{ formatExpressions(shareTarget) }}</span>
            </div>
          </div>

          <!-- 分享表单 -->
          <div class="vm-share-field">
            <label>Description</label>
            <textarea v-model="shareDesc" placeholder="Describe your model for the community..." rows="2"></textarea>
          </div>
          <div class="vm-share-field">
            <label>Tags (comma separated)</label>
            <input v-model="shareTags" placeholder="e.g. character, cyberpunk, AI" />
          </div>
        </div>

        <div class="vm-share-footer">
          <button class="vm-modal-btn vm-modal-btn--cancel" @click="showShareModal = false">Cancel</button>
          <button class="vm-modal-btn vm-modal-btn--confirm" @click="doShare" :disabled="shareLoading">
            {{ sharedMap[shareTarget?.id] ? 'Update Share' : 'Share' }}
          </button>
          <button v-if="sharedMap[shareTarget?.id]" class="vm-modal-btn vm-modal-btn--danger" @click="doUnshare" :disabled="shareLoading">
            Unshare
          </button>
        </div>

        <p v-if="shareMsg" class="vm-share-msg" :class="{ success: shareSuccess }">{{ shareMsg }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import axios from 'axios'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const characterId = route.params.characterId

const characterName = ref('')
const versions = ref([])
const editMode = ref(false)
const showDeleteConfirm = ref(false)
const showDeleteCharacterConfirm = ref(false)
const deleteTarget = ref(null)

// 分享相关状态
const showShareModal = ref(false)
const shareTarget = ref(null)
const shareDesc = ref('')
const shareTags = ref('')
const shareLoading = ref(false)
const shareMsg = ref('')
const shareSuccess = ref(false)
const sharedMap = reactive({}) // versionId -> boolean
const shareIdMap = reactive({}) // versionId -> shareId

const now = new Date()
const months = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC']
const currentDate = `${months[now.getMonth()]}. ${now.getDate()} ${now.getFullYear()}`

const truncate = (str, len) => {
  if (!str) return ''
  return str.length > len ? str.slice(0, len) + '...' : str
}

const barH = (n, value) => {
  const seed = (n * 7 + (value || 0) * 3) % 17
  return 10 + (seed / 17) * 18
}

const hasExpressions = (v) => {
  return [v.expression1, v.expression2, v.expression3, v.expression4, v.expression5, v.expression6, v.expression7]
    .some(e => e !== null && e !== undefined && e !== 0)
}

const formatExpressions = (v) => {
  return [v.expression1, v.expression2, v.expression3, v.expression4, v.expression5, v.expression6, v.expression7]
    .map((e, i) => `e${i+1}:${e ?? 0}`)
    .join('  ')
}

const loadCharacter = async () => {
  try {
    const userId = localStorage.getItem('userId') || 1
    const res = await axios.get(`/api/character/list?userId=${userId}`)
    const found = res.data.find(c => String(c.id) === String(characterId))
    if (found) characterName.value = found.name
  } catch (e) {
    console.error(e)
  }
}

const loadVersions = async () => {
  try {
    const res = await axios.get(`/api/version/list?characterId=${characterId}`)
    versions.value = Array.isArray(res.data) ? res.data : []
    // 检查每个版本的分享状态
    for (const v of versions.value) {
      checkShared(v.id)
    }
  } catch (e) {
    console.error(e)
  }
}

const checkShared = async (versionId) => {
  try {
    const res = await axios.get(`/api/community/isShared?versionId=${versionId}`)
    sharedMap[versionId] = res.data.shared
    if (res.data.shared && res.data.shareId) {
      shareIdMap[versionId] = res.data.shareId
    }
  } catch (e) {
    // ignore
  }
}

const goShareDetail = (versionId) => {
  const shareId = shareIdMap[versionId]
  if (shareId) {
    router.push(`/community/${shareId}`)
  }
}

const createNew = () => {
  router.push(`/editor/${characterId}`)
}

const openVersion = (versionId) => {
  if (editMode.value) return
  router.push(`/editor/${characterId}?versionId=${versionId}`)
}

const openShareModal = (v) => {
  shareTarget.value = v
  shareDesc.value = ''
  shareTags.value = ''
  shareMsg.value = ''
  showShareModal.value = true
}

const doShare = async () => {
  if (!shareTarget.value) return
  shareLoading.value = true
  shareMsg.value = ''
  try {
    const userId = localStorage.getItem('userId') || 1
    const res = await axios.post('/api/community/share', null, {
      params: {
        versionId: shareTarget.value.id,
        userId,
        authorName: 'User',
        description: shareDesc.value,
        tags: shareTags.value
      }
    })
    sharedMap[shareTarget.value.id] = true
    if (res.data && res.data.id) {
      shareIdMap[shareTarget.value.id] = res.data.id
    }
    shareSuccess.value = true
    shareMsg.value = '✅ Shared successfully'
  } catch (e) {
    shareSuccess.value = false
    shareMsg.value = '❌ Share failed'
  }
  shareLoading.value = false
}

const doUnshare = async () => {
  if (!shareTarget.value) return
  shareLoading.value = true
  shareMsg.value = ''
  try {
    await axios.delete(`/api/community/unshare?versionId=${shareTarget.value.id}`)
    sharedMap[shareTarget.value.id] = false
    shareSuccess.value = true
    shareMsg.value = '✅ Removed from community'
    setTimeout(() => { shareMsg.value = '' }, 2000)
  } catch (e) {
    shareSuccess.value = false
    shareMsg.value = '❌ Failed to unshare'
  }
  shareLoading.value = false
}

const confirmDeleteVersion = (version) => {
  deleteTarget.value = version
  showDeleteConfirm.value = true
}

const doDeleteCharacter = async () => {
  try {
    await axios.delete(`/api/character/delete?characterId=${characterId}`)
    showDeleteCharacterConfirm.value = false
    router.push('/home')
  } catch (e) {
    console.error(e)
  }
}

const doDeleteVersion = async () => {
  if (!deleteTarget.value) return
  try {
    await axios.delete(`/api/version/delete?versionId=${deleteTarget.value.id}`)
    showDeleteConfirm.value = false
    deleteTarget.value = null
    await loadVersions()
  } catch (e) {
    console.error(e)
  }
}

const moveVersion = async (idx, direction) => {
  const newIdx = idx + direction
  if (newIdx < 0 || newIdx >= versions.value.length) return

  const reordered = [...versions.value]
  const temp = reordered[idx]
  reordered[idx] = reordered[newIdx]
  reordered[newIdx] = temp
  versions.value = reordered

  const orderList = reordered.map((item, i) => ({ id: item.id, sortOrder: i }))
  try {
    await axios.put('/api/version/reorder', orderList)
  } catch (e) {
    console.error(e)
    await loadVersions()
  }
}

onMounted(() => {
  loadCharacter()
  loadVersions()
})
</script>

<style scoped>
/* ===== 全局容器 ===== */
.versions-magazine {
  min-height: 100vh;
  background: var(--bg-primary);
  color: var(--text-primary);
  font-family: 'Inter', 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  outline: none;
}

/* ===== 顶部刊头 ===== */
.vm-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 80px 32px 14px;
}

.vm-brand {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.vm-brand-thin {
  font: 300 10px/1 'Inter', sans-serif;
  letter-spacing: 0.45em;
  color: var(--text-muted);
}

.vm-brand-bold {
  font: 800 clamp(26px, 4vw, 42px)/0.94 'Georgia', 'Times New Roman', serif;
  letter-spacing: -0.025em;
  color: var(--text-primary);
}

.vm-header-right {
  display: flex;
  align-items: center;
  gap: 18px;
}

.vm-date {
  font: 400 10px/1 'Inter', sans-serif;
  letter-spacing: 0.26em;
  color: var(--text-muted);
}

.vm-counter {
  font: 700 9px/1 'Inter', sans-serif;
  letter-spacing: 0.3em;
  color: var(--text-secondary);
  text-transform: uppercase;
}

/* ===== 工具栏 ===== */
.vm-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 32px;
}

.vm-back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 9px 16px;
  border: 1px solid var(--border-card);
  border-radius: 0;
  background: transparent;
  color: var(--text-secondary);
  font: 500 10px/1 'Inter', sans-serif;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  cursor: pointer;
  transition: all 0.25s ease;
}

.vm-back-btn:hover {
  border-color: var(--text-primary);
  color: var(--text-primary);
  transform: translateX(-3px);
}

.vm-toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.vm-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid var(--border-card);
  border-radius: 0;
  background: transparent;
  color: var(--text-secondary);
  font: 500 10px/1 'Inter', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  cursor: pointer;
  transition: all 0.2s;
}

.vm-action-btn:hover {
  border-color: var(--text-primary);
  color: var(--text-primary);
}

.vm-action-btn.danger-outline {
  color: #e55;
  border-color: rgba(220, 50, 50, 0.28);
}

.vm-action-btn.danger-outline:hover {
  background: rgba(220, 50, 50, 0.1);
  border-color: rgba(220, 50, 50, 0.5);
}

/* ===== 分隔线 ===== */
.vm-bridge {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 32px 20px;
}

.vm-bridge-line {
  flex: 1;
  height: 0.5px;
  background: var(--border-card);
}

.vm-bridge-label {
  font: 700 9px/1 'Inter', sans-serif;
  letter-spacing: 0.35em;
  color: var(--text-muted);
}

/* ===== 内容区 ===== */
.vm-content {
  max-width: 960px;
  margin: 0 auto;
  padding: 0 32px 100px;
}

/* ===== 版本网格 ===== */
.vm-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
}

/* ===== 卡片通用 ===== */
.vm-card {
  position: relative;
  border: 1px solid var(--border-card);
  background: var(--bg-card);
  overflow: visible;
  cursor: pointer;
  transition: transform 0.35s cubic-bezier(0.22, 1, 0.36, 1), border-color 0.2s, box-shadow 0.35s;
}

.vm-card:hover {
  border-color: var(--text-muted);
  transform: translateY(-4px);
  box-shadow: 0 16px 38px rgba(0,0,0,0.08);
}

.vm-card-editing {
  border-color: color-mix(in srgb, var(--accent-hover) 40%, var(--border-card));
}

/* ===== 新建卡片 ===== */
.vm-card-new {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 340px;
  background: radial-gradient(circle at 40% 30%, color-mix(in srgb, var(--accent-hover) 8%, transparent), transparent 60%), var(--bg-card);
}

.vm-card-new-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: var(--text-secondary);
  transition: color 0.2s;
}

.vm-card-new:hover .vm-card-new-inner {
  color: var(--text-primary);
}

.vm-card-new-text {
  font: 700 10px/1 'Inter', sans-serif;
  letter-spacing: 0.25em;
  text-transform: uppercase;
}

/* ===== 卡片图片区 ===== */
.vm-card-visual {
  position: relative;
  width: 100%;
  aspect-ratio: 3/4;
  overflow: visible;
  background: var(--bg-secondary);
}

.vm-card-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.vm-card-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, color-mix(in srgb, var(--bg-secondary) 80%, var(--text-muted)), var(--bg-secondary));
}

.vm-card-placeholder span {
  font-size: 36px;
  color: var(--border-card);
}

/* ===== 邮戳标签 ===== */
.vm-stamp {
  position: absolute;
  right: -10px;
  top: 12px;
  transform: rotate(12deg);
  z-index: 10;
  pointer-events: none;
}

.vm-stamp-inner {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 58px;
  height: 58px;
  border: 2px double var(--text-primary);
  border-radius: 50%;
  background: var(--bg-card);
  box-shadow: 0 2px 12px rgba(0,0,0,0.12);
}

.vm-stamp-ring {
  position: absolute;
  inset: 3px;
  border: 1px dashed var(--text-muted);
  border-radius: 50%;
}

.vm-stamp-text {
  font: 800 8px/1 'Inter', sans-serif;
  letter-spacing: 0.12em;
  color: var(--text-primary);
  text-transform: uppercase;
  max-width: 42px;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.vm-stamp-sub {
  font: 600 6px/1 'Inter', sans-serif;
  letter-spacing: 0.1em;
  color: var(--text-secondary);
  margin-top: 2px;
  text-transform: uppercase;
}

/* ===== 超大半透明编号 ===== */
.vm-big-num {
  position: absolute;
  left: -14px;
  bottom: -10px;
  font: 900 80px/1 'Georgia', 'Times New Roman', serif;
  color: var(--border-card);
  letter-spacing: -0.04em;
  pointer-events: none;
  user-select: none;
  z-index: 1;
}

/* ===== 卡片信息区 ===== */
.vm-card-body {
  padding: 12px 14px 14px;
}

.vm-card-meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.vm-card-id {
  font: 500 9px/1 'Inter', sans-serif;
  letter-spacing: 0.2em;
  color: var(--text-muted);
  text-transform: uppercase;
}

.vm-card-status {
  font: 700 8px/1 'Inter', sans-serif;
  padding: 3px 8px;
  background: var(--bg-card);
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.15em;
}

.vm-card-status.created { color: var(--text-primary); background: var(--border-card); }
.vm-card-status[class*="2D_DONE"] { color: var(--text-primary); }
.vm-card-status[class*="3D_DONE"] { color: var(--accent-hover); }

.vm-card-prompt {
  margin: 0 0 10px;
  font: 300 11px/1.6 'Georgia', serif;
  color: var(--text-secondary);
  letter-spacing: 0.01em;
  border-left: 2px solid var(--border-card);
  padding-left: 10px;
}

.vm-card-prompt--empty {
  font-style: italic;
  opacity: 0.5;
  border-left: none;
  padding-left: 0;
}

/* ===== 条形码装饰 ===== */
.vm-card-barcode {
  display: flex;
  align-items: flex-end;
  gap: 1.5px;
  height: 24px;
  margin-bottom: 10px;
}

.vm-bar {
  display: block;
  width: 2px;
  background: var(--text-primary);
  border-radius: 0.5px;
  min-height: 4px;
}

/* ===== 卡片操作按钮 ===== */
.vm-card-actions {
  display: flex;
  gap: 6px;
}

.vm-share-btn {
  flex: 1;
  padding: 7px 0;
  border: 1px solid var(--border-card);
  background: transparent;
  color: var(--text-secondary);
  font: 500 10px/1 'Inter', sans-serif;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  cursor: pointer;
  transition: all 0.2s;
}

.vm-share-btn:hover {
  border-color: var(--text-primary);
  color: var(--text-primary);
}

.vm-share-btn.shared {
  color: var(--text-primary);
  border-color: var(--text-secondary);
}

.vm-view-btn {
  padding: 7px 12px;
  border: 1px solid var(--border-card);
  background: transparent;
  color: var(--text-secondary);
  font: 500 10px/1 'Inter', sans-serif;
  letter-spacing: 0.1em;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.vm-view-btn:hover {
  border-color: var(--text-primary);
  color: var(--text-primary);
}

/* ===== 编辑模式操作栏 ===== */
.vm-card-edit-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  padding: 10px 10px 0;
}

.vm-card-edit-btn {
  width: 28px;
  height: 28px;
  border: 1px solid var(--border-card);
  background: var(--bg-secondary);
  color: var(--text-secondary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.vm-card-edit-btn:hover:not(:disabled) {
  border-color: var(--accent-hover);
  color: var(--accent-hover);
}

.vm-card-edit-btn--danger {
  color: #e55;
  border-color: rgba(220, 50, 50, 0.25);
}

.vm-card-edit-btn--danger:hover {
  background: rgba(220, 50, 50, 0.08);
  border-color: rgba(220, 50, 50, 0.4);
}

.vm-card-edit-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

/* ===== 底部进度 ===== */
.vm-footer {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 32px;
  padding-top: 16px;
  border-top: 1px solid var(--border-card);
}

.vm-progress-line {
  flex: 1;
  height: 1px;
  background: var(--border-card);
  overflow: hidden;
}

.vm-progress-fill {
  height: 100%;
  background: var(--text-secondary);
  transition: width 0.35s ease;
}

.vm-footer-counter {
  display: flex;
  align-items: baseline;
  gap: 3px;
}

.vm-footer-cur {
  font: 700 18px/1 'Georgia', 'Times New Roman', serif;
  color: var(--text-primary);
}

.vm-footer-sep {
  font: 400 12px/1 'Inter', sans-serif;
  color: var(--text-muted);
}

.vm-footer-total {
  font: 400 12px/1 'Inter', sans-serif;
  color: var(--text-muted);
}

.vm-footer-label {
  font: 700 8px/1 'Inter', sans-serif;
  letter-spacing: 0.25em;
  color: var(--text-muted);
  margin-left: 8px;
}

/* ===== 弹窗通用 ===== */
.vm-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.vm-modal-box {
  background: var(--bg-secondary);
  border: 1px solid var(--border-card);
  padding: 28px;
  max-width: 400px;
  width: 100%;
  position: relative;
}

.vm-modal-corner {
  position: absolute;
  width: 14px;
  height: 14px;
  border-color: var(--text-secondary);
  border-style: solid;
  opacity: 0.25;
  pointer-events: none;
}

.vm-modal-corner--tl {
  top: 8px;
  left: 8px;
  border-width: 1px 0 0 1px;
}

.vm-modal-corner--br {
  bottom: 8px;
  right: 8px;
  border-width: 0 1px 1px 0;
}

.vm-modal-title {
  font: 500 14px/1.3 'Inter', sans-serif;
  margin: 0 0 8px;
  color: var(--text-primary);
}

.vm-modal-desc {
  font: 300 12px/1.6 'Georgia', serif;
  color: var(--text-secondary);
  margin: 0 0 20px;
}

.vm-modal-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.vm-modal-btn {
  padding: 8px 18px;
  font: 500 11px/1 'Inter', sans-serif;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid var(--border-card);
}

.vm-modal-btn--cancel {
  background: var(--input-bg);
  color: var(--text-secondary);
}

.vm-modal-btn--cancel:hover {
  color: var(--text-primary);
  background: var(--bg-card);
}

.vm-modal-btn--confirm {
  background: var(--accent);
  border-color: var(--accent);
  color: #fff;
}

.vm-modal-btn--confirm:hover:not(:disabled) {
  background: var(--accent-hover);
  border-color: var(--accent-hover);
}

.vm-modal-btn--danger {
  background: rgba(220, 50, 50, 0.15);
  border-color: rgba(220, 50, 50, 0.3);
  color: #e55;
}

.vm-modal-btn--danger:hover:not(:disabled) {
  background: rgba(220, 50, 50, 0.25);
}

.vm-modal-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* ===== 分享弹窗 ===== */
.vm-share-modal {
  background: var(--bg-secondary);
  border: 1px solid var(--border-card);
  width: 100%;
  max-width: 480px;
  max-height: 80vh;
  overflow-y: auto;
  position: relative;
}

.vm-share-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-card);
}

.vm-share-header h3 {
  margin: 0;
  font: 600 15px/1 'Inter', sans-serif;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.vm-share-close {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s;
}

.vm-share-close:hover {
  color: var(--text-primary);
}

.vm-share-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.vm-share-preview {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--border-card);
  background: var(--input-bg);
}

.vm-share-preview-img {
  width: 64px;
  height: 64px;
  object-fit: cover;
}

.vm-share-preview-placeholder {
  width: 64px;
  height: 64px;
  background: var(--bg-card);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  opacity: 0.4;
}

.vm-share-preview-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.vm-share-preview-id {
  font-size: 14px;
  font-weight: 600;
}

.vm-share-preview-status {
  font: 700 8px/1 'Inter', sans-serif;
  padding: 3px 8px;
  background: var(--bg-card);
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.15em;
  width: fit-content;
}

.vm-share-params {
  border: 1px solid var(--border-card);
  padding: 12px;
  background: var(--input-bg);
}

.vm-share-params-title {
  font: 700 9px/1 'Inter', sans-serif;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.25em;
  margin: 0 0 10px;
}

.vm-share-param {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 12px;
  line-height: 1.5;
}

.vm-share-param:last-child { margin-bottom: 0; }

.vm-share-param-key {
  color: var(--text-secondary);
  min-width: 70px;
  flex-shrink: 0;
}

.vm-share-param-val {
  color: var(--text-primary);
  word-break: break-word;
}

.vm-share-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.vm-share-field label {
  font-size: 11px;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.vm-share-field textarea,
.vm-share-field input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--border-card);
  background: var(--input-bg);
  color: var(--text-primary);
  font-size: 13px;
  font-family: inherit;
  outline: none;
  resize: vertical;
  transition: border-color 0.2s;
}

.vm-share-field textarea:focus,
.vm-share-field input:focus {
  border-color: var(--accent-hover);
}

.vm-share-field textarea::placeholder,
.vm-share-field input::placeholder {
  color: var(--text-secondary);
}

.vm-share-footer {
  display: flex;
  gap: 8px;
  padding: 16px 20px;
  border-top: 1px solid var(--border-card);
}

.vm-share-msg {
  font-size: 12px;
  color: #e55;
  text-align: center;
  padding: 0 20px 12px;
}

.vm-share-msg.success {
  color: #0b4;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .vm-header {
    padding: 68px 16px 12px;
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .vm-header-right { gap: 12px; }
  .vm-toolbar { padding: 10px 16px; }
  .vm-bridge { padding: 6px 16px 16px; }
  .vm-content { padding: 0 16px 80px; }

  .vm-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 14px;
  }

  .vm-brand-bold { font-size: clamp(22px, 6vw, 32px); }
  .vm-big-num { font-size: 56px; left: -8px; bottom: -6px; }
  .vm-stamp-inner { width: 48px; height: 48px; }
  .vm-stamp-text { font-size: 7px; max-width: 34px; }
  .vm-stamp-sub { font-size: 5px; }

  .vm-toolbar-actions { flex-wrap: wrap; gap: 6px; }
  .vm-action-btn { padding: 6px 10px; font-size: 9px; }
}

@media (max-width: 480px) {
  .vm-header { padding: 60px 12px 10px; }
  .vm-toolbar { padding: 8px 12px; flex-wrap: wrap; gap: 8px; }
  .vm-bridge { padding: 4px 12px 12px; }
  .vm-content { padding: 0 12px 60px; }

  .vm-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .vm-card-new { min-height: 260px; }
  .vm-big-num { font-size: 44px; }
  .vm-stamp { right: -6px; top: 8px; }
  .vm-stamp-inner { width: 42px; height: 42px; }
}
</style>