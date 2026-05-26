import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const DEV_PROXY_TARGET =
  process.env.VITE_DEV_PROXY_TARGET ?? 'http://localhost'

const proxyRule = {
  target: DEV_PROXY_TARGET,
  changeOrigin: true,
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': proxyRule,
      '/v3/api-docs': proxyRule,
      '/swagger-ui': proxyRule,
    },
  },
})
