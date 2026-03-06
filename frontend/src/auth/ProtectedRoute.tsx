import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from './AuthContext'
import type { Role } from './types'

export function ProtectedRoute({ requireRole }: { requireRole?: Role }) {
  const { token, user, isLoading } = useAuth()

  if (isLoading) return <div style={{ padding: 24 }}>Ачаалж байна…</div>
  if (!token) return <Navigate to="/login" replace />
  if (requireRole && user?.role !== requireRole) {
    return <div style={{ padding: 24 }}>Хандах эрхгүй.</div>
  }

  return <Outlet />
}

