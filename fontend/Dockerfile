# ============================================================
#  阶段1: 用 Node 编译 Vue 产物
# ============================================================
FROM node:20-alpine AS builder

WORKDIR /build

# 走 npm 缓存层
COPY package.json package-lock.json* ./
RUN npm ci --no-audit --no-fund || npm install --no-audit --no-fund

# 复制源码并编译
COPY . .
RUN npm run build

# ============================================================
#  阶段2: nginx 静态托管 + 反代后端
# ============================================================
FROM nginx:1.27-alpine

# 移除默认站点
RUN rm -rf /usr/share/nginx/html/* /etc/nginx/conf.d/default.conf

# 复制自定义 nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 复制打包好的前端静态文件
COPY --from=builder /build/dist /usr/share/nginx/html

# MediaPipe 需要的 wasm/binarypb 静态文件
# （已经在 vite build 时打入 dist，无需额外复制）

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]