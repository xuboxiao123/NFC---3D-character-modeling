<template>
  <div class="login-page">
    <div class="login-card">
      <h1 class="login-title">Welcome</h1>
      <p class="login-sub">Enter your user ID to continue</p>

      <div class="input-group">
        <input
          v-model="userId"
          placeholder="User ID"
          class="login-input"
          @keyup.enter="login"
        />
        <button class="login-btn" @click="login">→</button>
      </div>
    </div>

    <!-- 显示当前服务器访问地址 -->
    <div class="server-info" v-if="accessUrls.length > 0">
      <p class="server-label">Mobile Access URL</p>
      <div v-for="url in accessUrls" :key="url" class="server-url">
        {{ url }}
      </div>
      <p class="server-hint">Connect to the same WiFi and enter the URL above in your mobile browser</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const userId = ref('1')
const router = useRouter()
const accessUrls = ref([])

const login = () => {
  if (!userId.value.trim()) return
  localStorage.setItem('userId', userId.value)
  router.push('/home')
}

const loadServerInfo = async () => {
  try {
    const res = await axios.get('/api/system/info')
    accessUrls.value = res.data.accessUrls || []
  } catch (e) {
    // 如果接口不可用，用当前页面地址
    accessUrls.value = [window.location.origin]
  }
}

onMounted(() => {
  loadServerInfo()
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--bg-primary);
  padding: 24px;
  font-family: 'Inter', 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  position: relative;
}

.login-card {
  width: 100%;
  max-width: 360px;
  text-align: center;
  position: relative;
  padding: 48px 32px;
  border: 1px solid var(--border-card);
  border-radius: 2px;
}

/* 角标装饰 */
.login-card::before,
.login-card::after {
  content: '';
  position: absolute;
  width: 20px;
  height: 20px;
  border-color: var(--text-secondary);
  border-style: solid;
  opacity: 0.25;
  pointer-events: none;
}
.login-card::before {
  top: 8px; left: 8px;
  border-width: 1px 0 0 1px;
}
.login-card::after {
  bottom: 8px; right: 8px;
  border-width: 0 1px 1px 0;
}

.login-title {
  font: 800 clamp(32px, 6vw, 42px)/0.9 'Georgia', 'Times New Roman', serif;
  letter-spacing: -0.02em;
  margin: 0 0 12px;
  color: var(--text-primary);
}

.login-sub {
  font: 300 12px/1.4 'Inter', sans-serif;
  color: var(--text-secondary);
  margin: 0 0 36px;
  letter-spacing: 0.15em;
  text-transform: uppercase;
}

.input-group {
  display: flex;
  gap: 8px;
}

.login-input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid var(--border-card);
  border-radius: 2px;
  background: var(--input-bg);
  color: var(--text-primary);
  font: 400 14px/1.4 'Inter', sans-serif;
  outline: none;
  transition: border-color 0.2s;
  letter-spacing: 0.05em;
}

.login-input::placeholder {
  color: var(--text-secondary);
  letter-spacing: 0.1em;
}

.login-input:focus {
  border-color: var(--text-primary);
}

.login-btn {
  width: 48px;
  height: 48px;
  border-radius: 2px;
  border: 1.5px solid var(--text-primary);
  background: transparent;
  color: var(--text-primary);
  font: 400 18px/1 'Georgia', serif;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s, color 0.2s, transform 0.1s;
  flex-shrink: 0;
}

.login-btn:hover {
  background: var(--text-primary);
  color: var(--bg-primary);
}

.login-btn:active {
  transform: scale(0.95);
}

/* 服务器信息 — 几何化 */
.server-info {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  text-align: center;
  max-width: 360px;
  width: 100%;
  padding: 0 24px;
}

.server-label {
  font: 700 8px/1 'Inter', sans-serif;
  color: var(--text-secondary);
  margin: 0 0 8px;
  letter-spacing: 0.25em;
  text-transform: uppercase;
}

.server-url {
  font: 500 12px/1.4 'Inter', sans-serif;
  color: var(--text-primary);
  padding: 8px 12px;
  background: var(--input-bg);
  border: 1px solid var(--border-card);
  border-radius: 2px;
  margin-bottom: 6px;
  word-break: break-all;
  letter-spacing: 0.02em;
}

.server-hint {
  font: 300 10px/1.4 'Inter', sans-serif;
  color: var(--text-secondary);
  margin-top: 8px;
  opacity: 0.7;
  letter-spacing: 0.05em;
}
</style>