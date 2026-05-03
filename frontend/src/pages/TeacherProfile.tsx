import { useEffect, useMemo, useState } from 'react'
import { ApiError, fetchJson, postFormData } from '../lib/api'
import type { TeacherProfile } from '../auth/types'

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
        if (err instanceof ApiError) setError(err.message)
        else setError('Профайл ачаалж чадсангүй')
      } finally {
        setIsLoading(false)
      }
    })()
  }, [])

  async function onSave(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setIsSaving(true)
    try {
      const saved = await fetchJson<TeacherProfile>('/api/teachers/me', {
        method: 'PUT',
        body: JSON.stringify(payload),
      })
      setProfile(saved)
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError('Хадгалах үед алдаа гарлаа')
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
      if (err instanceof ApiError) setError(err.message)
      else setError('Зураг ачаалахад алдаа гарлаа')
    } finally {
      setIsUploadingAvatar(false)
    }
  }

  const shownAvatarSrc = avatarPreviewUrl || avatarUrl.trim() || profile?.avatarUrl || ''

  if (isLoading) {
    return (
      <div className="page">
        <h1>Багшийн профайл</h1>
        <p className="muted">Ачаалж байна…</p>
      </div>
    )
  }

  return (
    <div className="page">
      <h1>Багшийн профайл</h1>
      {profile ? (
        <p className="muted">
          Профайл ID: {profile.id} • Админ баталгаажуулалт: {profile.verified ? 'Тийм' : 'Хүлээгдэж буй'} • Шинэчилсэн:{' '}
          {new Date(profile.updatedAt).toLocaleString('mn-MN')}
        </p>
      ) : null}

      <form className="card form" onSubmit={onSave}>
        <h2>Ерөнхий</h2>
        <label>
          Товч гарчиг
          <input value={headline} onChange={(e) => setHeadline(e.target.value)} maxLength={120} />
        </label>
        <label>
          Танилцуулга
          <textarea value={bio} onChange={(e) => setBio(e.target.value)} rows={5} maxLength={2000} />
        </label>

        <div className="avatarUploadBlock">
          <p className="muted small" style={{ margin: '0 0 8px' }}>
            Профайл зураг (Cloudinary — profiles хавтас). Сонгоход урьдчилан харагдана.
          </p>
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
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, alignItems: 'center' }}>
            <button type="button" disabled={!avatarPick || isUploadingAvatar} onClick={() => void onUploadAvatar()}>
              {isUploadingAvatar ? 'Ачаалж байна…' : 'Зургийг серверт ачаалах'}
            </button>
            {avatarPick ? (
              <button
                type="button"
                className="linkButton"
                onClick={() => {
                  setAvatarPick(null)
                  setAvatarUploadMsg(null)
                }}
              >
                Сонголтыг цуцлах
              </button>
            ) : null}
          </div>
          {avatarUploadMsg ? <p className="muted small" style={{ margin: 0 }}>{avatarUploadMsg}</p> : null}
        </div>

        {/*<label>*/}
        {/*  Эсвэл зургийн URL (сонголттой)*/}
        {/*  <input*/}
        {/*    value={avatarUrl}*/}
        {/*    onChange={(e) => setAvatarUrl(e.target.value)}*/}
        {/*    placeholder="https://…"*/}
        {/*  />*/}
        {/*</label>*/}

        <h2>Мэргэжлийн мэдээлэл</h2>
        <label>
          Заах хичээлүүд (таслалаар тусгаарлана)
          <input value={subjectsCsv} onChange={(e) => setSubjectsCsv(e.target.value)} />
        </label>
        <p className="muted small" style={{ margin: '-4px 0 0' }}>
          Өөрийн хичээлийн нэрээ чөлөөтэй бичнэ; хадгалахад систем хичээлийн жагсаалтад автоматаар бүртгэнэ.
        </p>
        <label>
          Ур чадварууд (таслалаар тусгаарлана)
          <input value={skillsCsv} onChange={(e) => setSkillsCsv(e.target.value)} />
        </label>
        <label>
          Хэлнүүд (таслалаар тусгаарлана)
          <input value={languagesCsv} onChange={(e) => setLanguagesCsv(e.target.value)} />
        </label>
        <label>
          Цагийн үнэ
          <input
            value={hourlyRate}
            onChange={(e) => setHourlyRate(e.target.value)}
            inputMode="decimal"
            placeholder="ж: 25"
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

        <h2>Холбоо барих</h2>
        <label>
          Байршил
          <input value={location} onChange={(e) => setLocation(e.target.value)} />
        </label>
        <label>
          Утас
          <input value={phone} onChange={(e) => setPhone(e.target.value)} maxLength={40} />
        </label>

        {error ? <div className="error">{error}</div> : null}
        <button disabled={isSaving} type="submit">
          {isSaving ? 'Хадгалж байна…' : 'Профайл хадгалах'}
        </button>
      </form>
    </div>
  )
}
