// Thin fetch wrapper for the ACMEsuite REST API. Every module talks to the backend
// exclusively through this (contract-first, never against internal code).
//
// Base path defaults to same-origin "/api" — in dev, Vite proxies that to the local
// TLS edge (see vite.config.ts); in production the app is served behind the same edge.
// Auth is a JWT bearer from ACMEbase; role (WATCH/WORK/ADMIN) is enforced server-side.

/** API base path — same-origin "/api" (dev proxy → backend), overridable via VITE_API_BASE. */
export const API_BASE = (import.meta.env.VITE_API_BASE as string | undefined) ?? '/api'

const BASE = API_BASE

let authToken: string | null = null

export function setAuthToken(token: string | null): void {
  authToken = token
}

/** The current Base bearer token — for callers that bypass `request` (e.g. the SSE stream). */
export function getAuthToken(): string | null {
  return authToken
}

export interface ApiError extends Error {
  status: number
  /** RFC-7807 problem body, when the server returned one. */
  problem?: unknown
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (authToken) headers['Authorization'] = `Bearer ${authToken}`

  const res = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  if (!res.ok) {
    let problem: unknown
    try {
      problem = await res.json()
    } catch {
      /* non-JSON error body */
    }
    const err = new Error(`${method} ${path} → ${res.status}`) as ApiError
    err.status = res.status
    err.problem = problem
    throw err
  }

  if (res.status === 204) return undefined as T
  return (await res.json()) as T
}

export const api = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body),
  put: <T>(path: string, body?: unknown) => request<T>('PUT', path, body),
  patch: <T>(path: string, body?: unknown) => request<T>('PATCH', path, body),
  del: <T>(path: string) => request<T>('DELETE', path),
}
