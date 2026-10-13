import type { ApiProblem, Session } from './types'

export class ApiError extends Error {
  constructor(public readonly status: number, public readonly problem: ApiProblem) {
    super(problem.detail || problem.title || 'The request could not be completed.')
    this.name = 'ApiError'
  }
}

function csrfToken(): string | undefined {
  const entry = document.cookie.split('; ').find((cookie) => cookie.startsWith('XSRF-TOKEN='))
  if (!entry) return undefined
  try {
    return decodeURIComponent(entry.slice('XSRF-TOKEN='.length))
  } catch {
    return entry.slice('XSRF-TOKEN='.length)
  }
}

async function fetchResponse(path: string, init: RequestInit = {}): Promise<Response> {
  const method = (init.method ?? 'GET').toUpperCase()
  const headers = new Headers(init.headers)
  if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    const token = csrfToken()
    if (token) headers.set('X-XSRF-TOKEN', token)
  }

  let response: Response
  try {
    response = await fetch(path, { ...init, method, headers, credentials: 'same-origin' })
  } catch {
    throw new Error('Unable to reach the service. Check your connection and try again.')
  }

  if (response.status === 401 && path !== '/api/v1/session') {
    window.dispatchEvent(new Event('salary:session-expired'))
  }
  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as Partial<ApiProblem> | null
    const problem: ApiProblem = {
      type: body?.type ?? 'about:blank',
      title: body?.title ?? response.statusText,
      status: body?.status ?? response.status,
      detail: body?.detail ?? 'The request could not be completed.',
      errors: body?.errors,
    }
    throw new ApiError(response.status, problem)
  }
  return response
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetchResponse(path, init)
  if (response.status === 204) return undefined as T
  if (response.headers.get('content-type')?.includes('application/json')) return response.json() as Promise<T>
  return (await response.blob()) as T
}

export const getSession = () => request<Session>('/api/v1/session')

function responseFilename(response: Response, fallback: string): string {
  const disposition = response.headers.get('content-disposition') ?? ''
  const encodedName = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1]
  const plainName = disposition.match(/filename="?([^";]+)"?/i)?.[1]
  let filename = fallback
  if (encodedName) {
    try { filename = decodeURIComponent(encodedName) } catch { filename = encodedName }
  } else if (plainName) filename = plainName
  return filename.replace(/[\\/:*?"<>|\u0000-\u001f]/g, '_') || fallback
}

export async function downloadCsv(path: string, fallbackFilename: string): Promise<void> {
  const response = await fetchResponse(path)
  const blob = await response.blob()
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = responseFilename(response, fallbackFilename)
  anchor.hidden = true
  document.body.append(anchor)
  anchor.click()
  anchor.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}
