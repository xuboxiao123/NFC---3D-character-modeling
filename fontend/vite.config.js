import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import fs from 'fs'
import path from 'path'

const certPath = path.resolve(__dirname, '.cert')
const keyPath = path.resolve(__dirname, '.key')

const httpsConfig = fs.existsSync(certPath) && fs.existsSync(keyPath)
  ? {
      cert: fs.readFileSync(certPath),
      key: fs.readFileSync(keyPath)
    }
  : true

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    https: httpsConfig,

    proxy: {
      '/api': {
        target: 'http://localhost:8088',
        changeOrigin: true
      },
      '/models': {
        target: 'http://localhost:8088',
        changeOrigin: true
      }
    }
  }
})