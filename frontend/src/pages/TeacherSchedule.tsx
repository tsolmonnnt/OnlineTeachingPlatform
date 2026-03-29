import { useEffect, useState } from 'react'
import { ApiError, fetchJson } from '../lib/api'
import type { AvailabilitySlot } from '../auth/types'

export default function TeacherSchedulePage() {
  const [slots, setSlots] = useState<AvailabilitySlot[]>([])
  const [startTime, setStartTime] = useState('')
  const [endTime, setEndTime] = useState('')
  const [error, setError] = useState<string | null>(null)

  async function load() {
    setError(null)
    try {
      const result = await fetchJson<AvailabilitySlot[]>('/api/schedules/me', { method: 'GET' })
      setSlots(result)
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError('Хуваарь ачаалж чадсангүй')
    }
  }

  useEffect(() => {
    void load()
  }, [])

  async function addSlot(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    try {
      const created = await fetchJson<AvailabilitySlot>('/api/schedules/me', {
        method: 'POST',
        body: JSON.stringify({
          startTime: new Date(startTime).toISOString().slice(0, 19),
          endTime: new Date(endTime).toISOString().slice(0, 19),
        }),
      })
      setSlots((prev) => [...prev, created].sort((a, b) => a.startTime.localeCompare(b.startTime)))
      setStartTime('')
      setEndTime('')
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError('Цагийн слот нэмэх үед алдаа гарлаа')
    }
  }

  async function deleteSlot(slotId: number) {
    setError(null)
    try {
      await fetchJson<void>(`/api/schedules/me/${slotId}`, { method: 'DELETE' })
      setSlots((prev) => prev.filter((s) => s.id !== slotId))
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError('Слот устгах үед алдаа гарлаа')
    }
  }

  return (
    <div className="page">
      <h1>Цагийн хуваарь</h1>
      <form className="card form" onSubmit={addSlot}>
        <label>
          Эхлэх цаг
          <input type="datetime-local" value={startTime} onChange={(e) => setStartTime(e.target.value)} required />
        </label>
        <label>
          Дуусах цаг
          <input type="datetime-local" value={endTime} onChange={(e) => setEndTime(e.target.value)} required />
        </label>
        <button type="submit">Слот нэмэх</button>
      </form>

      {error ? <div className="error" style={{ marginTop: 12 }}>{error}</div> : null}

      <div style={{ marginTop: 12, display: 'grid', gap: 10 }}>
        {slots.map((slot) => (
          <div key={slot.id} className="card">
            <p>
              {new Date(slot.startTime).toLocaleString('mn-MN')} - {new Date(slot.endTime).toLocaleString('mn-MN')}
            </p>
            <p><strong>Төлөв:</strong> {slot.booked ? 'Захиалагдсан' : 'Сул'}</p>
            {!slot.booked ? (
              <button type="button" onClick={() => deleteSlot(slot.id)}>
                Устгах
              </button>
            ) : null}
          </div>
        ))}
        {!slots.length ? <div className="muted">Одоогоор слот байхгүй.</div> : null}
      </div>
    </div>
  )
}

