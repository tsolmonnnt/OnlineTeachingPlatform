import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ApiError, fetchJson } from '../lib/api'
import type { CourseSubject, QuizSummary } from '../auth/types'

function pickCourseSubjectId(teaching: CourseSubject[], rawParam: string | null): number | '' {
  const n = rawParam ? Number(rawParam) : NaN
  if (!Number.isNaN(n) && teaching.some((s) => s.id === n)) return n
  if (teaching.length === 1) return teaching[0].id
  return ''
}

export default function TeacherQuizzesPage() {
  const [searchParams] = useSearchParams()
  const [list, setList] = useState<QuizSummary[]>([])
  const [title, setTitle] = useState('Жишээ тест')
  const [description, setDescription] = useState('Эхний асуулттай жишээ')
  const [timeLimit, setTimeLimit] = useState(15)
  const [subjects, setSubjects] = useState<CourseSubject[]>([])
  const [courseSubjectId, setCourseSubjectId] = useState<number | ''>('')
  const [error, setError] = useState<string | null>(null)

  async function load() {
    try {
      const [res, teaching] = await Promise.all([
        fetchJson<QuizSummary[]>('/api/quizzes/mine', { method: 'GET' }),
        fetchJson<CourseSubject[]>('/api/course/subjects/teaching', { method: 'GET' }),
      ])
      setList(res)
      setSubjects(teaching)
      setCourseSubjectId(pickCourseSubjectId(teaching, searchParams.get('courseSubjectId')))
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
    }
  }

  useEffect(() => {
    void load()
  }, [searchParams])

  const rawUrl = searchParams.get('courseSubjectId')
  const urlNum = rawUrl ? Number(rawUrl) : NaN
  const urlMatches = !Number.isNaN(urlNum) && subjects.some((s) => s.id === urlNum)
  const hideSubjectPicker = subjects.length === 1 || urlMatches

  async function createSample() {
    setError(null)
    if (courseSubjectId === '') {
      setError('Хичээл сонгоно уу')
      return
    }
    try {
      await fetchJson('/api/quizzes', {
        method: 'POST',
        body: JSON.stringify({
          title,
          description,
          timeLimitMinutes: timeLimit,
          courseSubjectId,
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
      await load()
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError('Үүсгэхэд алдаа гарлаа')
    }
  }

  async function removeQuiz(id: number) {
    setError(null)
    try {
      await fetchJson(`/api/quizzes/mine/${id}`, { method: 'DELETE' })
      await load()
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
    }
  }

  const selectedName = subjects.find((s) => s.id === courseSubjectId)?.name

  return (
    <div className="page">
      <h1>Тестүүд</h1>
      {error ? <div className="error">{error}</div> : null}

      {!subjects.length ? (
        <div className="card" style={{ marginBottom: 12 }}>
          <p className="muted">
            Таны зааж буй хичээл алга. <Link to="/teacher/profile">Профайл</Link> дээр заах хичээлүүдээ оруулна уу.
          </p>
        </div>
      ) : null}

      <div className="card form">
        <h2 style={{ marginTop: 0 }}>Жишээ тест үүсгэх</h2>
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
          <input value={title} onChange={(e) => setTitle(e.target.value)} />
        </label>
        <label>
          Тайлбар
          <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={2} />
        </label>
        <label>
          Хугацаа (минут)
          <input type="number" min={1} value={timeLimit} onChange={(e) => setTimeLimit(Number(e.target.value))} />
        </label>
        <button type="button" onClick={() => void createSample()}>
          Жишээ тест нэмэх (2 асуулт)
        </button>
      </div>

      <h2>Миний тестүүд</h2>
      <div style={{ display: 'grid', gap: 8 }}>
        {list.map((q) => (
          <div key={q.id} className="card" style={{ display: 'flex', justifyContent: 'space-between' }}>
            <div>
              <strong>{q.title}</strong>
              {q.courseSubjectName ? (
                <div className="muted small">Хичээл: {q.courseSubjectName}</div>
              ) : null}
              <div className="muted small">{q.questionCount} асуулт · {q.timeLimitMinutes} мин</div>
            </div>
            <button type="button" onClick={() => removeQuiz(q.id)}>
              Устгах
            </button>
          </div>
        ))}
        {!list.length ? <div className="muted">Тест алга.</div> : null}
      </div>
    </div>
  )
}
