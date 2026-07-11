import { execSync } from 'node:child_process'
import { readFileSync } from 'node:fs'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// The frontend talks to the ACMEsuite REST API exclusively (contract-first, never
// against internal code). In dev, /api is proxied to the local TLS edge so the app
// runs same-origin without CORS. Override the target with ACMESUITE_API_ORIGIN.
const apiOrigin = process.env.ACMESUITE_API_ORIGIN ?? 'https://localhost:8543'

// Build stamp — version (package.json) + git commit, baked in at build time so the
// running app can report exactly which build it is (see src/version.ts).
const pkg = JSON.parse(readFileSync(new URL('./package.json', import.meta.url), 'utf-8'))
function gitCommit(): string {
  if (process.env.GIT_COMMIT) return process.env.GIT_COMMIT
  try {
    return execSync('git rev-parse --short HEAD', { stdio: ['ignore', 'pipe', 'ignore'] })
      .toString()
      .trim()
  } catch {
    return 'unknown'
  }
}

export default defineConfig({
  define: {
    __APP_VERSION__: JSON.stringify(pkg.version as string),
    __GIT_COMMIT__: JSON.stringify(gitCommit()),
    __BUILD_TIME__: JSON.stringify(new Date().toISOString()),
  },
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
