import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { fetchJson } from '../lib/api'
import { dateToApiLocalDateTime, formatLocalDateTime } from '../lib/datetime'
import { useAuth } from '../auth/AuthContext'
import { TeacherAvatar } from '../components/TeacherAvatar'
import { AlertBanner } from '../components/AlertBanner'
import { EmptyState } from '../components/EmptyState'
import { InfoList } from '../components/InfoList'
import { PageHeader } from '../components/PageHeader'
import { SectionCard } from '../components/SectionCard'
import { StatusPill } from '../components/StatusPill'
import type { AvailabilitySlot, Booking, QuizSummary, ReviewItem, TeacherDetail, TeachingMaterial } from '../auth/types'
import { getFriendlyErrorMessage } from '../lib/errorMessages'

function formatPrice(value: TeacherDetail['hourlyRate']) {
  if (value == null || value === '') return 'Тохиролцоно'
  return `${value}₮ / цаг`
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
        const fromParam = dateToApiLocalDateTime(from)
        const toParam = dateToApiLocalDateTime(to)
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
        setError(getFriendlyErrorMessage(err, 'Багшийн мэдээллийг ачаалж чадсангүй.'))
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
      setError(getFriendlyErrorMessage(err, 'Захиалга үүсгэх үед алдаа гарлаа.'))
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
      setError(getFriendlyErrorMessage(err, 'Сэтгэгдэл илгээх үед алдаа гарлаа.'))
    }
  }

  if (isLoading) {
    return (
      <div className="page pageWide pageStack">
        <Link to="/teachers" className="pageBackLink">
          ← Багш хайлт руу буцах
        </Link>
        <p className="muted">Ачаалж байна…</p>
      </div>
    )
  }

  if (!teacher) {
    return (
      <div className="page pageWide pageStack">
        <Link to="/teachers" className="pageBackLink">
          ← Багш хайлт руу буцах
        </Link>
        <EmptyState
          title="Багшийн профайл олдсонгүй"
          description="Холбоос буруу эсвэл тухайн багшийн мэдээлэл одоогоор харагдахгүй байна."
          action={
            <Link className="buttonLink" to="/teachers">
              Багш хайх хуудас руу очих
            </Link>
          }
        />
      </div>
    )
  }

  return (
    <div className="page pageWide pageStack">
      <Link to="/teachers" className="pageBackLink">
        ← Багш хайлт руу буцах
      </Link>

      <PageHeader
        eyebrow="Багшийн профайл"
        title={teacher.fullName}
        subtitle={teacher.headline ?? 'Товч танилцуулга оруулаагүй байна.'}
        actions={<StatusPill label={teacher.verified ? 'Баталгаажсан багш' : 'Профайл шалгаж байна'} tone={teacher.verified ? 'success' : 'warning'} />}
      />

      {error ? <AlertBanner variant="error">{error}</AlertBanner> : null}
      {success ? <AlertBanner variant="success">{success}</AlertBanner> : null}

      <div className="detailLayout">
        <div className="detailMain">
          <SectionCard className="detailHeroCard">
            <div className="detailHero">
              <TeacherAvatar url={teacher.avatarUrl} name={teacher.fullName} size="lg" />
              <div className="detailHeroBody">
                {(teacher.subjects.length || teacher.skills.length) ? (
                  <div className="chipRow">
                    {(teacher.subjects ?? []).map((item) => (
                      <span key={`subject-${item}`} className="chip chip-subject">
                        {item}
                      </span>
                    ))}
                    {(teacher.skills ?? []).map((item) => (
                      <span key={`skill-${item}`} className="chip">
                        {item}
                      </span>
                    ))}
                  </div>
                ) : null}

                <InfoList
                  className="detailInfoGrid"
                  items={[
                    { label: 'Үнэ', value: formatPrice(teacher.hourlyRate) },
                    { label: 'Туршлага', value: teacher.yearsExperience != null ? `${teacher.yearsExperience} жил` : 'Мэдээлэлгүй' },
                    { label: 'Хэл', value: (teacher.languages ?? []).join(', ') || 'Мэдээлэлгүй' },
                    { label: 'Байршил', value: teacher.location ?? 'Онлайн / тодорхойгүй' },
                    { label: 'Утас', value: teacher.phone ?? 'Оруулаагүй' },
                    {
                      label: 'Үнэлгээ',
                      value:
                        teacher.reviewCount > 0 && teacher.averageRating != null
                          ? `${teacher.averageRating.toFixed(1)} / 5 (${teacher.reviewCount} сэтгэгдэл)`
                          : 'Үнэлгээ хараахан аваагүй',
                    },
                  ]}
                />
              </div>
            </div>

            <div className="detailBio">
              <p>{teacher.bio ?? 'Дэлгэрэнгүй танилцуулга оруулаагүй байна.'}</p>
            </div>
          </SectionCard>

          <SectionCard
            title="Материал"
            subtitle="Тухайн багшийн байршуулсан файл, хичээлийн материалууд."
          >
            {materials.length ? (
              <div className="resourceList">
                {materials.map((m) => (
                  <div key={m.id} className="resourceItem">
                    <div>
                      <div className="resourceTitle">{m.title}</div>
                      <div className="muted small">
                        {m.courseSubjectName ?? 'Хичээл тодорхойгүй'}
                        {m.description ? ` · ${m.description}` : ''}
                      </div>
                    </div>
                    {m.secureUrl ? (
                      <a className="buttonLink" href={m.secureUrl} target="_blank" rel="noreferrer">
                        Нээх
                      </a>
                    ) : (
                      <span className="muted small" title="Захиалга баталгаажсан сурагчид линк харагдана">
                        Нэвтэрсэн сурагчид нээгдэнэ
                      </span>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <EmptyState
                title="Материал хараахан байршуулаагүй"
                description="Энэ багш одоогоор энэ хичээл дээр файл оруулаагүй байна."
              />
            )}
          </SectionCard>

          <SectionCard
            title="Нийтлэгдсэн тестүүд"
            subtitle="Тухайн багшийн нийтэлсэн тестүүд. Баталгаажсан захиалгатай сурагчид оролцоно."
          >
            {quizzes.length ? (
              <div className="resourceList">
                {quizzes.map((q) => (
                  <div key={q.id} className="resourceItem">
                    <div>
                      <div className="resourceTitle">{q.title}</div>
                      <div className="muted small">
                        {q.courseSubjectName ? `${q.courseSubjectName} · ` : ''}
                        {q.questionCount} асуулт · {q.timeLimitMinutes} минут
                      </div>
                    </div>
                    <Link className="buttonLink" to={`/quizzes/${q.id}/take`}>
                      Тест нээх
                    </Link>
                  </div>
                ))}
              </div>
            ) : (
              <EmptyState
                title="Нийтлэгдсэн тест алга"
                description="Энэ багш одоогоор олон нийтэд харагдах тест оруулаагүй байна."
              />
            )}
          </SectionCard>

          <SectionCard title="Сэтгэгдэл" subtitle="Бусад сурагчдын үлдээсэн үнэлгээ, сэтгэгдлүүд.">
            {reviews.length ? (
              <div className="reviewList">
                {reviews.map((r) => (
                  <div key={r.id} className="reviewItem">
                    <div className="reviewItemHeader">
                      <strong>{r.studentName}</strong>
                      <StatusPill label={`${r.rating}★`} tone="info" />
                    </div>
                    <div className="muted small">{formatLocalDateTime(r.createdAt)}</div>
                    {r.comment ? <p>{r.comment}</p> : <p className="muted">Тайлбаргүй үнэлгээ үлдээсэн.</p>}
                  </div>
                ))}
              </div>
            ) : (
              <EmptyState title="Сэтгэгдэл хараахан алга" description="Энэ багшид хараахан үнэлгээ, сэтгэгдэл үлдээгээгүй байна." />
            )}

            {user?.role === 'STUDENT' && reviewableBookings.length ? (
              <div className="reviewComposer">
                <h3 className="sectionInlineTitle">Сэтгэгдэл үлдээх</h3>
                <div className="form detailFormGrid">
                  <label>
                    Баталгаажсан захиалга
                    <select
                      value={reviewBookingId ?? ''}
                      onChange={(e) => setReviewBookingId(e.target.value ? Number(e.target.value) : null)}
                    >
                      <option value="">Сонгох</option>
                      {reviewableBookings.map((b) => (
                        <option key={b.id} value={b.id}>
                          #{b.id} — {b.courseSubjectName ?? b.subject} — {formatLocalDateTime(b.slotStartTime)}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Үнэлгээ (1–5)
                    <input
                      type="number"
                      min={1}
                      max={5}
                      value={reviewRating}
                      onChange={(e) => setReviewRating(Number(e.target.value))}
                    />
                  </label>
                </div>
                <label>
                  Сэтгэгдэл
                  <textarea value={reviewComment} onChange={(e) => setReviewComment(e.target.value)} rows={3} />
                </label>
                <div className="buttonRow">
                  <button type="button" onClick={() => void submitReview()}>
                    Сэтгэгдэл илгээх
                  </button>
                </div>
              </div>
            ) : null}
          </SectionCard>
        </div>

        <aside className="detailSide">
          <SectionCard
            title="Цаг захиалах"
            subtitle="Сурагч эрхтэй нэвтэрсэн үед доорх сул цагуудаас сонгон захиална."
            className="bookingSidebar"
          >
            {selectableSlots.length ? (
              <>
                <label>
                  Сул цаг
                  <select
                    value={selectedSlotId ?? ''}
                    onChange={(e) => setSelectedSlotId(e.target.value ? Number(e.target.value) : null)}
                  >
                    <option value="">Сонгох</option>
                    {selectableSlots.map((slot) => (
                      <option key={slot.id} value={slot.id}>
                        {slot.courseSubjectName ?? 'Хичээл'} · {formatLocalDateTime(slot.startTime)} – {formatLocalDateTime(slot.endTime)}
                      </option>
                    ))}
                  </select>
                </label>

                <label>
                  Хичээлийн нэр
                  <input value={subject} onChange={(e) => setSubject(e.target.value)} />
                </label>

                <label>
                  Тэмдэглэл
                  <textarea value={note} onChange={(e) => setNote(e.target.value)} rows={4} />
                </label>
              </>
            ) : (
              <EmptyState
                title="Одоогоор сул цаг алга"
                description="Өөр багш хайх эсвэл дараа дахин орж хуваарийг шалгана уу."
              />
            )}

            <div className="bookingSidebarFooter">
              <button type="button" onClick={onBook} disabled={user?.role !== 'STUDENT' || !selectableSlots.length}>
                {user?.role === 'STUDENT' ? 'Захиалга илгээх' : 'Сурагч эрхээр нэвтэрч захиална'}
              </button>
            </div>
          </SectionCard>
        </aside>
      </div>
    </div>
  )
}
