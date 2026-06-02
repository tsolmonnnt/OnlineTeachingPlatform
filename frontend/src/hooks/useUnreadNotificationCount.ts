import { useCallback, useEffect, useState } from 'react'
import { fetchJson } from '../lib/api'

const REFRESH_EVENT = 'otp-notifications-refresh'

export function notifyNotificationsChanged() {
  window.dispatchEvent(new Event(REFRESH_EVENT))
}

export function useUnreadNotificationCount(enabled: boolean) {
  const [count, setCount] = useState(0)

  const refresh = useCallback(async () => {
    if (!enabled) {
      setCount(0)
      return
    }
    try {
      const result = await fetchJson<{ count: number }>('/api/notifications/me/unread-count', {
        method: 'GET',
      })
      setCount(Math.max(0, result.count))
    } catch {
      // Keep last known count on transient errors.
    }
  }, [enabled])

  useEffect(() => {
    void refresh()
  }, [refresh])

  useEffect(() => {
    if (!enabled) return

    const onFocus = () => void refresh()
    const onRefresh = () => void refresh()
    const intervalId = window.setInterval(() => void refresh(), 60_000)

    window.addEventListener('focus', onFocus)
    window.addEventListener(REFRESH_EVENT, onRefresh)

    return () => {
      window.clearInterval(intervalId)
      window.removeEventListener('focus', onFocus)
      window.removeEventListener(REFRESH_EVENT, onRefresh)
    }
  }, [enabled, refresh])

  return { count, refresh }
}
