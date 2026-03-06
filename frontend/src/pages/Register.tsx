import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError } from '../lib/api'
import { useAuth } from '../auth/AuthContext'
import type { Role } from '../auth/types'

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()

  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<Role>('STUDENT')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setIsSubmitting(true)
    try {
      await register({ fullName, email, password, role })
      if (role === 'TEACHER') navigate('/teacher/profile', { replace: true })
      else navigate('/', { replace: true })
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError('Бүртгүүлэхэд алдаа гарлаа')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="page">
      <h1>Бүртгүүлэх</h1>
      <form className="card form" onSubmit={onSubmit}>
        <label>
          Овог нэр
          <input value={fullName} onChange={(e) => setFullName(e.target.value)} required />
        </label>
        <label>
          И-мэйл
          <input value={email} onChange={(e) => setEmail(e.target.value)} type="email" required />
        </label>
        <label>
          Нууц үг (хамгийн багадаа 8 тэмдэгт)
          <input
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            type="password"
            minLength={8}
            required
          />
        </label>
        <label>
          Төрөл
          <select value={role} onChange={(e) => setRole(e.target.value as Role)}>
            <option value="STUDENT">Сурагч</option>
            <option value="TEACHER">Багш</option>
          </select>
        </label>
        {error ? <div className="error">{error}</div> : null}
        <button disabled={isSubmitting} type="submit">
          {isSubmitting ? 'Үүсгэж байна…' : 'Бүртгэл үүсгэх'}
        </button>
      </form>
      <p className="muted">
        Бүртгэлтэй юу? <Link to="/login">Нэвтрэх</Link>
      </p>
    </div>
  )
}

