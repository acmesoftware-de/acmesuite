import { useEffect, useState, type FormEvent } from 'react'
import { LogoGlyph } from '../shell/LogoGlyph'
import { useAuth, apiStatus } from './AuthContext'
import { authApi } from './authApi'
import type { ProviderOption } from './types'
import { versionLabel } from '../version'

const OIDC_MESSAGES: Record<string, string> = {
  pending: 'Dein Konto wartet auf Freischaltung durch einen Admin.',
  config: 'Der Identity-Provider ist nicht vollständig konfiguriert.',
  idp: 'Die Anmeldung wurde vom Identity-Provider abgelehnt.',
  error: 'Anmeldung über den Identity-Provider fehlgeschlagen.',
}

export function LoginScreen() {
  const { login, oidcError } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [federated, setFederated] = useState<ProviderOption[]>([])

  useEffect(() => {
    authApi
      .providers()
      .then((list) => setFederated(list.filter((p) => p.kind !== 'LOCAL')))
      .catch(() => setFederated([]))
  }, [])

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setBusy(true)
    try {
      await login(username.trim(), password)
    } catch (err) {
      setError(apiStatus(err) === 401 ? 'Benutzername oder Passwort falsch.' : 'Anmeldung fehlgeschlagen.')
      setBusy(false)
    }
  }

  return (
    <div className="acme-login">
      <form className="acme-login-card" onSubmit={onSubmit}>
        <div className="acme-login-brand">
          <LogoGlyph />
          <span className="acme-brand">
            ACME<span className="acme-brand-dim">SUITE</span>
          </span>
        </div>

        <div className="acme-login-title">Anmelden</div>

        {oidcError && (
          <div className="acme-error">{OIDC_MESSAGES[oidcError] ?? OIDC_MESSAGES.error}</div>
        )}

        <label className="acme-field">
          <span className="acme-label">Benutzername</span>
          <input
            className="acme-input"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoFocus
            autoComplete="username"
          />
        </label>

        <label className="acme-field">
          <span className="acme-label">Passwort</span>
          <input
            className="acme-input"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
          />
        </label>

        {error && <div className="acme-error">{error}</div>}

        <button className="acme-btn acme-btn--full" type="submit" disabled={busy || !username || !password}>
          {busy ? 'Anmelden…' : 'Anmelden'}
        </button>

        {federated.length > 0 && (
          <div className="acme-login-alt">
            <div className="acme-login-alt-sep">oder</div>
            {federated.map((p) => (
              <button
                key={p.id}
                type="button"
                className="acme-btn acme-btn--ghost acme-btn--full"
                onClick={() => {
                  window.location.href = authApi.oidcStartUrl(p.id)
                }}
              >
                Mit {p.displayName} anmelden
              </button>
            ))}
          </div>
        )}

        <div className="acme-login-foot">{versionLabel}</div>
      </form>
    </div>
  )
}
