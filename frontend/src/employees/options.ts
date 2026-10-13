export const countries = [
  { code: 'IN', name: 'India' },
  { code: 'US', name: 'United States' },
  { code: 'GB', name: 'United Kingdom' },
  { code: 'DE', name: 'Germany' },
  { code: 'CA', name: 'Canada' },
]

export const currencies = [
  { code: 'INR', name: 'Indian Rupee' },
  { code: 'USD', name: 'US Dollar' },
  { code: 'GBP', name: 'Pound Sterling' },
  { code: 'EUR', name: 'Euro' },
  { code: 'CAD', name: 'Canadian Dollar' },
]

export const departments = ['Engineering', 'People', 'Finance', 'Operations', 'Sales']
export const jobTitles = ['Software Engineer', 'People Partner', 'Financial Analyst', 'Operations Specialist', 'Account Executive']

export function countryName(code: string): string {
  return countries.find((country) => country.code === code)?.name ?? code
}

export function todayUtc(): string {
  return new Date().toISOString().slice(0, 10)
}
