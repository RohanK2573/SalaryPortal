import { request } from '../api/client'
import type { CompensationGroup } from '../api/types'

export type ReportGrouping = 'country' | 'countryJobTitle' | 'countryDepartment'
export type CompensationReport = { headcount: number; groups: CompensationGroup[] }
export type ReportFilters = { groupBy: ReportGrouping; countryCode?: string; department?: string }

export function reportUrl(filters: ReportFilters): string {
  const params = new URLSearchParams({ groupBy: filters.groupBy })
  if (filters.countryCode) params.set('countryCode', filters.countryCode)
  if (filters.department?.trim()) params.set('department', filters.department.trim())
  return `/api/v1/reports/compensation?${params.toString()}`
}

export const getCompensationReport = (filters: ReportFilters) => request<CompensationReport>(reportUrl(filters))
