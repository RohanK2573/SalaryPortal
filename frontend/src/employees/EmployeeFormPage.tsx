import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { ArrowLeft, ArrowRight, BadgeDollarSign } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { Alert, Button, PageHeader } from '../components/primitives'
import { countries, currencies, todayUtc } from './options'
import { createEmployee } from './employeeApi'
import { employeeSchema } from './validation'
import type { EmployeeFormValues } from './validation'

export function EmployeeFormPage() {
  const navigate = useNavigate()
  const [message, setMessage] = useState<string | null>(null)
  const form = useForm<EmployeeFormValues>({
    resolver: zodResolver(employeeSchema),
    defaultValues: { employeeCode: '', fullName: '', countryCode: 'IN', department: '', jobTitle: '', status: 'ACTIVE', annualBaseAmount: '', currencyCode: 'INR', effectiveDate: todayUtc() },
    shouldFocusError: true,
  })
  const mutation = useMutation({
    mutationFn: createEmployee,
    onSuccess: (employee) => navigate(`/employees/${employee.id}`, { replace: true }),
    onError: (error: unknown) => {
      setMessage(null)
      if (error instanceof ApiError) {
        if (error.status === 409) {
          setMessage('That employee code is already in use. Choose a different code; your other entries are saved.')
          return
        }
        if (error.status === 422) {
          const unmapped: string[] = []
          for (const fieldError of error.problem.errors ?? []) {
            const field = fieldError.field as keyof EmployeeFormValues
            if (field in form.getValues()) form.setError(field, { type: 'server', message: fieldError.message })
            else unmapped.push(fieldError.message)
          }
          setMessage(unmapped.length ? `${error.problem.detail} ${unmapped.join(' ')}` : error.problem.detail)
          return
        }
        setMessage(error.status === 403 ? 'You do not have permission to add employees.' : error.message)
        return
      }
      setMessage(error instanceof Error ? error.message : 'Employee could not be created. Try again.')
    },
  })

  function submit(values: EmployeeFormValues) {
    setMessage(null)
    mutation.mutate({ ...values, employeeCode: values.employeeCode.toUpperCase() })
  }

  const errors = form.formState.errors
  return <section className="employee-form-page">
    <Link className="back-link" to="/employees"><ArrowLeft size={16} /> Back to employees</Link>
    <PageHeader title="Add employee" description="Create an employee profile and record their starting annual base salary." />
    {message && <Alert>{message}</Alert>}
    <form className="employee-form-card" onSubmit={form.handleSubmit(submit)} noValidate>
      <section className="form-section" aria-labelledby="employee-form-details-title">
        <div className="form-section-heading"><div><h2 id="employee-form-details-title">Employee details</h2><p>Employee code is a permanent identifier and cannot be changed later.</p></div><span className="step-number">01</span></div>
        <div className="form-grid">
          <label className="field"><span>Employee code</span><input {...form.register('employeeCode')} placeholder="e.g. EMP-1042" autoCapitalize="characters" maxLength={64} aria-invalid={Boolean(errors.employeeCode)} />{errors.employeeCode && <span className="field-error">{errors.employeeCode.message}</span>}</label>
          <label className="field"><span>Full name</span><input {...form.register('fullName')} placeholder="Employee full name" maxLength={200} aria-invalid={Boolean(errors.fullName)} />{errors.fullName && <span className="field-error">{errors.fullName.message}</span>}</label>
          <label className="field"><span>Country</span><select {...form.register('countryCode')} aria-invalid={Boolean(errors.countryCode)}>{countries.map((country) => <option value={country.code} key={country.code}>{country.name} ({country.code})</option>)}</select>{errors.countryCode && <span className="field-error">{errors.countryCode.message}</span>}</label>
          <label className="field"><span>Department</span><input {...form.register('department')} placeholder="e.g. Engineering" maxLength={120} aria-invalid={Boolean(errors.department)} />{errors.department && <span className="field-error">{errors.department.message}</span>}</label>
          <label className="field"><span>Job title</span><input {...form.register('jobTitle')} placeholder="e.g. Software Engineer" maxLength={120} aria-invalid={Boolean(errors.jobTitle)} />{errors.jobTitle && <span className="field-error">{errors.jobTitle.message}</span>}</label>
          <label className="field"><span>Employment status</span><select {...form.register('status')}><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option></select></label>
        </div>
      </section>
      <section className="form-section salary-form-section" aria-labelledby="employee-form-salary-title">
        <div className="form-section-heading"><div className="section-heading-icon"><BadgeDollarSign size={18} /><div><h2 id="employee-form-salary-title">Initial salary</h2><p>Enter gross annual base salary in the employee’s local currency.</p></div></div><span className="step-number">02</span></div>
        <div className="form-grid form-grid-salary">
          <label className="field"><span>Annual base salary</span><input {...form.register('annualBaseAmount')} inputMode="decimal" placeholder="e.g. 850000" maxLength={40} aria-describedby="salary-format-help" aria-invalid={Boolean(errors.annualBaseAmount)} />{errors.annualBaseAmount && <span className="field-error">{errors.annualBaseAmount.message}</span>}<span className="field-hint" id="salary-format-help">Positive amount; up to four decimal places.</span></label>
          <label className="field"><span>Currency</span><select {...form.register('currencyCode')}>{currencies.map((currency) => <option value={currency.code} key={currency.code}>{currency.code} · {currency.name}</option>)}</select></label>
          <label className="field"><span>Effective date</span><input {...form.register('effectiveDate')} type="date" max={todayUtc()} aria-invalid={Boolean(errors.effectiveDate)} />{errors.effectiveDate && <span className="field-error">{errors.effectiveDate.message}</span>}<span className="field-hint">Cannot be later than today.</span></label>
        </div>
      </section>
      <div className="employee-form-actions"><Link className="button button-secondary" to="/employees">Cancel</Link><Button type="submit" variant="primary" busy={mutation.isPending}>Create employee <ArrowRight size={16} /></Button></div>
    </form>
  </section>
}
