import './App.css'
import { Route, Routes } from 'react-router-dom'
import AppShell from './layout/AppShell'
import { ProtectedRoute } from './auth/ProtectedRoute'
import AdminDashboardPage from './pages/AdminDashboard'
import BookingsPage from './pages/Bookings'
import HomePage from './pages/HomePage'
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

function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route path="/" element={<HomePage />} />
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
      </Route>
    </Routes>
  )
}

export default App
