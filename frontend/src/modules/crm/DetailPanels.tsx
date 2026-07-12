import { useState } from 'react'
import type { Deal, MailThread } from './crmApi'
import { amountOf, fmtK, ownerInitials, stageLabel } from './pipelineModel'

/** Initials for a person/company name, e.g. "Sara Mena" → "SM". */
export function nameInitials(name: string): string {
  const parts = name.trim().split(/\s+/)
  return (parts.length > 1 ? parts[0][0] + parts[1][0] : name.slice(0, 2)).toUpperCase()
}

function fmtDateTime(iso?: string | null): string {
  if (!iso) return ''
  const d = new Date(iso)
  if (isNaN(d.getTime())) return ''
  return d.toLocaleString('de-DE', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })
}

/** Compact list of the deals belonging to a customer/contact. */
export function DealMiniList({ deals, showContact = false }: { deals: Deal[]; showContact?: boolean }) {
  if (deals.length === 0) return <div className="acme-empty">Keine Deals.</div>
  return (
    <div className="acme-deal-mini-list">
      {deals.map((d) => (
        <div className="acme-deal-mini" key={d.id}>
          <span className="acme-avatar acme-avatar--card" data-owner={ownerInitials(d)}>
            {ownerInitials(d)}
          </span>
          <div className="acme-deal-mini-main">
            <div className="acme-deal-mini-title">{d.company}</div>
            {showContact && d.contact && <div className="acme-deal-mini-sub">{d.contact}</div>}
          </div>
          <span className="acme-stage-chip" data-stage={d.stage}>{stageLabel(d.stage)}</span>
          <span className="acme-deal-mini-value">{fmtK(amountOf(d))}</span>
        </div>
      ))}
    </div>
  )
}

/** Mail threads for a customer/contact; a thread expands to show its messages. */
export function ThreadPanel({ threads }: { threads: MailThread[] }) {
  const [openId, setOpenId] = useState<string | null>(null)
  if (threads.length === 0) return <div className="acme-empty">Keine Korrespondenz.</div>
  return (
    <div className="acme-thread-list">
      {threads.map((t) => {
        const open = openId === t.id
        return (
          <div className={`acme-thread${open ? ' is-open' : ''}`} key={t.id}>
            <button className="acme-thread-head" onClick={() => setOpenId(open ? null : t.id)}>
              <span className="acme-thread-subject">{t.subject}</span>
              <span className="acme-thread-meta">
                {t.messageCount ?? t.messages?.length ?? 0} · {fmtDateTime(t.lastMessageAt)}
              </span>
              {!open && t.preview && <span className="acme-thread-preview">{t.preview}</span>}
            </button>
            {open && (
              <div className="acme-thread-messages">
                {(t.messages ?? []).map((m) => (
                  <div className={`acme-msg acme-msg--${m.direction.toLowerCase()}`} key={m.id}>
                    <div className="acme-msg-head">
                      <span className="acme-msg-from">{m.from ?? (m.direction === 'OUTBOUND' ? 'Wir' : '—')}</span>
                      <span className="acme-msg-time">{fmtDateTime(m.sentAt)}</span>
                    </div>
                    <div className="acme-msg-body">{m.body ?? m.snippet}</div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )
      })}
    </div>
  )
}
