import { useEffect } from 'react'

/** A single entry in the Chatter change timeline (ADR-0010). */
export interface ChatterEvent {
  id: string
  /** Uppercase mono badge, e.g. "GEÄNDERT". */
  badge: string
  kind: 'add' | 'mod' | 'del'
  /** Version number — shown only to AUDIT holders. */
  version?: number
  actor: string | null
  /** ISO-8601 timestamp. */
  at: string
  /** Optional one-line snapshot summary. */
  summary?: string
}

interface ChatterProps {
  open: boolean
  onClose: () => void
  title: string
  subtitle?: string
  /** Last-change stamp — shown to everyone (ADR-0010). */
  lastChanged: { by: string | null; at: string | null }
  /** AUDIT holders additionally see the full versioned timeline. */
  canSeeVersions: boolean
  /** null = loading, [] = no history yet. Ignored when !canSeeVersions. */
  events: ChatterEvent[] | null
}

function fmt(iso: string | null): string {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleString('de-DE', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

/**
 * Chatter — the per-record change feed (ADR-0010). Everyone sees who/when the record last
 * changed; only AUDIT holders see the versioned timeline (version numbers + earlier states).
 * A right-side drawer, reusable across entities.
 */
export function Chatter({ open, onClose, title, subtitle, lastChanged, canSeeVersions, events }: ChatterProps) {
  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])

  if (!open) return null

  return (
    <div className="acme-drawer-scrim" onClick={onClose}>
      <aside className="acme-drawer" onClick={(e) => e.stopPropagation()} role="dialog" aria-label="Chatter">
        <div className="acme-drawer-head">
          <div>
            <div className="acme-section-label">Chatter</div>
            <div className="acme-drawer-title">{title}</div>
            {subtitle && <div className="acme-drawer-sub">{subtitle}</div>}
          </div>
          <button className="acme-drawer-close" onClick={onClose} aria-label="Schließen">✕</button>
        </div>

        <div className="acme-lastchange">
          Zuletzt geändert von <strong>{lastChanged.by ?? '—'}</strong> · {fmt(lastChanged.at)}
        </div>

        {!canSeeVersions ? (
          <div className="acme-chatter-locked">
            Die vollständige Versionshistorie ist nur mit der AUDIT-Berechtigung sichtbar.
          </div>
        ) : events === null ? (
          <div className="acme-empty">Historie wird geladen …</div>
        ) : events.length === 0 ? (
          <div className="acme-empty">Keine Historie.</div>
        ) : (
          <ol className="acme-timeline">
            {events.map((ev) => (
              <li className="acme-tl-item" key={ev.id}>
                <span className={`acme-tl-dot acme-tl-dot--${ev.kind}`} aria-hidden="true" />
                <div className="acme-tl-body">
                  <div className="acme-tl-line">
                    <span className={`acme-tl-badge acme-tl-badge--${ev.kind}`}>{ev.badge}</span>
                    {ev.version !== undefined && <span className="acme-tl-ver">v{ev.version}</span>}
                    <span className="acme-tl-actor">{ev.actor ?? 'system'}</span>
                  </div>
                  {ev.summary && <div className="acme-tl-summary">{ev.summary}</div>}
                  <div className="acme-tl-time">{fmt(ev.at)}</div>
                </div>
              </li>
            ))}
          </ol>
        )}
      </aside>
    </div>
  )
}
