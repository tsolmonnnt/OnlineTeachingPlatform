import { ApiError, fetchJson } from './api'

type MaterialDownloadUrlResponse = {
  url: string
}

export async function openMaterialDownload(materialId: number): Promise<void> {
  const token = localStorage.getItem('accessToken')
  if (!token) {
    throw new ApiError('Нэвтрэх шаардлагатай', 401, null)
  }

  const { url } = await fetchJson<MaterialDownloadUrlResponse>(
    `/api/materials/${materialId}/download-url`,
    { method: 'GET', token },
  )

  if (!url) {
    throw new ApiError('Файл олдсонгүй', 404, null)
  }

  window.open(url, '_blank', 'noopener,noreferrer')
}
