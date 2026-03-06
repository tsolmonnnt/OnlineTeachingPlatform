import './App.css'
import { Link, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'
import { ProtectedRoute } from './auth/ProtectedRoute'
import LoginPage from './pages/Login'
import RegisterPage from './pages/Register'
import TeacherProfilePage from './pages/TeacherProfile'

function App() {
  const { user, logout } = useAuth()

  return (
    <div className="appShell">
      <header className="topbar">
        <Link className="brand" to="/">
          ОнлайнСургалтынПлатформ
        </Link>
        <nav className="nav">
          {user ? (
            <>
              <span className="muted">{user.fullName}</span>
              {user.role === 'TEACHER' ? (
                <Link to="/teacher/profile">Багшийн профайл</Link>
              ) : null}
              <button className="linkButton" onClick={logout}>
                Гарах
              </button>
            </>
          ) : (
            <>
              <Link to="/login">Нэвтрэх</Link>
              <Link to="/register">Бүртгүүлэх</Link>
            </>
          )}
        </nav>
      </header>

      <main className="main">
        <Routes>
          <Route
            path="/"
            element={
              <div className="page">
                <h1>Тавтай морилно уу</h1>
                <p className="muted">
                  ОнлайнСургалтынПлатформ нь багш, сурагчдыг нэг дор холбож, шууд хичээл,
                  бичлэгтэй курс, гэрийн даалгавар, үнэлгээний процессыг нэг цэгээс удирдах
                  зориулалттай платформ юм.
                </p>
                <p className="muted">
                  {user
                    ? `${user.email} (${user.role}) эрхээр нэвтэрсэн байна.`
                    : 'Хичээл үүсгэх эсвэл хичээлд хамрагдахыг хүсвэл эхлээд нэвтэрч эсвэл бүртгүүлнэ үү.'}
                </p>
              </div>
            }
          />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          <Route element={<ProtectedRoute requireRole="TEACHER" />}>
            <Route path="/teacher/profile" element={<TeacherProfilePage />} />
          </Route>

          <Route path="*" element={<div className="page">Хуудас олдсонгүй</div>} />
        </Routes>
      </main>

      <footer className="footer">
        <div className="footer-inner">
          <div>
            <div className="footer-title">Холбоо барих</div>
            <p className="muted small">
              Асуулт, санал хүсэлт байвал бидэнтэй дараах хаягаар холбогдоорой.
            </p>
          </div>
          <div className="footer-grid">
            <div>
              <div className="footer-label">И-мэйл</div>
              <div>support@onlineteaching.mn</div>
            </div>
            <div>
              <div className="footer-label">Утас</div>
              <div>+976 9911 2233</div>
            </div>
            <div>
              <div className="footer-label">Байршил</div>
              <div>Улаанбаатар хот, Сүхбаатар дүүрэг, 1-р хороо, Их сургуулийн гудамж</div>
            </div>
          </div>
        </div>
      </footer>
    </div>
  )
}

export default App
