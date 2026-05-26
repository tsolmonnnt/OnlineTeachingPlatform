import type { ReactNode } from 'react'

type EmptyStateProps = {
  title: string
  description?: ReactNode
  action?: ReactNode
}

export function EmptyState({ title, description, action }: EmptyStateProps) {
  return (
    <div className="emptyState card">
      <div className="emptyStateIcon" aria-hidden>
        •
      </div>
      <div className="emptyStateTitle">{title}</div>
      {description ? <p className="emptyStateDescription">{description}</p> : null}
      {action ? <div className="emptyStateAction">{action}</div> : null}
    </div>
  )
}
