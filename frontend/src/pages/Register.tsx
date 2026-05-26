import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import type { Role } from '../auth/types'
import { getFriendlyErrorMessage } from '../lib/errorMessages'

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
      const flashState = { flashMessage: 'Бүртгэл амжилттай үүслээ.' }
      if (role === 'TEACHER') navigate('/teacher/profile', { replace: true, state: flashState })
      else if (role === 'ADMIN') navigate('/admin', { replace: true, state: flashState })
      else navigate('/', { replace: true, state: flashState })
    } catch (err) {
      setError(getFriendlyErrorMessage(err, 'Бүртгүүлэхэд алдаа гарлаа.'))
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
            <option value="ADMIN">Админ</option>
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
