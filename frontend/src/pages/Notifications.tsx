import { useEffect, useMemo, useState } from 'react'
import { fetchJson } from '../lib/api'
import { AlertBanner } from '../components/AlertBanner'
import { EmptyState } from '../components/EmptyState'
import { PageHeader } from '../components/PageHeader'
import { SectionCard } from '../components/SectionCard'
import { StatusPill } from '../components/StatusPill'
import type { NotificationItem } from '../auth/types'
import { getFriendlyErrorMessage } from '../lib/errorMessages'

export default function NotificationsPage() {
  const [items, setItems] = useState<NotificationItem[]>([])
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  async function load() {
    setError(null)
    setIsLoading(true)
    try {
      const result = await fetchJson<NotificationItem[]>('/api/notifications/me', { method: 'GET' })
      setItems(result)
    } catch (err) {
      setError(getFriendlyErrorMessage(err, 'Мэдэгдлүүдийг ачаалж чадсангүй.'))
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  async function markRead(id: number) {
    try {
      const updated = await fetchJson<NotificationItem>(`/api/notifications/${id}/read`, { method: 'PATCH' })
      setItems((prev) => prev.map((n) => (n.id === id ? updated : n)))
    } catch (err) {
      setError(getFriendlyErrorMessage(err, 'Мэдэгдлийг шинэчлэх үед алдаа гарлаа.'))
    }
  }

  const groupedNotifications = useMemo(
    () => ({
      unread: items.filter((item) => !item.isRead),
      read: items.filter((item) => item.isRead),
    }),
    [items],
  )

  return (
    <div className="page pageWide pageStack">
      <PageHeader
        eyebrow="Мэдэгдлүүд"
        title="Системийн шинэчлэлт ба мэдэгдлүүд"
        subtitle="Шинэ захиалга, төлөвийн өөрчлөлт болон бусад чухал мэдээллүүдийг эндээс хянаарай."
      />

      {error ? <AlertBanner variant="error">{error}</AlertBanner> : null}

      {isLoading ? (
        <p className="muted">Ачаалж байна…</p>
      ) : !items.length ? (
        <EmptyState
          title="Мэдэгдэл алга"
          description="Шинэ захиалга, баталгаажуулалт эсвэл системийн шинэ мэдээлэл ирэхэд энэ хэсэгт харагдана."
        />
      ) : (
        <div className="workflowStack">
          <SectionCard title="Шинэ, уншаагүй" subtitle={`${groupedNotifications.unread.length} мэдэгдэл`}>
            {groupedNotifications.unread.length ? (
              <div className="notificationList">
                {groupedNotifications.unread.map((notification) => (
                  <article key={notification.id} className="notificationCard notificationCard-unread">
                    <div className="notificationCardHeader">
                      <div>
                        <div className="notificationCardTitle">{notification.title}</div>
                        <div className="muted small">
                          {new Date(notification.createdAt).toLocaleString('mn-MN', { dateStyle: 'medium', timeStyle: 'short' })}
                        </div>
                      </div>
                      <StatusPill label="Шинэ" tone="info" />
                    </div>
                    <p className="notificationCardMessage">{notification.message}</p>
                    <div className="notificationCardActions">
                      <button type="button" onClick={() => void markRead(notification.id)}>
                        Уншсан болгох
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            ) : (
              <EmptyState title="Уншаагүй мэдэгдэл алга" description="Одоогоор шинэ анхаарах зүйлгүй байна." />
            )}
          </SectionCard>

          <SectionCard title="Өмнөх мэдэгдлүүд" subtitle={`${groupedNotifications.read.length} мэдэгдэл`}>
            {groupedNotifications.read.length ? (
              <div className="notificationList">
                {groupedNotifications.read.map((notification) => (
                  <article key={notification.id} className="notificationCard">
                    <div className="notificationCardHeader">
                      <div>
                        <div className="notificationCardTitle">{notification.title}</div>
                        <div className="muted small">
                          {new Date(notification.createdAt).toLocaleString('mn-MN', { dateStyle: 'medium', timeStyle: 'short' })}
                        </div>
                      </div>
                      <StatusPill label="Уншсан" tone="neutral" />
                    </div>
                    <p className="notificationCardMessage">{notification.message}</p>
                  </article>
                ))}
              </div>
            ) : (
              <EmptyState title="Өмнөх мэдэгдэл алга" description="Уншсан мэдэгдлүүд энд хадгалагдана." />
            )}
          </SectionCard>
        </div>
      )}
    </div>
  )
}

