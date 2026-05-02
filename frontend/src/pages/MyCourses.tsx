import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiError, fetchJson } from '../lib/api'
import { useAuth } from '../auth/AuthContext'
import type { Booking, CourseSubject } from '../auth/types'

type EnrolledCourseCard = {
  key: string
  teacherId: number
  teacherName: string
  courseSubjectId: number | null
  courseSubjectName: string | null
  subjectLine: string
}

function groupConfirmedBookings(bookings: Booking[]): EnrolledCourseCard[] {
  const map = new Map<string, EnrolledCourseCard>()
  for (const b of bookings) {
    if (b.status !== 'CONFIRMED') continue
    const csId = b.courseSubjectId
    const key =
      csId != null ? `${b.teacherId}-${csId}` : `${b.teacherId}-subj:${b.subject}`
    if (!map.has(key)) {
      map.set(key, {
        key,
        teacherId: b.teacherId,
        teacherName: b.teacherName,
        courseSubjectId: csId,
        courseSubjectName: b.courseSubjectName,
        subjectLine: b.courseSubjectName ?? b.subject,
      })
    }
  }
  return Array.from(map.values()).sort((a, b) => a.subjectLine.localeCompare(b.subjectLine, 'mn'))
}

function CourseThumb({ seed }: { seed: number }) {
  const h = ((seed * 17) % 360 + 360) % 360
  const h2 = ((seed * 31 + 40) % 360 + 360) % 360
  return (
    <div
      style={{
        height: 120,
        borderRadius: '12px 12px 0 0',
        background: `linear-gradient(135deg, hsl(${h}, 70%, 55%) 0%, hsl(${h2}, 65%, 50%) 55%, hsl(${(h + 180) % 360}, 55%, 45%) 100%)`,
      }}
    />
  )
}

export default function MyCoursesPage() {
  const { user } = useAuth()
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [teacherSubjects, setTeacherSubjects] = useState<CourseSubject[]>([])
  const [studentCards, setStudentCards] = useState<EnrolledCourseCard[]>([])
  const [filter, setFilter] = useState<'all' | string>('all')
  const [search, setSearch] = useState('')
  const [sortBy, setSortBy] = useState<'name' | 'category'>('name')

  useEffect(() => {
    let cancelled = false
    async function run() {
      setLoading(true)
      setError(null)
      try {
        if (user?.role === 'TEACHER') {
          const list = await fetchJson<CourseSubject[]>('/api/course/subjects/teaching', { method: 'GET' })
          if (!cancelled) setTeacherSubjects(list)
        } else if (user?.role === 'STUDENT') {
          const bookings = await fetchJson<Booking[]>('/api/bookings/me', { method: 'GET' })
          if (!cancelled) setStudentCards(groupConfirmedBookings(bookings))
        }
      } catch (err) {
        if (!cancelled) {
          if (err instanceof ApiError) setError(err.message)
          else setError('Ачаалж чадсангүй')
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    void run()
    return () => {
      cancelled = true
    }
  }, [user?.role])

  const teacherCategories = useMemo(() => {
    const set = new Set<string>()
    for (const s of teacherSubjects) set.add(s.categoryName)
    return Array.from(set).sort((a, b) => a.localeCompare(b, 'mn'))
  }, [teacherSubjects])

  const filteredTeacherSubjects = useMemo(() => {
    let list = teacherSubjects
    if (filter !== 'all') {
      list = list.filter((s) => s.categoryName === filter)
    }
    const q = search.trim().toLowerCase()
    if (q) {
      list = list.filter(
        (s) =>
          s.name.toLowerCase().includes(q) ||
          s.categoryName.toLowerCase().includes(q) ||
          s.description?.toLowerCase().includes(q),
      )
    }
    const sorted = [...list]
    sorted.sort((a, b) =>
      sortBy === 'name'
        ? a.name.localeCompare(b.name, 'mn')
        : a.categoryName.localeCompare(b.categoryName, 'mn') || a.name.localeCompare(b.name, 'mn'),
    )
    return sorted
  }, [teacherSubjects, filter, search, sortBy])

  if (!user || (user.role !== 'TEACHER' && user.role !== 'STUDENT')) {
    return (
      <div className="page">
        <p>Энэ хуудсыг зөвхөн багш эсвэл сурагч ашиглана.</p>
      </div>
    )
  }

  if (loading) {
    return (
      <div className="page">
        <p className="muted">Ачаалж байна...</p>
      </div>
    )
  }

  return (
    <div className="page" style={{ maxWidth: 960 }}>
      <div
        style={{
          marginBottom: 20,
          padding: '12px 16px',
          borderRadius: 12,
          background: 'linear-gradient(90deg, rgba(234, 179, 8, 0.95) 0%, rgba(250, 204, 21, 0.85) 100%)',
          color: '#fff',
          fontWeight: 700,
          fontSize: 22,
          letterSpacing: '-0.02em',
        }}
      >
        Миний хичээлүүд
      </div>

      {error ? <div className="error">{error}</div> : null}

      {user.role === 'TEACHER' ? (
        <>
          <div className="card" style={{ marginBottom: 16 }}>
            <h2 style={{ margin: '0 0 12px', fontSize: 18, borderBottom: '2px solid rgba(234, 179, 8, 0.8)', paddingBottom: 8 }}>
              Хичээлийн жагсаалт
            </h2>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, alignItems: 'center', marginBottom: 12 }}>
              <label className="muted small" style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                Ангилал
                <select value={filter} onChange={(e) => setFilter(e.target.value)}>
                  <option value="all">Бүгд</option>
                  {teacherCategories.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </label>
              <label style={{ flex: '1 1 200px', minWidth: 0 }}>
                <span className="muted small">Хайх</span>
                <input
                  type="search"
                  placeholder="Нэр, ангилал..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  style={{ width: '100%' }}
                />
              </label>
              <label className="muted small" style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                Эрэмбэлэх
                <select value={sortBy} onChange={(e) => setSortBy(e.target.value as 'name' | 'category')}>
                  <option value="name">Нэрээр</option>
                  <option value="category">Ангиллаар</option>
                </select>
              </label>
            </div>
          </div>

          {!teacherSubjects.length ? (
            <div className="card">
              <p className="muted">
                Таны профайлд каталогтой таарах хичээл алга.{' '}
                <Link to="/teacher/profile">Профайл</Link> хэсэгт каталогийн хичээлийн нэрийг (яг адилхан) оруулна уу.
              </p>
            </div>
          ) : (
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))',
                gap: 16,
              }}
            >
              {filteredTeacherSubjects.map((s) => (
                <Link
                  key={s.id}
                  to={`/teacher/subject/${s.id}`}
                  style={{ textDecoration: 'none', color: 'inherit' }}
                >
                  <div
                    className="card myCourseCard"
                    style={{ padding: 0, overflow: 'hidden', display: 'flex', flexDirection: 'column', height: '100%' }}
                  >
                    <CourseThumb seed={s.id} />
                    <div style={{ padding: 12, flex: 1, display: 'flex', flexDirection: 'column', gap: 8 }}>
                      <div className="muted small">
                        {s.categoryName}
                        {s.description ? ` · ${s.description.slice(0, 40)}${s.description.length > 40 ? '…' : ''}` : ''}
                      </div>
                      <div style={{ fontWeight: 700, fontSize: 16, lineHeight: 1.3 }}>{s.name}</div>
                      <div style={{ marginTop: 'auto' }} className="muted small">
                        Нээх →
                      </div>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </>
      ) : (
        <>
          <div className="card" style={{ marginBottom: 16 }}>
            <h2 style={{ margin: '0 0 8px', fontSize: 18 }}>Баталгаажсан хичээлүүд</h2>
            <p className="muted small" style={{ margin: 0 }}>
              Захиалга батлагдсан хичээл бүрт нэг карт харуулна.
            </p>
          </div>
          {!studentCards.length ? (
            <div className="muted">Одоогоор баталгаажсан хичээл алга.</div>
          ) : (
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))',
                gap: 16,
              }}
            >
              {studentCards.map((c) => (
                <Link
                  key={c.key}
                  to={`/teachers/${c.teacherId}`}
                  style={{ textDecoration: 'none', color: 'inherit' }}
                >
                  <div className="card" style={{ padding: 0, overflow: 'hidden', height: '100%' }}>
                    <CourseThumb seed={c.courseSubjectId ?? c.teacherId * 7} />
                    <div style={{ padding: 12 }}>
                      <div className="muted small">{c.teacherName}</div>
                      <div style={{ fontWeight: 700, marginTop: 6 }}>{c.subjectLine}</div>
                      <div className="muted small" style={{ marginTop: 8 }}>
                        Багшийг харах →
                      </div>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  )
}
