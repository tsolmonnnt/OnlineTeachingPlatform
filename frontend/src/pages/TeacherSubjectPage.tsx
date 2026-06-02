import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError, fetchJson, postFormData } from '../lib/api'
import { calendarSelectionToApiDateTime, parseLocalDateTime } from '../lib/datetime'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { WeeklyAvailabilityCalendar } from '../components/WeeklyAvailabilityCalendar'
import type { CalendarSlotSelection } from '../components/WeeklyAvailabilityCalendar'
import { AlertBanner } from '../components/AlertBanner'
import { EmptyState } from '../components/EmptyState'
import { FileUploadField } from '../components/FileUploadField'
import { MaterialOpenButton } from '../components/MaterialOpenButton'
import { Modal } from '../components/Modal'
import { PageHeader } from '../components/PageHeader'
import { SectionCard } from '../components/SectionCard'
import { StatusPill } from '../components/StatusPill'
import type { AvailabilitySlot, CourseSubject, QuizSummary, TeachingMaterial } from '../auth/types'
import { getFriendlyErrorMessage } from '../lib/errorMessages'

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
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden>
      <rect x="3" y="4" width="18" height="18" rx="2" />
      <path d="M16 2v4M8 2v4M3 10h18" strokeLinecap="round" />
    </svg>
  )
}

function UploadIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden>
      <path d="M12 16V8" strokeLinecap="round" />
      <path d="m8.5 11.5 3.5-3.5 3.5 3.5" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M7 18.5h10a3.5 3.5 0 0 0 .57-6.954A5 5 0 0 0 8.063 9.57 3.5 3.5 0 0 0 7 18.5Z" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function QuizIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden>
      <path d="M9 9a3 3 0 1 1 6 0c0 2-3 2-3 5" strokeLinecap="round" />
      <path d="M12 18h.01" strokeLinecap="round" />
      <circle cx="12" cy="12" r="9" />
    </svg>
  )
}

function formatSlotCard(slot: AvailabilitySlot) {
  const start = parseLocalDateTime(slot.startTime)
  const end = parseLocalDateTime(slot.endTime)
  const day = start.toLocaleDateString('mn-MN', { weekday: 'long' })
  const t1 = start.toLocaleTimeString('mn-MN', { hour: '2-digit', minute: '2-digit', hour12: false })
  const t2 = end.toLocaleTimeString('mn-MN', { hour: '2-digit', minute: '2-digit', hour12: false })
  return { day, range: `${t1} – ${t2}` }
}

function formatFileSize(sizeBytes: number | null) {
  if (!sizeBytes || sizeBytes <= 0) {
    return 'Хэмжээ тодорхойгүй'
  }

  if (sizeBytes >= 1024 * 1024) {
    return `${(sizeBytes / (1024 * 1024)).toFixed(1)} MB`
  }

  return `${Math.max(1, Math.round(sizeBytes / 1024))} KB`
}

function formatMaterialMeta(material: TeachingMaterial) {
  const parts = [
    material.contentType ? material.contentType : null,
    material.sizeBytes != null ? formatFileSize(material.sizeBytes) : null,
    new Date(material.createdAt).toLocaleString('mn-MN', { dateStyle: 'medium', timeStyle: 'short' }),
  ].filter(Boolean)

  return parts.join(' · ')
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
  const [success, setSuccess] = useState<string | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)

  const [calendarModalOpen, setCalendarModalOpen] = useState(false)
  const [calendarRange, setCalendarRange] = useState<{ start: Date; end: Date } | null>(null)
  const [slotPendingDelete, setSlotPendingDelete] = useState<AvailabilitySlot | null>(null)
  const [isDeletingSlot, setIsDeletingSlot] = useState(false)

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
      setLoadError(getFriendlyErrorMessage(err, 'Хичээлийн workspace-ийг ачаалж чадсангүй.'))
    } finally {
      setLoading(false)
    }
  }, [subjectId])

  useEffect(() => {
    void refresh()
  }, [refresh])

  const calendarSlots = useMemo(() => {
    if (!calendarRange) return slots
    return slots.filter((slot) => {
      const start = parseLocalDateTime(slot.startTime)
      return start >= calendarRange.start && start < calendarRange.end
    })
  }, [slots, calendarRange])

  function openCalendarModal() {
    setCalendarModalOpen(true)
  }

  async function createSlot(selection: CalendarSlotSelection) {
    setError(null)
    setSuccess(null)
    if (!Number.isFinite(subjectId)) return
    const payload = JSON.stringify({
      startTime: calendarSelectionToApiDateTime(selection.startStr, selection.start),
      courseSubjectId: subjectId,
    })
    try {
      const created = await fetchJson<AvailabilitySlot>('/api/schedules/me', {
        method: 'POST',
        body: payload,
      })
      setSlots((prev) => [...prev, created].sort((a, b) => a.startTime.localeCompare(b.startTime)))
      setSuccess('Шинэ слот амжилттай нэмэгдлээ.')
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError('Цагийн слот нэмэх үед алдаа гарлаа.')
    }
  }

  async function deleteSlot(slotId: number) {
    setError(null)
    setSuccess(null)
    setIsDeletingSlot(true)
    try {
      await fetchJson<void>(`/api/schedules/me/${slotId}`, { method: 'DELETE' })
      setSlots((prev) => prev.filter((s) => s.id !== slotId))
      setSlotPendingDelete(null)
      setSuccess('Слот амжилттай устгагдлаа.')
    } catch (err) {
      setError(getFriendlyErrorMessage(err, 'Слот устгах үед алдаа гарлаа.'))
    } finally {
      setIsDeletingSlot(false)
    }
  }

  async function submitMaterial(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSuccess(null)
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
      setSuccess('Материал амжилттай байршлаа.')
    } catch (err) {
      setError(getFriendlyErrorMessage(err, 'Материал байршуулах үед алдаа гарлаа.'))
    }
  }

  async function submitQuiz(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSuccess(null)
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
      setSuccess('Жишээ тест амжилттай үүслээ.')
    } catch (err) {
      setError(getFriendlyErrorMessage(err, 'Тест үүсгэх үед алдаа гарлаа.'))
    }
  }

  async function deleteMaterial(id: number) {
    setError(null)
    setSuccess(null)
    try {
      await fetchJson(`/api/materials/${id}`, { method: 'DELETE' })
      await refresh()
      setSuccess('Материал амжилттай устгагдлаа.')
    } catch (err) {
      setError(getFriendlyErrorMessage(err, 'Материал устгах үед алдаа гарлаа.'))
    }
  }

  async function deleteQuiz(id: number) {
    setError(null)
    setSuccess(null)
    try {
      await fetchJson(`/api/quizzes/mine/${id}`, { method: 'DELETE' })
      await refresh()
      setSuccess('Тест амжилттай устгагдлаа.')
    } catch (err) {
      setError(getFriendlyErrorMessage(err, 'Тест устгах үед алдаа гарлаа.'))
    }
  }

  if (!Number.isFinite(subjectId)) {
    return (
      <div className="page pageWide pageStack">
        <Link to="/my-courses" className="pageBackLink">
          ← Миний хичээлүүд
        </Link>
        <EmptyState title="Хичээлийн дугаар буруу байна" description="Хүссэн хичээлийн холбоос хүчингүй эсвэл олдсонгүй." />
      </div>
    )
  }

  if (loading) {
    return (
      <div className="page pageWide pageStack">
        <Link to="/my-courses" className="pageBackLink">
          ← Миний хичээлүүд
        </Link>
        <p className="muted">Ачаалж байна…</p>
      </div>
    )
  }

  if (loadError || !subject) {
    return (
      <div className="page pageWide pageStack">
        <Link to="/my-courses" className="pageBackLink">
          ← Миний хичээлүүд
        </Link>
        <AlertBanner variant="error">{loadError ?? 'Олдсонгүй'}</AlertBanner>
      </div>
    )
  }

  return (
    <div className="page pageWide pageStack subjectWorkspace">
      <Link to="/my-courses" className="pageBackLink">
        ← Миний хичээлүүд
      </Link>

      <PageHeader
        eyebrow={subject.categoryName}
        title={subject.name}
        subtitle={subject.description ?? 'Энэ хичээлийн цаг, материал, тестийг нэг workspace-ээс удирдана.'}
      />

      {error ? <AlertBanner variant="error">{error}</AlertBanner> : null}
      {success ? <AlertBanner variant="success">{success}</AlertBanner> : null}

      <div className="subjectStatGrid">
        <div className="card subjectStatCard">
          <div className="subjectStatLabel">Сул цаг</div>
          <div className="subjectStatValue">{slots.length}</div>
          <div className="subjectStatHint">Захиалга авах боломжтой цагууд</div>
        </div>
        <div className="card subjectStatCard">
          <div className="subjectStatLabel">Материал</div>
          <div className="subjectStatValue">{materials.length}</div>
          <div className="subjectStatHint">Энэ хичээлд байршуулсан файлууд</div>
        </div>
        <div className="card subjectStatCard">
          <div className="subjectStatLabel">Тест</div>
          <div className="subjectStatValue">{quizzes.length}</div>
          <div className="subjectStatHint">Сурагчдад өгөх боломжтой тестүүд</div>
        </div>
      </div>

      <section className="quickActions" aria-label="Хурдан үйлдэл">
        <button type="button" className="quickActionBtn" onClick={openCalendarModal}>
          <span className="quickActionIcon" aria-hidden>
            <CalendarIcon />
          </span>
          <span className="quickActionTitle">Цаг нэмэх</span>
          <span className="quickActionDescription">Шинэ 30 минутын слот үүсгэх</span>
        </button>
        <button type="button" className="quickActionBtn" onClick={() => setMaterialModalOpen(true)}>
          <span className="quickActionIcon" aria-hidden>
            <UploadIcon />
          </span>
          <span className="quickActionTitle">Файл оруулах</span>
          <span className="quickActionDescription">Хичээлийн материал байршуулах</span>
        </button>
        <button type="button" className="quickActionBtn" onClick={() => setQuizModalOpen(true)}>
          <span className="quickActionIcon" aria-hidden>
            <QuizIcon />
          </span>
          <span className="quickActionTitle">Тест үүсгэх</span>
          <span className="quickActionDescription">Сурагчдад зориулсан жишээ тест бэлдэх</span>
        </button>
      </section>

      <SectionCard
        title="Сул цагууд"
        subtitle="30 минутын слотуудыг долоо хоногийн каленараар нэмж, устгана (06:00–22:00)."
        actions={
          <button type="button" className="btnGhost" onClick={openCalendarModal}>
            Календар нээх
          </button>
        }
      >
        {slots.length ? (
          <div className="slotCardGrid subjectSlotSummary">
            {slots.map((slot) => {
              const { day, range } = formatSlotCard(slot)
              return (
                <div key={slot.id} className="slotCard">
                  <div className="slotCardHeader">
                    <div className="slotCardDay">{day}</div>
                    <StatusPill label={slot.booked ? 'Захиалагдсан' : 'Сул'} tone={slot.booked ? 'warning' : 'success'} />
                  </div>
                  <div className="slotCardTime">
                    <ClockIcon />
                    <span>{range}</span>
                  </div>
                  <div className="slotCardMeta muted small">
                    {slot.booked ? 'Сурагч захиалсан.' : 'Календар дээр засварлах боломжтой.'}
                  </div>
                </div>
              )
            })}
          </div>
        ) : (
          <EmptyState
            title="Сул цаг үүсээгүй байна"
            description="Хичээлээ захиалгад нээхийн тулд календар дээр эхний боломжит цагаа сонгоно уу."
            action={
              <button type="button" onClick={openCalendarModal}>
                Цаг нэмэх
              </button>
            }
          />
        )}
      </SectionCard>

      <SectionCard
        title="Материал"
        subtitle="Сурагчдад харагдах файл, тайлбар болон холбоосуудаа нэг дороос удирдана."
        actions={
          <button type="button" className="btnGhost" onClick={() => setMaterialModalOpen(true)}>
            Материал оруулах
          </button>
        }
      >
        {materials.length ? (
          <div className="subjectList">
            {materials.map((material) => (
              <article key={material.id} className="card subjectResourceRow">
                <div className="subjectResourceMain">
                  <div className="subjectResourceTitleRow">
                    <strong>{material.title}</strong>
                    <StatusPill label="Материал" tone="info" />
                  </div>
                  <div className="muted small">{formatMaterialMeta(material)}</div>
                  <div className="subjectResourceDescription">
                    {material.description?.trim() ? material.description : 'Нэмэлт тайлбар оруулаагүй байна.'}
                  </div>
                </div>
                <div className="subjectResourceActions">
                  {material.secureUrl ? (
                    <MaterialOpenButton materialId={material.id} />
                  ) : (
                    <span className="muted small">Линк олдсонгүй</span>
                  )}
                  <button type="button" className="btnGhost smallBtn danger" onClick={() => void deleteMaterial(material.id)}>
                    Устгах
                  </button>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <EmptyState
            title="Материал хараахан алга"
            description="Эхний файл, зураг эсвэл хичээлийн материалaa байршуулснаар энэ хэсэгт харагдана."
            action={
              <button type="button" onClick={() => setMaterialModalOpen(true)}>
                Файл оруулах
              </button>
            }
          />
        )}
      </SectionCard>

      <SectionCard
        title="Тестүүд"
        subtitle="Сурагчдад зориулсан богино шалгалт, дасгалын тестүүд."
        actions={
          <button type="button" className="btnGhost" onClick={() => setQuizModalOpen(true)}>
            Тест нэмэх
          </button>
        }
      >
        {quizzes.length ? (
          <div className="subjectList">
            {quizzes.map((quiz) => (
              <article key={quiz.id} className="card subjectResourceRow">
                <div className="subjectResourceMain">
                  <div className="subjectResourceTitleRow">
                    <strong>{quiz.title}</strong>
                    <StatusPill label={quiz.published ? 'Нийтлэгдсэн' : 'Ноорог'} tone={quiz.published ? 'success' : 'neutral'} />
                  </div>
                  <div className="muted small">
                    {quiz.questionCount} асуулт · {quiz.timeLimitMinutes} минут ·{' '}
                    {new Date(quiz.createdAt).toLocaleString('mn-MN', { dateStyle: 'medium', timeStyle: 'short' })}
                  </div>
                  <div className="subjectResourceDescription">
                    {quiz.description?.trim() ? quiz.description : 'Тайлбаргүй тест.'}
                  </div>
                </div>
                <div className="subjectResourceActions">
                  <button type="button" className="btnGhost smallBtn danger" onClick={() => void deleteQuiz(quiz.id)}>
                    Устгах
                  </button>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <EmptyState
            title="Тест үүсгээгүй байна"
            description="Сурагчдад өгөх жишээ тест эсвэл хурдан шалгалтаа эндээс эхлүүлнэ үү."
            action={
              <button type="button" onClick={() => setQuizModalOpen(true)}>
                Тест үүсгэх
              </button>
            }
          />
        )}
      </SectionCard>

      <Modal
        title="Цаг нэмэх"
        isOpen={calendarModalOpen}
        wide
        onClose={() => setCalendarModalOpen(false)}
      >
        <p className="muted small modalIntro">
          Долоо хоногийн хүснэгт дээр 30 минутын нүд сонгоно (06:00–22:00). Сул слот дээр дарж устгана.
        </p>
        <WeeklyAvailabilityCalendar
          mode="teacher"
          slots={calendarSlots}
          onCreateSlot={(selection) => void createSlot(selection)}
          onDeleteSlot={(slot) => {
            if (slot.booked) return
            setSlotPendingDelete(slot)
          }}
          onRangeChange={(start, end) => setCalendarRange({ start, end })}
        />
      </Modal>

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
          <FileUploadField
            file={matFile}
            onFileChange={setMatFile}
            label="Материалын файл"
            helperText="PNG, JPG, WEBP, PDF, DOCX, PPTX зэрэг материалыг энэ хэсэг дээр дарж сонгоно уу."
          />
          <div className="buttonRow subjectModalActions">
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
          <div className="buttonRow subjectModalActions">
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
