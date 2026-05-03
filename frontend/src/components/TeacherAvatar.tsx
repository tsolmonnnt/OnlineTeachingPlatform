type Size = 'sm' | 'md' | 'lg'

const sizesPx: Record<Size, number> = {
  sm: 52,
  md: 80,
  lg: 112,
}

export function TeacherAvatar({
  url,
  name,
  size = 'md',
}: {
  url: string | null | undefined
  name: string
  size?: Size
}) {
  const dim = sizesPx[size]
  const initial = name.trim().charAt(0).toLocaleUpperCase() || '?'

  if (url && url.trim()) {
    return (
      <img
        className={`teacherAvatarImg teacherAvatar-${size}`}
        src={url}
        alt={`${name} — профайл`}
        width={dim}
        height={dim}
        loading="lazy"
        decoding="async"
      />
    )
  }

  return (
    <div className={`teacherAvatarFallback teacherAvatar-${size}`} aria-hidden>
      {initial}
    </div>
  )
}
