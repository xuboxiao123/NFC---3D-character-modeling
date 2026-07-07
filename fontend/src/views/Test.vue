<template>
  <div class="container">

    <!-- 标题 -->
    <div class="title">NFC 3D Character Editor</div>

    <!-- 当前版本 -->
    <div class="panel">
      <div class="label">当前版本ID：</div>
      <input v-model="versionId" />
      <button @click="setCurrent">绑定版本</button>
    </div>

    <!-- 表情参数展示 -->
    <div class="panel">
      <div class="label">表情参数（实时）</div>

      <div class="grid">
        <div class="item">Exp1: {{ version.expression1 }}</div>
        <div class="item">Exp2: {{ version.expression2 }}</div>
        <div class="item">Exp3: {{ version.expression3 }}</div>
        <div class="item">Exp4: {{ version.expression4 }}</div>
        <div class="item">Exp5: {{ version.expression5 }}</div>
        <div class="item">Exp6: {{ version.expression6 }}</div>
        <div class="item">Exp7: {{ version.expression7 }}</div>
      </div>
    </div>

    <!-- 状态 -->
    <div class="panel">
      <div class="label">状态：</div>
      <div>{{ version.status }}</div>
    </div>

    <!-- 图片 -->
    <div class="panel">
      <div class="label">2D预览：</div>
      <img v-if="version.previewImageUrl" :src="version.previewImageUrl" />
    </div>

    <!-- 模型 -->
    <div class="panel">
      <div class="label">3D模型：</div>
      <a v-if="version.modelUrl" :href="version.modelUrl" target="_blank">
        打开模型
      </a>
    </div>

  </div>
</template>

<script>
export default {
  data() {
    return {
      versionId: 1,
      version: {},
      timer: null
    }
  },

  mounted() {
    this.setCurrent();
    this.startPolling();
  },

  beforeUnmount() {
    clearInterval(this.timer);
  },

  methods: {

    // ① 绑定当前版本（必须）
    async setCurrent() {
      await fetch(`/api/nfc/setCurrent?versionId=${this.versionId}`);
      console.log("已绑定版本", this.versionId);
    },

    // ② 获取版本数据
    async fetchVersion() {
      try {
        const res = await fetch(`/api/version/get?versionId=${this.versionId}`);
        this.version = await res.json();
      } catch (e) {
        console.error("获取失败", e);
      }
    },

    // ③ 轮询（核心）
    startPolling() {
      this.fetchVersion(); // 先执行一次

      this.timer = setInterval(() => {
        this.fetchVersion();
      }, 1000);
    }
  }
}
</script>

<style scoped>

.container {
  background: var(--bg-primary);
  min-height: 100vh;
  color: var(--text-primary);
  padding: 100px 24px 60px;
  font-family: 'Inter', 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  max-width: 640px;
  margin: 0 auto;
}

.title {
  font: 700 clamp(24px, 4vw, 32px)/1.1 'Georgia', 'Times New Roman', serif;
  margin-bottom: 28px;
  letter-spacing: -0.02em;
  padding-bottom: 14px;
  border-bottom: 2px solid var(--border-card);
  position: relative;
}

.title::after {
  content: '';
  position: absolute;
  bottom: -5px;
  left: 0;
  right: 0;
  height: 1px;
  background: var(--border-card);
}

.panel {
  border: 1px solid var(--border-card);
  padding: 20px;
  margin-bottom: 20px;
  border-radius: 2px;
  background: var(--input-bg);
  position: relative;
}

/* 面板角标 */
.panel::before,
.panel::after {
  content: '';
  position: absolute;
  width: 10px;
  height: 10px;
  border-color: var(--text-secondary);
  border-style: solid;
  opacity: 0.2;
  pointer-events: none;
}
.panel::before {
  top: 6px; left: 6px;
  border-width: 1px 0 0 1px;
}
.panel::after {
  bottom: 6px; right: 6px;
  border-width: 0 1px 1px 0;
}

.label {
  font: 700 9px/1 'Inter', sans-serif;
  letter-spacing: 0.25em;
  text-transform: uppercase;
  color: var(--text-secondary);
  margin-bottom: 12px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.item {
  padding: 10px;
  border: 1px solid var(--border-card);
  text-align: center;
  border-radius: 2px;
  font: 400 12px/1.4 'Inter', sans-serif;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
}

input {
  background: var(--input-bg);
  border: 1px solid var(--border-card);
  color: var(--text-primary);
  padding: 8px 12px;
  margin-right: 10px;
  border-radius: 2px;
  font: 400 13px/1.4 'Inter', sans-serif;
  outline: none;
  transition: border-color 0.2s;
}

input:focus {
  border-color: var(--text-primary);
}

button {
  background: transparent;
  border: 1.5px solid var(--text-primary);
  color: var(--text-primary);
  padding: 8px 16px;
  cursor: pointer;
  border-radius: 2px;
  font: 500 11px/1 'Inter', sans-serif;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  transition: background 0.2s, color 0.2s;
}

button:hover {
  background: var(--text-primary);
  color: var(--bg-primary);
}

img {
  max-width: 300px;
  border: 1px solid var(--border-card);
  margin-top: 10px;
  border-radius: 0;
}

</style>