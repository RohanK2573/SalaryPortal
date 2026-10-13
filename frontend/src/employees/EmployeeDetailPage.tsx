import { useEffect, useMemo, useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { ArrowLeft, BadgeDollarSign, CalendarDays, ChevronLeft, ChevronRight, Pencil, Plus, UserRound } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import type { Employee, SalaryRecord } from '../api/types'
import { formatMoney } from '../api/types'
import { Alert, Button, Dialog, EmptyState, PageHeader, Skeleton, StatusBadge } from '../components/primitives'
import { countries, currencies, countryName, todayUtc } from './options'
import { correctSalary, addSalary, getEmployee, getSalaryHistory, updateEmployee } from './employeeApi'
import { correctionSchema, employeeSchema, salarySchema } from './validation'
import type { CorrectionFormValues, EmployeeFormValues, SalaryFormValues } from './validation'

function messageFor(error: unknown): string {
  if (error instanceof ApiError && error.status === 403) return 'You do not have permission to make this change.'
  return error instanceof Error ? error.message : 'The request could not be completed. Try again.'
}

function applyFieldErrors(error: unknown, setError: (name: never, error: { type: string; message: string }) => void): string | null {
  if (!(error instanceof ApiError) || error.status !== 422) return null
  const remaining: string[] = []
  for (const item of error.problem.errors ?? []) {
    if (['fullName', 'countryCode', 'department', 'jobTitle', 'status', 'annualBaseAmount', 'currencyCode', 'effectiveDate', 'reason'].includes(item.field)) setError(item.field as never, { type: 'server', message: item.message })
    else remaining.push(item.message)
  }
  return remaining.length ? `${error.problem.detail} ${remaining.join(' ')}` : error.problem.detail
}

export function EmployeeDetailPage() {
  const { id = '' } = useParams()
  const queryClient = useQueryClient()
  const [profileOpen, setProfileOpen] = useState(false)
  const [profileMessage, setProfileMessage] = useState<string | null>(null)
  const [profileStale, setProfileStale] = useState(false)
  const [profileVersion, setProfileVersion] = useState(0)
  const [salaryOpen, setSalaryOpen] = useState(false)
  const [salaryMessage, setSalaryMessage] = useState<string | null>(null)
  const [correctionRecord, setCorrectionRecord] = useState<SalaryRecord | null>(null)
  const [correctionMessage, setCorrectionMessage] = useState<string | null>(null)
  const [correctionStale, setCorrectionStale] = useState(false)
  const [correctionVersion, setCorrectionVersion] = useState(0)
  const [historyPage, setHistoryPage] = useState(1)

  const employeeQuery = useQuery({ queryKey: ['employee', id], queryFn: () => getEmployee(id), enabled: Boolean(id) })
  const historyQuery = useQuery({ queryKey: ['salary-history', id, historyPage], queryFn: () => getSalaryHistory(id, historyPage), enabled: Boolean(id) })
  const employee = employeeQuery.data

  const profileForm = useForm<EmployeeFormValues>({ resolver: zodResolver(employeeSchema), shouldFocusError: true })
  const salaryForm = useForm<SalaryFormValues>({ resolver: zodResolver(salarySchema), defaultValues: { annualBaseAmount: '', currencyCode: 'INR', effectiveDate: todayUtc() }, shouldFocusError: true })
  const correctionForm = useForm<CorrectionFormValues>({ resolver: zodResolver(correctionSchema), shouldFocusError: true })

  useEffect(() => {
    if (employee && profileOpen) {
      profileForm.reset({ employeeCode: employee.employeeCode, fullName: employee.fullName, countryCode: employee.countryCode, department: employee.department, jobTitle: employee.jobTitle, status: employee.status, annualBaseAmount: employee.currentSalary?.annualBaseAmount ?? '1', currencyCode: employee.currentSalary?.currencyCode ?? 'INR', effectiveDate: employee.currentSalary?.effectiveDate ?? todayUtc() })
      setProfileVersion(employee.version)
    }
  }, [employee, profileOpen, profileForm])

  const refreshEmployee = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['employee', id] }),
      queryClient.invalidateQueries({ queryKey: ['salary-history', id] }),
      queryClient.invalidateQueries({ queryKey: ['employees'] }),
    ])
  }

  const profileMutation = useMutation({
    mutationFn: (payload: Partial<Pick<Employee, 'fullName' | 'countryCode' | 'department' | 'jobTitle' | 'status'>> & { version: number }) => updateEmployee(id, payload),
    onSuccess: async () => { await refreshEmployee(); setProfileOpen(false); setProfileMessage(null); setProfileStale(false) },
    onError: (error: unknown) => {
      const fieldMessage = applyFieldErrors(error, profileForm.setError)
      if (error instanceof ApiError && error.status === 409) { setProfileStale(true); setProfileMessage('This profile changed after you opened it. Review the latest data before saving your draft again.'); return }
      setProfileMessage(fieldMessage ?? messageFor(error))
    },
  })
  const salaryMutation = useMutation({
    mutationFn: (values: SalaryFormValues) => addSalary(id, values),
    onSuccess: async () => { await refreshEmployee(); setSalaryOpen(false); setSalaryMessage(null); salaryForm.reset({ annualBaseAmount: '', currencyCode: 'INR', effectiveDate: todayUtc() }); setHistoryPage(1) },
    onError: (error: unknown) => {
      const fieldMessage = applyFieldErrors(error, salaryForm.setError)
      setSalaryMessage(error instanceof ApiError && error.status === 409 ? 'A salary record already exists for that effective date. Choose another date; your entries are saved.' : fieldMessage ?? messageFor(error))
    },
  })
  const correctionMutation = useMutation({
    mutationFn: (payload: Partial<Pick<SalaryRecord, 'annualBaseAmount' | 'currencyCode' | 'effectiveDate'>> & { version: number; reason: string }) => correctSalary(id, correctionRecord!.id, payload),
    onSuccess: async () => { await refreshEmployee(); setCorrectionRecord(null); setCorrectionMessage(null); setCorrectionStale(false) },
    onError: (error: unknown) => {
      const fieldMessage = applyFieldErrors(error, correctionForm.setError)
      if (error instanceof ApiError && error.status === 409) {
        setCorrectionStale(true)
        setCorrectionMessage(error.message.toLowerCase().includes('effective date') ? 'Another salary record already uses that effective date. Choose a different date; your entries are saved.' : 'This salary record changed after you opened it. Review the latest record before saving your correction again.')
        return
      }
      setCorrectionMessage(fieldMessage ?? messageFor(error))
    },
  })

  function openProfile() { setProfileMessage(null); setProfileStale(false); setProfileOpen(true) }
  function closeProfile() {
    if (profileForm.formState.isDirty && !window.confirm('Discard your unsaved profile changes?')) return
    setProfileOpen(false)
  }
  function closeSalary() {
    if (salaryForm.formState.isDirty && !window.confirm('Discard this salary record draft?')) return
    setSalaryOpen(false)
  }
  function closeCorrection() {
    if (correctionForm.formState.isDirty && !window.confirm('Discard your unsaved correction?')) return
    setCorrectionRecord(null)
  }
  function openCorrection(record: SalaryRecord) {
    setCorrectionRecord(record)
    setCorrectionVersion(record.version)
    setCorrectionStale(false)
    setCorrectionMessage(null)
    correctionForm.reset({ annualBaseAmount: record.annualBaseAmount, currencyCode: record.currencyCode, effectiveDate: record.effectiveDate, reason: '' })
  }
  async function reviewProfile() {
    const previous = employee
    const draft = profileForm.getValues()
    const latest = await queryClient.fetchQuery({ queryKey: ['employee', id], queryFn: () => getEmployee(id), staleTime: 0 })
    const rebased: EmployeeFormValues = {
      ...draft,
      employeeCode: latest.employeeCode,
      fullName: draft.fullName === previous?.fullName ? latest.fullName : draft.fullName,
      countryCode: draft.countryCode === previous?.countryCode ? latest.countryCode : draft.countryCode,
      department: draft.department === previous?.department ? latest.department : draft.department,
      jobTitle: draft.jobTitle === previous?.jobTitle ? latest.jobTitle : draft.jobTitle,
      status: draft.status === previous?.status ? latest.status : draft.status,
      annualBaseAmount: latest.currentSalary?.annualBaseAmount ?? '1',
      currencyCode: latest.currentSalary?.currencyCode ?? 'INR',
      effectiveDate: latest.currentSalary?.effectiveDate ?? todayUtc(),
    }
    profileForm.reset(rebased)
    setProfileVersion(latest.version)
    setProfileStale(false)
    setProfileMessage('Latest profile version loaded. Your draft is unchanged; save again to apply it.')
  }
  async function reviewCorrection() {
    const previous = correctionRecord
    const draft = correctionForm.getValues()
    const latest = await queryClient.fetchQuery({ queryKey: ['salary-history', id, historyPage], queryFn: () => getSalaryHistory(id, historyPage), staleTime: 0 })
    const latestRecord = latest.items.find((record) => record.id === correctionRecord?.id)
    if (latestRecord) {
      setCorrectionVersion(latestRecord.version)
      setCorrectionRecord(latestRecord)
      correctionForm.reset({
        annualBaseAmount: draft.annualBaseAmount === previous?.annualBaseAmount ? latestRecord.annualBaseAmount : draft.annualBaseAmount,
        currencyCode: draft.currencyCode === previous?.currencyCode ? latestRecord.currencyCode : draft.currencyCode,
        effectiveDate: draft.effectiveDate === previous?.effectiveDate ? latestRecord.effectiveDate : draft.effectiveDate,
        reason: draft.reason,
      })
      setCorrectionStale(false)
      setCorrectionMessage('Latest record version loaded. Your correction draft is unchanged; save again to apply it.')
    } else {
      setCorrectionMessage('This salary record is no longer on this page. Close this dialog and refresh the history.')
    }
  }
  function submitProfile(values: EmployeeFormValues) {
    if (!employee) return
    setProfileMessage(null)
    const fields = ['fullName', 'countryCode', 'department', 'jobTitle', 'status'] as const
    const changed = Object.fromEntries(fields.filter((field) => values[field] !== employee[field]).map((field) => [field, values[field]])) as Partial<Pick<Employee, typeof fields[number]>>
    if (Object.keys(changed).length === 0) return
    profileMutation.mutate({ ...changed, version: profileVersion })
  }
  function submitSalary(values: SalaryFormValues) { setSalaryMessage(null); salaryMutation.mutate(values) }
  function submitCorrection(values: CorrectionFormValues) {
    if (!correctionRecord) return
    const patch: Partial<SalaryRecord> & { version: number; reason: string } = { version: correctionVersion, reason: values.reason.trim() }
    if (values.annualBaseAmount !== correctionRecord.annualBaseAmount) patch.annualBaseAmount = values.annualBaseAmount
    if (values.currencyCode !== correctionRecord.currencyCode) patch.currencyCode = values.currencyCode
    if (values.effectiveDate !== correctionRecord.effectiveDate) patch.effectiveDate = values.effectiveDate
    if (Object.keys(patch).length === 2) { setCorrectionMessage('Change at least one salary value before submitting a correction.'); return }
    setCorrectionMessage(null)
    correctionMutation.mutate(patch)
  }

  const profileValues = profileForm.watch(['fullName', 'countryCode', 'department', 'jobTitle', 'status'])
  const profileChanged = useMemo(() => Boolean(employee && profileValues.some((value, index) => value !== [employee.fullName, employee.countryCode, employee.department, employee.jobTitle, employee.status][index])), [employee, profileValues])
  const correctionValues = correctionForm.watch(['annualBaseAmount', 'currencyCode', 'effectiveDate', 'reason'])
  const correctionChanged = Boolean(correctionRecord && (correctionValues[0] !== correctionRecord.annualBaseAmount || correctionValues[1] !== correctionRecord.currencyCode || correctionValues[2] !== correctionRecord.effectiveDate))
  const correctionCanSave = correctionChanged && Boolean(correctionValues[3]?.trim()) && !correctionStale

  if (employeeQuery.isPending) return <section><Skeleton className="detail-skeleton" label="Loading employee" /><Skeleton className="detail-skeleton" /><Skeleton className="detail-skeleton" /></section>
  if (employeeQuery.isError && employeeQuery.error instanceof ApiError && employeeQuery.error.status === 404) return <section><EmptyState title="Employee unavailable" description="This employee may have been removed or the link is no longer valid." action={<Link className="button button-secondary" to="/employees">Back to employees</Link>} /></section>
  if (employeeQuery.isError && !employee) return <section><Alert>{messageFor(employeeQuery.error)} <button className="text-button" onClick={() => void employeeQuery.refetch()}>Retry</button></Alert><Link className="back-link" to="/employees">Back to employees</Link></section>
  if (!employee) return null

  const history = historyQuery.data
  const historyPages = history ? Math.max(1, Math.ceil(history.total / history.size)) : 1
  const currentSalary = employee.currentSalary
  const salaryErrors = salaryForm.formState.errors
  const correctionErrors = correctionForm.formState.errors
  const profileErrors = profileForm.formState.errors

  return <section className="employee-detail-page">
    <Link className="back-link" to="/employees"><ArrowLeft size={16} /> Back to employees</Link>
    <PageHeader title={employee.fullName} description={`Employee code ${employee.employeeCode}`} actions={<Button onClick={openProfile}><Pencil size={15} /> Edit profile</Button>} />
    {employeeQuery.isError && <Alert tone="info">Showing saved employee information. The latest refresh failed. <button className="text-button" onClick={() => void employeeQuery.refetch()}>Retry</button></Alert>}
    <div className="employee-overview">
      <section className="profile-card" aria-labelledby="profile-title">
        <div className="card-heading"><div className="section-heading-icon"><UserRound size={18} /><h2 id="profile-title">Profile</h2></div><StatusBadge tone={employee.status === 'ACTIVE' ? 'positive' : 'neutral'}>{employee.status === 'ACTIVE' ? 'Active' : 'Inactive'}</StatusBadge></div>
        <dl className="profile-grid"><div><dt>Employee code</dt><dd>{employee.employeeCode}</dd></div><div><dt>Country</dt><dd>{countryName(employee.countryCode)} <span className="employee-code">{employee.countryCode}</span></dd></div><div><dt>Department</dt><dd>{employee.department}</dd></div><div><dt>Job title</dt><dd>{employee.jobTitle}</dd></div></dl>
        {employee.status === 'INACTIVE' && <p className="inactive-note">Inactive employees remain searchable and are excluded from compensation reports.</p>}
      </section>
      <section className="current-salary-card" aria-labelledby="current-salary-title">
        <div className="card-heading"><div className="section-heading-icon"><BadgeDollarSign size={19} /><h2 id="current-salary-title">Current annual base salary</h2></div><Button variant="quiet" onClick={() => { setSalaryMessage(null); setSalaryOpen(true) }}><Plus size={15} /> Add salary</Button></div>
        {currentSalary ? <><p className="current-salary-value">{formatMoney(currentSalary.annualBaseAmount, currentSalary.currencyCode)}</p><p className="current-salary-date"><CalendarDays size={15} /> Effective {currentSalary.effectiveDate}</p></> : <div className="salary-not-recorded"><p>No salary has been recorded.</p><Button variant="secondary" onClick={() => { setSalaryMessage(null); setSalaryOpen(true) }}><Plus size={15} /> Add salary</Button></div>}
      </section>
    </div>

    <section className="salary-history-section" aria-labelledby="salary-history-title">
      <div className="history-heading"><div><h2 id="salary-history-title">Salary history</h2><p>Past records are preserved when a new salary is added.</p></div><span className="history-count">{history ? `${history.total} ${history.total === 1 ? 'record' : 'records'}` : ' '}</span></div>
      {historyQuery.isPending && <div className="history-loading"><Skeleton /><Skeleton /><Skeleton /></div>}
      {historyQuery.isError && !history && <Alert>{messageFor(historyQuery.error)} <button className="text-button" onClick={() => void historyQuery.refetch()}>Retry</button></Alert>}
      {historyQuery.isError && history && <Alert tone="info">Showing saved salary history. The latest refresh failed. <button className="text-button" onClick={() => void historyQuery.refetch()}>Retry</button></Alert>}
      {history && history.items.length === 0 && <EmptyState title="No salary records" description="Add an annual base salary to start the employee’s salary history." />}
      {history && history.items.length > 0 && <div className="history-list">{history.items.map((record) => <article className="history-row" key={record.id}>
        <div className="history-icon"><BadgeDollarSign size={18} /></div><div className="history-main"><strong>{formatMoney(record.annualBaseAmount, record.currencyCode)}</strong><span>Effective {record.effectiveDate}{record.id === currentSalary?.id ? <em className="current-label">Current</em> : null}</span></div><Button variant="quiet" onClick={() => openCorrection(record)}><Pencil size={15} /> Correct</Button>
      </article>)}</div>}
      {history && history.total > history.size && <div className="history-pagination"><span>Page {history.page} of {historyPages}</span><div><button type="button" className="icon-button" aria-label="Previous salary history page" disabled={historyPage <= 1} onClick={() => setHistoryPage((page) => page - 1)}><ChevronLeft size={18} /></button><button type="button" className="icon-button" aria-label="Next salary history page" disabled={historyPage >= historyPages} onClick={() => setHistoryPage((page) => page + 1)}><ChevronRight size={18} /></button></div></div>}
    </section>

    <Dialog open={profileOpen} title="Edit employee profile" description="Employee code is permanent. Changes are saved to the employee profile." onClose={closeProfile}>
      {profileMessage && <Alert tone={profileStale ? 'error' : 'info'}>{profileMessage}{profileStale && <Button variant="quiet" onClick={() => void reviewProfile()}>Review latest data</Button>}</Alert>}
      <form className="dialog-form" onSubmit={profileForm.handleSubmit(submitProfile)} noValidate>
        <label className="field"><span>Employee code</span><input value={employee.employeeCode} disabled /></label>
        <label className="field"><span>Full name</span><input {...profileForm.register('fullName')} maxLength={200} aria-invalid={Boolean(profileErrors.fullName)} />{profileErrors.fullName && <span className="field-error">{profileErrors.fullName.message}</span>}</label>
        <label className="field"><span>Country</span><select {...profileForm.register('countryCode')}>{countries.map((country) => <option value={country.code} key={country.code}>{country.name} ({country.code})</option>)}</select></label>
        <label className="field"><span>Department</span><input {...profileForm.register('department')} maxLength={120} aria-invalid={Boolean(profileErrors.department)} />{profileErrors.department && <span className="field-error">{profileErrors.department.message}</span>}</label>
        <label className="field"><span>Job title</span><input {...profileForm.register('jobTitle')} maxLength={120} aria-invalid={Boolean(profileErrors.jobTitle)} />{profileErrors.jobTitle && <span className="field-error">{profileErrors.jobTitle.message}</span>}</label>
        <label className="field"><span>Employment status</span><select {...profileForm.register('status')}><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option></select></label>
        <p className="field-hint">Inactive employees remain searchable and are excluded from compensation reports.</p>
        <div className="dialog-actions"><Button type="button" onClick={closeProfile}>Cancel</Button><Button type="submit" variant="primary" busy={profileMutation.isPending} disabled={!profileChanged || profileStale}>Save changes</Button></div>
      </form>
    </Dialog>

    <Dialog open={salaryOpen} title="Add salary record" description="This creates a dated salary record and preserves earlier records." onClose={closeSalary}>
      {salaryMessage && <Alert>{salaryMessage}</Alert>}
      <form className="dialog-form" onSubmit={salaryForm.handleSubmit(submitSalary)} noValidate>
        <label className="field"><span>Annual base salary</span><input {...salaryForm.register('annualBaseAmount')} inputMode="decimal" maxLength={40} placeholder="e.g. 850000" aria-invalid={Boolean(salaryErrors.annualBaseAmount)} />{salaryErrors.annualBaseAmount && <span className="field-error">{salaryErrors.annualBaseAmount.message}</span>}</label>
        <label className="field"><span>Currency</span><select {...salaryForm.register('currencyCode')}>{currencies.map((currency) => <option value={currency.code} key={currency.code}>{currency.code} · {currency.name}</option>)}</select></label>
        <label className="field"><span>Effective date</span><input {...salaryForm.register('effectiveDate')} type="date" max={todayUtc()} aria-invalid={Boolean(salaryErrors.effectiveDate)} />{salaryErrors.effectiveDate && <span className="field-error">{salaryErrors.effectiveDate.message}</span>}</label>
        <div className="dialog-actions"><Button type="button" onClick={closeSalary}>Cancel</Button><Button type="submit" variant="primary" busy={salaryMutation.isPending}>Add salary</Button></div>
      </form>
    </Dialog>

    <Dialog open={Boolean(correctionRecord)} title="Correct salary record" description="Corrections are audited. Enter a reason and change at least one value." onClose={closeCorrection}>
      {correctionMessage && <Alert tone={correctionStale ? 'error' : 'info'}>{correctionMessage}{correctionStale && !correctionMessage.startsWith('Another') && <Button variant="quiet" onClick={() => void reviewCorrection()}>Review latest record</Button>}</Alert>}
      <form className="dialog-form" onSubmit={correctionForm.handleSubmit(submitCorrection)} noValidate>
        <label className="field"><span>Annual base salary</span><input {...correctionForm.register('annualBaseAmount')} inputMode="decimal" maxLength={40} aria-invalid={Boolean(correctionErrors.annualBaseAmount)} />{correctionErrors.annualBaseAmount && <span className="field-error">{correctionErrors.annualBaseAmount.message}</span>}</label>
        <label className="field"><span>Currency</span><select {...correctionForm.register('currencyCode')}>{currencies.map((currency) => <option value={currency.code} key={currency.code}>{currency.code} · {currency.name}</option>)}</select></label>
        <label className="field"><span>Effective date</span><input {...correctionForm.register('effectiveDate')} type="date" max={todayUtc()} aria-invalid={Boolean(correctionErrors.effectiveDate)} />{correctionErrors.effectiveDate && <span className="field-error">{correctionErrors.effectiveDate.message}</span>}</label>
        <label className="field"><span>Correction reason</span><textarea {...correctionForm.register('reason')} maxLength={500} rows={3} placeholder="Explain why this correction is needed" aria-invalid={Boolean(correctionErrors.reason)} />{correctionErrors.reason && <span className="field-error">{correctionErrors.reason.message}</span>}</label>
        <div className="dialog-actions"><Button type="button" onClick={closeCorrection}>Cancel</Button><Button type="submit" variant="primary" busy={correctionMutation.isPending} disabled={!correctionCanSave}>Save correction</Button></div>
      </form>
    </Dialog>
  </section>
}
