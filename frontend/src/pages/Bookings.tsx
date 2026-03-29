import { useEffect, useState } from 'react'
import { ApiError, fetchJson } from '../lib/api'
import { useAuth } from '../auth/AuthContext'
import type { Booking } from '../auth/types'

function formatDate(value: string) {
  return new Date(value).toLocaleString('mn-MN')
}

export default function BookingsPage() {
  const { user } = useAuth()
  const [bookings, setBookings] = useState<Booking[]>([])
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  async function loadBookings() {
    setIsLoading(true)
    setError(null)
    try {
      const result = await fetchJson<Booking[]>('/api/bookings/me', { method: 'GET' })
      setBookings(result)
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError('Захиалгууд ачаалж чадсангүй')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    void loadBookings()
  }, [])

  async function updateStatus(bookingId: number, action: 'confirm' | 'cancel') {
    setError(null)
    try {
      const updated = await fetchJson<Booking>(`/api/bookings/${bookingId}/${action}`, { method: 'PATCH' })
      setBookings((prev) => prev.map((b) => (b.id === updated.id ? updated : b)))
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError('Төлөв шинэчлэх үед алдаа гарлаа')
    }
  }

  if (isLoading) {
    return <div className="page"><p className="muted">Ачаалж байна...</p></div>
  }

  return (
    <div className="page">
      <h1>Миний захиалгууд</h1>
      {error ? <div className="error">{error}</div> : null}
      <div style={{ display: 'grid', gap: 12 }}>
        {bookings.map((booking) => (
          <div key={booking.id} className="card">
            <p><strong>Хичээл:</strong> {booking.subject}</p>
            <p><strong>Багш:</strong> {booking.teacherName}</p>
            <p><strong>Сурагч:</strong> {booking.studentName}</p>
            <p><strong>Цаг:</strong> {formatDate(booking.slotStartTime)} - {formatDate(booking.slotEndTime)}</p>
            <p><strong>Төлөв:</strong> {booking.status}</p>

            <div style={{ display: 'flex', gap: 8 }}>
              {user?.role === 'TEACHER' && booking.status === 'PENDING' ? (
                <button type="button" onClick={() => updateStatus(booking.id, 'confirm')}>
                  Батлах
                </button>
              ) : null}
              {booking.status !== 'CANCELLED' ? (
                <button type="button" onClick={() => updateStatus(booking.id, 'cancel')}>
                  Цуцлах
                </button>
              ) : null}
            </div>
          </div>
        ))}
        {!bookings.length ? <div className="muted">Захиалга алга.</div> : null}
      </div>
    </div>
  )
}

