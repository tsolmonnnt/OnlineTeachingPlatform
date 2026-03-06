import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError } from '../lib/api'
import { useAuth } from '../auth/AuthContext'

export default function LoginPage() {
  const { login, user } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setIsSubmitting(true)
    try {
      await login(email, password)
      const role = user?.role
      if (role === 'TEACHER') navigate('/teacher/profile', { replace: true })
      else navigate('/', { replace: true })
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError('Нэвтрэхэд алдаа гарлаа')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="page">
      <h1>Нэвтрэх</h1>
      <form className="card form" onSubmit={onSubmit}>
        <label>
          И-мэйл
          <input value={email} onChange={(e) => setEmail(e.target.value)} type="email" required />
        </label>
        <label>
          Нууц үг
          <input
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            type="password"
            required
          />
        </label>
        {error ? <div className="error">{error}</div> : null}
        <button disabled={isSubmitting} type="submit">
          {isSubmitting ? 'Нэвтэрч байна…' : 'Нэвтрэх'}
        </button>
      </form>
      <p className="muted">
        Бүртгэлгүй юу? <Link to="/register">Бүртгүүлэх</Link>
      </p>
    </div>
  )
}

