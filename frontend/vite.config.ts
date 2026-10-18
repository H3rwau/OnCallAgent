import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxy = {
    '/api': {
      target: env.API_PROXY_TARGET || 'http://127.0.0.1:9900',
      changeOrigin: true,
      // /ai_ops may legitimately take the backend's full 10-minute SSE window.
      timeout: 650_000,
      proxyTimeout: 650_000,
    },
  }
  return {
    plugins: [vue()],
    server: { port: 5173, strictPort: true, proxy },
    preview: { port: 4173, proxy },
  }
})
