import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchJson } from '../lib/api'
import { datetimeLocalInputToApi } from '../lib/datetime'
import { TeacherAvatar } from '../components/TeacherAvatar'
import { AlertBanner } from '../components/AlertBanner'
import { EmptyState } from '../components/EmptyState'
import { FilterBar } from '../components/FilterBar'
import { InfoList } from '../components/InfoList'
import { PageHeader } from '../components/PageHeader'
import { StatusPill } from '../components/StatusPill'
import type { CourseSubject, TeacherSummary } from '../auth/types'
import { getFriendlyErrorMessage } from '../lib/errorMessages'

function formatPrice(value: TeacherSummary['hourlyRate']) {
  if (value == null || value === '') return 'Тохиролцоно'
  return `${value}₮ / цаг`
}

function formatRating(teacher: TeacherSummary) {
  if (teacher.reviewCount > 0 && teacher.averageRating != null) {
    return `${teacher.averageRating.toFixed(1)} / 5`
  }
  return 'Үнэлгээ аваагүй'
}

export default function TeacherListPage() {
  const [query, setQuery] = useState('')
  const [subject, setSubject] = useState('')
  const [skill, setSkill] = useState('')
  const [availableAfter, setAvailableAfter] = useState('')

  const [subjects, setSubjects] = useState<CourseSubject[]>([])
  const [teachers, setTeachers] = useState<TeacherSummary[]>([])
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    ;(async () => {
      try {
        const list = await fetchJson<CourseSubject[]>('/api/course/subjects', { method: 'GET' })
        setSubjects(list)
      } catch {
        // Optional list; search still works without it.
      }
    })()
  }, [])

  async function searchTeachers() {
    setError(null)
    setIsLoading(true)
    try {
      const params = new URLSearchParams()
      if (query.trim()) params.set('query', query.trim())
      if (subject.trim()) params.set('subject', subject.trim())
      if (skill.trim()) params.set('skill', skill.trim())
      if (availableAfter) params.set('availableAfter', datetimeLocalInputToApi(availableAfter))

      const result = await fetchJson<TeacherSummary[]>(`/api/teachers?${params.toString()}`, { method: 'GET' })
      setTeachers(result)
    } catch (err) {
      setError(getFriendlyErrorMessage(err, 'Багш хайх үед алдаа гарлаа.'))
    } finally {
      setIsLoading(false)
    }
  }

  function clearFilters() {
    setQuery('')
    setSubject('')
    setSkill('')
    setAvailableAfter('')
  }

  useEffect(() => {
    void searchTeachers()
  }, [])

  return (
    <div className="page pageWide pageStack teacherListPage">
      <PageHeader
        eyebrow="Багш хайх"
        title="Өөрт тохирох багшаа олоорой"
        subtitle="Хичээл, ур чадвар, боломжит цагийн дагуу шүүж, баталгаажсан багшийн профайл болон хуваарийг шууд үзээрэй."
      />


      <FilterBar
        actions={
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' , marginTop:"1em"}}>
            <button onClick={() => void searchTeachers()} disabled={isLoading} type="button">
              {isLoading ? 'Хайж байна…' : 'Илэрц шинэчлэх'}
            </button>
            <button type="button" className="btnGhost" onClick={clearFilters}>
              Цэвэрлэх
            </button>
          </div>
        }
      >
        <label>
          Нэр эсвэл түлхүүр үг
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="ж: Математик, IELTS, Java"
          />
        </label>
        <label>
          Хичээлийн төрөл
          <select value={subject} onChange={(e) => setSubject(e.target.value)}>
            <option value="">Бүгд</option>
            {subjects.map((s) => (
              <option key={s.id} value={s.name}>
                {s.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          Ур чадвар
          <input value={skill} onChange={(e) => setSkill(e.target.value)} placeholder="ж: IELTS, Algebra" />
        </label>
        <label>
          Боломжит эхлэх хугацаа
          <input type="datetime-local" value={availableAfter} onChange={(e) => setAvailableAfter(e.target.value)} />
        </label>
      </FilterBar>

      {error ? <AlertBanner variant="error">{error}</AlertBanner> : null}

      <div className="teacherDiscoveryCount muted small">
        {isLoading
          ? 'Илэрцийг шинэчилж байна…'
          : teachers.length
            ? `Нийт ${teachers.length} багшийн илэрц олдлоо.`
            : 'Таны шалгуурт тохирох багш одоогоор олдсонгүй.'}
      </div>

      {teachers.length ? (
        <div className="teacherDiscoveryGrid">
          {teachers.map((teacher) => (
            <article key={teacher.id} className="card teacherCard">
              <div className="teacherCardHeader">
                <TeacherAvatar url={teacher.avatarUrl} name={teacher.fullName} size="md" />
                <div className="teacherCardIdentity">
                  <div className="teacherCardTitleRow">
                    <h3 className="teacherCardTitle">{teacher.fullName}</h3>
                    <StatusPill label={teacher.verified ? 'Баталгаажсан' : 'Шалгаж байна'} tone={teacher.verified ? 'success' : 'warning'} />
                  </div>
                  <p className="teacherCardHeadline">{teacher.headline ?? 'Товч танилцуулга оруулаагүй байна.'}</p>
                </div>
              </div>

              {(teacher.subjects?.length || teacher.skills?.length) ? (
                <div className="chipRow">
                  {(teacher.subjects ?? []).slice(0, 3).map((item) => (
                    <span key={`subject-${teacher.id}-${item}`} className="chip chip-subject">
                      {item}
                    </span>
                  ))}
                  {(teacher.skills ?? []).slice(0, 2).map((item) => (
                    <span key={`skill-${teacher.id}-${item}`} className="chip">
                      {item}
                    </span>
                  ))}
                </div>
              ) : null}

              <InfoList
                className="teacherCardInfo"
                items={[
                  { label: 'Үнэ', value: formatPrice(teacher.hourlyRate) },
                  { label: 'Туршлага', value: teacher.yearsExperience != null ? `${teacher.yearsExperience} жил` : 'Мэдээлэлгүй' },
                  { label: 'Үнэлгээ', value: `${formatRating(teacher)}${teacher.reviewCount ? ` (${teacher.reviewCount})` : ''}` },
                  { label: 'Байршил', value: teacher.location ?? 'Онлайн / тодорхойгүй' },
                ]}
              />

              <div className="teacherCardFooter">
                <Link className="buttonLink teacherCardCta" to={`/teachers/${teacher.id}`}>
                  Профайл үзэх
                </Link>
              </div>
            </article>
          ))}
        </div>
      ) : (
        !isLoading ? (
          <EmptyState
            title="Тохирох багш олдсонгүй"
            description="Шүүлтүүрээ өргөжүүлж дахин хайх эсвэл боломжит эхлэх хугацааг өөрчилж үзнэ үү."
            action={
              <button type="button" className="btnGhost" onClick={clearFilters}>
                Бүх шүүлтүүрийг цэвэрлэх
              </button>
            }
          />
        ) : null
      )}
    </div>
  )
}

