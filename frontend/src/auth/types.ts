export type AccessRole = 'WATCH' | 'WORK' | 'ADMIN'
export type UserStatus = 'ACTIVE' | 'PENDING' | 'DISABLED'

/** Current user as returned by GET /api/base/auth/me. */
export interface Me {
  id: string
  username: string | null
  email: string | null
  displayName: string | null
  role: AccessRole
  status: UserStatus
}

export interface LoginResponse {
  token: string
  mustSetPassword: boolean
  user: Me
}

/** A sign-in option from GET /api/base/auth/providers. */
export interface ProviderOption {
  id: string
  displayName: string
  kind: 'LOCAL' | 'OIDC'
}
