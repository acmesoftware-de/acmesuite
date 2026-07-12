import { useCallback, useEffect, useState } from 'react'
import { useAuth } from '../../auth/AuthContext'
import {
  crmApi,
  type Contact,
  type ContactWrite,
  type Customer,
  type CustomerWrite,
  type Deal,
  type DealCreate,
  type MailThread,
  type PipelineStage,
} from './crmApi'
import { probabilityFor } from './pipelineModel'
import { PipelineView } from './PipelineView'
import { CustomersView } from './CustomersView'
import { ContactsView } from './ContactsView'

/** Handlers the pipeline views use to mutate deals (no-ops for WATCH). */
export interface DealActions {
  canWrite: boolean
  setStage: (id: string, stage: PipelineStage) => void
  setCompany: (id: string, company: string) => void
  setValue: (id: string, amount: number) => void
}

/**
 * CRM module content. Loads the shared CRM data (pipeline deals, customers, contacts, mail
 * threads) and dispatches to the sub-view the shell selected: Pipeline · Kunden · Kontakte.
 * `newDealTick` increments when the shell's context "+ …" button is pressed.
 */
export function CrmModule({ subView, newDealTick }: { subView: string; newDealTick: number }) {
  const { canWrite } = useAuth()
  const [deals, setDeals] = useState<Deal[] | null>(null)
  const [customers, setCustomers] = useState<Customer[] | null>(null)
  const [contacts, setContacts] = useState<Contact[] | null>(null)
  const [threads, setThreads] = useState<MailThread[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  useEffect(() => {
    crmApi.listPipeline().then(setDeals).catch(() => setError('Konnte Pipeline nicht laden.'))
    crmApi.listCustomers().then(setCustomers).catch(() => undefined)
    crmApi.listContacts().then(setContacts).catch(() => undefined)
    crmApi.listThreads().then(setThreads).catch(() => undefined)
  }, [])

  /** Optimistically apply `local`, persist `body`; roll back + notify on failure. */
  const persist = useCallback(
    async (id: string, body: Parameters<typeof crmApi.updateDeal>[1], local: Partial<Deal>) => {
      setNotice(null)
      let snapshot: Deal[] | null = null
      setDeals((prev) => {
        snapshot = prev
        return prev?.map((d) => (d.id === id ? { ...d, ...local } : d)) ?? prev
      })
      try {
        const updated = await crmApi.updateDeal(id, body)
        setDeals((prev) => prev?.map((d) => (d.id === id ? updated : d)) ?? prev)
      } catch {
        setDeals(snapshot)
        setNotice('Änderung nicht möglich – zurückgesetzt.')
      }
    },
    [],
  )

  const dealActions: DealActions = {
    canWrite,
    setStage: (id, stage) => persist(id, { stage }, { stage, probability: probabilityFor(stage) }),
    setCompany: (id, company) => persist(id, { company }, { company }),
    setValue: (id, amount) =>
      persist(id, { value: { amount, currency: 'EUR' } }, { value: { amount, currency: 'EUR' } }),
  }

  const createDeal = useCallback(async (body: DealCreate) => {
    const created = await crmApi.createDeal(body)
    setDeals((prev) => [created, ...(prev ?? [])])
  }, [])

  const createCustomer = useCallback(async (body: CustomerWrite) => {
    const created = await crmApi.createCustomer(body)
    setCustomers((prev) => [created, ...(prev ?? [])])
  }, [])

  const createContact = useCallback(async (body: ContactWrite) => {
    const created = await crmApi.createContact(body)
    setContacts((prev) => [created, ...(prev ?? [])])
  }, [])

  const chrome = (
    <>
      {notice && <div className="acme-notice">{notice}</div>}
      {error && <div className="acme-error">{error}</div>}
    </>
  )

  if (subView === 'kunden') {
    return (
      <div className="acme-content">
        {chrome}
        <CustomersView
          customers={customers}
          contacts={contacts}
          deals={deals}
          threads={threads}
          canWrite={canWrite}
          createTick={newDealTick}
          onCreate={createCustomer}
        />
      </div>
    )
  }

  if (subView === 'kontakte') {
    return (
      <div className="acme-content">
        {chrome}
        <ContactsView
          contacts={contacts}
          customers={customers}
          deals={deals}
          threads={threads}
          canWrite={canWrite}
          createTick={newDealTick}
          onCreate={createContact}
        />
      </div>
    )
  }

  return (
    <div className="acme-content">
      {chrome}
      {!deals && !error && <div className="acme-empty">Pipeline wird geladen…</div>}
      {deals && (
        <PipelineView
          deals={deals}
          actions={dealActions}
          createTick={newDealTick}
          onCreate={createDeal}
        />
      )}
    </div>
  )
}
