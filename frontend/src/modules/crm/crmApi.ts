import { api } from '../../api/client'

// CRM "Pipeline" — the sales overlay projection over leads/quotes/orders. See the contract
// api/acme-crm.yaml (Pipeline tag, v0.2.0). The five stages are derived from quote/order
// status server-side; the frontend talks only to /crm/pipeline via the shared api client.

export type PipelineStage = 'NEU' | 'QUALIFIZIERT' | 'ANGEBOT' | 'VERHANDLUNG' | 'GEWONNEN'
export type DealSource = 'LEAD' | 'QUOTE' | 'ORDER'

/** Mirrors acme-base.yaml#/components/schemas/Money. */
export interface Money {
  amount: number | null
  currency: string | null
  unlimited?: boolean
}

export interface Owner {
  initials: string
  name?: string | null
}

export type CustomerKind = 'CUSTOMER' | 'RESELLER'
export type CustomerStatus = 'PROSPECT' | 'ACTIVE' | 'BLOCKED'

export interface Customer {
  id: string
  name: string
  kind: CustomerKind
  status: CustomerStatus
  email?: string | null
  country?: string | null
  parentResellerId?: string | null
  priceListId?: string | null
}

export interface Contact {
  id: string
  customerId?: string | null
  name: string
  role?: string | null
  email?: string | null
  phone?: string | null
  primary?: boolean
  /** Newsletter opt-in (set by web-form newsletter actions). */
  newsletter?: boolean
}

export type MailDirection = 'INBOUND' | 'OUTBOUND'

export interface MailMessage {
  id: string
  direction: MailDirection
  from?: string | null
  to?: string | null
  sentAt: string
  snippet?: string | null
  body?: string | null
}

export interface MailThread {
  id: string
  subject: string
  customerId?: string | null
  contactId?: string | null
  participants?: string[]
  messageCount?: number
  lastMessageAt?: string | null
  preview?: string | null
  messages?: MailMessage[]
}

export interface Deal {
  id: string
  source: DealSource
  quoteId?: string | null
  orderId?: string | null
  customerId?: string | null
  contactId?: string | null
  company: string
  contact?: string | null
  stage: PipelineStage
  probability: number
  value: Money
  owner?: Owner
  lastActivity?: string | null
  lastActivityAt?: string | null
  ageDays?: number | null
}

export interface DealCreate {
  company: string
  contact?: string | null
  customerId?: string | null
  contactId?: string | null
  stage?: PipelineStage
  value?: Money
  ownerInitials?: string | null
}

export interface ContactWrite {
  customerId?: string | null
  name: string
  role?: string | null
  email?: string | null
  phone?: string | null
  primary?: boolean
  newsletter?: boolean
}

export interface CustomerWrite {
  name: string
  kind: CustomerKind
  status?: CustomerStatus
  email?: string | null
  country?: string | null
}

export interface DealUpdate {
  stage?: PipelineStage
  company?: string
  contact?: string | null
  value?: Money
  ownerInitials?: string | null
}

function query(params: Record<string, string | undefined>): string {
  const q = new URLSearchParams(
    Object.entries(params).filter(([, v]) => v != null && v !== '') as [string, string][],
  ).toString()
  return q ? `?${q}` : ''
}

/** ACMEcrm endpoints (see api/acme-crm.yaml). Frontend talks only through here. */
export const crmApi = {
  // Pipeline (sales overlay)
  listPipeline: (filter: { customerId?: string; contactId?: string } = {}) =>
    api.get<Deal[]>(`/crm/pipeline${query(filter)}`),
  createDeal: (body: DealCreate) => api.post<Deal>('/crm/pipeline', body),
  updateDeal: (id: string, body: DealUpdate) => api.patch<Deal>(`/crm/pipeline/${id}`, body),

  // Customers (companies) — /customers is master data in the contract.
  listCustomers: (filter: { q?: string } = {}) => api.get<Customer[]>(`/crm/customers${query(filter)}`),
  createCustomer: (body: CustomerWrite) => api.post<Customer>('/crm/customers', body),

  // Contacts (people at a customer)
  listContacts: (filter: { customerId?: string; q?: string } = {}) =>
    api.get<Contact[]>(`/crm/contacts${query(filter)}`),
  createContact: (body: ContactWrite) => api.post<Contact>('/crm/contacts', body),

  // Mail threads (correspondence overlay)
  listThreads: (filter: { customerId?: string; contactId?: string } = {}) =>
    api.get<MailThread[]>(`/crm/threads${query(filter)}`),
}
