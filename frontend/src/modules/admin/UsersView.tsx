import { useEffect, useState } from 'react'
import type { AccessRole, UserStatus } from '../../auth/types'
import { useAuth } from '../../auth/AuthContext'
import { Chatter, type ChatterEvent } from '../../shell/Chatter'
import { adminApi, type AdminUser, type RevisionType, type UserRevision } from './adminApi'

const ROLES: AccessRole[] = ['WATCH', 'WORK', 'ADMIN']
const STATUSES: UserStatus[] = ['ACTIVE', 'PENDING', 'DISABLED']

const BADGE: Record<RevisionType, string> = { ADD: 'ANGELEGT', MOD: 'GEÄNDERT', DEL: 'GELÖSCHT' }
const KIND: Record<RevisionType, ChatterEvent['kind']> = { ADD: 'add', MOD: 'mod', DEL: 'del' }

function initials(u: AdminUser): string {
  const base = u.displayName || u.username || u.email || '?'
  const parts = base.trim().split(/\s+/)
  return (parts.length > 1 ? parts[0][0] + parts[1][0] : base.slice(0, 2)).toUpperCase()
}

function toEvent(r: UserRevision, actorName: (id: string | null) => string | null): ChatterEvent {
  const summary = `Rolle ${r.role} · Status ${r.status}${r.auditor ? ' · AUDIT' : ''}`
  return {
    id: String(r.revision),
    badge: r.deleted ? BADGE.DEL : BADGE[r.changeType],
    kind: r.deleted ? 'del' : KIND[r.changeType],
    version: r.revision,
    actor: actorName(r.changedBy),
    at: r.changedAt,
    summary,
  }
}

export function UsersView() {
  const { isAuditor } = useAuth()
  const [users, setUsers] = useState<AdminUser[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)
  const [tempPassword, setTempPassword] = useState<{ username: string; password: string } | null>(null)

  const [form, setForm] = useState({ username: '', displayName: '', email: '', role: 'WORK' as AccessRole })

  // Chatter drawer: the record whose change feed is open, plus its lazily-loaded history.
  const [chatterFor, setChatterFor] = useState<AdminUser | null>(null)
  const [history, setHistory] = useState<ChatterEvent[] | null>(null)

  useEffect(() => {
    adminApi.listUsers().then(setUsers).catch(() => setError('Konnte Nutzer nicht laden.'))
  }, [])

  // Actor stamps store the user id; resolve to a friendly name where we know it ('von wem').
  function actorName(id: string | null): string | null {
    if (!id) return null
    const u = users?.find((x) => x.id === id)
    return u ? (u.displayName || u.username || id) : id
  }

  function patch(updated: AdminUser) {
    setUsers((prev) => prev?.map((u) => (u.id === updated.id ? updated : u)) ?? null)
    setChatterFor((prev) => (prev?.id === updated.id ? updated : prev))
  }

  async function changeRole(id: string, role: AccessRole) {
    patch(await adminApi.setRole(id, role))
  }

  async function changeStatus(id: string, status: UserStatus) {
    patch(await adminApi.setStatus(id, status))
  }

  async function changeAuditor(id: string, auditor: boolean) {
    patch(await adminApi.setAuditor(id, auditor))
  }

  function openChatter(u: AdminUser) {
    setChatterFor(u)
    setHistory(null)
    if (isAuditor) {
      adminApi
        .getUserHistory(u.id)
        .then((revs) => setHistory(revs.map((r) => toEvent(r, actorName)).reverse())) // newest first
        .catch(() => setHistory([]))
    }
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
        <span className="acme-th">AUDIT</span>
        <span className="acme-th">PROVIDER</span>
        <span className="acme-th" />
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
          <label className="acme-switch" title="AUDIT-Berechtigung (Versionshistorie)">
            <input type="checkbox" checked={u.auditor}
              onChange={(e) => changeAuditor(u.id, e.target.checked)} />
            <span className="acme-switch-track" />
          </label>
          <span className="acme-provider-tag">{u.authProvider}</span>
          <button className="acme-icon-btn" title="Chatter — Änderungshistorie"
            onClick={() => openChatter(u)}>
            ⌗
          </button>
        </div>
      ))}
      {users && users.length === 0 && <div className="acme-empty">Keine Nutzer.</div>}

      <Chatter
        open={chatterFor !== null}
        onClose={() => setChatterFor(null)}
        title={chatterFor?.displayName || chatterFor?.username || chatterFor?.email || ''}
        subtitle={chatterFor ? `${chatterFor.role} · ${chatterFor.status}` : undefined}
        lastChanged={{ by: actorName(chatterFor?.updatedBy ?? null), at: chatterFor?.updatedAt ?? null }}
        canSeeVersions={isAuditor}
        events={history}
      />
    </div>
  )
}
