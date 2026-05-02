import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError, fetchJson } from '../lib/api'
import { useAuth } from '../auth/AuthContext'
import type { AttemptResult, QuizPublic } from '../auth/types'

export default function QuizTakePage() {
  const { quizId } = useParams()
  const { user } = useAuth()
  const id = Number(quizId)
  const [quiz, setQuiz] = useState<QuizPublic | null>(null)
  const [answers, setAnswers] = useState<Record<number, string>>({})
  const [result, setResult] = useState<AttemptResult | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return
    ;(async () => {
      try {
        const q = await fetchJson<QuizPublic>(`/api/quizzes/${id}/public`, { method: 'GET' })
        setQuiz(q)
      } catch (err) {
        if (err instanceof ApiError) {
          if (err.status === 401) {
            setError(
              'Тестийг ачаалахын тулд нэвтэрнэ үү. Тухайн багш, тухайн хичээлээр баталгаажсан захиалгатай сурагчид л хандах боломжтой.',
            )
          } else if (err.status === 403) {
            setError(
              'Энэ тестийг үзэх эрх байхгүй. Тухайн багш, тухайн хичээлээр баталгаажсан захиалга шаардлагатай.',
            )
          } else {
            setError(err.message)
          }
        } else setError('Тест ачаалж чадсангүй')
      }
    })()
  }, [id])

  async function submit() {
    if (!quiz) return
    setError(null)
    try {
      const payload = {
        answers: quiz.questions.map((q) => ({
          questionId: q.id,
          answer: answers[q.id] ?? '',
        })),
      }
      const res = await fetchJson<AttemptResult>(`/api/quizzes/${quiz.id}/attempts`, {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      setResult(res)
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 403) {
          setError(
            'Оролцох эрх байхгүй. Тухайн багш, тухайн хичээлээр баталгаажсан захиалга шаардлагатай.',
          )
        } else {
          setError(err.message)
        }
      } else setError('Илгээхэд алдаа гарлаа')
    }
  }

  if (!id) return <div className="page">Буруу холбоос</div>

  if (!quiz) {
    return (
      <div className="page">
        {error ? (
          <div className="error" style={{ marginBottom: 12 }}>
            {error}
            {user ? null : (
              <div style={{ marginTop: 8 }}>
                <Link to="/login">Нэвтрэх хуудас руу очих</Link>
              </div>
            )}
          </div>
        ) : (
          <p className="muted">Ачаалж байна...</p>
        )}
      </div>
    )
  }

  return (
    <div className="page">
      <Link to="/">Буцах</Link>
      <h1>{quiz.title}</h1>
      <p className="muted">{quiz.description}</p>
      <p className="muted">Хугацаа: {quiz.timeLimitMinutes} мин · Асуулт: {quiz.questions.length}</p>

      {error ? <div className="error">{error}</div> : null}

      {result ? (
        <div className="card" style={{ borderColor: '#86efac' }}>
          <strong>Дүн: {result.score} / {result.maxScore}</strong> ({result.percent}%)
        </div>
      ) : (
        <>
          {quiz.questions.map((q) => (
            <div key={q.id} className="card">
              <p><strong>{q.orderIndex + 1}. {q.prompt}</strong></p>
              {q.questionType === 'MCQ' && q.optionsJson ? (
                <select
                  value={answers[q.id] ?? ''}
                  onChange={(e) => setAnswers((a) => ({ ...a, [q.id]: e.target.value }))}
                >
                  <option value="">Сонгох</option>
                  {(JSON.parse(q.optionsJson) as string[]).map((opt, idx) => (
                    <option key={idx} value={String(idx)}>
                      {opt}
                    </option>
                  ))}
                </select>
              ) : null}
              {q.questionType === 'TRUE_FALSE' ? (
                <select
                  value={answers[q.id] ?? ''}
                  onChange={(e) => setAnswers((a) => ({ ...a, [q.id]: e.target.value }))}
                >
                  <option value="">Сонгох</option>
                  <option value="true">Үнэн</option>
                  <option value="false">Худал</option>
                </select>
              ) : null}
              {q.questionType === 'SHORT_ANSWER' ? (
                <input
                  value={answers[q.id] ?? ''}
                  onChange={(e) => setAnswers((a) => ({ ...a, [q.id]: e.target.value }))}
                />
              ) : null}
            </div>
          ))}
          <button type="button" disabled={user?.role !== 'STUDENT'} onClick={() => void submit()}>
            {user?.role === 'STUDENT' ? 'Илгээх' : 'Сурагчаар нэвтэрнэ үү'}
          </button>
        </>
      )}
    </div>
  )
}
