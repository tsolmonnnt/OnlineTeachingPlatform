import { Modal } from './Modal'

type ConfirmDialogProps = {
  isOpen: boolean
  title: string
  message: string
  confirmLabel?: string
  cancelLabel?: string
  confirmTone?: 'danger' | 'primary'
  isConfirming?: boolean
  onConfirm: () => void
  onClose: () => void
}

export function ConfirmDialog({
  isOpen,
  title,
  message,
  confirmLabel = 'Тийм',
  cancelLabel = 'Цуцлах',
  confirmTone = 'primary',
  isConfirming = false,
  onConfirm,
  onClose,
}: ConfirmDialogProps) {
  return (
    <Modal
      title={title}
      isOpen={isOpen}
      onClose={onClose}
      footer={
        <div className="buttonRow buttonRow-wrap confirmDialogActions">
          <button type="button" className="btnGhost" onClick={onClose} disabled={isConfirming}>
            {cancelLabel}
          </button>
          <button
            type="button"
            className={confirmTone === 'danger' ? 'btnGhost danger' : undefined}
            onClick={onConfirm}
            disabled={isConfirming}
          >
            {isConfirming ? 'Хүлээнэ үү…' : confirmLabel}
          </button>
        </div>
      }
    >
      <p className="confirmDialogMessage">{message}</p>
    </Modal>
  )
}
