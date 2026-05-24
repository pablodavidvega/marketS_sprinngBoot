import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: true,
        // Timeout extendido para subidas de imágenes y video
        proxyTimeout: 120000,
        timeout: 120000,
      },
      '/uploads': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: true,
      },
    },
  },
})
