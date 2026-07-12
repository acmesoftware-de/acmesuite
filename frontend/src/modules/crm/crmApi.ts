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

export interface Deal {
  id: string
  source: DealSource
  quoteId?: string | null
  orderId?: string | null
  customerId?: string | null
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
  stage?: PipelineStage
  value?: Money
  ownerInitials?: string | null
}

export interface DealUpdate {
  stage?: PipelineStage
  company?: string
  contact?: string | null
  value?: Money
  ownerInitials?: string | null
}

/** ACMEcrm pipeline endpoints (see api/acme-crm.yaml). */
export const crmApi = {
  listPipeline: () => api.get<Deal[]>('/crm/pipeline'),
  createDeal: (body: DealCreate) => api.post<Deal>('/crm/pipeline', body),
  updateDeal: (id: string, body: DealUpdate) => api.patch<Deal>(`/crm/pipeline/${id}`, body),
}
