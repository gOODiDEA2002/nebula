import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 4010,
    proxy: {
      '/api': {
        target: 'http://localhost:8081'
      },
      '/health': {
        target: 'http://localhost:8081'
      },
      '/performance': {
        target: 'http://localhost:8081'
      }
    }
  }
})
