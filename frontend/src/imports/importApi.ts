import { downloadCsv, request } from '../api/client'
import type { ImportBatch, ImportDetails } from '../api/types'

export const importTemplate = () => downloadCsv('/api/v1/imports/template', 'employee-salary-template.csv')

export function previewCsv(file: File): Promise<ImportBatch> {
  const body = new FormData()
  body.append('file', file, file.name)
  return request<ImportBatch>('/api/v1/imports/preview', { method: 'POST', body })
}

export const getImportDetails = (id: string, page = 1, size = 25) =>
  request<ImportDetails>(`/api/v1/imports/${encodeURIComponent(id)}?page=${page}&size=${size}`)

export const commitImport = (id: string) =>
  request<ImportBatch>(`/api/v1/imports/${encodeURIComponent(id)}/commit`, { method: 'POST' })
