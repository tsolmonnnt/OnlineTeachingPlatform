import { useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { ApiError, fetchJson } from '../lib/api'
import { useAuth } from '../auth/AuthContext'
import type { AvailabilitySlot, Booking, TeacherDetail } from '../auth/types'

function formatIsoDate(value: string) {
  return new Date(value).toLocaleString('mn-MN')
}

export default function TeacherDetailPage() {
  const { teacherId } = useParams()
  const { user } = useAuth()

  const [teacher, setTeacher] = useState<TeacherDetail | null>(null)
  const [slots, setSlots] = useState<AvailabilitySlot[]>([])
  const [subject, setSubject] = useState('')
  const [note, setNote] = useState('')
  const [selectedSlotId, setSelectedSlotId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const activeTeacherId = Number(teacherId)

  const selectableSlots = useMemo(() => slots.filter((s) => !s.booked), [slots])

  useEffect(() => {
    ;(async () => {
      if (!activeTeacherId) return
      setIsLoading(true)
      setError(null)
      try {
        const detail = await fetchJson<TeacherDetail>(`/api/teachers/${activeTeacherId}`, { method: 'GET' })
        const from = new Date()
        const to = new Date()
        to.setDate(to.getDate() + 21)
        const fromParam = from.toISOString().slice(0, 19)
        const toParam = to.toISOString().slice(0, 19)
        const schedule = await fetchJson<AvailabilitySlot[]>(
          `/api/schedules/teacher/${activeTeacherId}?from=${fromParam}&to=${toParam}`,
          { method: 'GET' },
        )

        setTeacher(detail)
        setSlots(schedule)
        setSubject(detail.subjects?.[0] ?? '')
      } catch (err) {
        if (err instanceof ApiError) setError(err.message)
        else setError('Багшийн мэдээлэл ачаалж чадсангүй')
      } finally {
        setIsLoading(false)
      }
    })()
  }, [activeTeacherId])

  async function onBook() {
    if (!selectedSlotId) {
      setError('Цаг сонгоно уу')
      return
    }
    if (!subject.trim()) {
      setError('Хичээлийн нэр оруулна уу')
      return
    }

    setError(null)
    setSuccess(null)
    try {
      const booking = await fetchJson<Booking>('/api/bookings', {
        method: 'POST',
        body: JSON.stringify({
          teacherId: activeTeacherId,
          slotId: selectedSlotId,
          subject: subject.trim(),
          note: note.trim() || null,
        }),
      })
      setSuccess(`Захиалга амжилттай. Төлөв: ${booking.status}`)
      setSlots((prev) => prev.map((s) => (s.id === selectedSlotId ? { ...s, booked: true } : s)))
      setSelectedSlotId(null)
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError('Захиалга үүсгэх үед алдаа гарлаа')
    }
  }

  if (isLoading) {
    return <div className="page"><p className="muted">Ачаалж байна...</p></div>
  }

  if (!teacher) {
    return <div className="page"><p className="muted">Багш олдсонгүй.</p></div>
  }

  return (
    <div className="page">
      <h1>{teacher.fullName}</h1>
      <div className="card" style={{ marginBottom: 12 }}>
        <p className="muted">{teacher.headline ?? 'Товч танилцуулга байхгүй'}</p>
        <p>{teacher.bio ?? 'Дэлгэрэнгүй танилцуулга оруулаагүй.'}</p>
        <p><strong>Хичээл:</strong> {(teacher.subjects ?? []).join(', ') || '-'}</p>
        <p><strong>Ур чадвар:</strong> {(teacher.skills ?? []).join(', ') || '-'}</p>
        <p><strong>Хэл:</strong> {(teacher.languages ?? []).join(', ') || '-'}</p>
        <p><strong>Үнэ:</strong> {teacher.hourlyRate ?? '-'}</p>
      </div>

      <div className="card form">
        <h2>Боломжит цаг сонгож захиалах</h2>
        {selectableSlots.length ? (
          <label>
            Сул цаг
            <select
              value={selectedSlotId ?? ''}
              onChange={(e) => setSelectedSlotId(e.target.value ? Number(e.target.value) : null)}
            >
              <option value="">Сонгох</option>
              {selectableSlots.map((slot) => (
                <option key={slot.id} value={slot.id}>
                  {formatIsoDate(slot.startTime)} - {formatIsoDate(slot.endTime)}
                </option>
              ))}
            </select>
          </label>
        ) : (
          <p className="muted">Одоогоор сул цаг алга.</p>
        )}

        <label>
          Хичээлийн нэр
          <input value={subject} onChange={(e) => setSubject(e.target.value)} />
        </label>

        <label>
          Тэмдэглэл
          <textarea value={note} onChange={(e) => setNote(e.target.value)} rows={4} />
        </label>

        {error ? <div className="error">{error}</div> : null}
        {success ? <div className="card" style={{ borderColor: '#86efac' }}>{success}</div> : null}

        <button type="button" onClick={onBook} disabled={user?.role !== 'STUDENT' || !selectableSlots.length}>
          {user?.role === 'STUDENT' ? 'Захиалах' : 'Захиалга хийхийн тулд сурагч эрхтэй нэвтэрнэ үү'}
        </button>
      </div>
    </div>
  )
}

