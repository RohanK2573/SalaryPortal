import { z } from 'zod'
import { todayUtc } from './options'

const amount = z.string()
  .trim()
  .regex(/^(?:0|[1-9]\d{0,14})(?:\.\d{1,4})?$/, 'Enter a positive amount with up to 15 digits before the decimal and four after it.')
  .refine((value) => /[1-9]/.test(value), 'Salary must be greater than zero.')

function isCalendarDate(value: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false
  const date = new Date(`${value}T00:00:00Z`)
  return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === value
}

const effectiveDate = z.string()
  .refine(isCalendarDate, 'Use a valid date.')
  .refine((value) => value <= todayUtc(), 'Effective date cannot be in the future.')

export const employeeSchema = z.object({
  employeeCode: z.string().trim().min(1, 'Enter an employee code.').max(64).transform((value) => value.toUpperCase()),
  fullName: z.string().trim().min(1, 'Enter the employee name.').max(200),
  countryCode: z.string().length(2, 'Choose a country.'),
  department: z.string().trim().min(1, 'Enter a department.').max(120),
  jobTitle: z.string().trim().min(1, 'Enter a job title.').max(120),
  status: z.enum(['ACTIVE', 'INACTIVE']),
  annualBaseAmount: amount,
  currencyCode: z.string().length(3, 'Choose a currency.'),
  effectiveDate,
})

export const salarySchema = z.object({ annualBaseAmount: amount, currencyCode: z.string().length(3), effectiveDate })

export const correctionSchema = z.object({
  annualBaseAmount: amount,
  currencyCode: z.string().length(3),
  effectiveDate,
  reason: z.string().trim().min(1, 'Enter a correction reason.').max(500, 'Reason must be 500 characters or fewer.'),
})

export type EmployeeFormValues = z.input<typeof employeeSchema>
export type SalaryFormValues = z.input<typeof salarySchema>
export type CorrectionFormValues = z.input<typeof correctionSchema>
