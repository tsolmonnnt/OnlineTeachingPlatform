import { useEffect, useState } from 'react'
import { ApiError, fetchJson } from '../lib/api'
import type { AdminStats, AdminTeacherRow, UserRow } from '../auth/types'

export default function AdminDashboardPage() {
  const [stats, setStats] = useState<AdminStats | null>(null)
  const [users, setUsers] = useState<UserRow[]>([])
  const [teachers, setTeachers] = useState<AdminTeacherRow[]>([])
  const [error, setError] = useState<string | null>(null)

  async function loadAll() {
    setError(null)
    try {
      const [s, u, t] = await Promise.all([
        fetchJson<AdminStats>('/api/admin/stats', { method: 'GET' }),
        fetchJson<UserRow[]>('/api/users', { method: 'GET' }),
        fetchJson<AdminTeacherRow[]>('/api/admin/teachers', { method: 'GET' }),
      ])
      setStats(s)
      setUsers(u)
      setTeachers(t)
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError('Мэдээлэл ачаалж чадсангүй')
    }
  }

  useEffect(() => {
    void loadAll()
  }, [])

  async function toggleVerified(row: AdminTeacherRow) {
    setError(null)
    try {
      await fetchJson(`/api/admin/teachers/${row.teacherProfileId}/verification`, {
        method: 'PATCH',
        body: JSON.stringify({ verified: !row.verified }),
      })
      await loadAll()
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError('Шинэчлэхэд алдаа гарлаа')
    }
  }

  return (
    <div className="page">
      <h1>Админ самбар</h1>
      {error ? <div className="error">{error}</div> : null}

      {stats ? (
        <div className="card" style={{ marginBottom: 16 }}>
          <h2 style={{ marginTop: 0 }}>Тойм статистик</h2>
          <ul className="muted" style={{ margin: 0, paddingLeft: 20 }}>
            <li>Нийт хэрэглэгч: {stats.totalUsers}</li>
            <li>Сурагч: {stats.studentCount}</li>
            <li>Багш: {stats.teacherCount}</li>
            <li>Админ: {stats.adminCount}</li>
            <li>Нийт захиалга: {stats.totalBookings}</li>
            <li>Баталгаажсан багш: {stats.verifiedTeacherCount} / {stats.totalTeachers}</li>
          </ul>
        </div>
      ) : (
        <p className="muted">Ачаалж байна...</p>
      )}

      <h2>Багшийн баталгаажуулалт</h2>
      <div style={{ display: 'grid', gap: 8 }}>
        {teachers.map((row) => (
          <div key={row.teacherProfileId} className="card" style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'center' }}>
            <div>
              <strong>{row.fullName}</strong>
              <div className="muted small">{row.email}</div>
              <div className="muted small">Профайл № {row.teacherProfileId}</div>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span>{row.verified ? 'Баталгаажсан' : 'Хүлээгдэж буй'}</span>
              <button type="button" onClick={() => toggleVerified(row)}>
                {row.verified ? 'Цуцлах' : 'Батлах'}
              </button>
            </div>
          </div>
        ))}
        {!teachers.length ? <div className="muted">Багш олдсонгүй.</div> : null}
      </div>

      <h2 style={{ marginTop: 24 }}>Хэрэглэгчид</h2>
      <div style={{ display: 'grid', gap: 8 }}>
        {users.map((u) => (
          <div key={u.id} className="card">
            <strong>{u.fullName}</strong> — {u.email} — {u.role}
          </div>
        ))}
      </div>
    </div>
  )
}
