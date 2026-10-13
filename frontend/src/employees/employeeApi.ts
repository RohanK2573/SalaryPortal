import { request } from '../api/client'
import type { ApiPage, Employee, SalaryRecord } from '../api/types'

export type EmployeeSearch = {
  q?: string
  countryCode?: string
  department?: string
  status?: 'ACTIVE' | 'INACTIVE'
  page: number
  size: number
  sort: string
}

export type EmployeeInput = {
  employeeCode: string
  fullName: string
  countryCode: string
  department: string
  jobTitle: string
  status: 'ACTIVE' | 'INACTIVE'
  annualBaseAmount: string
  currencyCode: string
  effectiveDate: string
}

export type SalaryInput = Pick<SalaryRecord, 'annualBaseAmount' | 'currencyCode' | 'effectiveDate'>

export function employeeSearchUrl(search: EmployeeSearch): string {
  const params = new URLSearchParams()
  if (search.q) params.set('q', search.q)
  if (search.countryCode) params.set('countryCode', search.countryCode)
  if (search.department) params.set('department', search.department)
  if (search.status) params.set('status', search.status)
  params.set('page', String(search.page))
  params.set('size', String(search.size))
  params.set('sort', search.sort)
  return `/api/v1/employees?${params.toString()}`
}

export const searchEmployees = (search: EmployeeSearch) => request<ApiPage<Employee>>(employeeSearchUrl(search))
export const getEmployee = (id: string) => request<Employee>(`/api/v1/employees/${encodeURIComponent(id)}`)
export const createEmployee = (input: EmployeeInput) => request<Employee>('/api/v1/employees', { method: 'POST', body: JSON.stringify(input) })
export const updateEmployee = (id: string, patch: Partial<Pick<Employee, 'fullName' | 'countryCode' | 'department' | 'jobTitle' | 'status'>> & { version: number }) =>
  request<Employee>(`/api/v1/employees/${encodeURIComponent(id)}`, { method: 'PATCH', body: JSON.stringify(patch) })

export const getSalaryHistory = (id: string, page: number, size = 25) =>
  request<ApiPage<SalaryRecord>>(`/api/v1/employees/${encodeURIComponent(id)}/salary-records?page=${page}&size=${size}`)
export const addSalary = (id: string, input: SalaryInput) =>
  request<SalaryRecord>(`/api/v1/employees/${encodeURIComponent(id)}/salary-records`, { method: 'POST', body: JSON.stringify(input) })
export const correctSalary = (employeeId: string, salaryId: string, patch: Partial<SalaryInput> & { version: number; reason: string }) =>
  request<SalaryRecord>(`/api/v1/employees/${encodeURIComponent(employeeId)}/salary-records/${encodeURIComponent(salaryId)}`, { method: 'PATCH', body: JSON.stringify(patch) })
