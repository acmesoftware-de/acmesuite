import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { setAuthToken, type ApiError } from '../api/client'
import { authApi } from './authApi'
import type { AccessRole, Me } from './types'

const TOKEN_KEY = 'acmesuite.token'

type Phase = 'loading' | 'anon' | 'mustSetPassword' | 'authed'

interface AuthContextValue {
  phase: Phase
  user: Me | null
  /** true for WORK and ADMIN (may perform operational writes). */
  canWrite: boolean
  isAdmin: boolean
  login: (username: string, password: string) => Promise<void>
  completeSetPassword: (newPassword: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

const ROLE_RANK: Record<AccessRole, number> = { WATCH: 0, WORK: 1, ADMIN: 2 }

function persist(token: string | null) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
  setAuthToken(token)
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [phase, setPhase] = useState<Phase>('loading')
  const [user, setUser] = useState<Me | null>(null)

  // Restore a session from a stored token on load.
  useEffect(() => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (!token) {
      setPhase('anon')
      return
    }
    setAuthToken(token)
    authApi
      .me()
      .then((me) => {
        setUser(me)
        setPhase('authed')
      })
      .catch(() => {
        persist(null)
        setPhase('anon')
      })
  }, [])

  const login = useCallback(async (username: string, password: string) => {
    const res = await authApi.login(username, password)
    persist(res.token)
    setUser(res.user)
    setPhase(res.mustSetPassword ? 'mustSetPassword' : 'authed')
  }, [])

  const completeSetPassword = useCallback(async (newPassword: string) => {
    await authApi.setPassword(newPassword)
    const me = await authApi.me()
    setUser(me)
    setPhase('authed')
  }, [])

  const logout = useCallback(() => {
    persist(null)
    setUser(null)
    setPhase('anon')
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      phase,
      user,
      canWrite: user ? ROLE_RANK[user.role] >= ROLE_RANK.WORK : false,
      isAdmin: user?.role === 'ADMIN',
      login,
      completeSetPassword,
      logout,
    }),
    [phase, user, login, completeSetPassword, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within <AuthProvider>')
  return ctx
}

/** Narrow an unknown error to the API status code, for message selection. */
export function apiStatus(e: unknown): number | undefined {
  return (e as ApiError)?.status
}
