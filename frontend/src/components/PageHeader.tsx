import type { ReactNode } from 'react'

type PageHeaderProps = {
  title: string
  subtitle?: ReactNode
  eyebrow?: string
  actions?: ReactNode
}

export function PageHeader({ title, subtitle, eyebrow, actions }: PageHeaderProps) {
  return (
    <header className="pageHeaderBlock">
      <div className="pageHeaderText">
        {eyebrow ? <p className="pageHeaderEyebrow">{eyebrow}</p> : null}
        <div className="pageHeaderTitleRow">
          <h1 className="pageHeaderTitle">{title}</h1>
          {actions ? <div className="pageHeaderActions">{actions}</div> : null}
        </div>
        {subtitle ? <p className="pageHeaderSubtitle">{subtitle}</p> : null}
      </div>
    </header>
  )
}
