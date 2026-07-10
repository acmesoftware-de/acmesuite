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

/** ACMEbase admin endpoints (ADMIN only). */
export const adminApi = {
  listUsers: () => api.get<AdminUser[]>('/base/users'),
  createUser: (body: { username: string; displayName: string; email: string; role: AccessRole }) =>
    api.post<CreateUserResponse>('/base/users', body),
  setRole: (id: string, role: AccessRole) => api.put<AdminUser>(`/base/users/${id}/role`, { role }),
  setStatus: (id: string, status: UserStatus) =>
    api.put<AdminUser>(`/base/users/${id}/status`, { status }),

  listProviderConfigs: () => api.get<ProviderConfig[]>('/base/auth/provider-configs'),
  upsertProviderConfig: (id: string, body: { enabled: boolean; values: Record<string, string> }) =>
    api.put<ProviderConfig>(`/base/auth/provider-configs/${id}`, body),
}
