import { useEffect, useState } from 'react'
import { ApiError, fetchJson } from '../lib/api'
import type { NotificationItem } from '../auth/types'

export default function NotificationsPage() {
  const [items, setItems] = useState<NotificationItem[]>([])
  const [error, setError] = useState<string | null>(null)

  async function load() {
    setError(null)
    try {
      const result = await fetchJson<NotificationItem[]>('/api/notifications/me', { method: 'GET' })
      setItems(result)
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError('Мэдэгдэл ачаалж чадсангүй')
    }
  }

  useEffect(() => {
    void load()
  }, [])

  async function markRead(id: number) {
    try {
      const updated = await fetchJson<NotificationItem>(`/api/notifications/${id}/read`, { method: 'PATCH' })
      setItems((prev) => prev.map((n) => (n.id === id ? updated : n)))
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
    }
  }

  return (
    <div className="page">
      <h1>Мэдэгдлүүд</h1>
      {error ? <div className="error">{error}</div> : null}
      <div style={{ display: 'grid', gap: 12 }}>
        {items.map((n) => (
          <article key={n.id} className="card">
            <p style={{ marginTop: 0 }}><strong>{n.title}</strong></p>
            <p>{n.message}</p>
            <p className="muted">{new Date(n.createdAt).toLocaleString('mn-MN')}</p>
            {!n.isRead ? (
              <button type="button" onClick={() => markRead(n.id)}>
                Уншсан болгох
              </button>
            ) : (
              <span className="muted">Уншсан</span>
            )}
          </article>
        ))}
        {!items.length ? <div className="muted">Мэдэгдэл алга.</div> : null}
      </div>
    </div>
  )
}

