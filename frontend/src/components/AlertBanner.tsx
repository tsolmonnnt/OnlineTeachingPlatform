import type { ReactNode } from 'react'

type AlertVariant = 'error' | 'success' | 'info'

type AlertBannerProps = {
  variant?: AlertVariant
  children: ReactNode
  className?: string
}

export function AlertBanner({ variant = 'info', children, className }: AlertBannerProps) {
  const variantClassName =
    variant === 'error' ? 'error' : variant === 'success' ? 'success' : 'alertInfo'
  const alertClassName = [variantClassName, 'alertBanner', className].filter(Boolean).join(' ')

  return <div className={alertClassName}>{children}</div>
}
