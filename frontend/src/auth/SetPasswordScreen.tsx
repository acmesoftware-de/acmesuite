import { useState, type FormEvent } from 'react'
import { LogoGlyph } from '../shell/LogoGlyph'
import { useAuth } from './AuthContext'

const MIN_LENGTH = 10

export function SetPasswordScreen() {
  const { user, completeSetPassword, logout } = useAuth()
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
      await completeSetPassword(pw)
    } catch {
      setError('Konnte nicht gesetzt werden.')
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

        <div className="acme-login-title">Neues Passwort setzen</div>
        <div className="acme-login-sub">
          Willkommen{user?.displayName ? `, ${user.displayName}` : ''}. Bitte vergib ein eigenes
          Passwort, um fortzufahren.
        </div>

        <label className="acme-field">
          <span className="acme-label">Neues Passwort</span>
          <input className="acme-input" type="password" value={pw} onChange={(e) => setPw(e.target.value)} autoFocus />
        </label>
        <label className="acme-field">
          <span className="acme-label">Bestätigen</span>
          <input
            className="acme-input"
            type="password"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
          />
        </label>

        {error && <div className="acme-error">{error}</div>}

        <button className="acme-btn acme-btn--full" type="submit" disabled={busy}>
          {busy ? 'Speichern…' : 'Passwort setzen'}
        </button>
        <button type="button" className="acme-btn acme-btn--ghost acme-btn--full" onClick={logout}>
          Abbrechen
        </button>
      </form>
    </div>
  )
}
