import { useEffect, useMemo } from 'react'

function formatFileSize(size: number) {
  if (size >= 1024 * 1024) {
    return `${(size / (1024 * 1024)).toFixed(1)} MB`
  }

  return `${Math.max(1, Math.round(size / 1024))} KB`
}

function getDisplayType(file: File) {
  if (file.type) {
    return file.type
  }

  const extension = file.name.split('.').pop()
  return extension ? extension.toUpperCase() : 'Файл'
}

export function FileUploadPreview({ file }: { file: File | null }) {
  const objectUrl = useMemo(() => {
    if (!file || !file.type.startsWith('image/')) return null
    return URL.createObjectURL(file)
  }, [file])

  useEffect(() => {
    if (!objectUrl) return
    return () => URL.revokeObjectURL(objectUrl)
  }, [objectUrl])

  if (!file) return null

  if (file.type.startsWith('image/') && objectUrl) {
    return (
      <div className="filePreviewBox filePreviewBox-image">
        <img src={objectUrl} alt="Сонгосон файлын урьдчилсан харагдац" className="filePreviewImg" />
        <div className="filePreviewMeta">
          <strong className="filePreviewName">{file.name}</strong>
          <span className="muted small">
            {getDisplayType(file)} · {formatFileSize(file.size)}
          </span>
        </div>
      </div>
    )
  }

  return (
    <div className="filePreviewBox filePreviewBox-file">
      <div className="filePreviewFileBadge" aria-hidden>
        Файл
      </div>
      <div className="filePreviewMeta">
        <strong className="filePreviewName">{file.name}</strong>
        <span className="muted small">
          {getDisplayType(file)} · {formatFileSize(file.size)}
        </span>
      </div>
    </div>
  )
}