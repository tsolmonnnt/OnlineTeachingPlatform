import { useState } from 'react'
import { openMaterialDownload } from '../lib/materials'
import { getFriendlyErrorMessage } from '../lib/errorMessages'

type Props = {
  materialId: number
  label?: string
}

export function MaterialOpenButton({ materialId, label = 'Нээх' }: Props) {
  const [isOpening, setIsOpening] = useState(false)

  return (
    <button
      type="button"
      className="buttonLink"
      disabled={isOpening}
      onClick={() => {
        setIsOpening(true)
        void openMaterialDownload(materialId)
          .catch((err) => {
            window.alert(getFriendlyErrorMessage(err, 'Файл нээхэд алдаа гарлаа.'))
          })
          .finally(() => setIsOpening(false))
      }}
    >
      {isOpening ? 'Нээж байна…' : label}
    </button>
  )
}
