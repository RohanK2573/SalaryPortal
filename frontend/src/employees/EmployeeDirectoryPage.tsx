import { useEffect, useMemo, useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router-dom'
import { ArrowDownUp, ChevronLeft, ChevronRight, Plus, Search, SlidersHorizontal, Users } from 'lucide-react'
import { ApiError } from '../api/client'
import { formatMoney } from '../api/types'
import { Alert, Button, EmptyState, PageHeader, Skeleton, StatusBadge, TableFrame } from '../components/primitives'
import { countryName, countries } from './options'
import { searchEmployees } from './employeeApi'
import type { EmployeeSearch } from './employeeApi'

const sortOptions = [
  ['employeeCode,asc', 'Employee code A–Z'], ['employeeCode,desc', 'Employee code Z–A'],
  ['fullName,asc', 'Name A–Z'], ['fullName,desc', 'Name Z–A'],
  ['countryCode,asc', 'Country A–Z'], ['department,asc', 'Department A–Z'], ['jobTitle,asc', 'Job title A–Z'],
] as const

function positiveInt(value: string | null, fallback: number): number {
  const number = Number(value)
  return Number.isInteger(number) && number > 0 ? number : fallback
}

function EmployeeLoading() {
  return <div className="employee-table-skeleton" aria-label="Loading employees">
    {Array.from({ length: 7 }, (_, index) => <div className="employee-skeleton-row" key={index}><Skeleton /><Skeleton /><Skeleton /><Skeleton /><Skeleton /><Skeleton /></div>)}
  </div>
}

export function EmployeeDirectoryPage() {
  const [params, setParams] = useSearchParams()
  const qParam = params.get('q') ?? ''
  const [queryDraft, setQueryDraft] = useState(qParam)

  useEffect(() => setQueryDraft(qParam), [qParam])
  useEffect(() => {
    const timer = window.setTimeout(() => {
      const next = new URLSearchParams(params)
      if (queryDraft.trim()) next.set('q', queryDraft.trim())
      else next.delete('q')
      if ((params.get('q') ?? '') !== (queryDraft.trim())) next.set('page', '1')
      if (next.toString() !== params.toString()) setParams(next, { replace: true })
    }, 300)
    return () => window.clearTimeout(timer)
  }, [queryDraft, params, setParams])

  const search = useMemo<EmployeeSearch>(() => ({
    q: params.get('q') || undefined,
    countryCode: params.get('countryCode') || undefined,
    department: params.get('department') || undefined,
    status: (params.get('status') as EmployeeSearch['status']) || undefined,
    page: positiveInt(params.get('page'), 1),
    size: [25, 50, 100].includes(positiveInt(params.get('size'), 25)) ? positiveInt(params.get('size'), 25) : 25,
    sort: params.get('sort') || 'employeeCode,asc',
  }), [params])

  const employeesQuery = useQuery({
    queryKey: ['employees', search],
    queryFn: () => searchEmployees(search),
    placeholderData: keepPreviousData,
  })

  function updateParam(name: string, value: string) {
    const next = new URLSearchParams(params)
    if (value) next.set(name, value)
    else next.delete(name)
    if (['countryCode', 'department', 'status', 'size', 'sort'].includes(name)) next.set('page', '1')
    setParams(next)
  }

  function clearFilters() {
    setQueryDraft('')
    setParams(new URLSearchParams({ page: '1', size: '25', sort: 'employeeCode,asc' }))
  }

  const result = employeesQuery.data
  const pageCount = result ? Math.max(1, Math.ceil(result.total / result.size)) : 1
  const firstRow = result && result.total > 0 ? (result.page - 1) * result.size + 1 : 0
  const lastRow = result ? Math.min(result.page * result.size, result.total) : 0
  const hasFilters = Boolean(search.q || search.countryCode || search.department || search.status)
  const apiError = employeesQuery.error
  const errorMessage = apiError instanceof ApiError ? apiError.message : apiError instanceof Error ? apiError.message : 'Employees could not be loaded.'

  return <section className="employee-directory">
    <PageHeader title="Employees" description="Find employees and review their current annual base salary." actions={<Link className="button button-primary" to="/employees/new"><Plus size={17} /> Add employee</Link>} />

    <div className="directory-toolbar">
      <label className="search-field"><Search size={17} aria-hidden="true" /><span className="visually-hidden">Search employees</span><input value={queryDraft} onChange={(event) => setQueryDraft(event.target.value)} placeholder="Search by name or employee code" maxLength={100} /></label>
      <div className="filter-control"><SlidersHorizontal size={16} aria-hidden="true" /><label className="visually-hidden" htmlFor="country-filter">Country</label><select id="country-filter" value={search.countryCode ?? ''} onChange={(event) => updateParam('countryCode', event.target.value)}><option value="">All countries</option>{countries.map((country) => <option value={country.code} key={country.code}>{country.name}</option>)}</select></div>
      <label className="visually-hidden" htmlFor="department-filter">Department</label><input id="department-filter" className="toolbar-input" list="department-options" value={search.department ?? ''} onChange={(event) => updateParam('department', event.target.value)} placeholder="All departments" maxLength={120} />
      <datalist id="department-options"><option value="Engineering" /><option value="People" /><option value="Finance" /><option value="Operations" /><option value="Sales" /></datalist>
      <label className="visually-hidden" htmlFor="status-filter">Employment status</label><select id="status-filter" className="toolbar-input" value={search.status ?? ''} onChange={(event) => updateParam('status', event.target.value)}><option value="">All statuses</option><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option></select>
    </div>

    <div className="directory-options">
      <label className="sort-control"><ArrowDownUp size={15} /><span>Sort</span><select aria-label="Sort employees" value={search.sort} onChange={(event) => updateParam('sort', event.target.value)}>{sortOptions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
      {hasFilters && <button type="button" className="text-button" onClick={clearFilters}>Clear filters</button>}
      <span className="directory-count">{result ? `${firstRow.toLocaleString()}–${lastRow.toLocaleString()} of ${result.total.toLocaleString()}` : 'Loading employees'}</span>
    </div>

    {employeesQuery.isPending && <EmployeeLoading />}
    {employeesQuery.isError && !employeesQuery.data && <Alert><span>{errorMessage}</span><button type="button" className="text-button" onClick={() => void employeesQuery.refetch()}>Retry</button></Alert>}
    {result && result.items.length === 0 && <EmptyState title={hasFilters ? 'No employees match these filters' : 'No employees yet'} description={hasFilters ? 'Try changing your search or filters.' : 'Add an employee to start managing current salary information.'} action={hasFilters ? <Button onClick={clearFilters}>Clear filters</Button> : <Link className="button button-primary" to="/employees/new"><Plus size={16} /> Add employee</Link>} />}

    {result && result.items.length > 0 && <>
      {employeesQuery.isError && <Alert tone="info">Showing the last results. The latest refresh failed. <button type="button" className="text-button" onClick={() => void employeesQuery.refetch()}>Retry</button></Alert>}
      <TableFrame label="Employee directory">
        <table className="employee-table">
          <thead><tr><th scope="col">Employee</th><th scope="col">Country</th><th scope="col">Department</th><th scope="col">Job title</th><th scope="col">Status</th><th scope="col" className="salary-column">Current salary</th></tr></thead>
          <tbody>{result.items.map((employee) => <tr key={employee.id}>
            <td><Link className="employee-identity" to={`/employees/${employee.id}`}><span className="employee-name">{employee.fullName}</span><span className="employee-code">{employee.employeeCode}</span></Link></td>
            <td><span className="country-cell">{countryName(employee.countryCode)} <span>{employee.countryCode}</span></span></td>
            <td>{employee.department}</td><td>{employee.jobTitle}</td>
            <td><StatusBadge tone={employee.status === 'ACTIVE' ? 'positive' : 'neutral'}>{employee.status === 'ACTIVE' ? 'Active' : 'Inactive'}</StatusBadge></td>
            <td className="salary-column">{employee.currentSalary ? <span className="salary-value">{formatMoney(employee.currentSalary.annualBaseAmount, employee.currentSalary.currencyCode)}</span> : <span className="not-recorded">Not recorded</span>}</td>
          </tr>)}</tbody>
        </table>
      </TableFrame>
      <div className="employee-mobile-list">{result.items.map((employee) => <Link className="employee-mobile-card" to={`/employees/${employee.id}`} key={employee.id}>
        <div className="employee-mobile-heading"><div><strong>{employee.fullName}</strong><span>{employee.employeeCode}</span></div><StatusBadge tone={employee.status === 'ACTIVE' ? 'positive' : 'neutral'}>{employee.status === 'ACTIVE' ? 'Active' : 'Inactive'}</StatusBadge></div>
        <div className="employee-mobile-meta"><span>{countryName(employee.countryCode)} · {employee.department}</span><span>{employee.jobTitle}</span></div>
        <div className="employee-mobile-salary">{employee.currentSalary ? formatMoney(employee.currentSalary.annualBaseAmount, employee.currentSalary.currencyCode) : <span className="not-recorded">Not recorded</span>}</div>
      </Link>)}</div>
      <div className="pagination-bar">
        <label>Rows per page <select value={search.size} onChange={(event) => updateParam('size', event.target.value)}><option value={25}>25</option><option value={50}>50</option><option value={100}>100</option></select></label>
        <span>Page {result.page} of {pageCount}</span>
        <div className="pagination-actions"><button aria-label="Previous page" className="icon-button" type="button" disabled={result.page <= 1 || employeesQuery.isFetching} onClick={() => updateParam('page', String(result.page - 1))}><ChevronLeft size={18} /></button><button aria-label="Next page" className="icon-button" type="button" disabled={result.page >= pageCount || employeesQuery.isFetching} onClick={() => updateParam('page', String(result.page + 1))}><ChevronRight size={18} /></button></div>
      </div>
    </>}
    <p className="directory-footnote"><Users size={15} /> Inactive employees remain searchable and are excluded from compensation reports.</p>
  </section>
}
