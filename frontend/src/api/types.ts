export type Session = { authenticated: boolean; email?: string | null }

export type SalaryRecord = {
  id: string
  annualBaseAmount: string
  currencyCode: string
  effectiveDate: string
  version: number
}

export type Employee = {
  id: string
  employeeCode: string
  fullName: string
  countryCode: string
  department: string
  jobTitle: string
  status: 'ACTIVE' | 'INACTIVE'
  version: number
  currentSalary: SalaryRecord | null
}

export type ApiPage<T> = { items: T[]; page: number; size: number; total: number }

export type ApiFieldError = { field: string; message: string }
export type ApiProblem = {
  type: string
  title: string
  status: number
  detail: string
  errors?: ApiFieldError[] | null
}

export type CompensationGroup = {
  countryCode: string
  jobTitle: string | null
  department: string | null
  currencyCode: string
  headcount: number
  minimum: string
  maximum: string
  average: string
  total: string
  median: string
}

export type ImportBatch = {
  id: string
  status: 'PREVIEWED' | 'COMMITTED' | 'EXPIRED'
  createdAt: string
  expiresAt: string
  committedAt: string | null
  totalRowCount: number
  createCount: number
  updateCount: number
  noOpCount: number
  invalidCount: number
}

export type ImportRow = {
  rowNumber: number
  employeeCode: string
  action: 'CREATE' | 'UPDATE' | 'NO_OP' | 'INVALID'
  fullName: string | null
  countryCode: string | null
  department: string | null
  jobTitle: string | null
  status: 'ACTIVE' | 'INACTIVE' | null
  annualBaseAmount: string | null
  currencyCode: string | null
  effectiveDate: string | null
  errors: ApiFieldError[]
}

export type ImportDetails = { batch: ImportBatch; rows: ApiPage<ImportRow> }

export function formatMoney(amount: string, currencyCode: string): string {
  const [integerPart, fractionPart] = amount.split('.')
  const sign = integerPart.startsWith('-') ? '-' : ''
  const unsignedInteger = sign ? integerPart.slice(1) : integerPart
  const grouped = unsignedInteger.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return `${currencyCode} ${sign}${grouped}${fractionPart === undefined ? '' : `.${fractionPart}`}`
}
