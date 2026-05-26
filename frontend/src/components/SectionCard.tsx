import type { ReactNode } from 'react'

type SectionCardProps = {
  title?: string
  subtitle?: ReactNode
  actions?: ReactNode
  children: ReactNode
  className?: string
}

export function SectionCard({ title, subtitle, actions, children, className }: SectionCardProps) {
  const sectionClassName = ['card', 'sectionCard', className].filter(Boolean).join(' ')

  return (
    <section className={sectionClassName}>
      {title || subtitle || actions ? (
        <div className="sectionCardHeader">
          <div className="sectionCardTitleWrap">
            {title ? <h2 className="sectionCardTitle">{title}</h2> : null}
            {subtitle ? <p className="sectionCardSubtitle">{subtitle}</p> : null}
          </div>
          {actions ? <div className="sectionCardActions">{actions}</div> : null}
        </div>
      ) : null}
      <div className="sectionCardBody">{children}</div>
    </section>
  )
}
