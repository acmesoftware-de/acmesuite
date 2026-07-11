import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// The frontend talks to the ACMEsuite REST API exclusively (contract-first, never
// against internal code). In dev, /api is proxied to the local TLS edge so the app
// runs same-origin without CORS. Override the target with ACMESUITE_API_ORIGIN.
const apiOrigin = process.env.ACMESUITE_API_ORIGIN ?? 'https://localhost:8543'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5273,
    proxy: {
      '/api': {
        target: apiOrigin,
        changeOrigin: true,
        secure: false,
      },
    },
  },
})
