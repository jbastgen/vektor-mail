import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface AuthState {
  token: string | null
  email: string | null
  roles: string[]
  setAuth: (token: string, email: string, roles: string[]) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    set => ({
      token: null,
      email: null,
      roles: [],
      setAuth: (token, email, roles) => set({ token, email, roles }),
      logout: () => set({ token: null, email: null, roles: [] }),
    }),
    { name: 'vektor-auth' },
  ),
)
