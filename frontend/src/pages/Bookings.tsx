import { useEffect, useMemo, useState } from 'react'
import { fetchJson } from '../lib/api'
import { useAuth } from '../auth/AuthContext'
import { AlertBanner } from '../components/AlertBanner'
import { EmptyState } from '../components/EmptyState'
import { InfoList } from '../components/InfoList'
import { PageHeader } from '../components/PageHeader'
import { SectionCard } from '../components/SectionCard'
import { StatusPill } from '../components/StatusPill'
import type { Booking } from '../auth/types'
import { getFriendlyErrorMessage } from '../lib/errorMessages'

function formatDate(value: string) {
  return new Date(value).toLocaleString('mn-MN')
}

function getStatusMeta(status: Booking['status']) {
  if (status === 'CONFIRMED') return { label: 'Баталгаажсан', tone: 'success' as const }
  if (status === 'CANCELLED') return { label: 'Цуцлагдсан', tone: 'danger' as const }
  return { label: 'Хүлээгдэж буй', tone: 'warning' as const }
}

function getStatusDescription(status: Booking['status']) {
  if (status === 'CONFIRMED') return 'Цаг батлагдсан тул хичээлдээ бэлдэж эхэлнэ үү.'
  if (status === 'CANCELLED') return 'Энэ захиалга цуцлагдсан тул дахин ашиглах боломжгүй.'
  return 'Тухайн багшийн баталгаажуулалтыг хүлээж байна.'
}

export default function BookingsPage() {
  const { user } = useAuth()
  const [bookings, setBookings] = useState<Booking[]>([])
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

  const groupedBookings = useMemo(
    () => ({
      pending: bookings.filter((booking) => booking.status === 'PENDING'),
      confirmed: bookings.filter((booking) => booking.status === 'CONFIRMED'),
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
                          { label: user?.role === 'TEACHER' ? 'Сурагч' : 'Багш', value: user?.role === 'TEACHER' ? booking.studentName : booking.teacherName },
                          { label: 'Цаг', value: `${formatDate(booking.slotStartTime)} – ${formatDate(booking.slotEndTime)}` },
                          { label: 'Хичээл', value: booking.subject },
                        ]}
                      />

                      <div className="bookingCardActions">
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

