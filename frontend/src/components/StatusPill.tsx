type StatusTone = 'success' | 'warning' | 'danger' | 'neutral' | 'info'

type StatusPillProps = {
  label: string
  tone?: StatusTone
}

export function StatusPill({ label, tone = 'neutral' }: StatusPillProps) {
  return <span className={`statusPill statusPill-${tone}`}>{label}</span>
}
