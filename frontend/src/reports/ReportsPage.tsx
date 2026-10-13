import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { BarChart3, ChevronDown, CircleHelp, Info, LoaderCircle, Search } from 'lucide-react'
import { useSearchParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { formatMoney } from '../api/types'
import { Alert, EmptyState, PageHeader, Skeleton, TableFrame } from '../components/primitives'
import { countries, countryName } from '../employees/options'
import { getCompensationReport } from './reportApi'

function reportError(error: unknown): string {
  if (error instanceof ApiError) {
    const fieldErrors = error.problem.errors?.map((item) => item.message).filter(Boolean) ?? []
    return fieldErrors.length ? `${error.problem.detail} ${fieldErrors.join(' ')}` : error.problem.detail
  }
  return error instanceof Error ? error.message : 'The report could not be loaded.'
}

function ReportSkeleton({ columns = 5 }: { columns?: number }) {
  return <div className="report-skeleton" aria-label="Loading report" role="status">
    <div className="report-skeleton-head">{Array.from({ length: columns }, (_, i) => <Skeleton key={i} />)}</div>
    {Array.from({ length: 5 }, (_, row) => <div className="report-skeleton-row" key={row}>{Array.from({ length: columns }, (_, i) => <Skeleton key={i} />)}</div>)}
  </div>
}

export function ReportsPage() {
  const [params, setParams] = useSearchParams()
  const [departmentInsightsOpen, setDepartmentInsightsOpen] = useState(false)
  const view = params.get('view') === 'jobTitle' ? 'jobTitle' : 'country'
  const countryCode = params.get('countryCode') ?? ''
  const department = params.get('department') ?? ''

  function updateParam(name: string, value: string) {
    const next = new URLSearchParams(params)
    if (value) next.set(name, value)
    else next.delete(name)
    setParams(next)
  }

  const countryFilters = useMemo(() => ({ groupBy: 'country' as const, countryCode: countryCode || undefined, department: department || undefined }), [countryCode, department])
  const titleFilters = useMemo(() => ({ groupBy: 'countryJobTitle' as const, countryCode: countryCode || undefined, department: department || undefined }), [countryCode, department])
  const departmentFilters = useMemo(() => ({ groupBy: 'countryDepartment' as const, countryCode: countryCode || undefined, department: department || undefined }), [countryCode, department])

  const countryQuery = useQuery({ queryKey: ['compensation-report', countryFilters], queryFn: () => getCompensationReport(countryFilters), enabled: view === 'country' })
  const titleQuery = useQuery({ queryKey: ['compensation-report', titleFilters], queryFn: () => getCompensationReport(titleFilters), enabled: view === 'jobTitle' && Boolean(countryCode) })
  const departmentQuery = useQuery({ queryKey: ['compensation-report', departmentFilters], queryFn: () => getCompensationReport(departmentFilters), enabled: departmentInsightsOpen })
  const activeQuery = view === 'country' ? countryQuery : titleQuery
  const report = activeQuery.data
  const departmentReport = departmentQuery.data
  const countrySummary = countryCode && view === 'country' ? report?.groups ?? [] : []

  return <section className="reports-page">
    <PageHeader title="Compensation reports" description="Understand current annual base salary by country, job title, and department." />
    <div className="report-explainer"><Info size={17} /><p>Reports include active employees with a current salary. Salaries stay in each employee’s local currency; currencies are not converted or combined.</p></div>

    <div className="report-controls">
      <div className="report-tabs" role="tablist" aria-label="Compensation report">
        <button type="button" role="tab" aria-selected={view === 'country'} className={view === 'country' ? 'report-tab report-tab-active' : 'report-tab'} onClick={() => updateParam('view', 'country')}><BarChart3 size={16} /> By country</button>
        <button type="button" role="tab" aria-selected={view === 'jobTitle'} className={view === 'jobTitle' ? 'report-tab report-tab-active' : 'report-tab'} onClick={() => updateParam('view', 'jobTitle')}><Search size={16} /> By job title</button>
      </div>
      <div className="report-filters">
        <label className="report-filter"><span>{view === 'jobTitle' ? 'Country (required)' : 'Country'}</span><select value={countryCode} onChange={(event) => updateParam('countryCode', event.target.value)}><option value="">{view === 'jobTitle' ? 'Choose a country' : 'All countries'}</option>{countries.map((country) => <option value={country.code} key={country.code}>{country.name} ({country.code})</option>)}</select></label>
        <label className="report-filter"><span>Department <small>optional</small></span><input value={department} onChange={(event) => updateParam('department', event.target.value)} placeholder="All departments" maxLength={120} list="report-departments" /></label>
        <datalist id="report-departments"><option value="Engineering" /><option value="People" /><option value="Finance" /><option value="Operations" /><option value="Sales" /></datalist>
        {(countryCode || department) && <button className="text-button report-clear" type="button" onClick={() => { setParams(new URLSearchParams(view === 'country' ? { view: 'country' } : { view: 'jobTitle' })); setDepartmentInsightsOpen(false) }}>Clear filters</button>}
      </div>
    </div>

    <section className="report-content" aria-label={view === 'country' ? 'Salary by country' : 'Average salary by job title'}>
      {view === 'jobTitle' && !countryCode && <EmptyState title="Choose a country to continue" description="Job title averages are reported within a country, with each currency shown separately." />}
      {view === 'country' && countrySummary.length > 0 && <div className="currency-summary-grid" aria-label="Country salary summaries by currency">{countrySummary.map((group) => <article className="currency-summary-card" key={`${group.countryCode}-${group.currencyCode}`}>
        <div className="currency-summary-heading"><div><span>{countryName(group.countryCode)} · {group.countryCode}</span><strong>{group.currencyCode}</strong></div><span>{group.headcount.toLocaleString()} {group.headcount === 1 ? 'employee' : 'employees'}</span></div>
        <dl className="metric-grid"><div><dt>Minimum</dt><dd>{formatMoney(group.minimum, group.currencyCode)}</dd></div><div><dt>Maximum</dt><dd>{formatMoney(group.maximum, group.currencyCode)}</dd></div><div className="metric-average"><dt>Average</dt><dd>{formatMoney(group.average, group.currencyCode)}</dd></div></dl>
      </article>)}</div>}
      {activeQuery.isPending && (view === 'country' || Boolean(countryCode)) && <ReportSkeleton columns={view === 'country' ? 6 : 6} />}
      {activeQuery.isError && !report && <Alert><span>{reportError(activeQuery.error)}</span><button className="text-button" type="button" onClick={() => void activeQuery.refetch()}>Retry</button></Alert>}
      {activeQuery.isError && report && <Alert tone="info">Showing the last report for these filters. Refresh failed. <button className="text-button" type="button" onClick={() => void activeQuery.refetch()}>Retry</button></Alert>}
      {activeQuery.isFetching && report && <p className="report-updating" role="status"><LoaderCircle size={14} /> Updating report…</p>}
      {report && report.groups.length === 0 && <EmptyState title="No salary data matches these filters" description="Try another country or department. Only active employees with a current salary appear in this report." />}

      {report && report.groups.length > 0 && view === 'country' && <>
        <div className="report-section-title"><div><h2>Salary by country</h2><p>Minimum, maximum, and average annual base salary by currency.</p></div><span>{report.groups.length} {report.groups.length === 1 ? 'group' : 'groups'}</span></div>
        <TableFrame label="Salary by country and currency"><table className="report-table">
          <thead><tr><th scope="col">Country</th><th scope="col">Currency</th><th scope="col">Employees</th><th scope="col">Minimum</th><th scope="col">Maximum</th><th scope="col">Average</th></tr></thead>
          <tbody>{report.groups.map((group) => <tr key={`${group.countryCode}-${group.currencyCode}`}><th scope="row">{countryName(group.countryCode)} <span className="country-code">{group.countryCode}</span></th><td><span className="currency-pill">{group.currencyCode}</span></td><td>{group.headcount.toLocaleString()}</td><td>{formatMoney(group.minimum, group.currencyCode)}</td><td>{formatMoney(group.maximum, group.currencyCode)}</td><td className="report-average">{formatMoney(group.average, group.currencyCode)}</td></tr>)}</tbody>
        </table></TableFrame>
      </>}

      {report && report.groups.length > 0 && view === 'jobTitle' && <>
        <div className="report-section-title"><div><h2>Average salary by job title</h2><p>{countryName(countryCode)} · separate rows for each currency.</p></div><span>{report.groups.length} {report.groups.length === 1 ? 'group' : 'groups'}</span></div>
        <TableFrame label="Average salary by job title"><table className="report-table">
          <thead><tr><th scope="col">Job title</th><th scope="col">Currency</th><th scope="col">Employees</th><th scope="col">Average</th><th scope="col">Minimum</th><th scope="col">Maximum</th></tr></thead>
          <tbody>{report.groups.map((group) => <tr key={`${group.jobTitle}-${group.currencyCode}`}><th scope="row">{group.jobTitle || 'Unspecified title'}</th><td><span className="currency-pill">{group.currencyCode}</span></td><td>{group.headcount.toLocaleString()}</td><td className="report-average">{formatMoney(group.average, group.currencyCode)}</td><td>{formatMoney(group.minimum, group.currencyCode)}</td><td>{formatMoney(group.maximum, group.currencyCode)}</td></tr>)}</tbody>
        </table></TableFrame>
      </>}
    </section>

    <section className="more-insights">
      <button className="more-insights-toggle" type="button" aria-expanded={departmentInsightsOpen} onClick={() => setDepartmentInsightsOpen((open) => !open)}><span><CircleHelp size={16} /><span><strong>More insights</strong><small>Compare salary ranges by department in the selected country.</small></span></span><ChevronDown size={17} className={departmentInsightsOpen ? 'chevron-open' : ''} /></button>
      {departmentInsightsOpen && <div className="department-insights">
        <div className="report-section-title"><div><h2>By department</h2><p>Secondary view. Currency groups remain separate.</p></div></div>
        {departmentQuery.isPending && <ReportSkeleton columns={6} />}
        {departmentQuery.isError && !departmentReport && <Alert><span>{reportError(departmentQuery.error)}</span><button className="text-button" type="button" onClick={() => void departmentQuery.refetch()}>Retry</button></Alert>}
        {departmentQuery.isError && departmentReport && <Alert tone="info">Showing saved results. Refresh failed. <button className="text-button" type="button" onClick={() => void departmentQuery.refetch()}>Retry</button></Alert>}
        {departmentQuery.isFetching && departmentReport && <p className="report-updating" role="status"><LoaderCircle size={14} /> Updating report…</p>}
        {departmentReport && departmentReport.groups.length === 0 && <EmptyState title="No department data matches these filters" description="Try another country or remove the department filter." />}
        {departmentReport && departmentReport.groups.length > 0 && <TableFrame label="Salary by department"><table className="report-table">
          <thead><tr><th scope="col">Department</th><th scope="col">Currency</th><th scope="col">Employees</th><th scope="col">Average</th><th scope="col">Minimum</th><th scope="col">Maximum</th></tr></thead>
          <tbody>{departmentReport.groups.map((group) => <tr key={`${group.department}-${group.currencyCode}`}><th scope="row">{group.department || 'Unspecified department'}</th><td><span className="currency-pill">{group.currencyCode}</span></td><td>{group.headcount.toLocaleString()}</td><td className="report-average">{formatMoney(group.average, group.currencyCode)}</td><td>{formatMoney(group.minimum, group.currencyCode)}</td><td>{formatMoney(group.maximum, group.currencyCode)}</td></tr>)}</tbody>
        </table></TableFrame>}
      </div>}
    </section>
    <p className="report-footnote"><Info size={14} /> Metrics use the latest salary record effective today. Historical trends and currency conversion are not included.</p>
  </section>
}
