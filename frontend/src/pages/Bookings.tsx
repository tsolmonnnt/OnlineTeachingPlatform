import { useEffect, useMemo, useState } from 'react'
import { fetchJson } from '../lib/api'
import { useAuth } from '../auth/AuthContext'
import { AlertBanner } from '../components/AlertBanner'
import { EmptyState } from '../components/EmptyState'
import { InfoList } from '../components/InfoList'
import { PageHeader } from '../components/PageHeader'
import { SectionCard } from '../components/SectionCard'
import { StatusPill } from '../components/StatusPill'
import type { Booking, BookingType } from '../auth/types'
import { getFriendlyErrorMessage } from '../lib/errorMessages'

function formatDate(value: string) {
  return new Date(value).toLocaleString('mn-MN')
}

function getStatusMeta(status: Booking['status']) {
  if (status === 'IN_PROGRESS') return { label: 'Явагдаж байна', tone: 'info' as const }
  if (status === 'COMPLETED') return { label: 'Дууссан', tone: 'success' as const }
  if (status === 'REVIEWED') return { label: 'Үнэлгээ өгсөн', tone: 'info' as const }
  if (status === 'CONFIRMED') return { label: 'Баталгаажсан', tone: 'success' as const }
  if (status === 'CANCELLED') return { label: 'Цуцлагдсан', tone: 'danger' as const }
  return { label: 'Хүлээгдэж буй', tone: 'warning' as const }
}

function getBookingTypeLabel(type: BookingType) {
  if (type === 'PACKAGE_LESSON') return 'Багц хичээл'
  return 'Нэг хичээл'
}

function extraBookingInfoItems(booking: Booking) {
  const items: { label: string; value: string }[] = [
    { label: 'Төрөл', value: getBookingTypeLabel(booking.bookingType) },
  ]
  if (booking.bookingType === 'PACKAGE_LESSON' && booking.packageTotalLessons != null) {
    const total = booking.packageTotalLessons
    items.push({
      label: 'Багц явц',
      value: `${booking.packageCompletedLessons ?? 0}/${total} хичээл`,
    })
    if (booking.packageBookedLessons != null) {
      items.push({
        label: 'Үлдсэн эрх',
        value: `${Math.max(0, total - booking.packageBookedLessons)}/${total}`,
      })
    }
  }
  if (booking.parentBookingId != null) {
    items.push({ label: 'Багц захиалга', value: `#${booking.parentBookingId}` })
  }
  return items
}

function getStatusDescription(status: Booking['status']) {
  if (status === 'IN_PROGRESS') return 'Хичээл одоо явагдаж байна.'
  if (status === 'COMPLETED') return 'Хичээл дууссан. Хугацаа дуусаагүй бол үнэлгээ өгч болно.'
  if (status === 'REVIEWED') return 'Энэ хичээлд үнэлгээ өгсөн.'
  if (status === 'CONFIRMED') return 'Цаг батлагдсан тул хичээлдээ бэлдэж эхэлнэ үү.'
  if (status === 'CANCELLED') return 'Энэ захиалга цуцлагдсан тул дахин ашиглах боломжгүй.'
  return 'Тухайн багшийн баталгаажуулалтыг хүлээж байна.'
}

export default function BookingsPage() {
  const { user } = useAuth()
  const [bookings, setBookings] = useState<Booking[]>([])
  const [meetingLinks, setMeetingLinks] = useState<Record<number, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  async function loadBookings() {
    setIsLoading(true)
    setError(null)
    try {
      const result = await fetchJson<Booking[]>('/api/bookings/me', { method: 'GET' })
      setBookings(result)
    } catch (err) {
      setError(getFriendlyErrorMessage(err, 'Захиалгуудыг ачаалж чадсангүй.'))
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    void loadBookings()
  }, [])

  async function updateStatus(bookingId: number, action: 'confirm' | 'cancel') {
    setError(null)
    setSuccess(null)
    try {
      const updated = await fetchJson<Booking>(`/api/bookings/${bookingId}/${action}`, { method: 'PATCH' })
      setBookings((prev) => prev.map((b) => (b.id === updated.id ? updated : b)))
      setSuccess(action === 'confirm' ? 'Захиалга амжилттай баталгаажлаа.' : 'Захиалга амжилттай цуцлагдлаа.')
    } catch (err) {
      setError(getFriendlyErrorMessage(err, 'Захиалгын төлөв шинэчлэх үед алдаа гарлаа.'))
    }
  }

  async function updateMeetingLink(bookingId: number) {
    const raw = (meetingLinks[bookingId] ?? '').trim()
    if (!raw) {
      setError('Meeting link хоосон байна')
      return
    }
    setError(null)
    setSuccess(null)
    try {
      const updated = await fetchJson<Booking>(`/api/bookings/${bookingId}/meeting-link`, {
        method: 'PATCH',
        body: JSON.stringify({ meetingLink: raw }),
      })
      setBookings((prev) => prev.map((b) => (b.id === updated.id ? updated : b)))
      setSuccess('Meeting link амжилттай хадгалагдлаа.')
    } catch (err) {
      setError(getFriendlyErrorMessage(err, 'Meeting link хадгалах үед алдаа гарлаа.'))
    }
  }

  function canJoin(booking: Booking) {
    return (booking.status === 'CONFIRMED' || booking.status === 'IN_PROGRESS') && !!booking.meetingLink
  }

  const groupedBookings = useMemo(
    () => ({
      pending: bookings.filter((booking) => booking.status === 'PENDING'),
      confirmed: bookings.filter((booking) => booking.status === 'CONFIRMED'),
      inProgress: bookings.filter((booking) => booking.status === 'IN_PROGRESS'),
      completed: bookings.filter((booking) => booking.status === 'COMPLETED'),
      reviewed: bookings.filter((booking) => booking.status === 'REVIEWED'),
      cancelled: bookings.filter((booking) => booking.status === 'CANCELLED'),
    }),
    [bookings],
  )

  if (isLoading) {
    return (
      <div className="page pageWide pageStack">
        <PageHeader
          eyebrow="Захиалгууд"
          title="Миний хичээлийн захиалгууд"
          subtitle="Баталгаажуулалт, цагийн мэдээлэл болон дараагийн үйлдлүүдийг нэг дороос хянаарай."
        />
        <p className="muted">Ачаалж байна…</p>
      </div>
    )
  }

  return (
    <div className="page pageWide pageStack">
      <PageHeader
        eyebrow="Захиалгууд"
        title="Миний хичээлийн захиалгууд"
        subtitle="Төлөв, цагийн мэдээлэл болон дараагийн үйлдлүүдийг нэг дэлгэцээс хянаарай."
      />

      {error ? <AlertBanner variant="error">{error}</AlertBanner> : null}
      {success ? <AlertBanner variant="success">{success}</AlertBanner> : null}

      {!bookings.length ? (
        <EmptyState
          title="Захиалга хараахан алга"
          description="Багшийн профайл руу орж тохирох сул цаг сонгон анхны захиалгаа үүсгэнэ үү."
        />
      ) : (
        <div className="workflowStack">
          <SectionCard title="Хүлээгдэж буй" subtitle={`${groupedBookings.pending.length} захиалга`}>
            {groupedBookings.pending.length ? (
              <div className="workflowCardGrid">
                {groupedBookings.pending.map((booking) => {
                  const status = getStatusMeta(booking.status)
                  return (
                    <div key={booking.id} className="bookingCard">
                      <div className="bookingCardHeader">
                        <div>
                          <div className="bookingCardTitle">{booking.courseSubjectName ?? booking.subject}</div>
                          <p className="muted small">#{booking.id}</p>
                        </div>
                        <StatusPill label={status.label} tone={status.tone} />
                      </div>

                      <p className="bookingCardNote">{getStatusDescription(booking.status)}</p>

                      <InfoList
                        items={[
                          ...extraBookingInfoItems(booking),
                          { label: user?.role === 'TEACHER' ? 'Сурагч' : 'Багш', value: user?.role === 'TEACHER' ? booking.studentName : booking.teacherName },
                          { label: 'Цаг', value: `${formatDate(booking.slotStartTime)} – ${formatDate(booking.slotEndTime)}` },
                          { label: 'Тэмдэглэл', value: booking.note?.trim() ? booking.note : 'Оруулаагүй' },
                        ]}
                      />

                      <div className="bookingCardActions">
                        {user?.role === 'TEACHER' ? (
                          <button type="button" onClick={() => void updateStatus(booking.id, 'confirm')}>
                            Батлах
                          </button>
                        ) : null}
                        <button type="button" className="btnGhost" onClick={() => void updateStatus(booking.id, 'cancel')}>
                          Цуцлах
                        </button>
                      </div>
                    </div>
                  )
                })}
              </div>
            ) : (
              <EmptyState title="Хүлээгдэж буй захиалга алга" description="Одоогоор баталгаажуулалт хүлээж буй шинэ захиалга үүсээгүй байна." />
            )}
          </SectionCard>

          <SectionCard title="Баталгаажсан" subtitle={`${groupedBookings.confirmed.length} захиалга`}>
            {groupedBookings.confirmed.length ? (
              <div className="workflowCardGrid">
                {groupedBookings.confirmed.map((booking) => {
                  const status = getStatusMeta(booking.status)
                  return (
                    <div key={booking.id} className="bookingCard">
                      <div className="bookingCardHeader">
                        <div>
                          <div className="bookingCardTitle">{booking.courseSubjectName ?? booking.subject}</div>
                          <p className="muted small">#{booking.id}</p>
                        </div>
                        <StatusPill label={status.label} tone={status.tone} />
                      </div>

                      <InfoList
                        items={[
                          ...extraBookingInfoItems(booking),
                          { label: user?.role === 'TEACHER' ? 'Сурагч' : 'Багш', value: user?.role === 'TEACHER' ? booking.studentName : booking.teacherName },
                          { label: 'Цаг', value: `${formatDate(booking.slotStartTime)} – ${formatDate(booking.slotEndTime)}` },
                          { label: 'Хичээл', value: booking.subject },
                        ]}
                      />

                      <div className="bookingCardActions">
                        {canJoin(booking) ? (
                          <a className="buttonLink" href={booking.meetingLink ?? '#'} target="_blank" rel="noopener noreferrer">
                            Хичээлд орох
                          </a>
                        ) : null}
                        {user?.role === 'TEACHER' ? (
                          <>
                            <input
                              value={meetingLinks[booking.id] ?? booking.meetingLink ?? ''}
                              onChange={(e) => setMeetingLinks((prev) => ({ ...prev, [booking.id]: e.target.value }))}
                              placeholder="Zoom / Meet link"
                            />
                            <button type="button" className="btnGhost" onClick={() => void updateMeetingLink(booking.id)}>
                              Link хадгалах
                            </button>
                          </>
                        ) : null}
                        <button type="button" className="btnGhost" onClick={() => void updateStatus(booking.id, 'cancel')}>
                          Цуцлах
                        </button>
                      </div>
                    </div>
                  )
                })}
              </div>
            ) : (
              <EmptyState title="Баталгаажсан захиалга алга" description="Цаг баталгаажсан даруй энэ хэсэгт харагдана." />
            )}
          </SectionCard>

          <SectionCard title="Явагдаж буй хичээл" subtitle={`${groupedBookings.inProgress.length} захиалга`}>
            {groupedBookings.inProgress.length ? (
              <div className="workflowCardGrid">
                {groupedBookings.inProgress.map((booking) => {
                  const status = getStatusMeta(booking.status)
                  return (
                    <div key={booking.id} className="bookingCard">
                      <div className="bookingCardHeader">
                        <div>
                          <div className="bookingCardTitle">{booking.courseSubjectName ?? booking.subject}</div>
                          <p className="muted small">#{booking.id}</p>
                        </div>
                        <StatusPill label={status.label} tone={status.tone} />
                      </div>
                      <InfoList
                        items={[
                          ...extraBookingInfoItems(booking),
                          { label: user?.role === 'TEACHER' ? 'Сурагч' : 'Багш', value: user?.role === 'TEACHER' ? booking.studentName : booking.teacherName },
                          { label: 'Цаг', value: `${formatDate(booking.slotStartTime)} – ${formatDate(booking.slotEndTime)}` },
                        ]}
                      />
                      {canJoin(booking) ? (
                        <div className="bookingCardActions">
                          <a className="buttonLink" href={booking.meetingLink ?? '#'} target="_blank" rel="noopener noreferrer">
                            Хичээлд орох
                          </a>
                        </div>
                      ) : null}
                    </div>
                  )
                })}
              </div>
            ) : (
              <EmptyState title="Явагдаж буй хичээл алга" description="Эхлэх цаг болсон хичээлүүд энд харагдана." />
            )}
          </SectionCard>

          <SectionCard title="Дууссан хичээл" subtitle={`${groupedBookings.completed.length} захиалга`}>
            {groupedBookings.completed.length ? (
              <div className="workflowCardGrid">
                {groupedBookings.completed.map((booking) => {
                  const status = getStatusMeta(booking.status)
                  return (
                    <div key={booking.id} className="bookingCard">
                      <div className="bookingCardHeader">
                        <div>
                          <div className="bookingCardTitle">{booking.courseSubjectName ?? booking.subject}</div>
                          <p className="muted small">#{booking.id}</p>
                        </div>
                        <StatusPill label={status.label} tone={status.tone} />
                      </div>
                      <InfoList
                        items={[
                          ...extraBookingInfoItems(booking),
                          { label: user?.role === 'TEACHER' ? 'Сурагч' : 'Багш', value: user?.role === 'TEACHER' ? booking.studentName : booking.teacherName },
                          { label: 'Цаг', value: `${formatDate(booking.slotStartTime)} – ${formatDate(booking.slotEndTime)}` },
                          { label: 'Үнэлгээ өгөх боломж', value: booking.canReview ? 'Тийм' : 'Үгүй' },
                        ]}
                      />
                    </div>
                  )
                })}
              </div>
            ) : (
              <EmptyState title="Дууссан хичээл алга" description="Дууссан хичээлүүд энд харагдана." />
            )}
          </SectionCard>

          <SectionCard title="Үнэлгээ өгсөн хичээл" subtitle={`${groupedBookings.reviewed.length} захиалга`}>
            {groupedBookings.reviewed.length ? (
              <div className="workflowCardGrid">
                {groupedBookings.reviewed.map((booking) => {
                  const status = getStatusMeta(booking.status)
                  return (
                    <div key={booking.id} className="bookingCard bookingCard-muted">
                      <div className="bookingCardHeader">
                        <div>
                          <div className="bookingCardTitle">{booking.courseSubjectName ?? booking.subject}</div>
                          <p className="muted small">#{booking.id}</p>
                        </div>
                        <StatusPill label={status.label} tone={status.tone} />
                      </div>
                      <InfoList
                        items={[
                          ...extraBookingInfoItems(booking),
                          { label: user?.role === 'TEACHER' ? 'Сурагч' : 'Багш', value: user?.role === 'TEACHER' ? booking.studentName : booking.teacherName },
                          { label: 'Цаг', value: `${formatDate(booking.slotStartTime)} – ${formatDate(booking.slotEndTime)}` },
                        ]}
                      />
                    </div>
                  )
                })}
              </div>
            ) : (
              <EmptyState title="Үнэлгээ өгсөн хичээл алга" description="Үнэлгээ өгсөн захиалгууд энд харагдана." />
            )}
          </SectionCard>

          <SectionCard title="Цуцлагдсан" subtitle={`${groupedBookings.cancelled.length} захиалга`}>
            {groupedBookings.cancelled.length ? (
              <div className="workflowCardGrid">
                {groupedBookings.cancelled.map((booking) => {
                  const status = getStatusMeta(booking.status)
                  return (
                    <div key={booking.id} className="bookingCard bookingCard-muted">
                      <div className="bookingCardHeader">
                        <div>
                          <div className="bookingCardTitle">{booking.courseSubjectName ?? booking.subject}</div>
                          <p className="muted small">#{booking.id}</p>
                        </div>
                        <StatusPill label={status.label} tone={status.tone} />
                      </div>
                      <InfoList
                        items={[
                          ...extraBookingInfoItems(booking),
                          { label: user?.role === 'TEACHER' ? 'Сурагч' : 'Багш', value: user?.role === 'TEACHER' ? booking.studentName : booking.teacherName },
                          { label: 'Цаг', value: `${formatDate(booking.slotStartTime)} – ${formatDate(booking.slotEndTime)}` },
                        ]}
                      />
                    </div>
                  )
                })}
              </div>
            ) : (
              <EmptyState title="Цуцлагдсан захиалга алга" description="Цуцлагдсан захиалгууд энд ангилагдан харагдана." />
            )}
          </SectionCard>
        </div>
      )}
    </div>
  )
}

