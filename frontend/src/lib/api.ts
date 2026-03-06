export class ApiError extends Error {
  status: number
  body: unknown
  constructor(message: string, status: number, body: unknown) {
    super(message)
    this.status = status
    this.body = body
  }
}

export function getApiBaseUrl() {
  return import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
}

export async function fetchJson<T>(
  path: string,
  options?: RequestInit & { token?: string | null },
): Promise<T> {
  const baseUrl = getApiBaseUrl()
  const url = `${baseUrl}${path}`
  const token = options?.token ?? localStorage.getItem('accessToken')

  const res = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options?.headers ?? {}),
    },
  })

  let body: unknown = null
  const contentType = res.headers.get('content-type') ?? ''
  if (contentType.includes('application/json')) {
    body = await res.json()
  } else {
    body = await res.text()
  }

  if (!res.ok) {
    const message =
      typeof body === 'object' && body && 'message' in body
        ? String((body as any).message)
        : `Request failed (${res.status})`
    throw new ApiError(message, res.status, body)
  }

  return body as T
}

