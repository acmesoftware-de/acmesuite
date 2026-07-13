import { Fragment, useEffect, useState } from 'react'
import { adminApi, type DbTable } from './adminApi'

export function DbSchemaView() {
  const [tables, setTables] = useState<DbTable[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    adminApi.getDbSchema().then(setTables).catch(() => setError('Konnte Datenbankschema nicht laden.'))
  }, [])

  return (
    <div className="acme-content">
      <div className="acme-toolbar">
        <div className="acme-section-label">Datenbank — Tabellen &amp; Spalten</div>
      </div>

      {error && <div className="acme-error">{error}</div>}

      {tables?.map((t) => (
        <details className="acme-schema-table" key={t.name}>
          <summary className="acme-schema-summary">
            {t.name}
            <span className="acme-schema-count">{t.rowCount} Zeilen</span>
          </summary>
          <div className="acme-schema-cols">
            <span className="acme-th">SPALTE</span>
            <span className="acme-th">TYP</span>
            <span className="acme-th">NULLABLE</span>
            {t.columns.map((c) => (
              <Fragment key={c.name}>
                <span className="acme-schema-col-name">{c.name}</span>
                <span className="acme-schema-col-type">{c.type}</span>
                <span className="acme-schema-col-null">{c.nullable ? 'ja' : 'nein'}</span>
              </Fragment>
            ))}
          </div>
        </details>
      ))}
      {tables && tables.length === 0 && <div className="acme-empty">Keine Tabellen.</div>}
    </div>
  )
}
