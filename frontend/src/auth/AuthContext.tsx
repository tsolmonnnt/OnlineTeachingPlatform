import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { fetchJson } from '../lib/api'
import type { AuthResponse, Role, User } from './types'

type AuthState = {
  token: string | null
  user: User | null
  isLoading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (input: {
    fullName: string
    email: string
    password: string
    role: Role
  }) => Promise<void>
  logout: () => void
  refreshMe: () => Promise<void>
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(
    localStorage.getItem('accessToken'),
  )
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const setSession = useCallback((auth: AuthResponse) => {
    localStorage.setItem('accessToken', auth.accessToken)
    setToken(auth.accessToken)
    setUser(auth.user)
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('accessToken')
    setToken(null)
    setUser(null)
  }, [])

  const refreshMe = useCallback(async () => {
    const t = localStorage.getItem('accessToken')
    if (!t) {
      setUser(null)
      return
    }
    const me = await fetchJson<User>('/api/auth/me', { method: 'GET', token: t })
    setToken(t)
    setUser(me)
  }, [])

  const login = useCallback(
    async (email: string, password: string) => {
      const auth = await fetchJson<AuthResponse>('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      })
      setSession(auth)
    },
    [setSession],
  )

  const register = useCallback(
    async (input: { fullName: string; email: string; password: string; role: Role }) => {
      const auth = await fetchJson<AuthResponse>('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify(input),
      })
      setSession(auth)
    },
    [setSession],
  )

  useEffect(() => {
    ;(async () => {
      try {
        await refreshMe()
      } catch {
        logout()
      } finally {
        setIsLoading(false)
      }
    })()
  }, [logout, refreshMe])

  const value = useMemo<AuthState>(
    () => ({
      token,
      user,
      isLoading,
      login,
      register,
      logout,
      refreshMe,
    }),
    [token, user, isLoading, login, register, logout, refreshMe],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}

