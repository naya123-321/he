import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    port: 8081,
    hmr: {
      overlay: true
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        // 后端 context-path 已经是 /api，所以不需要路径重写
        // 前端请求 /api/xxx 会被代理到 http://localhost:8080/api/xxx
      },
      // 配置静态资源代理，用于访问上传的图片
      '/uploads': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        // 直接代理到后端，不需要路径重写
      }
    }
  },
  build: {
    chunkSizeWarningLimit: 1000,
    rollupOptions: {
      output: {
        manualChunks: undefined
      }
    }
  },
  optimizeDeps: {
    include: ['vue', 'vue-router', 'element-plus', '@element-plus/icons-vue']
  },
  esbuild: {
    target: 'esnext'
  }
})