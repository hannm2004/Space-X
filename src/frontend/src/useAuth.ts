import { createContext, useContext } from 'react'
import type { CurrentUser } from './authApi'

export type AuthState =
  | { status: 'loading' }
  | { status: 'anonymous' }
  | { status: 'authenticated'; user: CurrentUser }

export type AuthValue = {
  state: AuthState
  signIn: (email: string, password: string) => Promise<void>
  signOut: () => Promise<void>
}

export const AuthContext = createContext<AuthValue | null>(null)

export function useAuth(): AuthValue {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth phải dùng bên trong AuthProvider')
  return value
}
