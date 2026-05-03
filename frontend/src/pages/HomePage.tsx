import { Link } from 'react-router-dom'
import { useEffect, useMemo, useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import type {
  AdminStats,
  NotificationItem,
  PublicPlatformStats,
  StudentDashboard,
  TeacherDashboard,
} from '../auth/types'
import { ApiError, fetchJson } from '../lib/api'

import VerifiedUserIcon from '@mui/icons-material/VerifiedUser'
import GroupsIcon from '@mui/icons-material/Groups'
import EventAvailableIcon from '@mui/icons-material/EventAvailable'
import PeopleOutlineIcon from '@mui/icons-material/People'
import TodayIcon from '@mui/icons-material/Today'
import UpdateIcon from '@mui/icons-material/Update'
import PendingActionsIcon from '@mui/icons-material/PendingActions'
import StarRateIcon from '@mui/icons-material/StarRate'
import NotificationsActiveIcon from '@mui/icons-material/NotificationsActive'
import PeopleIcon from '@mui/icons-material/People'
import EventNoteIcon from '@mui/icons-material/EventNote'
import ArrowForwardIcon from '@mui/icons-material/ArrowForward'
import CircleIcon from '@mui/icons-material/Circle'

function formatSlot(iso: string) {
  try {
    return new Date(iso).toLocaleString('mn-MN', { dateStyle: 'short', timeStyle: 'short' })
  } catch {
    return iso
  }
}

export default function HomePage() {
  const { user, token } = useAuth()
  const [publicStats, setPublicStats] = useState<PublicPlatformStats | null>(null)
  const [teacherDash, setTeacherDash] = useState<TeacherDashboard | null>(null)
  const [studentDash, setStudentDash] = useState<StudentDashboard | null>(null)
  const [adminStats, setAdminStats] = useState<AdminStats | null>(null)
  const [notifications, setNotifications] = useState<NotificationItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const todayLabel = useMemo(
    () =>
      new Date().toLocaleDateString('mn-MN', {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      }),
    [],
  )

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)

    async function load() {
      try {
        if (!user) {
          const s = await fetchJson<PublicPlatformStats>('/api/public/stats', { method: 'GET' })
          if (!cancelled) setPublicStats(s)
          return
        }

        if (user.role === 'TEACHER') {
          const [d, n] = await Promise.all([
            fetchJson<TeacherDashboard>('/api/dashboard/teacher', { method: 'GET' }),
            fetchJson<NotificationItem[]>('/api/notifications/me', { method: 'GET' }),
          ])
          if (!cancelled) {
            setTeacherDash(d)
            setNotifications(n.slice(0, 5))
          }
          return
        }

        if (user.role === 'STUDENT') {
          const [d, n] = await Promise.all([
            fetchJson<StudentDashboard>('/api/dashboard/student', { method: 'GET' }),
            fetchJson<NotificationItem[]>('/api/notifications/me', { method: 'GET' }),
          ])
          if (!cancelled) {
            setStudentDash(d)
            setNotifications(n.slice(0, 5))
          }
          return
        }

        if (user.role === 'ADMIN') {
          const [s, n] = await Promise.all([
            fetchJson<AdminStats>('/api/admin/stats', { method: 'GET' }),
            fetchJson<NotificationItem[]>('/api/notifications/me', { method: 'GET' }),
          ])
          if (!cancelled) {
            setAdminStats(s)
            setNotifications(n.slice(0, 5))
          }
        }
      } catch (e) {
        if (!cancelled) {
          const msg = e instanceof ApiError ? e.message : 'Өгөгдөл ачаалахад алдаа гарлаа.'
          setError(msg)
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    void load()
    return () => {
      cancelled = true
    }
  }, [user, token])

  const recentBookings =
    user?.role === 'TEACHER' ? teacherDash?.recentBookings : user?.role === 'STUDENT' ? studentDash?.recentBookings : []

  return (
    <div className="homePage pageWide">
      <section className="homeHero">
        <p className="homeDate">{todayLabel}</p>
        <h1 className="homeTitle">Тавтай морилно уу</h1>
        <p className="homeLead">
          Онлайн платформоор баталгаажсан багш нартай холбогдож, сул цаг сонгож, хичээл захиалж, төлөвөө нэг дороос хянаарай.
        </p>
        {user && (
          <p className="homeUserDesc small">
            {user.fullName} — {user.email} ({user.role})
          </p>
        )}
      </section>

      {error ? <div className="error homeError">{error}</div> : null}

      {loading ? (
        <p className="muted homeLoading">Ачаалж байна…</p>
      ) : (
        <>
          {!user && publicStats ? (
            <section className="homeSection">
              <h2 className="homeSectionTitle">Платформын тоо баримт</h2>
              <div className="dashboardGrid">
                <div className="statCard">
                  <div className="statCardIcon _teal"><VerifiedUserIcon /></div>
                  <div className="statCardDetails">
                    <div className="statCardValue">{publicStats.verifiedTeacherCount}</div>
                    <div className="statCardLabel">Баталгаажсан багш</div>
                  </div>
                </div>
                <div className="statCard">
                  <div className="statCardIcon _indigo"><GroupsIcon /></div>
                  <div className="statCardDetails">
                    <div className="statCardValue">{publicStats.totalTeachers}</div>
                    <div className="statCardLabel">Нийт багш</div>
                  </div>
                </div>
                <div className="statCard">
                  <div className="statCardIcon _amber"><EventAvailableIcon /></div>
                  <div className="statCardDetails">
                    <div className="statCardValue">{publicStats.totalBookings}</div>
                    <div className="statCardLabel">Нийт захиалга</div>
                  </div>
                </div>
                <div className="statCard">
                  <div className="statCardIcon _blue"><PeopleOutlineIcon /></div>
                  <div className="statCardDetails">
                    <div className="statCardValue">{publicStats.studentCount}</div>
                    <div className="statCardLabel">Сурагчид</div>
                  </div>
                </div>
              </div>
            </section>
          ) : null}

          {user?.role === 'TEACHER' && teacherDash ? (
            <section className="homeSection">
              <h2 className="homeSectionTitle">Таны хураангуй</h2>
              <p className="muted small homeTzNote">
                Өдрийн тоо нь серверийн цагийн бүсээр тооцогдоно.
              </p>
              <div className="dashboardGrid">
                <div className="statCard">
                  <div className="statCardIcon _teal"><TodayIcon /></div>
                  <div className="statCardDetails">
                    <div className="statCardValue">{teacherDash.confirmedLessonsToday}</div>
                    <div className="statCardLabel">Өнөөдрийн хичээл</div>
                  </div>
                </div>
                <div className="statCard">
                  <div className="statCardIcon _indigo"><UpdateIcon /></div>
                  <div className="statCardDetails">
                    <div className="statCardValue">{teacherDash.confirmedLessonsTomorrow}</div>
                    <div className="statCardLabel">Маргаашийн хичээл</div>
                  </div>
                </div>
                <div className="statCard">
                  <div className="statCardIcon _amber"><PendingActionsIcon /></div>
                  <div className="statCardDetails">
                    <div className="statCardValue">{teacherDash.pendingBookingsAsTeacher}</div>
                    <div className="statCardLabel">Хүлээгдэж буй захиалга</div>
                  </div>
                </div>
                <div className="statCard">
                  <div className="statCardIcon _blue"><StarRateIcon /></div>
                  <div className="statCardDetails">
                    <div className="statCardValue">
                      {teacherDash.averageRating != null ? teacherDash.averageRating.toFixed(1) : '—'}
                    </div>
                    <div className="statCardLabel">Дундаж үнэлгээ ({teacherDash.reviewCount})</div>
                  </div>
                </div>
                <div className="statCard">
                  <div className="statCardIcon _rose"><NotificationsActiveIcon /></div>
                  <div className="statCardDetails">
                    <div className="statCardValue">{teacherDash.unreadNotifications}</div>
                    <div className="statCardLabel">Уншаагүй мэдэгдэл</div>
                  </div>
                </div>
                <div className="statCard">
                  <div className="statCardIcon _teal"><PeopleIcon /></div>
                  <div className="statCardDetails">
                    <div className="statCardValue">{teacherDash.uniqueStudentsConfirmed}</div>
                    <div className="statCardLabel">Баталгаажсан сурагч</div>
                  </div>
                </div>
              </div>
            </section>
          ) : null}

          {user?.role === 'STUDENT' && studentDash ? (
            <section className="homeSection">
              <h2 className="homeSectionTitle">Таны хураангуй</h2>
              <p className="muted small homeTzNote">Удах хичээл: ирээдүйн 21 хоногт баталгаажсан захиалга.</p>
              <div className="dashboardGrid dashboardGridNarrow">
                <div className="statCard">
                  <div className="statCardIcon _teal"><EventNoteIcon /></div>
                  <div className="statCardDetails">
                    <div className="statCardValue">{studentDash.upcomingConfirmedLessons}</div>
                    <div className="statCardLabel">Удах баталгаажсан хичээл</div>
                  </div>
                </div>
                <div className="statCard">
                  <div className="statCardIcon _amber"><PendingActionsIcon /></div>
                  <div className="statCardDetails">
                    <div className="statCardValue">{studentDash.pendingBookingsAsStudent}</div>
                    <div className="statCardLabel">Хүлээгдэж буй захиалга</div>
                  </div>
                </div>
                <div className="statCard">
                  <div className="statCardIcon _rose"><NotificationsActiveIcon /></div>
                  <div className="statCardDetails">
                    <div className="statCardValue">{studentDash.unreadNotifications}</div>
                    <div className="statCardLabel">Уншаагүй мэдэгдэл</div>
                  </div>
                </div>
              </div>
            </section>
          ) : null}

          {user?.role === 'ADMIN' && adminStats ? (
            <section className="homeSection">
              <h2 className="homeSectionTitle">Системийн хураангуй</h2>
              <div className="dashboardGrid">
                <div className="statCard">
                  <div className="statCardIcon _indigo"><GroupsIcon /></div>
                  <div className="statCardDetails">
                    <div className="statCardValue">{adminStats.totalUsers}</div>
                    <div className="statCardLabel">Нийт хэрэглэгч</div>
                  </div>
                </div>
                <div className="statCard">
                  <div className="statCardIcon _teal"><VerifiedUserIcon /></div>
                  <div className="statCardDetails">
                    <div className="statCardValue">{adminStats.verifiedTeacherCount}</div>
                    <div className="statCardLabel">Баталгаажсан багш</div>
                  </div>
                </div>
                <div className="statCard">
                  <div className="statCardIcon _amber"><EventAvailableIcon /></div>
                  <div className="statCardDetails">
                    <div className="statCardValue">{adminStats.totalBookings}</div>
                    <div className="statCardLabel">Нийт захиалга</div>
                  </div>
                </div>
                <Link className="statCard statCardLink" to="/admin">
                  <div className="statCardLinkTitle">Админ самбар <ArrowForwardIcon fontSize="small"/></div>
                  <div className="statCardLabel">Дэлгэрэнгүй үзэх</div>
                </Link>
              </div>
            </section>
          ) : null}

          {user && recentBookings && recentBookings.length > 0 ? (
            <section className="homeSection">
              <h2 className="homeSectionTitle">Сүүлийн захиалгууд</h2>
              <div className="card homeTableWrap">
                <table className="homeTable">
                  <thead>
                    <tr>
                      <th>Төлөв</th>
                      <th>{user.role === 'TEACHER' ? 'Сурагч' : 'Багш'}</th>
                      <th>Цаг</th>
                      <th>Төрөл</th>
                    </tr>
                  </thead>
                  <tbody>
                    {recentBookings.map((b) => {
                      let badgeClass = 'pending';
                      if (b.status === 'CONFIRMED') badgeClass = 'confirmed';
                      if (b.status === 'CANCELLED') badgeClass = 'cancelled';
                      
                      return (
                        <tr key={b.id}>
                          <td><span className={`statusBadge ${badgeClass}`}>{b.status}</span></td>
                          <td>{user.role === 'TEACHER' ? b.studentName : b.teacherName}</td>
                          <td>{formatSlot(b.slotStartTime)}</td>
                          <td>{b.courseSubjectName ?? b.subject}</td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
              <p className="muted small" style={{ marginTop: '12px' }}>
                <Link to="/bookings" style={{ color: 'var(--shell-accent)', textDecoration: 'none', fontWeight: 600 }}>Бүх захиалгыг харах →</Link>
              </p>
            </section>
          ) : null}

          {user && notifications.length > 0 ? (
            <section className="homeSection">
              <h2 className="homeSectionTitle">Сүүлийн мэдэгдлүүд</h2>
              <ul className="homeNotifList card">
                {notifications.map((n) => (
                  <li key={n.id} className={`homeNotifItem${n.isRead ? '' : ' homeNotifUnread'}`}>
                    <div className="homeNotifIcon">
                      <CircleIcon sx={{ fontSize: n.isRead ? 12 : 16, color: n.isRead ? '#cbd5e1' : '#14b8a6' }} />
                    </div>
                    <div className="homeNotifContent">
                      <div className="homeNotifTitle">{n.title}</div>
                      <div className="homeNotifMessage">{n.message}</div>
                      <div className="homeNotifTime small">
                        {new Date(n.createdAt).toLocaleString('mn-MN', { dateStyle: 'short', timeStyle: 'short' })}
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
              <p className="muted small" style={{ marginTop: '12px' }}>
                <Link to="/notifications" style={{ color: 'var(--shell-accent)', textDecoration: 'none', fontWeight: 600 }}>Бүх мэдэгдэл →</Link>
              </p>
            </section>
          ) : null}
        </>
      )}
    </div>
  )
}
