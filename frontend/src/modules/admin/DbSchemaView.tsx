import { Fragment, useEffect, useState } from 'react'
import { adminApi, type DbRowPage, type DbTable } from './adminApi'

const PAGE_SIZE = 50

function formatCell(value: unknown): string {
  if (value === null || value === undefined) return '∅'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function TableRows({ table }: { table: string }) {
  const [page, setPage] = useState(0)
  const [data, setData] = useState<DbRowPage | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setData(null)
    setError(null)
    adminApi
      .getDbRows(table, page, PAGE_SIZE)
      .then(setData)
      .catch(() => setError('Konnte Zeilen nicht laden.'))
  }, [table, page])

  if (error) return <div className="acme-error">{error}</div>
  if (!data) return <div className="acme-empty">Lade…</div>

  const lastPage = Math.max(0, Math.ceil(data.totalRows / data.size) - 1)

  return (
    <div className="acme-schema-rows">
      <div className="acme-schema-rows-table">
        <table>
          <thead>
            <tr>
              {data.columns.map((c) => (
                <th key={c}>{c}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {data.rows.map((row, i) => (
              <tr key={i}>
                {data.columns.map((c) => (
                  <td key={c}>{formatCell(row[c])}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
        {data.rows.length === 0 && <div className="acme-empty">Keine Zeilen.</div>}
      </div>
      <div className="acme-schema-rows-pager">
        <button
          className="acme-btn acme-btn--ghost"
          disabled={page === 0}
          onClick={() => setPage((p) => p - 1)}
        >
          ← Zurück
        </button>
        <span className="acme-schema-rows-info">
          Seite {page + 1} von {lastPage + 1} · {data.totalRows} Zeilen
        </span>
        <button
          className="acme-btn acme-btn--ghost"
          disabled={page >= lastPage}
          onClick={() => setPage((p) => p + 1)}
        >
          Weiter →
        </button>
      </div>
    </div>
  )
}

export function DbSchemaView() {
  const [tables, setTables] = useState<DbTable[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [rowsFor, setRowsFor] = useState<string | null>(null)

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
          <button
            className="acme-btn acme-btn--ghost acme-schema-rows-toggle"
            onClick={() => setRowsFor((prev) => (prev === t.name ? null : t.name))}
          >
            {rowsFor === t.name ? 'Zeilen ausblenden' : 'Zeilen anzeigen'}
          </button>
          {rowsFor === t.name && <TableRows table={t.name} />}
        </details>
      ))}
      {tables && tables.length === 0 && <div className="acme-empty">Keine Tabellen.</div>}
    </div>
  )
}
