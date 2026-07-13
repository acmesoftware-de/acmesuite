import { useState, type FormEvent } from 'react'
import { LogoGlyph } from '../shell/LogoGlyph'
import { useAuth } from './AuthContext'
import { versionLabel } from '../version'

const MIN_LENGTH = 10

/**
 * Shown instead of the normal login form on a brand-new instance
 * (acme.base.auth.bootstrap.allow-self-claim=true, no admin exists yet). Whoever reaches this
 * first sets the initial admin password directly — no log-reading needed. Only ever enabled on
 * instances not reachable by untrusted parties before the real operator gets here.
 */
export function ClaimAdminScreen() {
  const { claimAdmin } = useAuth()
  const [username, setUsername] = useState('admin')
  const [pw, setPw] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    if (pw.length < MIN_LENGTH) {
      setError(`Mindestens ${MIN_LENGTH} Zeichen.`)
      return
    }
    if (pw !== confirm) {
      setError('Die Passwörter stimmen nicht überein.')
      return
    }
    setBusy(true)
    try {
      await claimAdmin(username.trim(), pw)
    } catch {
      setError('Konnte nicht angelegt werden — evtl. wurde der Admin gerade eben schon vergeben.')
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

        <div className="acme-login-title">Admin-Konto einrichten</div>
        <div className="acme-login-sub">
          Diese Instanz ist noch frisch — leg das erste Admin-Konto mit deinem eigenen Passwort an.
        </div>

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
            value={pw}
            onChange={(e) => setPw(e.target.value)}
            autoComplete="new-password"
          />
        </label>
        <label className="acme-field">
          <span className="acme-label">Bestätigen</span>
          <input
            className="acme-input"
            type="password"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            autoComplete="new-password"
          />
        </label>

        {error && <div className="acme-error">{error}</div>}

        <button className="acme-btn acme-btn--full" type="submit" disabled={busy || !username || !pw}>
          {busy ? 'Anlegen…' : 'Admin-Konto anlegen'}
        </button>

        <div className="acme-login-foot">{versionLabel}</div>
      </form>
    </div>
  )
}
