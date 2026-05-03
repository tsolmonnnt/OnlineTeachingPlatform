import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ApiError, fetchJson, postFormData } from '../lib/api'
import { useAuth } from '../auth/AuthContext'
import { FileUploadPreview } from '../components/FileUploadPreview'
import type { CourseSubject, TeachingMaterial } from '../auth/types'

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

  async function loadProfileAndMaterials() {
    setError(null)
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
      if (err instanceof ApiError) setError(err.message)
      else setError('Ачаалахад алдаа гарлаа')
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
      await postFormData<TeachingMaterial>('/api/materials', fd)
      setSuccess('Амжилттай байршигдлаа')
      setFile(null)
      setTitle('')
      setDescription('')
      await loadProfileAndMaterials()
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError('Байршуулахад алдаа гарлаа')
    }
  }

  async function onDelete(id: number) {
    setError(null)
    try {
      await fetchJson(`/api/materials/${id}`, { method: 'DELETE' })
      await loadProfileAndMaterials()
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError('Устгахад алдаа гарлаа')
    }
  }

  if (user?.role !== 'TEACHER') {
    return <div className="page"><p>Зөвхөн багш нэвтэрнэ үү.</p></div>
  }

  const selectedName = subjects.find((s) => s.id === courseSubjectId)?.name

  return (
    <div className="page">
      <h1>Хичээлийн материал</h1>
      <p className="muted">
        Cloudinary тохируулсан үед файл серверээр дамжин үүлэнд хадгалагдана. Материал нэг тодорхой хичээлд холбогдоно; тухайн хичээлээр баталгаажсан захиалгатай сурагчид л линк харна.
      </p>

      {error ? <div className="error">{error}</div> : null}
      {success ? <div className="card" style={{ borderColor: '#86efac' }}>{success}</div> : null}

      {!subjects.length ? (
        <div className="card" style={{ marginBottom: 12 }}>
          <p className="muted">
            Таны зааж буй хичээл алга. <Link to="/teacher/profile">Профайл</Link> дээр заах хичээлүүдээ оруулна уу.
          </p>
        </div>
      ) : null}

      <form className="card form" onSubmit={onUpload}>
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
        <label>
          Гарчиг
          <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Файлын нэр" />
        </label>
        <label>
          Тайлбар
          <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={2} />
        </label>
        <label>
          Файл
          <input type="file" onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
        </label>
        <FileUploadPreview file={file} />
        <button type="submit">Байршуулах</button>
      </form>

      <h2 style={{ marginTop: 24 }}>Миний материалууд</h2>
      <div style={{ display: 'grid', gap: 8 }}>
        {items.map((m) => (
          <div key={m.id} className="card" style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}>
            <div>
              <strong>{m.title}</strong>
              {m.courseSubjectName ? (
                <div className="muted small">Хичээл: {m.courseSubjectName}</div>
              ) : null}
              <div className="muted small">{m.description ?? ''}</div>
              {m.secureUrl ? (
                <a href={m.secureUrl} target="_blank" rel="noreferrer">
                  Нээх / татах
                </a>
              ) : (
                <span className="muted small">Линк байхгүй (энэ үзэгдэл хэвийн биш — админтай холбогдоно уу)</span>
              )}
            </div>
            <button type="button" onClick={() => onDelete(m.id)}>
              Устгах
            </button>
          </div>
        ))}
        {!items.length ? <div className="muted">Материал алга.</div> : null}
      </div>
      {teacherProfileId ? (
        <p className="muted small">Таны профайл № {teacherProfileId}</p>
      ) : null}
    </div>
  )
}
