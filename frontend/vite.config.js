import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  // 🔥 这一行是关键！必须加！不加就404！
  base: '/Medical-Equipment-Lifecycle-Management-System/',

  plugins: [vue()],

  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  }
})
