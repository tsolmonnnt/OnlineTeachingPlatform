import { useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import type { User } from '../auth/types'
import { AlertBanner } from '../components/AlertBanner'
import { PageHeader } from '../components/PageHeader'
import { SectionCard } from '../components/SectionCard'
import { fetchJson, postFormData } from '../lib/api'
import { getFriendlyErrorMessage } from '../lib/errorMessages'

function userInitials(fullName: string) {
  const parts = fullName.trim().split(/\s+/).filter(Boolean)
  if (parts.length === 0) return '?'
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase()
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
}

export default function AccountProfilePage() {
  const { user, refreshMe } = useAuth()
  const [fullName, setFullName] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [isSaving, setIsSaving] = useState(false)

  const [avatarPick, setAvatarPick] = useState<File | null>(null)
  const [avatarPreviewUrl, setAvatarPreviewUrl] = useState<string | null>(null)
  const [isUploadingAvatar, setIsUploadingAvatar] = useState(false)
  const [avatarUploadMsg, setAvatarUploadMsg] = useState<string | null>(null)

  useEffect(() => {
    setFullName(user?.fullName ?? '')
  }, [user?.fullName])

  useEffect(() => {
    if (!avatarPick) {
      setAvatarPreviewUrl(null)
      return
    }
    const url = URL.createObjectURL(avatarPick)
    setAvatarPreviewUrl(url)
    return () => URL.revokeObjectURL(url)
  }, [avatarPick])

  const shownAvatarSrc = avatarPreviewUrl || user?.avatarUrl || ''

  async function onSave(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSuccess(null)
    const trimmed = fullName.trim()
    if (trimmed.length < 2) {
      setError('Овог нэр хамгийн багадаа 2 тэмдэгт байх ёстой.')
      return
    }
    setIsSaving(true)
    try {
      await fetchJson<User>('/api/auth/me', {
        method: 'PATCH',
        body: JSON.stringify({ fullName: trimmed }),
      })
      await refreshMe()
      setSuccess('Профайл амжилттай хадгалагдлаа.')
    } catch (err) {
      setError(getFriendlyErrorMessage(err, 'Профайлыг хадгалах үед алдаа гарлаа.'))
    } finally {
      setIsSaving(false)
    }
  }

  async function onUploadAvatar() {
    if (!avatarPick) {
      setError('Эхлээд зураг сонгоно уу')
      return
    }
    setError(null)
    setSuccess(null)
    setAvatarUploadMsg(null)
    setIsUploadingAvatar(true)
    try {
      const fd = new FormData()
      fd.append('file', avatarPick)
      await postFormData<User>('/api/auth/me/avatar', fd)
      await refreshMe()
      setAvatarPick(null)
      setAvatarUploadMsg('Зураг амжилттай ачаалагдлаа.')
    } catch (err) {
      setError(getFriendlyErrorMessage(err, 'Зураг ачаалах үед алдаа гарлаа.'))
    } finally {
      setIsUploadingAvatar(false)
    }
  }

  return (
    <div className="page pageWide pageStack">
      <PageHeader
        eyebrow="Миний профайл"
        title="Хувийн мэдээлэл"
        subtitle="Овог нэр, профайл зураг эндээс шинэчилнэ. И-мэйл хаягийг өөрчлөх боломжгүй."
      />

      {success ? <AlertBanner variant="success">{success}</AlertBanner> : null}
      {error ? <AlertBanner variant="error">{error}</AlertBanner> : null}
      {avatarUploadMsg ? <AlertBanner variant="success">{avatarUploadMsg}</AlertBanner> : null}

      <form className="profileEditor pageStack" onSubmit={onSave}>
        <div className="profileEditorLayout">
          <SectionCard
            title="Профайл зураг"
            subtitle="Зураг нь платформ дээрх танил тэмдэглэгээ болно."
            className="profileEditorAside"
          >
            <div className="avatarUploadBlock">
              {shownAvatarSrc ? (
                <div className="avatarPreviewWrap">
                  <img className="avatarPreviewImg" src={shownAvatarSrc} alt="Профайл зураг" />
                </div>
              ) : (
                <div className="avatarPreviewPlaceholder shellAvatarFallback" aria-hidden>
                  {user ? userInitials(user.fullName) : '?'}
                </div>
              )}
              <label>
                Зураг сонгох
                <input
                  type="file"
                  accept="image/*"
                  onChange={(e) => {
                    setAvatarUploadMsg(null)
                    setAvatarPick(e.target.files?.[0] ?? null)
                  }}
                />
              </label>
              <div className="buttonRow buttonRow-wrap">
                <button type="button" disabled={!avatarPick || isUploadingAvatar} onClick={() => void onUploadAvatar()}>
                  {isUploadingAvatar ? 'Ачаалж байна…' : 'Зургийг хадгалах'}
                </button>
                {avatarPick ? (
                  <button
                    type="button"
                    className="btnGhost"
                    onClick={() => {
                      setAvatarPick(null)
                      setAvatarUploadMsg(null)
                    }}
                  >
                    Сонголтыг цуцлах
                  </button>
                ) : null}
              </div>
            </div>
          </SectionCard>

          <div className="profileEditorMain">
            <SectionCard title="Бүртгэлийн мэдээлэл">
              <div className="form profileFormGrid">
                <label>
                  И-мэйл
                  <input value={user?.email ?? ''} readOnly disabled />
                </label>
                <label>
                  Овог нэр
                  <input
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    maxLength={120}
                    required
                  />
                </label>
              </div>
            </SectionCard>
          </div>
        </div>

        <div className="profileFormFooter">
          <button disabled={isSaving} type="submit">
            {isSaving ? 'Хадгалж байна…' : 'Хадгалах'}
          </button>
        </div>
      </form>
    </div>
  )
}
