import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './base.css'
import { ThemeProvider } from './theme/ThemeProvider'
import { AuthProvider } from './auth/AuthContext'
import { App } from './App'
import { BUILD, versionLabel } from './version'

// Make the running build discoverable (console + a global for support/automation).
;(window as unknown as { __ACMESUITE_BUILD__: typeof BUILD }).__ACMESUITE_BUILD__ = BUILD
console.info(`ACMEsuite ${versionLabel} (built ${BUILD.time})`)

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider>
      <AuthProvider>
        <App />
      </AuthProvider>
    </ThemeProvider>
  </StrictMode>,
)
