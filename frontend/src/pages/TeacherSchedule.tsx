import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ApiError, fetchJson } from '../lib/api'
import type { AvailabilitySlot, CourseSubject } from '../auth/types'

function pickCourseSubjectId(teaching: CourseSubject[], rawParam: string | null): number | '' {
  const n = rawParam ? Number(rawParam) : NaN
  if (!Number.isNaN(n) && teaching.some((s) => s.id === n)) return n
  if (teaching.length === 1) return teaching[0].id
  return ''
}

export default function TeacherSchedulePage() {
  const [searchParams] = useSearchParams()
  const [slots, setSlots] = useState<AvailabilitySlot[]>([])
  const [subjects, setSubjects] = useState<CourseSubject[]>([])
  const [courseSubjectId, setCourseSubjectId] = useState<number | ''>('')
  const [startTime, setStartTime] = useState('')
  const [editingSlotId, setEditingSlotId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  function toDatetimeLocalValue(iso: string) {
    const d = new Date(iso)
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
  }

  async function load() {
    setError(null)
    try {
      const [slotResult, teaching] = await Promise.all([
        fetchJson<AvailabilitySlot[]>('/api/schedules/me', { method: 'GET' }),
        fetchJson<CourseSubject[]>('/api/course/subjects/teaching', { method: 'GET' }),
      ])
      setSlots(slotResult)
      setSubjects(teaching)
      setCourseSubjectId(pickCourseSubjectId(teaching, searchParams.get('courseSubjectId')))
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError('Хуваарь ачаалж чадсангүй')
    }
  }

  useEffect(() => {
    void load()
  }, [searchParams])

  const rawUrl = searchParams.get('courseSubjectId')
  const urlNum = rawUrl ? Number(rawUrl) : NaN
  const urlMatches = !Number.isNaN(urlNum) && subjects.some((s) => s.id === urlNum)
  const hideSubjectPicker = editingSlotId == null && (subjects.length === 1 || urlMatches)

  async function addOrUpdateSlot(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    if (!startTime || courseSubjectId === '') {
      setError('Хичээл болон эхлэх цаг сонгоно уу')
      return
    }
    const payload = JSON.stringify({
      startTime: new Date(startTime).toISOString().slice(0, 19),
      courseSubjectId,
    })
    try {
      if (editingSlotId != null) {
        const updated = await fetchJson<AvailabilitySlot>(`/api/schedules/me/${editingSlotId}`, {
          method: 'PUT',
          body: payload,
        })
        setSlots((prev) =>
          prev.map((s) => (s.id === editingSlotId ? updated : s)).sort((a, b) => a.startTime.localeCompare(b.startTime)),
        )
        setEditingSlotId(null)
      } else {
        const created = await fetchJson<AvailabilitySlot>('/api/schedules/me', {
          method: 'POST',
          body: payload,
        })
        setSlots((prev) => [...prev, created].sort((a, b) => a.startTime.localeCompare(b.startTime)))
      }
      setStartTime('')
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError(editingSlotId != null ? 'Слот шинэчлэх үед алдаа гарлаа' : 'Цагийн слот нэмэх үед алдаа гарлаа')
    }
  }

  function beginEdit(slot: AvailabilitySlot) {
    if (slot.booked) return
    setEditingSlotId(slot.id)
    const sid = slot.courseSubjectId
    setCourseSubjectId(sid != null ? sid : (subjects[0]?.id ?? ''))
    setStartTime(toDatetimeLocalValue(slot.startTime))
  }

  function cancelEdit() {
    setEditingSlotId(null)
    setStartTime('')
    setCourseSubjectId(pickCourseSubjectId(subjects, searchParams.get('courseSubjectId')))
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

  const selectedName = subjects.find((s) => s.id === courseSubjectId)?.name

  return (
    <div className="page">
      <h1>Цагийн хуваарь</h1>
      <p className="muted small">
        Нэг слот = <strong>30 минутын</strong> нэг хичээл. Эхлэлийг :00 эсвэл :30 минутанд тохируулна (жишээ нь 10:00–10:30).
      </p>
      {!subjects.length ? (
        <div className="card" style={{ marginBottom: 12 }}>
          <p className="muted">
            Таны зааж буй хичээл (каталогтой таарсан) алга. <Link to="/teacher/profile">Профайл</Link> дээр хичээлийн нэрээ оруулна уу.
          </p>
        </div>
      ) : null}
      <form className="card form" onSubmit={addOrUpdateSlot}>
        {courseSubjectId !== '' && hideSubjectPicker ? (
          <div>
            <span className="muted small">Хичээл</span>
            <p style={{ margin: '6px 0 0', fontWeight: 600 }}>
              {selectedName}
              {subjects.find((s) => s.id === courseSubjectId)?.categoryName
                ? ` (${subjects.find((s) => s.id === courseSubjectId)?.categoryName})`
                : ''}
            </p>
          </div>
        ) : (
          <label>
            Хичээл (таны профайл + каталогийн огноо)
            <select
              value={courseSubjectId}
              onChange={(e) => setCourseSubjectId(e.target.value ? Number(e.target.value) : '')}
              required
            >
              <option value="">Сонгох</option>
              {subjects.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name} ({s.categoryName})
                </option>
              ))}
            </select>
          </label>
        )}
        <label>
          Эхлэх цаг (дуусахыг систем автоматаар +30 минут)
          <input type="datetime-local" value={startTime} onChange={(e) => setStartTime(e.target.value)} required />
        </label>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
          <button type="submit">{editingSlotId != null ? 'Шинэчлэх' : 'Слот нэмэх'}</button>
          {editingSlotId != null ? (
            <button type="button" className="muted" onClick={cancelEdit}>
              Цуцлах
            </button>
          ) : null}
        </div>
      </form>

      {error ? <div className="error" style={{ marginTop: 12 }}>{error}</div> : null}

      <div style={{ marginTop: 12, display: 'grid', gap: 10 }}>
        {slots.map((slot) => (
          <div key={slot.id} className="card">
            <p>
              <strong>{slot.courseSubjectName ?? '—'}</strong>
              {' · '}
              {new Date(slot.startTime).toLocaleString('mn-MN')} – {new Date(slot.endTime).toLocaleString('mn-MN')}
            </p>
            <p><strong>Төлөв:</strong> {slot.booked ? 'Захиалагдсан' : 'Сул'}</p>
            {!slot.booked ? (
              <>
                <button type="button" onClick={() => beginEdit(slot)}>
                  Засах
                </button>
                <button type="button" onClick={() => deleteSlot(slot.id)}>
                  Устгах
                </button>
              </>
            ) : null}
          </div>
        ))}
        {!slots.length ? <div className="muted">Одоогоор слот байхгүй.</div> : null}
      </div>
    </div>
  )
}
