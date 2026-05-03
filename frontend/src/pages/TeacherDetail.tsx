import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError, fetchJson } from '../lib/api'
import { useAuth } from '../auth/AuthContext'
import { TeacherAvatar } from '../components/TeacherAvatar'
import type { AvailabilitySlot, Booking, QuizSummary, ReviewItem, TeacherDetail, TeachingMaterial } from '../auth/types'

function formatIsoDate(value: string) {
  return new Date(value).toLocaleString('mn-MN')
}

export default function TeacherDetailPage() {
  const { teacherId } = useParams()
  const { user } = useAuth()

  const [teacher, setTeacher] = useState<TeacherDetail | null>(null)
  const [slots, setSlots] = useState<AvailabilitySlot[]>([])
  const [materials, setMaterials] = useState<TeachingMaterial[]>([])
  const [reviews, setReviews] = useState<ReviewItem[]>([])
  const [quizzes, setQuizzes] = useState<QuizSummary[]>([])
  const [myBookings, setMyBookings] = useState<Booking[]>([])

  const [subject, setSubject] = useState('')
  const [note, setNote] = useState('')
  const [selectedSlotId, setSelectedSlotId] = useState<number | null>(null)
  const [reviewBookingId, setReviewBookingId] = useState<number | null>(null)
  const [reviewRating, setReviewRating] = useState(5)
  const [reviewComment, setReviewComment] = useState('')

  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const activeTeacherId = Number(teacherId)

  const selectableSlots = useMemo(() => slots.filter((s) => !s.booked), [slots])

  const reviewableBookings = useMemo(() => {
    if (user?.role !== 'STUDENT' || !activeTeacherId) return []
    return myBookings.filter(
      (b) => b.status === 'CONFIRMED' && b.teacherId === activeTeacherId,
    )
  }, [myBookings, user?.role, activeTeacherId])

  useEffect(() => {
    if (!selectedSlotId) return
    const slot = slots.find((s) => s.id === selectedSlotId)
    if (slot?.courseSubjectName) setSubject(slot.courseSubjectName)
  }, [selectedSlotId, slots])

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
        const mats = await fetchJson<TeachingMaterial[]>(
          `/api/materials/teacher/${activeTeacherId}`,
          { method: 'GET' },
        )
        const rev = await fetchJson<ReviewItem[]>(`/api/reviews/teacher/${activeTeacherId}`, { method: 'GET' })
        const qz = await fetchJson<QuizSummary[]>(
          `/api/quizzes/teacher/${activeTeacherId}/published`,
          { method: 'GET' },
        )

        setTeacher(detail)
        setSlots(schedule)
        setMaterials(mats)
        setReviews(rev)
        setQuizzes(qz)
        setSubject(detail.subjects?.[0] ?? '')

        if (user?.role === 'STUDENT') {
          const bookings = await fetchJson<Booking[]>('/api/bookings/me', { method: 'GET' })
          setMyBookings(bookings)
        }
      } catch (err) {
        if (err instanceof ApiError) setError(err.message)
        else setError('Багшийн мэдээлэл ачаалж чадсангүй')
      } finally {
        setIsLoading(false)
      }
    })()
  }, [activeTeacherId, user?.role])

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

  async function submitReview() {
    if (!reviewBookingId) {
      setError('Захиалга сонгоно уу')
      return
    }
    setError(null)
    setSuccess(null)
    try {
      await fetchJson('/api/reviews', {
        method: 'POST',
        body: JSON.stringify({
          bookingId: reviewBookingId,
          rating: reviewRating,
          comment: reviewComment.trim() || null,
        }),
      })
      setSuccess('Сэтгэгдэл бүртгэгдлээ')
      const rev = await fetchJson<ReviewItem[]>(`/api/reviews/teacher/${activeTeacherId}`, { method: 'GET' })
      setReviews(rev)
      setReviewComment('')
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError('Илгээхэд алдаа гарлаа')
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
      <div className="teacherDetailHeader">
        <TeacherAvatar url={teacher.avatarUrl} name={teacher.fullName} size="lg" />
        <div className="teacherDetailHeaderText">
          <h1 style={{ marginTop: 0 }}>{teacher.fullName}</h1>
          <p className="muted" style={{ marginTop: 0 }}>
            {teacher.headline ?? 'Товч танилцуулга байхгүй'}
          </p>
        </div>
      </div>
      <div className="card" style={{ marginBottom: 12 }}>
        <p>{teacher.bio ?? 'Дэлгэрэнгүй танилцуулга оруулаагүй.'}</p>
        <p><strong>Хичээл:</strong> {(teacher.subjects ?? []).join(', ') || '-'}</p>
        <p><strong>Ур чадвар:</strong> {(teacher.skills ?? []).join(', ') || '-'}</p>
        <p><strong>Хэл:</strong> {(teacher.languages ?? []).join(', ') || '-'}</p>
        <p><strong>Үнэ:</strong> {teacher.hourlyRate ?? '-'}</p>
        <p className="muted small">
          {teacher.verified ? 'Баталгаажсан багш' : 'Профайл хүлээгдэж буй'} · Дундаж үнэлгээ:{' '}
          {teacher.reviewCount > 0 && teacher.averageRating != null
            ? `${teacher.averageRating.toFixed(1)} (${teacher.reviewCount} сэтгэгдэл)`
            : '—'}
        </p>
      </div>

      {materials.length ? (
        <div className="card" style={{ marginBottom: 12 }}>
          <h2 style={{ marginTop: 0 }}>Материал</h2>
          <ul style={{ margin: 0, paddingLeft: 20 }}>
            {materials.map((m) => (
              <li key={m.id}>
                {m.secureUrl ? (
                  <a href={m.secureUrl} target="_blank" rel="noreferrer">
                    {m.title}
                    {m.courseSubjectName ? ` · ${m.courseSubjectName}` : ''}
                  </a>
                ) : (
                  <span className="muted" title="Тухайн хичээлээр баталгаажсан захиалгатай, нэвтэрсэн сурагчид линк харагдана">
                    {m.title}
                    {m.courseSubjectName ? ` · ${m.courseSubjectName}` : ''}
                  </span>
                )}
              </li>
            ))}
          </ul>
        </div>
      ) : null}

      {quizzes.length ? (
        <div className="card" style={{ marginBottom: 12 }}>
          <h2 style={{ marginTop: 0 }}>Нийтлэгдсэн тестүүд</h2>
          <p className="muted small" style={{ marginTop: 0 }}>
            Тест ачаалах, оролцох нь нэвтэрсэн хэрэглэгчдэд зориулагдсан. Сурагч тухайн багш, тухайн хичээлээр баталгаажсан захиалгатай үед л зөвшөөрөгдөнө.
          </p>
          <ul style={{ margin: 0, paddingLeft: 20 }}>
            {quizzes.map((q) => (
              <li key={q.id}>
                <Link to={`/quizzes/${q.id}/take`}>{q.title}</Link>
                {q.courseSubjectName ? (
                  <span className="muted small"> · {q.courseSubjectName}</span>
                ) : null}
                {' '}
                <span className="muted small">({q.questionCount} асуулт)</span>
              </li>
            ))}
          </ul>
        </div>
      ) : null}

      <div className="card" style={{ marginBottom: 12 }}>
        <h2 style={{ marginTop: 0 }}>Сэтгэгдэл</h2>
        {reviews.length ? (
          <div style={{ display: 'grid', gap: 8 }}>
            {reviews.map((r) => (
              <div key={r.id} className="muted">
                <strong>{r.studentName}</strong> — {r.rating}★ · {formatIsoDate(r.createdAt)}
                {r.comment ? <div>{r.comment}</div> : null}
              </div>
            ))}
          </div>
        ) : (
          <p className="muted">Сэтгэгдэл алга.</p>
        )}

        {user?.role === 'STUDENT' && reviewableBookings.length ? (
          <div className="form" style={{ marginTop: 12 }}>
            <h3>Сэтгэгдэл үлдээх (баталгаажсан захиалга)</h3>
            <label>
              Захиалга
              <select
                value={reviewBookingId ?? ''}
                onChange={(e) => setReviewBookingId(e.target.value ? Number(e.target.value) : null)}
              >
                <option value="">Сонгох</option>
                {reviewableBookings.map((b) => (
                  <option key={b.id} value={b.id}>
                    #{b.id} — {b.courseSubjectName ?? b.subject} — {formatIsoDate(b.slotStartTime)}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Он (1–5)
              <input
                type="number"
                min={1}
                max={5}
                value={reviewRating}
                onChange={(e) => setReviewRating(Number(e.target.value))}
              />
            </label>
            <label>
              Сэтгэгдэл
              <textarea value={reviewComment} onChange={(e) => setReviewComment(e.target.value)} rows={3} />
            </label>
            <button type="button" onClick={() => void submitReview()}>
              Илгээх
            </button>
          </div>
        ) : null}
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
                  {slot.courseSubjectName ?? 'Хичээл'} · {formatIsoDate(slot.startTime)} – {formatIsoDate(slot.endTime)}
                </option>
              ))}
            </select>
          </label>
        ) : (
          <p className="muted">Одоогоор сул цаг алга.</p>
        )}

        <label>
          Хичээлийн нэр (слот сонгоход автоматаар бөглөгдөнө; захиалгын баталгаанд хэрэглэгдэнэ)
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
