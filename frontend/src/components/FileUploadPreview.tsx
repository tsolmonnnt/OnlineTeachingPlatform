import { useEffect, useMemo } from 'react'

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
        <div className="filePreviewBox">
          <img src={objectUrl} alt="Сонгосон файлын урьдчилсан харагдац" className="filePreviewImg" />
        </div>
    )
  }

  return (
      <div className="filePreviewBox muted small">
        <strong>{file.name}</strong>
        <span> · {(file.size / 1024).toFixed(0)} KB</span>
      </div>
  )
}