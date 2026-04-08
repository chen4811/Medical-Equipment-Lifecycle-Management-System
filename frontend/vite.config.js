import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  // 核心：GitHub Pages子路径，必须和仓库名完全一致！
  base: '/Medical-Equipment-Lifecycle-Management-System/',
  plugins: [vue()],
  server: {
    port: 3000
  }
})
