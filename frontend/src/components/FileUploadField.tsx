import { useId, type ReactNode } from 'react'
import { FileUploadPreview } from './FileUploadPreview'

type FileUploadFieldProps = {
  file: File | null
  onFileChange: (file: File | null) => void
  label?: string
  helperText?: ReactNode
  accept?: string
}

function UploadCloudIcon() {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M7.5 18.5h8a4 4 0 0 0 .72-7.935A5.5 5.5 0 0 0 5.563 9.28 3.5 3.5 0 0 0 7.5 18.5Z"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path d="M12 8.5v7" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
      <path d="m9.5 11 2.5-2.5L14.5 11" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

export function FileUploadField({
  file,
  onFileChange,
  label = 'Файл',
  helperText = 'PNG, JPG, WEBP, PDF, DOCX зэрэг файлыг сонгож болно.',
  accept,
}: FileUploadFieldProps) {
  const inputId = useId()

  return (
    <div className="uploadField">
      <div className="uploadFieldLabel">{label}</div>
      <label htmlFor={inputId} className={`uploadDropzone${file ? ' uploadDropzone-selected' : ''}`}>
        <input
          id={inputId}
          className="uploadInput"
          type="file"
          accept={accept}
          onChange={(event) => onFileChange(event.target.files?.[0] ?? null)}
        />
        <span className="uploadDropzoneIcon">
          <UploadCloudIcon />
        </span>
        <span className="uploadDropzoneTitle">{file ? 'Өөр файл сонгох' : 'Файл сонгох'}</span>
        <span className="uploadDropzoneHint">{helperText}</span>
      </label>

      {file ? (
        <div className="uploadFieldSelected">
          <FileUploadPreview file={file} />
          <div className="uploadFieldActions">
            <button type="button" className="btnGhost smallBtn" onClick={() => onFileChange(null)}>
              Сонголтыг цэвэрлэх
            </button>
          </div>
        </div>
      ) : null}
    </div>
  )
}
