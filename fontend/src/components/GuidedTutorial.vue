<template>
  <Teleport to="body">
    <div v-if="visible" class="tutorial-overlay" @click.self="handleOverlayClick">
      <!-- 高亮遮罩：四块暗色区域围绕目标元素 -->
      <div class="mask-top" :style="maskTopStyle"></div>
      <div class="mask-bottom" :style="maskBottomStyle"></div>
      <div class="mask-left" :style="maskLeftStyle"></div>
      <div class="mask-right" :style="maskRightStyle"></div>

      <!-- 高亮框 -->
      <div class="highlight-box" :style="highlightStyle"></div>

      <!-- 提示气泡 -->
      <div class="tooltip-bubble" :style="tooltipStyle" :class="tooltipPosition">
        <div class="tooltip-arrow"></div>
        <div class="tooltip-header">
          <div class="tooltip-step">
            <span class="step-current">{{ currentStep + 1 }}</span>
            <span class="step-divider">/</span>
            <span class="step-total">{{ tutorialSteps.length }}</span>
          </div>
          <button class="tooltip-close" @click="endTutorial">×</button>
        </div>
        <h3 class="tooltip-title">{{ tutorialSteps[currentStep]?.title }}</h3>
        <p class="tooltip-desc">{{ tutorialSteps[currentStep]?.desc }}</p>

        <!-- 步骤进度条 -->
        <div class="tooltip-progress">
          <div
            v-for="(s, i) in tutorialSteps"
            :key="i"
            class="progress-dot"
            :class="{ active: i === currentStep, done: i < currentStep }"
          ></div>
        </div>

        <div class="tooltip-actions">
          <button v-if="currentStep > 0" class="tut-btn secondary" @click="prevTutStep">Back</button>
          <button v-if="currentStep < tutorialSteps.length - 1" class="tut-btn primary" @click="nextTutStep">
            Next →
          </button>
          <button v-else class="tut-btn primary" @click="finishTutorial">
            Start Creating! 🚀
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false }
})

const emit = defineEmits(['close', 'finish', 'step-change'])

const currentStep = ref(0)
const targetRect = ref({ top: 0, left: 0, width: 0, height: 0 })
const windowSize = ref({ w: window.innerWidth, h: window.innerHeight })

const padding = 8

const tutorialSteps = [
  {
    title: 'Name Your Character',
    desc: 'Type a name for your character in this input field. This will create a workspace where you can design its appearance using AI.',
    target: '.create-row .text-input',
    position: 'bottom'
  },
  {
    title: 'Create It!',
    desc: 'Click the "+" button to create your character. You\'ll then be able to upload references and generate 2D/3D models.',
    target: '.create-row .action-btn',
    position: 'bottom'
  },
  {
    title: 'Your Characters',
    desc: 'All your created characters appear here. Click any character to enter its editor and start designing versions.',
    target: '.gallery-section',
    position: 'bottom'
  },
  {
    title: 'Step-by-Step Guide',
    desc: 'This radio-style guide walks you through the full workflow — from creating a character to NFC interaction. Use the controls to browse steps.',
    target: '.radio-box',
    position: 'top'
  },
  {
    title: 'Explore Community',
    desc: 'Check out models shared by other users for inspiration. You can like, comment, and learn from their creations.',
    target: '.community-preview, .community-more-btn',
    position: 'top'
  }
]

// 计算目标元素位置
const updateTargetRect = () => {
  const step = tutorialSteps[currentStep.value]
  if (!step) return

  const el = document.querySelector(step.target)
  if (el) {
    const rect = el.getBoundingClientRect()
    targetRect.value = {
      top: rect.top,
      left: rect.left,
      width: rect.width,
      height: rect.height
    }
  } else {
    // 如果找不到目标，居中显示
    targetRect.value = {
      top: windowSize.value.h / 2 - 50,
      left: windowSize.value.w / 2 - 100,
      width: 200,
      height: 100
    }
  }
}

// 遮罩样式
const maskTopStyle = computed(() => ({
  top: '0',
  left: '0',
  width: '100%',
  height: `${Math.max(0, targetRect.value.top - padding)}px`
}))

const maskBottomStyle = computed(() => ({
  top: `${targetRect.value.top + targetRect.value.height + padding}px`,
  left: '0',
  width: '100%',
  height: `${Math.max(0, windowSize.value.h - targetRect.value.top - targetRect.value.height - padding)}px`
}))

const maskLeftStyle = computed(() => ({
  top: `${Math.max(0, targetRect.value.top - padding)}px`,
  left: '0',
  width: `${Math.max(0, targetRect.value.left - padding)}px`,
  height: `${targetRect.value.height + padding * 2}px`
}))

const maskRightStyle = computed(() => ({
  top: `${Math.max(0, targetRect.value.top - padding)}px`,
  left: `${targetRect.value.left + targetRect.value.width + padding}px`,
  width: `${Math.max(0, windowSize.value.w - targetRect.value.left - targetRect.value.width - padding)}px`,
  height: `${targetRect.value.height + padding * 2}px`
}))

// 高亮框样式
const highlightStyle = computed(() => ({
  top: `${targetRect.value.top - padding}px`,
  left: `${targetRect.value.left - padding}px`,
  width: `${targetRect.value.width + padding * 2}px`,
  height: `${targetRect.value.height + padding * 2}px`
}))

// 提示气泡位置
const tooltipPosition = computed(() => tutorialSteps[currentStep.value]?.position || 'bottom')

const tooltipStyle = computed(() => {
  const r = targetRect.value
  const pos = tooltipPosition.value
  const tooltipW = 320
  const tooltipH = 240

  let top, left

  if (pos === 'bottom') {
    top = r.top + r.height + padding + 16
    left = r.left + r.width / 2 - tooltipW / 2
  } else if (pos === 'top') {
    top = r.top - padding - tooltipH - 16
    left = r.left + r.width / 2 - tooltipW / 2
  } else if (pos === 'right') {
    top = r.top + r.height / 2 - tooltipH / 2
    left = r.left + r.width + padding + 16
  } else {
    top = r.top + r.height / 2 - tooltipH / 2
    left = r.left - padding - tooltipW - 16
  }

  // 边界修正
  left = Math.max(16, Math.min(left, windowSize.value.w - tooltipW - 16))
  top = Math.max(16, Math.min(top, windowSize.value.h - tooltipH - 16))

  return {
    top: `${top}px`,
    left: `${left}px`,
    width: `${tooltipW}px`
  }
})

const scrollToTarget = () => {
  const step = tutorialSteps[currentStep.value]
  if (!step) return
  const el = document.querySelector(step.target)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    // 等滚动完成后再更新位置
    setTimeout(() => updateTargetRect(), 400)
  }
}

const nextTutStep = () => {
  if (currentStep.value < tutorialSteps.length - 1) {
    currentStep.value++
    emit('step-change', currentStep.value)
    nextTick(() => scrollToTarget())
  }
}

const prevTutStep = () => {
  if (currentStep.value > 0) {
    currentStep.value--
    emit('step-change', currentStep.value)
    nextTick(() => scrollToTarget())
  }
}

const endTutorial = () => {
  currentStep.value = 0
  emit('close')
}

const finishTutorial = () => {
  currentStep.value = 0
  emit('finish')
}

const handleOverlayClick = () => {
  // 点击遮罩不关闭，引导用户按步骤操作
}

const onResize = () => {
  windowSize.value = { w: window.innerWidth, h: window.innerHeight }
  updateTargetRect()
}

watch(() => props.visible, (val) => {
  if (val) {
    currentStep.value = 0
    nextTick(() => {
      scrollToTarget()
    })
  }
})

watch(currentStep, () => {
  nextTick(() => updateTargetRect())
})

onMounted(() => {
  window.addEventListener('resize', onResize)
  window.addEventListener('scroll', updateTargetRect, true)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  window.removeEventListener('scroll', updateTargetRect, true)
})
</script>

<style scoped>
.tutorial-overlay {
  position: fixed;
  inset: 0;
  z-index: 10000;
  pointer-events: none;
}

/* 遮罩区域 */
.mask-top,
.mask-bottom,
.mask-left,
.mask-right {
  position: fixed;
  background: rgba(0, 0, 0, 0.55);
  pointer-events: auto;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 高亮框 */
.highlight-box {
  position: fixed;
  border: 2px solid rgba(26, 107, 111, 0.8);
  border-radius: 12px;
  box-shadow: 0 0 0 4px rgba(26, 107, 111, 0.2), 0 0 30px rgba(26, 107, 111, 0.15);
  pointer-events: none;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  animation: highlightPulse 2s ease-in-out infinite;
}

@keyframes highlightPulse {
  0%, 100% { box-shadow: 0 0 0 4px rgba(26, 107, 111, 0.2), 0 0 30px rgba(26, 107, 111, 0.15); }
  50% { box-shadow: 0 0 0 6px rgba(26, 107, 111, 0.3), 0 0 40px rgba(26, 107, 111, 0.25); }
}

/* 提示气泡 */
.tooltip-bubble {
  position: fixed;
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.2), 0 2px 8px rgba(0, 0, 0, 0.1);
  pointer-events: auto;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  animation: tooltipIn 0.3s ease-out;
  color: #1a1a2e;
}

@keyframes tooltipIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 箭头 */
.tooltip-arrow {
  position: absolute;
  width: 12px;
  height: 12px;
  background: #fff;
  transform: rotate(45deg);
}

.bottom .tooltip-arrow {
  top: -6px;
  left: 50%;
  margin-left: -6px;
  box-shadow: -2px -2px 4px rgba(0, 0, 0, 0.05);
}

.top .tooltip-arrow {
  bottom: -6px;
  left: 50%;
  margin-left: -6px;
  box-shadow: 2px 2px 4px rgba(0, 0, 0, 0.05);
}

.right .tooltip-arrow {
  left: -6px;
  top: 50%;
  margin-top: -6px;
  box-shadow: -2px 2px 4px rgba(0, 0, 0, 0.05);
}

.left .tooltip-arrow {
  right: -6px;
  top: 50%;
  margin-top: -6px;
  box-shadow: 2px -2px 4px rgba(0, 0, 0, 0.05);
}

.tooltip-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.tooltip-step {
  display: flex;
  align-items: baseline;
  gap: 2px;
  font-variant-numeric: tabular-nums;
}

.step-current {
  font-size: 18px;
  font-weight: 700;
  color: #1a6b6f;
}

.step-divider {
  font-size: 13px;
  color: #999;
  margin: 0 2px;
}

.step-total {
  font-size: 13px;
  color: #999;
}

.tooltip-close {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: #f0f0f0;
  color: #666;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.tooltip-close:hover {
  background: #e0e0e0;
  color: #333;
}

.tooltip-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 6px;
  color: #1a1a2e;
}

.tooltip-desc {
  font-size: 13px;
  line-height: 1.6;
  color: #555;
  margin: 0 0 14px;
}

/* 进度条 */
.tooltip-progress {
  display: flex;
  gap: 6px;
  margin-bottom: 16px;
}

.progress-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e0e0e0;
  transition: all 0.2s;
}

.progress-dot.active {
  background: #1a6b6f;
  transform: scale(1.3);
}

.progress-dot.done {
  background: #7ec8cb;
}

/* 按钮 */
.tooltip-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.tut-btn {
  padding: 8px 18px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.tut-btn.primary {
  background: #1a6b6f;
  color: #fff;
}

.tut-btn.primary:hover {
  background: #155a5e;
}

.tut-btn.secondary {
  background: #f0f0f0;
  color: #555;
}

.tut-btn.secondary:hover {
  background: #e0e0e0;
}
</style>