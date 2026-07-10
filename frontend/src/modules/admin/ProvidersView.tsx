import { useEffect, useState } from 'react'
import { adminApi, type ProviderConfig } from './adminApi'

export function ProvidersView() {
  const [configs, setConfigs] = useState<ProviderConfig[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    adminApi
      .listProviderConfigs()
      .then(setConfigs)
      .catch(() => setError('Konnte Provider-Konfiguration nicht laden.'))
  }, [])

  function onSaved(updated: ProviderConfig) {
    setConfigs((prev) => prev?.map((c) => (c.providerId === updated.providerId ? updated : c)) ?? null)
  }

  return (
    <div className="acme-content">
      <div className="acme-toolbar">
        <div className="acme-section-label">Authentifizierung — Identity-Provider</div>
      </div>
      <div className="acme-provider-hint">
        Föderierte Anmeldung; Rollen werden immer lokal vergeben. Secrets werden verschlüsselt
        gespeichert und nie zurückgegeben.
      </div>
      {error && <div className="acme-error">{error}</div>}
      {configs?.map((cfg) => (
        <ProviderCard key={cfg.providerId} config={cfg} onSaved={onSaved} />
      ))}
    </div>
  )
}

function ProviderCard({
  config,
  onSaved,
}: {
  config: ProviderConfig
  onSaved: (c: ProviderConfig) => void
}) {
  const [enabled, setEnabled] = useState(config.enabled)
  const [values, setValues] = useState<Record<string, string>>({ ...config.values })
  const [secretDrafts, setSecretDrafts] = useState<Record<string, string>>({})
  const [busy, setBusy] = useState(false)
  const [saved, setSaved] = useState(false)

  async function save() {
    setBusy(true)
    setSaved(false)
    // Non-secret values + only secrets the admin actually typed (blank keeps the stored one).
    const payload: Record<string, string> = { ...values }
    for (const [k, v] of Object.entries(secretDrafts)) {
      if (v) payload[k] = v
    }
    try {
      const updated = await adminApi.upsertProviderConfig(config.providerId, { enabled, values: payload })
      onSaved(updated)
      setValues({ ...updated.values })
      setSecretDrafts({})
      setSaved(true)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="acme-provider-card">
      <div className="acme-provider-head">
        <div>
          <div className="acme-provider-name">{config.displayName}</div>
          <div className="acme-provider-kind">{config.kind}</div>
        </div>
        <label className="acme-switch">
          <input type="checkbox" checked={enabled} onChange={(e) => setEnabled(e.target.checked)} />
          <span className="acme-switch-track" />
          <span className="acme-switch-label">{enabled ? 'Aktiv' : 'Inaktiv'}</span>
        </label>
      </div>

      <div className="acme-form-grid">
        {config.schema.map((f) => {
          if (f.type === 'SECRET') {
            const isSet = config.secretsSet.includes(f.key)
            return (
              <label className="acme-field" key={f.key}>
                <span className="acme-label">
                  {f.label}
                  {f.required ? ' *' : ''}
                </span>
                <input
                  className="acme-input"
                  type="password"
                  placeholder={isSet ? '•••••••• (gesetzt)' : ''}
                  value={secretDrafts[f.key] ?? ''}
                  onChange={(e) => setSecretDrafts({ ...secretDrafts, [f.key]: e.target.value })}
                />
              </label>
            )
          }
          return (
            <label className="acme-field" key={f.key}>
              <span className="acme-label">
                {f.label}
                {f.required ? ' *' : ''}
              </span>
              <input
                className="acme-input"
                value={values[f.key] ?? ''}
                onChange={(e) => setValues({ ...values, [f.key]: e.target.value })}
              />
            </label>
          )
        })}
      </div>

      <div className="acme-provider-actions">
        <button className="acme-btn" onClick={save} disabled={busy}>
          {busy ? 'Speichern…' : 'Speichern'}
        </button>
        {saved && <span className="acme-saved">Gespeichert ✓</span>}
      </div>
    </div>
  )
}
