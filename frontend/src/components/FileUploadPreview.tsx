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

const IMAGE_EXTENSION_PATTERN = /\.(jpe?g|png|gif|webp|bmp|heic|heif)$/i

function isImageFile(file: File) {
  if (file.type && file.type.startsWith('image/')) {
    return true
  }
  return IMAGE_EXTENSION_PATTERN.test(file.name)
}

export function FileUploadPreview({ file }: { file: File | null }) {
  const showImagePreview = file != null && isImageFile(file)

  const objectUrl = useMemo(() => {
    if (!file || !showImagePreview) return null
    return URL.createObjectURL(file)
  }, [file, showImagePreview])

  useEffect(() => {
    if (!objectUrl) return
    return () => URL.revokeObjectURL(objectUrl)
  }, [objectUrl])

  if (!file) return null

  if (showImagePreview && objectUrl) {
    return (
      <div className="filePreviewBox filePreviewBox-image">
        <div className="filePreviewImageFrame">
          <img src={objectUrl} alt="Сонгосон файлын урьдчилсан харагдац" className="filePreviewImg" />
        </div>
        <div className="filePreviewMeta filePreviewMeta-inline">
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
