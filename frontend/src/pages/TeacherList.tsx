import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiError, fetchJson } from '../lib/api'
import { TeacherAvatar } from '../components/TeacherAvatar'
import type { CourseSubject, TeacherSummary } from '../auth/types'

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
      if (availableAfter) params.set('availableAfter', new Date(availableAfter).toISOString().slice(0, 19))

      const result = await fetchJson<TeacherSummary[]>(`/api/teachers?${params.toString()}`, { method: 'GET' })
      setTeachers(result)
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError('Багш хайх үед алдаа гарлаа')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    void searchTeachers()
  }, [])

  return (
    <div className="page">
      <h1>Багш хайх</h1>
      <div className="card form">
        <label>
          Нэр эсвэл түлхүүр үг
          <input value={query} onChange={(e) => setQuery(e.target.value)} />
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
        <button onClick={searchTeachers} disabled={isLoading} type="button">
          {isLoading ? 'Хайж байна...' : 'Хайх'}
        </button>
      </div>

      {error ? <div className="error" style={{ marginTop: 12 }}>{error}</div> : null}

      <div style={{ marginTop: 16, display: 'grid', gap: 12 }}>
        {teachers.map((teacher) => (
          <article key={teacher.id} className="card teacherListRow">
            <TeacherAvatar url={teacher.avatarUrl} name={teacher.fullName} size="sm" />
            <div className="teacherListRowBody">
            <h3 style={{ marginTop: 0 }}>{teacher.fullName}</h3>
            <p className="muted">{teacher.headline ?? 'Танилцуулга оруулаагүй'}</p>
            <p>
              <strong>Хичээл:</strong> {(teacher.subjects ?? []).join(', ') || '-'}
            </p>
            <p>
              <strong>Ур чадвар:</strong> {(teacher.skills ?? []).join(', ') || '-'}
            </p>
            <p>
              <strong>Туршлага:</strong> {teacher.yearsExperience ?? '-'} жил
            </p>
            <p>
              <strong>Үнэ:</strong> {teacher.hourlyRate ?? '-'}
            </p>
            <p className="muted small">
              {teacher.verified ? 'Баталгаажсан багш' : 'Хүлээгдэж буй'} · Үнэлгээ:{' '}
              {teacher.reviewCount > 0 && teacher.averageRating != null
                ? `${teacher.averageRating.toFixed(1)} (${teacher.reviewCount})`
                : '—'}
            </p>
            <Link to={`/teachers/${teacher.id}`}>Дэлгэрэнгүй</Link>
            </div>
          </article>
        ))}
        {!teachers.length && !isLoading ? <div className="muted">Илэрц олдсонгүй.</div> : null}
      </div>
    </div>
  )
}

