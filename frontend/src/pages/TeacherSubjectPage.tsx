import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError, fetchJson, postFormData } from '../lib/api'
import { FileUploadPreview } from '../components/FileUploadPreview'
import { Modal } from '../components/Modal'
import type { AvailabilitySlot, CourseSubject, QuizSummary, TeachingMaterial } from '../auth/types'

function ClockIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden>
      <circle cx="12" cy="12" r="10" />
      <path d="M12 6v6l4 2" strokeLinecap="round" />
    </svg>
  )
}

function CalendarIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#6366f1" strokeWidth="2" aria-hidden>
      <rect x="3" y="4" width="18" height="18" rx="2" />
      <path d="M16 2v4M8 2v4M3 10h18" strokeLinecap="round" />
    </svg>
  )
}

function formatSlotCard(slot: AvailabilitySlot) {
  const start = new Date(slot.startTime)
  const end = new Date(slot.endTime)
  const day = start.toLocaleDateString('mn-MN', { weekday: 'long' })
  const t1 = start.toLocaleTimeString('mn-MN', { hour: '2-digit', minute: '2-digit', hour12: false })
  const t2 = end.toLocaleTimeString('mn-MN', { hour: '2-digit', minute: '2-digit', hour12: false })
  return { day, range: `${t1} – ${t2}` }
}

function toDatetimeLocalValue(iso: string) {
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export default function TeacherSubjectPage() {
  const { courseSubjectId: paramId } = useParams<{ courseSubjectId: string }>()
  const subjectId = paramId ? Number(paramId) : NaN

  const [loading, setLoading] = useState(true)
  const [subject, setSubject] = useState<CourseSubject | null>(null)
  const [slots, setSlots] = useState<AvailabilitySlot[]>([])
  const [materials, setMaterials] = useState<TeachingMaterial[]>([])
  const [quizzes, setQuizzes] = useState<QuizSummary[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)

  const [slotModalOpen, setSlotModalOpen] = useState(false)
  const [editingSlotId, setEditingSlotId] = useState<number | null>(null)
  const [slotStartTime, setSlotStartTime] = useState('')

  const [materialModalOpen, setMaterialModalOpen] = useState(false)
  const [matTitle, setMatTitle] = useState('')
  const [matDesc, setMatDesc] = useState('')
  const [matFile, setMatFile] = useState<File | null>(null)

  const [quizModalOpen, setQuizModalOpen] = useState(false)
  const [quizTitle, setQuizTitle] = useState('Жишээ тест')
  const [quizDesc, setQuizDesc] = useState('Эхний асуулттай жишээ')
  const [quizTime, setQuizTime] = useState(15)

  const refresh = useCallback(async () => {
    if (!Number.isFinite(subjectId)) return
    setError(null)
    setLoadError(null)
    setLoading(true)
    setSubject(null)
    try {
      const [teaching, profile, allSlots, allQuizzes] = await Promise.all([
        fetchJson<CourseSubject[]>('/api/course/subjects/teaching', { method: 'GET' }),
        fetchJson<{ id: number }>('/api/teachers/me', { method: 'GET' }),
        fetchJson<AvailabilitySlot[]>('/api/schedules/me', { method: 'GET' }),
        fetchJson<QuizSummary[]>('/api/quizzes/mine', { method: 'GET' }),
      ])
      const meta = teaching.find((s) => s.id === subjectId) ?? null
      setSubject(meta)
      if (!meta) {
        setLoadError('Энэ хичээл таны профайлын жагсаалтад байхгүй.')
        return
      }
      setSlots(allSlots.filter((x) => x.courseSubjectId === subjectId).sort((a, b) => a.startTime.localeCompare(b.startTime)))
      const allMat = await fetchJson<TeachingMaterial[]>(`/api/materials/teacher/${profile.id}`, { method: 'GET' })
      setMaterials(allMat.filter((m) => m.courseSubjectId === subjectId))
      setQuizzes(allQuizzes.filter((q) => q.courseSubjectId === subjectId))
    } catch (err) {
      if (err instanceof ApiError) setLoadError(err.message)
      else setLoadError('Ачаалж чадсангүй')
    } finally {
      setLoading(false)
    }
  }, [subjectId])

  useEffect(() => {
    void refresh()
  }, [refresh])

  function openAddSlot() {
    setEditingSlotId(null)
    setSlotStartTime('')
    setSlotModalOpen(true)
  }

  function openEditSlot(slot: AvailabilitySlot) {
    if (slot.booked) return
    setEditingSlotId(slot.id)
    setSlotStartTime(toDatetimeLocalValue(slot.startTime))
    setSlotModalOpen(true)
  }

  async function submitSlot(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    if (!slotStartTime) {
      setError('Эхлэх цаг оруулна уу')
      return
    }
    const payload = JSON.stringify({
      startTime: new Date(slotStartTime).toISOString().slice(0, 19),
      courseSubjectId: subjectId,
    })
    try {
      if (editingSlotId != null) {
        await fetchJson<AvailabilitySlot>(`/api/schedules/me/${editingSlotId}`, {
          method: 'PUT',
          body: payload,
        })
      } else {
        await fetchJson<AvailabilitySlot>('/api/schedules/me', {
          method: 'POST',
          body: payload,
        })
      }
      setSlotModalOpen(false)
      setEditingSlotId(null)
      setSlotStartTime('')
      await refresh()
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError('Слот хадгалахад алдаа гарлаа')
    }
  }

  async function deleteSlot(slotId: number) {
    setError(null)
    try {
      await fetchJson<void>(`/api/schedules/me/${slotId}`, { method: 'DELETE' })
      await refresh()
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError('Устгахад алдаа гарлаа')
    }
  }

  async function submitMaterial(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    if (!matFile) {
      setError('Файл сонгоно уу')
      return
    }
    const fd = new FormData()
    fd.append('file', matFile)
    fd.append('courseSubjectId', String(subjectId))
    if (matTitle.trim()) fd.append('title', matTitle.trim())
    if (matDesc.trim()) fd.append('description', matDesc.trim())
    try {
      await postFormData<TeachingMaterial>('/api/materials', fd)
      setMaterialModalOpen(false)
      setMatTitle('')
      setMatDesc('')
      setMatFile(null)
      await refresh()
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError('Байршуулахад алдаа гарлаа')
    }
  }

  async function submitQuiz(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    try {
      await fetchJson('/api/quizzes', {
        method: 'POST',
        body: JSON.stringify({
          title: quizTitle,
          description: quizDesc,
          timeLimitMinutes: quizTime,
          courseSubjectId: subjectId,
          questions: [
            {
              type: 'TRUE_FALSE',
              prompt: 'Spring Boot нь Java суурьтай уу?',
              optionsJson: null,
              correctAnswer: 'true',
            },
            {
              type: 'MCQ',
              prompt: 'HTTP GET-ийн зорилго юу вэ?',
              optionsJson: JSON.stringify(['Өгөгдөл авах', 'Өгөгдөл устгах', 'Сервер унтраах']),
              correctAnswer: '0',
            },
          ],
        }),
      })
      setQuizModalOpen(false)
      await refresh()
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError('Тест үүсгэхэд алдаа гарлаа')
    }
  }

  async function deleteMaterial(id: number) {
    setError(null)
    try {
      await fetchJson(`/api/materials/${id}`, { method: 'DELETE' })
      await refresh()
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError('Устгахад алдаа гарлаа')
    }
  }

  async function deleteQuiz(id: number) {
    setError(null)
    try {
      await fetchJson(`/api/quizzes/mine/${id}`, { method: 'DELETE' })
      await refresh()
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
    }
  }

  if (!Number.isFinite(subjectId)) {
    return (
      <div className="page pageWide">
        <p>Буруу хичээлийн дугаар.</p>
        <Link to="/my-courses">← Буцах</Link>
      </div>
    )
  }

  if (loading) {
    return (
      <div className="page pageWide">
        <Link to="/my-courses" className="muted small subjectBack" style={{ textDecoration: 'none' }}>
          ← Миний хичээлүүд
        </Link>
        <p className="muted" style={{ marginTop: 16 }}>
          Ачаалж байна…
        </p>
      </div>
    )
  }

  if (loadError || !subject) {
    return (
      <div className="page pageWide">
        <Link to="/my-courses" className="muted small subjectBack" style={{ textDecoration: 'none' }}>
          ← Миний хичээлүүд
        </Link>
        <div className="error" style={{ marginTop: 16 }}>
          {loadError ?? 'Олдсонгүй'}
        </div>
      </div>
    )
  }

  return (
    <div className="page pageWide subjectWorkspace">
      <Link to="/my-courses" className="subjectBack muted small">
        ← Миний хичээлүүд
      </Link>

      <header className="subjectHeader">
        <div>
          <p className="muted small" style={{ margin: 0 }}>
            {subject.categoryName}
          </p>
          <h1 style={{ margin: '6px 0 4px', fontSize: 28, letterSpacing: '-0.03em' }}>{subject.name}</h1>
          {subject.description ? <p className="muted small">{subject.description}</p> : null}
        </div>
      </header>

      {error ? <div className="error" style={{ marginBottom: 16 }}>{error}</div> : null}

      <section className="quickActions" aria-label="Хурдан үйлдэл">
        <button type="button" className="quickActionBtn" onClick={openAddSlot}>
          <span className="quickActionIcon" aria-hidden>
            📅
          </span>
          <span>Цаг нэмэх</span>
        </button>
        <button type="button" className="quickActionBtn" onClick={() => setMaterialModalOpen(true)}>
          <span className="quickActionIcon" aria-hidden>
            📁
          </span>
          <span>Файл оруулах</span>
        </button>
        <button type="button" className="quickActionBtn" onClick={() => setQuizModalOpen(true)}>
          <span className="quickActionIcon" aria-hidden>
            📝
          </span>
          <span>Тест үүсгэх</span>
        </button>
        {/*<Link to="/teacher/profile" className="quickActionBtn quickActionLink">*/}
        {/*  <span className="quickActionIcon" aria-hidden>*/}
        {/*    👤*/}
        {/*  </span>*/}
        {/*  <span>Профайл засах</span>*/}
        {/*</Link>*/}
      </section>

      <section className="subjectSection">
        <div className="subjectSectionHead">
          <CalendarIcon />
          <h2>Сул цагууд</h2>
        </div>
        {!slots.length ? (
          <p className="muted small">Энэ хичээлд үүссэн слот алга. «Цаг нэмэх» товчоор нэмнэ үү.</p>
        ) : (
          <div className="slotCardGrid">
            {slots.map((slot) => {
              const { day, range } = formatSlotCard(slot)
              return (
                <div key={slot.id} className="slotCard">
                  <div className="slotCardDay">{day}</div>
                  <div className="slotCardTime">
                    <ClockIcon />
                    <span>{range}</span>
                  </div>
                  <div className="slotCardMeta muted small">
                    {slot.booked ? 'Захиалагдсан' : 'Сул'}
                  </div>
                  {!slot.booked ? (
                    <div className="slotCardActions">
                      <button type="button" className="btnGhost smallBtn" onClick={() => openEditSlot(slot)}>
                        Засах
                      </button>
                      <button type="button" className="btnGhost smallBtn danger" onClick={() => void deleteSlot(slot.id)}>
                        Устгах
                      </button>
                    </div>
                  ) : null}
                </div>
              )
            })}
          </div>
        )}
      </section>

      <section className="subjectSection">
        <h2>Материал</h2>
        <div className="subjectList">
          {materials.map((m) => (
            <div key={m.id} className="subjectListRow card">
              <div>
                <strong>{m.title}</strong>
                <div className="muted small">{m.description ?? ''}</div>
                {m.secureUrl ? (
                  <a href={m.secureUrl} target="_blank" rel="noreferrer">
                    Нээх
                  </a>
                ) : null}
              </div>
              <button type="button" className="btnGhost smallBtn danger" onClick={() => void deleteMaterial(m.id)}>
                Устгах
              </button>
            </div>
          ))}
          {!materials.length ? <p className="muted small">Материал алга.</p> : null}
        </div>
      </section>

      <section className="subjectSection">
        <h2>Тестүүд</h2>
        <div className="subjectList">
          {quizzes.map((q) => (
            <div key={q.id} className="subjectListRow card">
              <div>
                <strong>{q.title}</strong>
                <div className="muted small">
                  {q.questionCount} асуулт · {q.timeLimitMinutes} мин
                </div>
              </div>
              <button type="button" className="btnGhost smallBtn danger" onClick={() => void deleteQuiz(q.id)}>
                Устгах
              </button>
            </div>
          ))}
          {!quizzes.length ? <p className="muted small">Тест алга.</p> : null}
        </div>
      </section>

      <Modal
        title={editingSlotId != null ? 'Слот засах' : 'Шинэ слот'}
        isOpen={slotModalOpen}
        onClose={() => {
          setSlotModalOpen(false)
          setEditingSlotId(null)
          setSlotStartTime('')
        }}
      >
        <form className="form modalForm" onSubmit={(e) => void submitSlot(e)}>
          <p className="muted small" style={{ marginTop: 0 }}>
            Нэг слот = 30 минут. Эхлэл :00 эсвэл :30.
          </p>
          <label>
            Эхлэх цаг
            <input type="datetime-local" value={slotStartTime} onChange={(e) => setSlotStartTime(e.target.value)} required />
          </label>
          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 8 }}>
            <button type="button" className="btnGhost" onClick={() => setSlotModalOpen(false)}>
              Цуцлах
            </button>
            <button type="submit">{editingSlotId != null ? 'Хадгалах' : 'Нэмэх'}</button>
          </div>
        </form>
      </Modal>

      <Modal
        title="Материал байршуулах"
        isOpen={materialModalOpen}
        onClose={() => {
          setMaterialModalOpen(false)
          setMatTitle('')
          setMatDesc('')
          setMatFile(null)
        }}
      >
        <form className="form modalForm" onSubmit={(e) => void submitMaterial(e)}>
          <label>
            Гарчиг
            <input value={matTitle} onChange={(e) => setMatTitle(e.target.value)} placeholder="Сонголттой" />
          </label>
          <label>
            Тайлбар
            <textarea value={matDesc} onChange={(e) => setMatDesc(e.target.value)} rows={2} />
          </label>
          <label>
            Файл
            <input type="file" onChange={(e) => setMatFile(e.target.files?.[0] ?? null)} />
          </label>
          <FileUploadPreview file={matFile} />
          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 8 }}>
            <button type="button" className="btnGhost" onClick={() => setMaterialModalOpen(false)}>
              Цуцлах
            </button>
            <button type="submit">Байршуулах</button>
          </div>
        </form>
      </Modal>

      <Modal
        title="Жишээ тест үүсгэх"
        isOpen={quizModalOpen}
        onClose={() => setQuizModalOpen(false)}
      >
        <form className="form modalForm" onSubmit={(e) => void submitQuiz(e)}>
          <label>
            Гарчиг
            <input value={quizTitle} onChange={(e) => setQuizTitle(e.target.value)} />
          </label>
          <label>
            Тайлбар
            <textarea value={quizDesc} onChange={(e) => setQuizDesc(e.target.value)} rows={2} />
          </label>
          <label>
            Хугацаа (минут)
            <input type="number" min={1} value={quizTime} onChange={(e) => setQuizTime(Number(e.target.value))} />
          </label>
          <p className="muted small">2 жишээ асуулт автоматаар нэмэгдэнэ.</p>
          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 8 }}>
            <button type="button" className="btnGhost" onClick={() => setQuizModalOpen(false)}>
              Цуцлах
            </button>
            <button type="submit">Үүсгэх</button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
