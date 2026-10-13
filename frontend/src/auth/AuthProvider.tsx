import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { PropsWithChildren } from 'react'
import type { QueryClient } from '@tanstack/react-query'
import { getSession, request } from '../api/client'
import type { Session } from '../api/types'

type AuthState = {
  session: Session | null
  loading: boolean
  error: string | null
  refresh: () => Promise<Session | null>
  signIn: (email: string, password: string) => Promise<Session>
  signOut: () => Promise<void>
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children, queryClient }: PropsWithChildren<{ queryClient: QueryClient }>) {
  const [session, setSession] = useState<Session | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    setError(null)
    try {
      const current = await getSession()
      setSession(current)
      return current
    } catch {
      setError('Salary Management is temporarily unavailable. Check the service and retry.')
      return null
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { void refresh() }, [refresh])

  useEffect(() => {
    const expire = () => {
      setSession({ authenticated: false, email: null })
      queryClient.clear()
    }
    window.addEventListener('salary:session-expired', expire)
    return () => window.removeEventListener('salary:session-expired', expire)
  }, [queryClient])

  const signIn = useCallback(async (email: string, password: string) => {
    await request<Session>('/api/v1/session', { method: 'POST', body: JSON.stringify({ email, password }) })
    const current = await getSession()
    setSession(current)
    if (!current.authenticated) throw new Error('Sign-in could not be confirmed. Try again.')
    return current
  }, [])

  const signOut = useCallback(async () => {
    try {
      await request<void>('/api/v1/session', { method: 'DELETE' })
    } finally {
      setSession({ authenticated: false, email: null })
      queryClient.clear()
    }
  }, [queryClient])

  const value = useMemo(() => ({ session, loading, error, refresh, signIn, signOut }), [session, loading, error, refresh, signIn, signOut])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
