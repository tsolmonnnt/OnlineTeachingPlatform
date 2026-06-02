import MenuIcon from '@mui/icons-material/Menu'
import NotificationsIcon from '@mui/icons-material/Notifications'
import CloseIcon from '@mui/icons-material/Close'
import DarkModeOutlinedIcon from '@mui/icons-material/DarkModeOutlined'
import LightModeOutlinedIcon from '@mui/icons-material/LightModeOutlined'
import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { useUnreadNotificationCount } from '../hooks/useUnreadNotificationCount'
import { useTheme } from '../theme/ThemeContext'

type ShellNavItem = {
  to: string
  label: string
  end?: boolean
}

function userInitials(fullName: string) {
  const parts = fullName.trim().split(/\s+/).filter(Boolean)
  if (parts.length === 0) return '?'
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase()
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
}

function getFlashMessage(state: unknown): string | null {
  if (!state || typeof state !== 'object' || Array.isArray(state)) {
    return null
  }

  const flashMessage = (state as { flashMessage?: unknown }).flashMessage
  return typeof flashMessage === 'string' && flashMessage.trim() ? flashMessage : null
}

export default function AppShell() {
  const { user, logout } = useAuth()
  const { isDark, toggleTheme } = useTheme()
  const location = useLocation()
  const navigate = useNavigate()
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const { count: unreadNotificationCount } = useUnreadNotificationCount(Boolean(user))

  const closeSidebar = () => setSidebarOpen(false)
  const unreadBadgeLabel =
    unreadNotificationCount > 9 ? '9+' : unreadNotificationCount > 0 ? String(unreadNotificationCount) : null
  const flashMessage = getFlashMessage(location.state)
  const guestNavItems: ShellNavItem[] = [
    { to: '/', label: 'Нүүр', end: true },
    { to: '/teachers', label: 'Багш хайх' },
    { to: '/login', label: 'Нэвтрэх' },
    { to: '/register', label: 'Бүртгүүлэх' },
  ]
  const primaryNavItems: ShellNavItem[] = [
    { to: '/', label: 'Нүүр', end: true },
    { to: '/teachers', label: 'Багш хайх' },
  ]
  const workflowNavItems: ShellNavItem[] = [
    { to: '/bookings', label: 'Захиалгууд' },
    { to: '/notifications', label: 'Мэдэгдлүүд' },
  ]
  const teacherNavItems: ShellNavItem[] = [
    { to: '/teacher/profile', label: 'Багшийн профайл' },
    { to: '/teacher/schedule', label: 'Хуваарь' },
    { to: '/teacher/materials', label: 'Материал' },
    { to: '/teacher/quizzes', label: 'Тестүүд' },
  ]
  const studentNavItems: ShellNavItem[] = [{ to: '/account', label: 'Миний профайл' }]
  const adminNavItems: ShellNavItem[] = [{ to: '/admin', label: 'Удирдлага' }]

  if (user?.role === 'TEACHER' || user?.role === 'STUDENT') {
    primaryNavItems.splice(2, 0, { to: '/my-courses', label: 'Миний хичээлүүд' })
  }

  useEffect(() => {
    if (!flashMessage) {
      return
    }

    const timeoutId = window.setTimeout(() => {
      void navigate(`${location.pathname}${location.search}${location.hash}`, {
        replace: true,
        state: null,
      })
    }, 4000)
    return () => window.clearTimeout(timeoutId)
  }, [flashMessage, location.hash, location.pathname, location.search, navigate])

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
            Цахим Сургалтын Платформ
          </Link>
        </div>
        {user ? (
          <div className="shellTopbarActions" aria-label="Хэрэгслийн хэсэг">
            <NavLink
              className={({ isActive }) => `shellIconLink${isActive ? ' shellTopLinkActive' : ''}`}
              to="/notifications"
              aria-label={
                unreadNotificationCount > 0
                  ? `Мэдэгдэл (${unreadNotificationCount} уншаагүй)`
                  : 'Мэдэгдэл'
              }
            >
              <span className="shellIconBadgeWrap">
                <NotificationsIcon fontSize="small" />
                {unreadBadgeLabel ? (
                  <span className="shellIconBadge" aria-hidden>
                    {unreadBadgeLabel}
                  </span>
                ) : null}
              </span>
            </NavLink>
            <button
              type="button"
              className="shellThemeBtn"
              onClick={toggleTheme}
              aria-label={isDark ? 'Гэрэл theme рүү шилжих' : 'Харанхуй theme рүү шилжих'}
              title={isDark ? 'Гэрэл горим' : 'Харанхуй горим'}
            >
              {isDark ? <LightModeOutlinedIcon fontSize="small" /> : <DarkModeOutlinedIcon fontSize="small" />}
              {/*<span className="shellThemeLabel">{isDark ? 'Гэрэл' : 'Харанхуй'}</span>*/}
            </button>
            {user.avatarUrl ? (
              <img className="shellAvatar" src={user.avatarUrl} alt="" />
            ) : (
              <span className="shellAvatar shellAvatarFallback" aria-hidden>
                {userInitials(user.fullName)}
              </span>
            )}
            <span className="shellUserName muted">{user.fullName}</span>
            <button className="shellLogoutBtn" type="button" onClick={logout}>
              Гарах
            </button>
          </div>
        ) : (
          <nav className="shellTopNav" aria-label="Үндсэн навигаци">
            {guestNavItems.map((item) => (
              <NavLink
                key={item.to}
                className={({ isActive }) => `shellTopLink${isActive ? ' shellTopLinkActive' : ''}`}
                to={item.to}
                end={item.end}
              >
                {item.label}
              </NavLink>
            ))}
            <button
              type="button"
              className="shellThemeBtn"
              onClick={toggleTheme}
              aria-label={isDark ? 'Гэрэл theme рүү шилжих' : 'Харанхуй theme рүү шилжих'}
              title={isDark ? 'Гэрэл горим' : 'Харанхуй горим'}
            >
              {isDark ? <LightModeOutlinedIcon fontSize="small" /> : <DarkModeOutlinedIcon fontSize="small" />}
              {/*<span className="shellThemeLabel">{isDark ? 'Гэрэл' : 'Харанхуй'}</span>*/}
            </button>
          </nav>
        )}
      </header>

      {user && sidebarOpen ? <button type="button" className="shellSidebarBackdrop" aria-label="Цэс хаах" onClick={closeSidebar} /> : null}

      <div className={`shellBody${user ? '' : ' shellBodyGuest'}`}>
        {user ? (
          <aside className={`shellSidebar${sidebarOpen ? ' shellSidebarOpen' : ''}`} onClick={(e) => e.stopPropagation()}>
            <div className="shellSidebarSection">
              <div className="shellSidebarLabel">Үндсэн</div>
              {primaryNavItems.map((item) => (
                <NavLink
                  key={item.to}
                  className={({ isActive }) => `shellSideLink${isActive ? ' shellSideLinkActive' : ''}`}
                  to={item.to}
                  end={item.end}
                  onClick={closeSidebar}
                >
                  {item.label}
                </NavLink>
              ))}
            </div>
            <div className="shellSidebarSection">
              <div className="shellSidebarLabel">Захиалга &amp; Мэдэгдэл</div>
              {workflowNavItems.map((item) => (
                <NavLink
                  key={item.to}
                  className={({ isActive }) => `shellSideLink${isActive ? ' shellSideLinkActive' : ''}`}
                  to={item.to}
                  onClick={closeSidebar}
                >
                  <span className="shellSideLinkLabel">{item.label}</span>
                  {item.to === '/notifications' && unreadBadgeLabel ? (
                    <span className="shellSideBadge" aria-hidden>
                      {unreadBadgeLabel}
                    </span>
                  ) : null}
                </NavLink>
              ))}
            </div>
            {user.role === 'TEACHER' ? (
              <div className="shellSidebarSection">
                <div className="shellSidebarLabel">Багш</div>
                {teacherNavItems.map((item) => (
                  <NavLink
                    key={item.to}
                    className={({ isActive }) => `shellSideLink${isActive ? ' shellSideLinkActive' : ''}`}
                    to={item.to}
                    onClick={closeSidebar}
                  >
                    {item.label}
                  </NavLink>
                ))}
              </div>
            ) : null}
            {user.role === 'STUDENT' ? (
              <div className="shellSidebarSection">
                <div className="shellSidebarLabel">Профайл</div>
                {studentNavItems.map((item) => (
                  <NavLink
                    key={item.to}
                    className={({ isActive }) => `shellSideLink${isActive ? ' shellSideLinkActive' : ''}`}
                    to={item.to}
                    onClick={closeSidebar}
                  >
                    {item.label}
                  </NavLink>
                ))}
              </div>
            ) : null}
            {user.role === 'ADMIN' ? (
              <div className="shellSidebarSection">
                <div className="shellSidebarLabel">Админ</div>
                <NavLink
                  className={({ isActive }) => `shellSideLink${isActive ? ' shellSideLinkActive' : ''}`}
                  to="/account"
                  onClick={closeSidebar}
                >
                  Миний профайл
                </NavLink>
                <NavLink
                  className={({ isActive }) => `shellSideLink${isActive ? ' shellSideLinkActive' : ''}`}
                  to={adminNavItems[0].to}
                  onClick={closeSidebar}
                >
                  {adminNavItems[0].label}
                </NavLink>
              </div>
            ) : null}
          </aside>
        ) : null}

        <div className="shellContentWrapper">
          <main className="shellMain">
            <div className="shellPageFrame">
              {flashMessage ? (
                <div className="success shellFlash">
                  <span>{flashMessage}</span>
                  <button
                    type="button"
                    className="linkButton shellFlashClose"
                    onClick={() =>
                      void navigate(`${location.pathname}${location.search}${location.hash}`, {
                        replace: true,
                        state: null,
                      })
                    }
                  >
                    Хаах
                  </button>
                </div>
              ) : null}
              <Outlet />
            </div>
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
