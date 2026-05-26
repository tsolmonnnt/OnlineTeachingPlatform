import { useEffect, useMemo, useState } from 'react'
import { fetchJson, postFormData } from '../lib/api'
import type { TeacherProfile } from '../auth/types'
import { AlertBanner } from '../components/AlertBanner'
import { PageHeader } from '../components/PageHeader'
import { SectionCard } from '../components/SectionCard'
import { StatusPill } from '../components/StatusPill'
import { getFriendlyErrorMessage } from '../lib/errorMessages'

function splitCsv(input: string): string[] {
  return input
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
}

function joinCsv(list: string[] | null | undefined) {
  return (list ?? []).join(', ')
}

export default function TeacherProfilePage() {
  const [profile, setProfile] = useState<TeacherProfile | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)

  const [headline, setHeadline] = useState('')
  const [bio, setBio] = useState('')
  const [subjectsCsv, setSubjectsCsv] = useState('')
  const [skillsCsv, setSkillsCsv] = useState('')
  const [avatarUrl, setAvatarUrl] = useState('')
  const [hourlyRate, setHourlyRate] = useState('')
  const [languagesCsv, setLanguagesCsv] = useState('')
  const [location, setLocation] = useState('')
  const [phone, setPhone] = useState('')
  const [yearsExperience, setYearsExperience] = useState('')

  const [avatarPick, setAvatarPick] = useState<File | null>(null)
  const [avatarPreviewUrl, setAvatarPreviewUrl] = useState<string | null>(null)
  const [isUploadingAvatar, setIsUploadingAvatar] = useState(false)
  const [avatarUploadMsg, setAvatarUploadMsg] = useState<string | null>(null)

  const payload = useMemo(() => {
    return {
      headline: headline.trim() || null,
      bio: bio.trim() || null,
      subjects: splitCsv(subjectsCsv),
      skills: splitCsv(skillsCsv),
      avatarUrl: avatarUrl.trim() || null,
      hourlyRate: hourlyRate.trim() ? Number(hourlyRate) : null,
      languages: splitCsv(languagesCsv),
      location: location.trim() || null,
      phone: phone.trim() || null,
      yearsExperience: yearsExperience.trim() ? Number(yearsExperience) : null,
    }
  }, [
    headline,
    bio,
    subjectsCsv,
    skillsCsv,
    avatarUrl,
    hourlyRate,
    languagesCsv,
    location,
    phone,
    yearsExperience,
  ])

  useEffect(() => {
    if (!avatarPick) {
      setAvatarPreviewUrl(null)
      return
    }
    const url = URL.createObjectURL(avatarPick)
    setAvatarPreviewUrl(url)
    return () => URL.revokeObjectURL(url)
  }, [avatarPick])

  useEffect(() => {
    ;(async () => {
      setError(null)
      setSuccess(null)
      setIsLoading(true)
      try {
        const p = await fetchJson<TeacherProfile>('/api/teachers/me', { method: 'GET' })
        setProfile(p)
        setHeadline(p.headline ?? '')
        setBio(p.bio ?? '')
        setSubjectsCsv(joinCsv(p.subjects))
        setSkillsCsv(joinCsv(p.skills))
        setAvatarUrl(p.avatarUrl ?? '')
        setHourlyRate(p.hourlyRate == null ? '' : String(p.hourlyRate))
        setLanguagesCsv(joinCsv(p.languages))
        setLocation(p.location ?? '')
        setPhone(p.phone ?? '')
        setYearsExperience(p.yearsExperience == null ? '' : String(p.yearsExperience))
      } catch (err) {
        setError(getFriendlyErrorMessage(err, 'Профайлыг ачаалж чадсангүй.'))
      } finally {
        setIsLoading(false)
      }
    })()
  }, [])

  async function onSave(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSuccess(null)
    setIsSaving(true)
    try {
      const saved = await fetchJson<TeacherProfile>('/api/teachers/me', {
        method: 'PUT',
        body: JSON.stringify(payload),
      })
      setProfile(saved)
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
      const saved = await postFormData<TeacherProfile>('/api/teachers/me/avatar', fd)
      setProfile(saved)
      setAvatarUrl(saved.avatarUrl ?? '')
      setAvatarPick(null)
      setAvatarUploadMsg('Зураг амжилттай ачаалагдлаа.')
    } catch (err) {
      setError(getFriendlyErrorMessage(err, 'Зураг ачаалах үед алдаа гарлаа.'))
    } finally {
      setIsUploadingAvatar(false)
    }
  }

  const shownAvatarSrc = avatarPreviewUrl || avatarUrl.trim() || profile?.avatarUrl || ''

  if (isLoading) {
    return (
      <div className="page pageWide pageStack">
        <PageHeader
          eyebrow="Багшийн профайл"
          title="Профайлаа бэлдэж байна"
          subtitle="Таны багшийн профайл, зураг болон мэргэжлийн мэдээллийг ачаалж байна."
        />
        <p className="muted">Ачаалж байна…</p>
      </div>
    )
  }

  return (
    <div className="page pageWide pageStack">
      <PageHeader
        eyebrow="Багшийн профайл"
        title="Профайлаа бүрэн гүйцэд болгоорой"
        subtitle={
          profile ? (
            <>
              Профайл ID: {profile.id} · Сүүлд шинэчилсэн: {new Date(profile.updatedAt).toLocaleString('mn-MN')}
            </>
          ) : (
            'Сурагчдад харагдах танилцуулга, ур чадвар, холбоо барих мэдээллээ эндээс шинэчилнэ.'
          )
        }
        actions={
          profile ? (
            <StatusPill label={profile.verified ? 'Админ баталгаажуулсан' : 'Админы шалгалтад хүлээгдэж байна'} tone={profile.verified ? 'success' : 'warning'} />
          ) : null
        }
      />

      {success ? <AlertBanner variant="success">{success}</AlertBanner> : null}
      {error ? <AlertBanner variant="error">{error}</AlertBanner> : null}

      <form className="profileEditor pageStack" onSubmit={onSave}>
        <div className="profileEditorLayout">
          <SectionCard
            title="Профайл зураг"
            subtitle="Зурагтай профайл илүү итгэл төрүүлдэг. Сонгосон зураг серверт ачаалагдсаны дараа сурагчдад харагдана."
            className="profileEditorAside"
          >
            <div className="avatarUploadBlock">
              {shownAvatarSrc ? (
                <div className="avatarPreviewWrap">
                  <img className="avatarPreviewImg" src={shownAvatarSrc} alt="Профайл зураг" />
                </div>
              ) : (
                <div className="avatarPreviewPlaceholder muted small">Зураг сонгоогүй байна</div>
              )}
              <label>
                Зураг сонгох
                <input
                  type="file"
                  accept="image/*"
                  onChange={(e) => {
                    setAvatarUploadMsg(null)
                    const f = e.target.files?.[0] ?? null
                    setAvatarPick(f)
                  }}
                />
              </label>
              <div className="buttonRow buttonRow-wrap">
                <button type="button" disabled={!avatarPick || isUploadingAvatar} onClick={() => void onUploadAvatar()}>
                  {isUploadingAvatar ? 'Ачаалж байна…' : 'Зургийг серверт ачаалах'}
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
              {avatarUploadMsg ? <AlertBanner variant="success">{avatarUploadMsg}</AlertBanner> : null}
            </div>
          </SectionCard>

          <div className="profileEditorMain">
            <SectionCard title="Ерөнхий танилцуулга" subtitle="Сурагчдад хамгийн түрүүнд харагдах мэдээлэл.">
              <div className="form profileFormGrid">
                <label>
                  Товч гарчиг
                  <input value={headline} onChange={(e) => setHeadline(e.target.value)} maxLength={120} />
                </label>
                <label className="fieldSpanFull">
                  Танилцуулга
                  <textarea value={bio} onChange={(e) => setBio(e.target.value)} rows={5} maxLength={2000} />
                </label>
              </div>
            </SectionCard>

            <SectionCard
              title="Мэргэжлийн мэдээлэл"
              subtitle="Заах хичээл, ур чадвар, хэлний мэдээллээ дэлгэрэнгүй оруулснаар хайлтад илүү сайн харагдана."
            >
              <div className="form profileFormGrid">
                <label className="fieldSpanFull">
                  Заах хичээлүүд (таслалаар тусгаарлана)
                  <input value={subjectsCsv} onChange={(e) => setSubjectsCsv(e.target.value)} />
                </label>
                <p className="fieldHint fieldSpanFull">
                  Өөрийн хичээлийн нэрээ чөлөөтэй бичиж болно. Хадгалахад систем хичээлийн жагсаалтад автоматаар бүртгэнэ.
                </p>
                <label>
                  Ур чадварууд
                  <input value={skillsCsv} onChange={(e) => setSkillsCsv(e.target.value)} />
                </label>
                <label>
                  Хэлнүүд
                  <input value={languagesCsv} onChange={(e) => setLanguagesCsv(e.target.value)} />
                </label>
                <label>
                  Цагийн үнэ
                  <input
                    value={hourlyRate}
                    onChange={(e) => setHourlyRate(e.target.value)}
                    inputMode="decimal"
                    placeholder="ж: 25000"
                  />
                </label>
                <label>
                  Туршлага (жил)
                  <input
                    value={yearsExperience}
                    onChange={(e) => setYearsExperience(e.target.value)}
                    inputMode="numeric"
                    placeholder="ж: 5"
                  />
                </label>
              </div>
            </SectionCard>

            <SectionCard title="Холбоо барих" subtitle="Хичээл эхлэхийн өмнөх зохион байгуулалтад хэрэгтэй мэдээллүүд.">
              <div className="form profileFormGrid">
                <label>
                  Байршил
                  <input value={location} onChange={(e) => setLocation(e.target.value)} />
                </label>
                <label>
                  Утас
                  <input value={phone} onChange={(e) => setPhone(e.target.value)} maxLength={40} />
                </label>
              </div>
            </SectionCard>
          </div>
        </div>

        <div className="profileFormFooter">
          <button disabled={isSaving} type="submit">
            {isSaving ? 'Хадгалж байна…' : 'Профайл хадгалах'}
          </button>
        </div>
      </form>
    </div>
  )
}
