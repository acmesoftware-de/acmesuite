import { useState } from 'react'
import { useAssist } from './useAssist'

interface AssistProps {
  module: string
  subView?: string
}

// German suggestion chips from the design handoff (ADR-0008 Appendix A / prototype).
const SUGGESTIONS = ['Forecast Q3', 'Nächste Aktionen', 'Top-Konten', 'Verlorene Deals']

/**
 * ACMEassist — the bottom-anchored co-pilot (ADR-0008). Collapsed bar + expandable panel; the turn
 * streams over SSE (as the signed-in user). Class-contract only; theming lives in themes/.
 */
export function Assist({ module, subView }: AssistProps) {
  const { open, setOpen, busy, turns, ask } = useAssist({ module, subView })
  const [input, setInput] = useState('')

  const submit = () => {
    ask(input)
    setInput('')
  }

  return (
    <>
      {open && (
        <div className="acme-assist-panel">
          <div className="acme-assist-head">
            <span className="acme-assist-mark">✦</span>
            <span className="acme-assist-head-title">ACMEASSIST</span>
            <span className="acme-assist-ctx">{subView ? `${module} · ${subView}` : module}</span>
            <button
              className="acme-assist-close"
              onClick={() => setOpen(false)}
              aria-label="Schließen"
            >
              ×
            </button>
          </div>

          <div className="acme-assist-body">
            {turns.length === 0 && (
              <div className="acme-assist-suggests">
                {SUGGESTIONS.map((s) => (
                  <button key={s} className="acme-assist-chip" onClick={() => ask(s)} disabled={busy}>
                    <span className="acme-assist-chip-mark">✦</span>
                    {s}
                  </button>
                ))}
              </div>
            )}

            {turns.map((turn, i) => (
              <div key={i} className="acme-assist-turn">
                <div className="acme-assist-q">{turn.question}</div>
                <div className="acme-assist-a">
                  <span className="acme-assist-turn-mark">✦</span>
                  <div className="acme-assist-a-body">
                    {turn.tools.length > 0 && (
                      <div className="acme-assist-tools">
                        {turn.tools.map((t, j) => (
                          <span key={j} className="acme-assist-tool">
                            {t}
                          </span>
                        ))}
                      </div>
                    )}
                    <div className="acme-assist-answer">
                      {turn.error ? 'Es ist ein Fehler aufgetreten.' : turn.answer}
                      {turn.streaming && <span className="acme-assist-cursor" />}
                    </div>
                    {turn.sources.length > 0 && (
                      <div className="acme-assist-source">Quellen: {turn.sources.join(' · ')}</div>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>

          <div className="acme-assist-input">
            <input
              className="acme-assist-field"
              placeholder="ACMEassist fragen …"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') submit()
              }}
              autoFocus
            />
            <button className="acme-assist-send" onClick={submit} disabled={busy || !input.trim()}>
              Senden
            </button>
          </div>

          <div className="acme-assist-disclosure">
            KI-Assistent — Antworten können Fehler enthalten. Handelt mit deiner Rolle; Schreibaktionen
            werden bestätigt.
          </div>
        </div>
      )}

      <button className="acme-assist" onClick={() => setOpen((o) => !o)} aria-expanded={open}>
        <span className="acme-assist-mark">✦</span>
        <span className="acme-assist-hint">ACMEassist — fragen, zusammenfassen, entwerfen</span>
        <span className="acme-assist-kbd">⌘K</span>
      </button>
    </>
  )
}
