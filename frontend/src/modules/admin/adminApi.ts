import { api } from '../../api/client'
import type { AccessRole, UserStatus } from '../../auth/types'

export interface AdminUser {
  id: string
  username: string | null
  email: string | null
  displayName: string | null
  role: AccessRole
  status: UserStatus
  authProvider: string
  /** Orthogonal AUDIT capability (ADR-0010). */
  auditor: boolean
  /** Last-change stamp — visible to everyone; the version number is not (ADR-0010). */
  updatedAt: string | null
  updatedBy: string | null
}

export type RevisionType = 'ADD' | 'MOD' | 'DEL'

/** One versioned revision of a user — returned only to AUDIT holders. */
export interface UserRevision {
  revision: number
  changedAt: string
  changedBy: string | null
  changeType: RevisionType
  username: string | null
  email: string | null
  displayName: string | null
  role: AccessRole
  status: UserStatus
  auditor: boolean
  deleted: boolean
}

export interface CreateUserResponse {
  user: AdminUser
  temporaryPassword: string
}

export type FieldType = 'TEXT' | 'SECRET' | 'URL' | 'BOOL'

export interface ProviderField {
  key: string
  label: string
  type: FieldType
  required: boolean
}

export interface ProviderConfig {
  providerId: string
  displayName: string
  kind: string
  enabled: boolean
  configured: boolean
  schema: ProviderField[]
  values: Record<string, string>
  secretsSet: string[]
}

export interface DbColumn {
  name: string
  type: string
  nullable: boolean
}

export interface DbTable {
  name: string
  rowCount: number
  columns: DbColumn[]
}

/** ACMEbase admin endpoints (ADMIN only). */
export const adminApi = {
  listUsers: () => api.get<AdminUser[]>('/base/users'),
  createUser: (body: { username: string; displayName: string; email: string; role: AccessRole }) =>
    api.post<CreateUserResponse>('/base/users', body),
  setRole: (id: string, role: AccessRole) => api.put<AdminUser>(`/base/users/${id}/role`, { role }),
  setStatus: (id: string, status: UserStatus) =>
    api.put<AdminUser>(`/base/users/${id}/status`, { status }),
  setAuditor: (id: string, auditor: boolean) =>
    api.put<AdminUser>(`/base/users/${id}/auditor`, { auditor }),
  /** Version history (Chatter timeline) — AUDIT capability only. */
  getUserHistory: (id: string) => api.get<UserRevision[]>(`/base/users/${id}/history`),

  listProviderConfigs: () => api.get<ProviderConfig[]>('/base/auth/provider-configs'),
  upsertProviderConfig: (id: string, body: { enabled: boolean; values: Record<string, string> }) =>
    api.put<ProviderConfig>(`/base/auth/provider-configs/${id}`, body),

  /** DB schema browser — information_schema introspection of the app's own Postgres schema. */
  getDbSchema: () => api.get<DbTable[]>('/base/db/schema'),
}
