import type { ReactNode } from 'react'

type FilterBarProps = {
  children: ReactNode
  actions?: ReactNode
  className?: string
}

export function FilterBar({ children, actions, className }: FilterBarProps) {
  const barClassName = ['card', 'filterBar', className].filter(Boolean).join(' ')

  return (
    <div className={barClassName}>
      <div className="filterBarGrid">{children}</div>
      {actions ? <div className="filterBarActions">{actions}</div> : null}
    </div>
  )
}
