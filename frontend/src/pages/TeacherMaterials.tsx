import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { fetchJson, postFormData } from '../lib/api'
import { useAuth } from '../auth/AuthContext'
import { AlertBanner } from '../components/AlertBanner'
import { EmptyState } from '../components/EmptyState'
import { FileUploadField } from '../components/FileUploadField'
import { PageHeader } from '../components/PageHeader'
import { SectionCard } from '../components/SectionCard'
import { MaterialOpenButton } from '../components/MaterialOpenButton'
import { StatusPill } from '../components/StatusPill'
import type { CourseSubject, TeachingMaterial } from '../auth/types'
import { getFriendlyErrorMessage } from '../lib/errorMessages'

function pickCourseSubjectId(teaching: CourseSubject[], rawParam: string | null): number | '' {
  const n = rawParam ? Number(rawParam) : NaN
  if (!Number.isNaN(n) && teaching.some((s) => s.id === n)) return n
  if (teaching.length === 1) return teaching[0].id
  return ''
}

export default function TeacherMaterialsPage() {
  const { user } = useAuth()
  const [searchParams] = useSearchParams()
  const [teacherProfileId, setTeacherProfileId] = useState<number | null>(null)
  const [items, setItems] = useState<TeachingMaterial[]>([])
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [file, setFile] = useState<File | null>(null)
  const [subjects, setSubjects] = useState<CourseSubject[]>([])
  const [courseSubjectId, setCourseSubjectId] = useState<number | ''>('')
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)

  function formatMaterialMeta(material: TeachingMaterial) {
    const parts = [
      material.contentType ? material.contentType : null,
      material.sizeBytes != null
        ? material.sizeBytes >= 1024 * 1024
          ? `${(material.sizeBytes / (1024 * 1024)).toFixed(1)} MB`
          : `${Math.max(1, Math.round(material.sizeBytes / 1024))} KB`
        : null,
      new Date(material.createdAt).toLocaleString('mn-MN', { dateStyle: 'medium', timeStyle: 'short' }),
    ].filter(Boolean)

    return parts.join(' · ')
  }

  async function loadProfileAndMaterials() {
    setError(null)
    setLoading(true)
    try {
      const [profile, teaching] = await Promise.all([
        fetchJson<{ id: number }>('/api/teachers/me', { method: 'GET' }),
        fetchJson<CourseSubject[]>('/api/course/subjects/teaching', { method: 'GET' }),
      ])
      setTeacherProfileId(profile.id)
      setSubjects(teaching)
      setCourseSubjectId(pickCourseSubjectId(teaching, searchParams.get('courseSubjectId')))
      const list = await fetchJson<TeachingMaterial[]>(`/api/materials/teacher/${profile.id}`, { method: 'GET' })
      setItems(list)
    } catch (err) {
      setError(getFriendlyErrorMessage(err, 'Материалын мэдээллийг ачаалж чадсангүй.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadProfileAndMaterials()
  }, [searchParams])

  const rawUrl = searchParams.get('courseSubjectId')
  const urlNum = rawUrl ? Number(rawUrl) : NaN
  const urlMatches = !Number.isNaN(urlNum) && subjects.some((s) => s.id === urlNum)
  const hideSubjectPicker = subjects.length === 1 || urlMatches

  async function onUpload(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSuccess(null)
    if (!file) {
      setError('Файл сонгоно уу')
      return
    }
    if (courseSubjectId === '') {
      setError('Хичээл сонгоно уу')
      return
    }
    const fd = new FormData()
    fd.append('file', file)
    fd.append('courseSubjectId', String(courseSubjectId))
    if (title.trim()) fd.append('title', title.trim())
    if (description.trim()) fd.append('description', description.trim())
    try {
      setIsSubmitting(true)
      await postFormData<TeachingMaterial>('/api/materials', fd)
      setSuccess('Материал амжилттай байршлаа.')
      setFile(null)
      setTitle('')
      setDescription('')
      await loadProfileAndMaterials()
    } catch (err) {
      setError(getFriendlyErrorMessage(err, 'Материал байршуулах үед алдаа гарлаа.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  async function onDelete(id: number) {
    setError(null)
    setSuccess(null)
    try {
      await fetchJson(`/api/materials/${id}`, { method: 'DELETE' })
      await loadProfileAndMaterials()
      setSuccess('Материал амжилттай устгагдлаа.')
    } catch (err) {
      setError(getFriendlyErrorMessage(err, 'Материал устгах үед алдаа гарлаа.'))
    }
  }

  if (user?.role !== 'TEACHER') {
    return (
      <div className="page pageWide pageStack">
        <EmptyState title="Энэ хэсэгт зөвхөн багш нэвтэрнэ" description="Материал байршуулах боломж зөвхөн багш эрхтэй хэрэглэгчдэд нээлттэй." />
      </div>
    )
  }

  const selectedName = subjects.find((s) => s.id === courseSubjectId)?.name

  if (loading) {
    return (
      <div className="page pageWide pageStack">
        <PageHeader
          eyebrow="Материал"
          title="Материалын хэсгийг ачаалж байна"
          subtitle="Таны байршуулсан файлууд болон хичээлийн жагсаалтыг бэлдэж байна."
        />
        <p className="muted">Ачаалж байна…</p>
      </div>
    )
  }

  return (
    <div className="page pageWide pageStack">
      <PageHeader
        eyebrow="Материал"
        title="Хичээлийн материал удирдах"
        subtitle="Файлуудаа тодорхой хичээлтэй холбож байршуулна. Баталгаажсан сурагчид тухайн материалын холбоосыг харах боломжтой."
      />

      {error ? <AlertBanner variant="error">{error}</AlertBanner> : null}
      {success ? <AlertBanner variant="success">{success}</AlertBanner> : null}

      {!subjects.length ? (
        <EmptyState
          title="Зааж буй хичээл алга"
          description={
            <>
              Та эхлээд <Link to="/teacher/profile">профайл</Link> дээрээ заах хичээлүүдээ оруулснаар материал байршуулах боломжтой болно.
            </>
          }
        />
      ) : null}

      {subjects.length ? (
        <SectionCard
          title="Шинэ материал байршуулах"
          subtitle="Файлаа сонгож, ямар хичээлтэй холбохоо заагаад шууд байршуулна."
        >
          <form className="form" onSubmit={onUpload}>
            {courseSubjectId !== '' && hideSubjectPicker ? (
              <div className="selectedSubjectCard">
                <span className="muted small">Сонгогдсон хичээл</span>
                <strong>
                  {selectedName}
                  {subjects.find((s) => s.id === courseSubjectId)?.categoryName
                    ? ` (${subjects.find((s) => s.id === courseSubjectId)?.categoryName})`
                    : ''}
                </strong>
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
            <label>
              Гарчиг
              <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Файлын нэр" />
            </label>
            <label>
              Тайлбар
              <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={2} />
            </label>
            <FileUploadField
              file={file}
              onFileChange={setFile}
              label="Материалын файл"
              helperText="PNG, JPG, WEBP, PDF, DOCX, PPTX зэрэг файлаа энэ хэсэг дээр дарж сонгоно уу."
            />
            <div className="buttonRow">
              <button type="submit" disabled={isSubmitting}>
                {isSubmitting ? 'Байршуулж байна…' : 'Байршуулах'}
              </button>
            </div>
          </form>
        </SectionCard>
      ) : null}

      <SectionCard title="Миний материалууд" subtitle={`${items.length} файл`}>
        {items.length ? (
          <div className="subjectList">
            {items.map((material) => (
              <article key={material.id} className="card subjectResourceRow">
                <div className="subjectResourceMain">
                  <div className="subjectResourceTitleRow">
                    <strong>{material.title}</strong>
                    <StatusPill label={material.courseSubjectName ?? 'Хичээлгүй'} tone="info" />
                  </div>
                  <div className="muted small">{formatMaterialMeta(material)}</div>
                  <div className="subjectResourceDescription">
                    {material.description?.trim() ? material.description : 'Нэмэлт тайлбар оруулаагүй байна.'}
                  </div>
                </div>
                <div className="subjectResourceActions">
                  {material.secureUrl ? (
                    <MaterialOpenButton materialId={material.id} label="Нээх / татах" />
                  ) : (
                    <span className="muted small">Линк олдсонгүй</span>
                  )}
                  <button type="button" className="btnGhost smallBtn danger" onClick={() => void onDelete(material.id)}>
                    Устгах
                  </button>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <EmptyState
            title="Материал хараахан алга"
            description="Анхны файлаа байршуулахад энэ хэсэгт жагсаалтаар харагдана."
          />
        )}
      </SectionCard>
      {teacherProfileId ? (
        <p className="muted small">Таны профайл № {teacherProfileId}</p>
      ) : null}
    </div>
  )
}
