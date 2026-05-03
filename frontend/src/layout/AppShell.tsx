import MenuIcon from '@mui/icons-material/Menu'
import NotificationsIcon from '@mui/icons-material/Notifications'
import CloseIcon from '@mui/icons-material/Close'
import { useState } from 'react'
import { Link, NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function AppShell() {
  const { user, logout } = useAuth()
  const [sidebarOpen, setSidebarOpen] = useState(false)

  const closeSidebar = () => setSidebarOpen(false)

  return (
    <div className="appShell">
      <header className="shellTopbar">
        <div className="shellTopbarLeft">
          {user ? (
            <button
              type="button"
              className="shellMenuBtn"
              aria-label="Цэс нээх"
              onClick={() => setSidebarOpen((o) => !o)}
            >
              {sidebarOpen ? <CloseIcon fontSize="small" /> : <MenuIcon fontSize="small" />}
            </button>
          ) : null}
          <Link className="shellBrand" to="/" onClick={closeSidebar}>
            ОнлайнСургалтынПлатформ
          </Link>
        </div>
        <nav className="shellTopNav" aria-label="Үндсэн навигаци">
          <NavLink className={({ isActive }) => `shellTopLink${isActive ? ' shellTopLinkActive' : ''}`} to="/" end>
            Нүүр
          </NavLink>
          <NavLink className={({ isActive }) => `shellTopLink${isActive ? ' shellTopLinkActive' : ''}`} to="/teachers">
            Багш хайх
          </NavLink>
          {user ? (
            <>
              {(user.role === 'TEACHER' || user.role === 'STUDENT') && (
                <NavLink
                  className={({ isActive }) => `shellTopLink${isActive ? ' shellTopLinkActive' : ''}`}
                  to="/my-courses"
                >
                  Миний хичээлүүд
                </NavLink>
              )}
              {user.role === 'TEACHER' && (
                <NavLink
                  className={({ isActive }) => `shellTopLink${isActive ? ' shellTopLinkActive' : ''}`}
                  to="/teacher/profile"
                >
                  Профайл
                </NavLink>
              )}
              {user.role === 'ADMIN' && (
                <NavLink
                  className={({ isActive }) => `shellTopLink${isActive ? ' shellTopLinkActive' : ''}`}
                  to="/admin"
                >
                  Админ
                </NavLink>
              )}
              <NavLink
                className={({ isActive }) => `shellTopLink${isActive ? ' shellTopLinkActive' : ''}`}
                to="/bookings"
              >
                Захиалгууд
              </NavLink>
              <NavLink
                className={({ isActive }) => `shellIconLink${isActive ? ' shellTopLinkActive' : ''}`}
                to="/notifications"
                aria-label="Мэдэгдэл"
              >
                <NotificationsIcon fontSize="small" />
              </NavLink>
              <span className="shellUserName muted">{user.fullName}</span>
              <button className="shellLogoutBtn" type="button" onClick={logout}>
                Гарах
              </button>
            </>
          ) : (
            <>
              <NavLink className={({ isActive }) => `shellTopLink${isActive ? ' shellTopLinkActive' : ''}`} to="/login">
                Нэвтрэх
              </NavLink>
              <NavLink
                className={({ isActive }) => `shellTopLink${isActive ? ' shellTopLinkActive' : ''}`}
                to="/register"
              >
                Бүртгүүлэх
              </NavLink>
            </>
          )}
        </nav>
      </header>

      {user && sidebarOpen ? <button type="button" className="shellSidebarBackdrop" aria-label="Цэс хаах" onClick={closeSidebar} /> : null}

      <div className={`shellBody${user ? '' : ' shellBodyGuest'}`}>
        {user ? (
          <aside className={`shellSidebar${sidebarOpen ? ' shellSidebarOpen' : ''}`} onClick={(e) => e.stopPropagation()}>
            <div className="shellSidebarSection">
              <div className="shellSidebarLabel">Үндсэн</div>
              <NavLink
                className={({ isActive }) => `shellSideLink${isActive ? ' shellSideLinkActive' : ''}`}
                to="/"
                end
                onClick={closeSidebar}
              >
                Нүүр
              </NavLink>
              {(user.role === 'TEACHER' || user.role === 'STUDENT') && (
                <NavLink
                  className={({ isActive }) => `shellSideLink${isActive ? ' shellSideLinkActive' : ''}`}
                  to="/my-courses"
                  onClick={closeSidebar}
                >
                  Миний хичээлүүд
                </NavLink>
              )}
              {user.role === 'TEACHER' && (
                <NavLink
                  className={({ isActive }) => `shellSideLink${isActive ? ' shellSideLinkActive' : ''}`}
                  to="/teacher/profile"
                  onClick={closeSidebar}
                >
                  Багшийн профайл
                </NavLink>
              )}
              {user.role === 'STUDENT' && (
                <NavLink
                  className={({ isActive }) => `shellSideLink${isActive ? ' shellSideLinkActive' : ''}`}
                  to="/teachers"
                  onClick={closeSidebar}
                >
                  Багш хайх
                </NavLink>
              )}
            </div>
            <div className="shellSidebarSection">
              <div className="shellSidebarLabel">Захиалга &amp; Мэдэгдэл</div>
              <NavLink
                className={({ isActive }) => `shellSideLink${isActive ? ' shellSideLinkActive' : ''}`}
                to="/bookings"
                onClick={closeSidebar}
              >
                Захиалгууд
              </NavLink>
              <NavLink
                className={({ isActive }) => `shellSideLink${isActive ? ' shellSideLinkActive' : ''}`}
                to="/notifications"
                onClick={closeSidebar}
              >
                Мэдэгдлүүд
              </NavLink>
            </div>
            {user.role === 'TEACHER' ? (
              <div className="shellSidebarSection">
                <div className="shellSidebarLabel">Багш</div>
                <NavLink
                  className={({ isActive }) => `shellSideLink${isActive ? ' shellSideLinkActive' : ''}`}
                  to="/teacher/schedule"
                  onClick={closeSidebar}
                >
                  Хуваарь
                </NavLink>
                <NavLink
                  className={({ isActive }) => `shellSideLink${isActive ? ' shellSideLinkActive' : ''}`}
                  to="/teacher/materials"
                  onClick={closeSidebar}
                >
                  Материал
                </NavLink>
                <NavLink
                  className={({ isActive }) => `shellSideLink${isActive ? ' shellSideLinkActive' : ''}`}
                  to="/teacher/quizzes"
                  onClick={closeSidebar}
                >
                  Тестүүд
                </NavLink>
              </div>
            ) : null}
            {user.role === 'ADMIN' ? (
              <div className="shellSidebarSection">
                <div className="shellSidebarLabel">Админ</div>
                <NavLink
                  className={({ isActive }) => `shellSideLink${isActive ? ' shellSideLinkActive' : ''}`}
                  to="/admin"
                  onClick={closeSidebar}
                >
                  Удирдлага
                </NavLink>
              </div>
            ) : null}
          </aside>
        ) : null}

        <div className="shellContentWrapper">
          <main className="shellMain">
            <Outlet />
          </main>

          <footer className="footer shellFooter">
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
      </div>
    </div>
  )
}
