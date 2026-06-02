import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ApiError, fetchJson } from '../lib/api'
import { calendarSelectionToApiDateTime, parseLocalDateTime } from '../lib/datetime'
import { ConfirmDialog } from '../components/ConfirmDialog'
import type { AvailabilitySlot, CourseSubject } from '../auth/types'
import { WeeklyAvailabilityCalendar } from '../components/WeeklyAvailabilityCalendar'
import type { CalendarSlotSelection } from '../components/WeeklyAvailabilityCalendar'

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
  const [error, setError] = useState<string | null>(null)
  const [slotPendingDelete, setSlotPendingDelete] = useState<AvailabilitySlot | null>(null)
  const [isDeletingSlot, setIsDeletingSlot] = useState(false)

  async function load(range?: { start: Date; end: Date }) {
    setError(null)
    try {
      const teaching = await fetchJson<CourseSubject[]>('/api/course/subjects/teaching', { method: 'GET' })
      setSubjects(teaching)
      setCourseSubjectId(pickCourseSubjectId(teaching, searchParams.get('courseSubjectId')))
      if (range) {
        const meSlots = await fetchJson<AvailabilitySlot[]>('/api/schedules/me', { method: 'GET' })
        const filtered = meSlots.filter((slot) => {
          const start = parseLocalDateTime(slot.startTime)
          return start >= range.start && start < range.end
        })
        setSlots(filtered)
      } else {
        const slotResult = await fetchJson<AvailabilitySlot[]>('/api/schedules/me', { method: 'GET' })
        setSlots(slotResult)
      }
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError('Хуваарь ачаалж чадсангүй')
    }
  }

  useEffect(() => {
    const task = () => {
      void load()
    }
    const id = setTimeout(task, 0)
    return () => clearTimeout(id)
  }, [searchParams])

  const rawUrl = searchParams.get('courseSubjectId')
  const urlNum = rawUrl ? Number(rawUrl) : NaN
  const urlMatches = !Number.isNaN(urlNum) && subjects.some((s) => s.id === urlNum)
  const hideSubjectPicker = subjects.length === 1 || urlMatches

  async function createSlot(selection: CalendarSlotSelection) {
    setError(null)
    if (courseSubjectId === '') {
      setError('Хичээл болон эхлэх цаг сонгоно уу')
      return
    }
    const payload = JSON.stringify({
      startTime: calendarSelectionToApiDateTime(selection.startStr, selection.start),
      courseSubjectId,
    })
    try {
      const created = await fetchJson<AvailabilitySlot>('/api/schedules/me', {
        method: 'POST',
        body: payload,
      })
      setSlots((prev) => [...prev, created].sort((a, b) => a.startTime.localeCompare(b.startTime)))
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError('Цагийн слот нэмэх үед алдаа гарлаа')
    }
  }

  async function deleteSlot(slotId: number) {
    setError(null)
    setIsDeletingSlot(true)
    try {
      await fetchJson<void>(`/api/schedules/me/${slotId}`, { method: 'DELETE' })
      setSlots((prev) => prev.filter((s) => s.id !== slotId))
      setSlotPendingDelete(null)
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError('Слот устгах үед алдаа гарлаа')
    } finally {
      setIsDeletingSlot(false)
    }
  }

  const selectedName = useMemo(
    () => subjects.find((s) => s.id === courseSubjectId)?.name,
    [subjects, courseSubjectId],
  )

  return (
    <div className="page">
      <h1>Цагийн хуваарь</h1>
      <p className="muted small">
        Нэг слот = <strong>30 минутын</strong> нэг хичээл. Эхлэлийг :00 эсвэл :30 минутанд тохируулна (жишээ нь 10:00–10:30).
      </p>
      {!subjects.length ? (
        <div className="card" style={{ marginBottom: 12 }}>
          <p className="muted">
            Таны зааж буй хичээл алга. <Link to="/teacher/profile">Профайл</Link> дээр заах хичээлүүдээ оруулна уу.
          </p>
        </div>
      ) : null}
      <div className="card form">
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
            Хичээл (таны профайл)
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
        <p className="muted small">Календар дээр хоосон 30 минутын нүд сонгож слот нэмнэ. Сул слот дээр дарж устгана.</p>
      </div>

      {error ? <div className="error" style={{ marginTop: 12 }}>{error}</div> : null}

      <div className="card">
        <WeeklyAvailabilityCalendar
          mode="teacher"
          slots={slots}
          onCreateSlot={(selection) => void createSlot(selection)}
          onDeleteSlot={(slot) => {
            if (slot.booked) return
            setSlotPendingDelete(slot)
          }}
          onRangeChange={(start, end) => {
            void load({ start, end })
          }}
        />
        {!slots.length ? <div className="muted" style={{ marginTop: 10 }}>Сонгосон 7 хоногт слот байхгүй.</div> : null}
      </div>

      <ConfirmDialog
        isOpen={slotPendingDelete != null}
        title="Слот устгах"
        message="Энэ слотыг устгах уу?"
        confirmLabel="Устгах"
        cancelLabel="Цуцлах"
        confirmTone="danger"
        isConfirming={isDeletingSlot}
        onClose={() => {
          if (!isDeletingSlot) setSlotPendingDelete(null)
        }}
        onConfirm={() => {
          if (slotPendingDelete) void deleteSlot(slotPendingDelete.id)
        }}
      />
    </div>
  )
}
