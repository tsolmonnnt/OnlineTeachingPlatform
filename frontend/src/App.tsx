import './App.css'
import { Link, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'
import { ProtectedRoute } from './auth/ProtectedRoute'
import AdminDashboardPage from './pages/AdminDashboard'
import BookingsPage from './pages/Bookings'
import LoginPage from './pages/Login'
import NotificationsPage from './pages/Notifications'
import QuizTakePage from './pages/QuizTake'
import RegisterPage from './pages/Register'
import TeacherDetailPage from './pages/TeacherDetail'
import TeacherListPage from './pages/TeacherList'
import TeacherMaterialsPage from './pages/TeacherMaterials'
import TeacherProfilePage from './pages/TeacherProfile'
import TeacherQuizzesPage from './pages/TeacherQuizzes'
import TeacherSchedulePage from './pages/TeacherSchedule'
import MyCoursesPage from './pages/MyCourses'
import TeacherSubjectPage from './pages/TeacherSubjectPage'
import NotificationsIcon from '@mui/icons-material/Notifications';

function App() {
  const { user, logout } = useAuth()

  return (
    <div className="appShell">
      <header className="topbar">
        <Link className="brand" to="/">
          ОнлайнСургалтынПлатформ
        </Link>
        <nav className="nav">
          <Link to="/teachers">Багш хайх</Link>
          {user ? (
            <>
              <span className="muted">{user.fullName}</span>
              {user.role === 'TEACHER' || user.role === 'STUDENT' ? (
                <Link to="/my-courses">Миний хичээлүүд</Link>
              ) : null}
              {user.role === 'TEACHER' ? <Link to="/teacher/profile">Профайл</Link> : null}
              {user.role === 'ADMIN' ? <Link to="/admin">Админ</Link> : null}
              <Link to="/bookings">Захиалгууд</Link>
              <Link to="/notifications"><NotificationsIcon/></Link>
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
                  Онлайн платформоор багш хайх, сул цаг харах, хичээл захиалах, захиалгын төлөвөө хянах боломжтой.
                </p>
                <p className="muted">
                  {user
                    ? `${user.email} (${user.role}) эрхээр нэвтэрсэн байна.`
                    : 'Эхлээд бүртгүүлэх эсвэл нэвтэрч системийн бүх үйлдлийг ашиглана уу.'}
                </p>
              </div>
            }
          />
          <Route path="/teachers" element={<TeacherListPage />} />
          <Route path="/teachers/:teacherId" element={<TeacherDetailPage />} />
          <Route path="/quizzes/:quizId/take" element={<QuizTakePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          <Route element={<ProtectedRoute />}>
            <Route path="/my-courses" element={<MyCoursesPage />} />
            <Route path="/bookings" element={<BookingsPage />} />
            <Route path="/notifications" element={<NotificationsPage />} />
          </Route>

          <Route element={<ProtectedRoute requireRole="ADMIN" />}>
            <Route path="/admin" element={<AdminDashboardPage />} />
          </Route>

          <Route element={<ProtectedRoute requireRole="TEACHER" />}>
            <Route path="/teacher/subject/:courseSubjectId" element={<TeacherSubjectPage />} />
            <Route path="/teacher/profile" element={<TeacherProfilePage />} />
            <Route path="/teacher/schedule" element={<TeacherSchedulePage />} />
            <Route path="/teacher/materials" element={<TeacherMaterialsPage />} />
            <Route path="/teacher/quizzes" element={<TeacherQuizzesPage />} />
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
