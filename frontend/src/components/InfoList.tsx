import type { ReactNode } from 'react'

type InfoListItem = {
  label: string
  value: ReactNode
}

type InfoListProps = {
  items: InfoListItem[]
  className?: string
}

export function InfoList({ items, className }: InfoListProps) {
  const listClassName = ['infoList', className].filter(Boolean).join(' ')

  return (
    <dl className={listClassName}>
      {items.map((item) => (
        <div key={item.label} className="infoListItem">
          <dt className="infoListLabel">{item.label}</dt>
          <dd className="infoListValue">{item.value}</dd>
        </div>
      ))}
    </dl>
  )
}
