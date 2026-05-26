export type ApiFieldError = {
  field?: string
  code?: string
  message?: string
}

export class ApiError extends Error {
  status: number
  body: unknown
  code?: string
  fieldErrors: ApiFieldError[]

  constructor(
    message: string,
    status: number,
    body: unknown,
    code?: string,
    fieldErrors: ApiFieldError[] = [],
  ) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.body = body
    this.code = code
    this.fieldErrors = fieldErrors
  }
}

export function getApiBaseUrl() {
  return import.meta.env.VITE_API_BASE_URL ?? ''
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function normalizeFieldErrors(value: unknown): ApiFieldError[] {
  if (!Array.isArray(value)) return []

  return value
    .filter(isRecord)
    .map((fieldError) => ({
      field: typeof fieldError.field === 'string' ? fieldError.field : undefined,
      code: typeof fieldError.code === 'string' ? fieldError.code : undefined,
      message: typeof fieldError.message === 'string' ? fieldError.message : undefined,
    }))
}

function parseApiErrorPayload(body: unknown) {
  if (!isRecord(body)) return null

  return {
    code: typeof body.code === 'string' ? body.code : undefined,
    message: typeof body.message === 'string' ? body.message : undefined,
    fieldErrors: normalizeFieldErrors(body.fieldErrors),
  }
}

async function readResponseBody(res: Response): Promise<unknown> {
  const contentType = res.headers.get('content-type') ?? ''
  if (contentType.includes('application/json')) {
    return res.json()
  }
  return res.text()
}

function toApiError(res: Response, body: unknown) {
  const payload = parseApiErrorPayload(body)
  const message = payload?.message ?? `Request failed (${res.status})`
  return new ApiError(message, res.status, body, payload?.code, payload?.fieldErrors ?? [])
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

  const body = await readResponseBody(res)

  if (!res.ok) {
    throw toApiError(res, body)
  }

  return body as T
}

export async function postFormData<T>(path: string, formData: FormData): Promise<T> {
  const baseUrl = getApiBaseUrl()
  const url = `${baseUrl}${path}`
  const token = localStorage.getItem('accessToken')

  const res = await fetch(url, {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    body: formData,
  })

  const body = await readResponseBody(res)

  if (!res.ok) {
    throw toApiError(res, body)
  }

  return body as T
}

