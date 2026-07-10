import { useEffect, useState } from 'react'
import type { AccessRole, UserStatus } from '../../auth/types'
import { adminApi, type AdminUser } from './adminApi'

const ROLES: AccessRole[] = ['WATCH', 'WORK', 'ADMIN']
const STATUSES: UserStatus[] = ['ACTIVE', 'PENDING', 'DISABLED']

function initials(u: AdminUser): string {
  const base = u.displayName || u.username || u.email || '?'
  const parts = base.trim().split(/\s+/)
  return (parts.length > 1 ? parts[0][0] + parts[1][0] : base.slice(0, 2)).toUpperCase()
}

export function UsersView() {
  const [users, setUsers] = useState<AdminUser[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)
  const [tempPassword, setTempPassword] = useState<{ username: string; password: string } | null>(null)

  const [form, setForm] = useState({ username: '', displayName: '', email: '', role: 'WORK' as AccessRole })

  useEffect(() => {
    adminApi.listUsers().then(setUsers).catch(() => setError('Konnte Nutzer nicht laden.'))
  }, [])

  async function changeRole(id: string, role: AccessRole) {
    const updated = await adminApi.setRole(id, role)
    setUsers((prev) => prev?.map((u) => (u.id === id ? updated : u)) ?? null)
  }

  async function changeStatus(id: string, status: UserStatus) {
    const updated = await adminApi.setStatus(id, status)
    setUsers((prev) => prev?.map((u) => (u.id === id ? updated : u)) ?? null)
  }

  async function submitCreate() {
    try {
      const res = await adminApi.createUser(form)
      setUsers((prev) => [...(prev ?? []), res.user])
      setTempPassword({ username: res.user.username ?? '', password: res.temporaryPassword })
      setForm({ username: '', displayName: '', email: '', role: 'WORK' })
      setCreating(false)
    } catch {
      setError('Anlegen fehlgeschlagen (Name schon vergeben?).')
    }
  }

  return (
    <div className="acme-content">
      <div className="acme-toolbar">
        <div className="acme-section-label">Nutzer &amp; Rollen</div>
        <button className="acme-btn" onClick={() => setCreating((v) => !v)}>
          {creating ? 'Abbrechen' : '+ Nutzer'}
        </button>
      </div>

      {tempPassword && (
        <div className="acme-notice">
          Nutzer <strong>{tempPassword.username}</strong> angelegt. Einmal-Passwort (wird nur jetzt
          angezeigt): <code className="acme-code">{tempPassword.password}</code>
          <button className="acme-notice-close" onClick={() => setTempPassword(null)}>
            ✕
          </button>
        </div>
      )}

      {creating && (
        <div className="acme-form-card">
          <div className="acme-form-grid">
            <label className="acme-field">
              <span className="acme-label">Benutzername</span>
              <input className="acme-input" value={form.username}
                onChange={(e) => setForm({ ...form, username: e.target.value })} />
            </label>
            <label className="acme-field">
              <span className="acme-label">Anzeigename</span>
              <input className="acme-input" value={form.displayName}
                onChange={(e) => setForm({ ...form, displayName: e.target.value })} />
            </label>
            <label className="acme-field">
              <span className="acme-label">E-Mail</span>
              <input className="acme-input" value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </label>
            <label className="acme-field">
              <span className="acme-label">Rolle</span>
              <select className="acme-select" value={form.role}
                onChange={(e) => setForm({ ...form, role: e.target.value as AccessRole })}>
                {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
              </select>
            </label>
          </div>
          <button className="acme-btn" disabled={!form.username} onClick={submitCreate}>
            Anlegen
          </button>
        </div>
      )}

      {error && <div className="acme-error">{error}</div>}

      <div className="acme-users-head">
        <span className="acme-th">NUTZER</span>
        <span className="acme-th">ROLLE</span>
        <span className="acme-th">STATUS</span>
        <span className="acme-th">PROVIDER</span>
      </div>
      {users?.map((u) => (
        <div className="acme-users-row" key={u.id}>
          <div className="acme-user-cell">
            <span className="acme-avatar acme-avatar--sm">{initials(u)}</span>
            <div className="acme-user-meta">
              <div className="acme-user-name">{u.displayName || u.username}</div>
              <div className="acme-user-sub">{u.email || u.username}</div>
            </div>
          </div>
          <select className="acme-select" value={u.role}
            onChange={(e) => changeRole(u.id, e.target.value as AccessRole)}>
            {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
          </select>
          <select className="acme-select" value={u.status}
            onChange={(e) => changeStatus(u.id, e.target.value as UserStatus)}>
            {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
          <span className="acme-provider-tag">{u.authProvider}</span>
        </div>
      ))}
      {users && users.length === 0 && <div className="acme-empty">Keine Nutzer.</div>}
    </div>
  )
}
