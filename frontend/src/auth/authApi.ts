import { api, API_BASE } from '../api/client'
import type { LoginResponse, Me, ProviderOption } from './types'

/** Auth endpoints of ACMEbase (see api/acme-base.yaml). */
export const authApi = {
  login: (username: string, password: string) =>
    api.post<LoginResponse>('/base/auth/login', { username, password }),

  me: () => api.get<Me>('/base/auth/me'),

  setPassword: (newPassword: string) =>
    api.post<void>('/base/auth/password', { newPassword }),

  providers: () => api.get<ProviderOption[]>('/base/auth/providers'),

  /** Full-page navigation target that starts the OIDC redirect flow for a provider. */
  oidcStartUrl: (providerId: string) => `${API_BASE}/base/auth/oidc/${providerId}/start`,
}
