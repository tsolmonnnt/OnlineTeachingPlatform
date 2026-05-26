import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchJson } from '../lib/api'
import { useAuth } from '../auth/AuthContext'
import { AlertBanner } from '../components/AlertBanner'
import { EmptyState } from '../components/EmptyState'
import { FilterBar } from '../components/FilterBar'
import { PageHeader } from '../components/PageHeader'
import { SectionCard } from '../components/SectionCard'
import { StatusPill } from '../components/StatusPill'
import type { Booking, CourseSubject } from '../auth/types'
import { getFriendlyErrorMessage } from '../lib/errorMessages'

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
  return (
    <div
      className="courseThumb courseThumbCard"
      style={{
        background: `hsl(${h}, 38%, 42%)`,
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
          setError(getFriendlyErrorMessage(err, 'Хичээлийн мэдээллийг ачаалж чадсангүй.'))
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
      <div className="page pageWide pageStack">
        <EmptyState title="Энэ хэсэгт хандах боломжгүй" description="Энэ хуудсыг зөвхөн багш эсвэл сурагч эрхтэй хэрэглэгчид ашиглана." />
      </div>
    )
  }

  if (loading) {
    return (
      <div className="page pageWide pageStack">
        <PageHeader eyebrow="Миний хичээлүүд" title="Хичээлүүдийг бэлдэж байна" subtitle="Таны заадаг болон баталгаажсан хичээлүүдийг ачаалж байна." />
        <p className="muted">Ачаалж байна...</p>
      </div>
    )
  }

  return (
    <div className="page pageWide pageStack">
      <PageHeader
        eyebrow="Миний хичээлүүд"
        title={user.role === 'TEACHER' ? 'Заадаг хичээлүүдээ удирдах' : 'Баталгаажсан хичээлүүдээ хянах'}
        subtitle={
          user.role === 'TEACHER'
            ? 'Хичээл бүрийн хуваарь, материал, тестийг нэг дороос удирдана.'
            : 'Баталгаажсан хичээлүүд дээрээ багшийн профайл, материал болон хичээлийн мэдээллийг хянаарай.'
        }
      />

      {error ? <AlertBanner variant="error">{error}</AlertBanner> : null}

      {user.role === 'TEACHER' ? (
        <>
          <FilterBar>
            <label>
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
            <label>
              Хайх
              <input
                type="search"
                placeholder="Нэр, ангилал..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </label>
            <label>
              Эрэмбэлэх
              <select value={sortBy} onChange={(e) => setSortBy(e.target.value as 'name' | 'category')}>
                <option value="name">Нэрээр</option>
                <option value="category">Ангиллаар</option>
              </select>
            </label>
          </FilterBar>

          <div className="courseCatalogSummary muted small">
            {filteredTeacherSubjects.length
              ? `Нийт ${filteredTeacherSubjects.length} хичээл удирдах боломжтой байна.`
              : 'Таны шүүлтүүрт тохирох хичээл олдсонгүй.'}
          </div>

          {!teacherSubjects.length ? (
            <EmptyState
              title="Заадаг хичээл хараахан алга"
              description={
                <>
                  Профайл дээрээ заах хичээлүүдээ оруулснаар энд автоматаар жагсана.{' '}
                  <Link to="/teacher/profile">Профайл руу орох</Link>
                </>
              }
            />
          ) : (
            <div className="courseCatalogGrid">
              {filteredTeacherSubjects.map((s) => (
                <Link key={s.id} to={`/teacher/subject/${s.id}`} className="courseCardLink">
                  <div className="card myCourseCard courseCardSurface">
                    <CourseThumb seed={s.id} />
                    <div className="courseCardBody">
                      <div className="courseCardTop">
                        <StatusPill label={s.categoryName} tone="info" />
                        <span className="courseCardMeta">Удирдлагын workspace</span>
                      </div>
                      <div className="courseCardTitle">{s.name}</div>
                      <p className="courseCardDescription">
                        {s.description?.trim()
                          ? s.description
                          : 'Эндээс тухайн хичээлийн цаг, материал, тестүүдээ нэг дороос удирдана.'}
                      </p>
                      <div className="courseCardFeatures">
                        <span className="courseCardFeature">Сул цаг</span>
                        <span className="courseCardFeature">Материал</span>
                        <span className="courseCardFeature">Тест</span>
                      </div>
                      <div className="courseCardFooter">
                        <div className="courseCardHint">Цаг, материал, тест удирдах</div>
                        <div className="courseCardArrow" aria-hidden>
                          →
                        </div>
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
          <SectionCard
            title="Баталгаажсан хичээлүүд"
            subtitle="Захиалга батлагдсан хичээл бүр энд нэг картаар харагдана."
          >
            <p className="muted small">Багшийн профайл руу орж материал, тест болон ирэх хичээлийн мэдээллийг хянаарай.</p>
          </SectionCard>
          {!studentCards.length ? (
            <EmptyState
              title="Баталгаажсан хичээл алга"
              description="Багшийн сул цагийг сонгож захиалга баталгаажсаны дараа энэ хэсэгт харагдана."
            />
          ) : (
            <div className="courseCatalogGrid">
              {studentCards.map((c) => (
                <Link key={c.key} to={`/teachers/${c.teacherId}`} className="courseCardLink">
                  <div className="card courseCardSurface">
                    <CourseThumb seed={c.courseSubjectId ?? c.teacherId * 7} />
                    <div className="courseCardBody">
                      <div className="courseCardTop">
                        <StatusPill label="Баталгаажсан" tone="success" />
                        <span className="courseCardMeta">{c.teacherName}</span>
                      </div>
                      <div className="courseCardTitle">{c.subjectLine}</div>
                      <p className="courseCardDescription">
                        Багшийн профайл, материал болон тестийн мэдээллийг энэ хичээлээс харах боломжтой.
                      </p>
                      <div className="courseCardFeatures">
                        <span className="courseCardFeature">Материал</span>
                        <span className="courseCardFeature">Тест</span>
                        <span className="courseCardFeature">Хуваарь</span>
                      </div>
                      <div className="courseCardFooter">
                        <div className="courseCardHint">Багшийн профайл харах</div>
                        <div className="courseCardArrow" aria-hidden>
                          →
                        </div>
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
