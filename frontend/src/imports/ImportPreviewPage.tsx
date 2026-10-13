import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertTriangle, ArrowLeft, CheckCircle2, ChevronLeft, ChevronRight, Clock3, FileUp, Info } from 'lucide-react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { formatMoney } from '../api/types'
import type { ImportDetails, ImportRow } from '../api/types'
import { Alert, Button, Dialog, EmptyState, PageHeader, Skeleton, StatusBadge, TableFrame } from '../components/primitives'
import { getImportDetails, commitImport } from './importApi'

function errorMessage(error: unknown): string {
  if (error instanceof ApiError) return error.problem.detail
  return error instanceof Error ? error.message : 'Import details could not be loaded.'
}

function positiveInt(value: string | null, fallback: number): number {
  const parsed = Number(value)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback
}

function dateTime(value: string): string {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(date)
}

function ActionBadge({ action }: { action: ImportRow['action'] }) {
  const tone = action === 'CREATE' ? 'positive' : action === 'UPDATE' ? 'warning' : action === 'INVALID' ? 'danger' : 'neutral'
  const label = action === 'NO_OP' ? 'No change' : action[0] + action.slice(1).toLowerCase()
  return <StatusBadge tone={tone}>{label}</StatusBadge>
}

function ImportCounts({ batch }: { batch: ImportDetails['batch'] }) {
  return <div className="import-count-grid" aria-label="Import preview counts">
    <div className="import-count-total"><span>Total rows</span><strong>{batch.totalRowCount.toLocaleString()}</strong></div>
    <div><span>Create</span><strong>{batch.createCount.toLocaleString()}</strong></div>
    <div><span>Update</span><strong>{batch.updateCount.toLocaleString()}</strong></div>
    <div><span>No change</span><strong>{batch.noOpCount.toLocaleString()}</strong></div>
    <div className={batch.invalidCount > 0 ? 'import-count-invalid' : ''}><span>Invalid</span><strong>{batch.invalidCount.toLocaleString()}</strong></div>
  </div>
}

export function ImportPreviewPage() {
  const { id = '' } = useParams()
  const [params, setParams] = useSearchParams()
  const queryClient = useQueryClient()
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [commitMessage, setCommitMessage] = useState<string | null>(null)
  const [stalePreview, setStalePreview] = useState(false)
  const [commitUnavailable, setCommitUnavailable] = useState(false)
  const [uncertainCommit, setUncertainCommit] = useState(false)
  const page = positiveInt(params.get('page'), 1)
  const sizeCandidate = positiveInt(params.get('size'), 25)
  const size = [25, 50, 100].includes(sizeCandidate) ? sizeCandidate : 25
  const detailKey = ['import-details', id, page, size]
  const detailsQuery = useQuery({ queryKey: detailKey, queryFn: () => getImportDetails(id, page, size), enabled: Boolean(id), staleTime: 0 })
  const details = detailsQuery.data
  const batch = details?.batch

  const commitMutation = useMutation({
    mutationFn: () => commitImport(id),
    onSuccess: async (summary) => {
      setConfirmOpen(false)
      setCommitMessage('Import committed. The saved counts below are the final result.')
      setStalePreview(false)
      setUncertainCommit(false)
      queryClient.setQueryData<ImportDetails>(detailKey, (current) => current ? { ...current, batch: summary } : current)
      await queryClient.invalidateQueries({ queryKey: ['import-details', id] })
    },
    onError: async (error: unknown) => {
      setConfirmOpen(false)
      if (error instanceof ApiError && error.status === 409) {
        setStalePreview(true)
        setCommitMessage('Employee data changed after this preview. This preview cannot be committed. Upload a fresh CSV to review the latest data.')
        return
      }
      if (error instanceof ApiError && error.status === 410) {
        setCommitMessage('This preview has expired. Upload a fresh CSV to continue.')
        await queryClient.invalidateQueries({ queryKey: ['import-details', id] })
        return
      }
      if (error instanceof ApiError && error.status === 403) {
        setCommitUnavailable(true)
        setCommitMessage('You do not have permission to commit this import.')
        return
      }
      setUncertainCommit(true)
      setCommitMessage('We could not confirm the commit response. Checking the saved batch status before enabling another commit.')
      try {
        const latest = await queryClient.fetchQuery({ queryKey: detailKey, queryFn: () => getImportDetails(id, page, size), staleTime: 0 })
        setUncertainCommit(false)
        setCommitMessage(latest.batch.status === 'COMMITTED' ? 'The import was committed. The saved counts below are the final result.' : 'The batch is still awaiting commit. Review the saved preview status before trying again.')
      } catch (refreshError) {
        if (refreshError instanceof ApiError && refreshError.status === 410) {
          setUncertainCommit(false)
          setCommitMessage('This preview has expired. Upload a fresh CSV to continue.')
        } else {
          setCommitMessage('The commit result is still unknown because the batch status could not be checked. Retry the status check before committing again.')
        }
      }
    },
  })

  function updateParams(nextPage: number, nextSize = size) {
    const next = new URLSearchParams(params)
    next.set('page', String(nextPage))
    next.set('size', String(nextSize))
    setParams(next)
  }

  async function checkStatus() {
    setUncertainCommit(true)
    try {
      const latest = await queryClient.fetchQuery({ queryKey: detailKey, queryFn: () => getImportDetails(id, page, size), staleTime: 0 })
      setUncertainCommit(false)
      setCommitMessage(latest.batch.status === 'COMMITTED' ? 'The import was committed. The saved counts below are the final result.' : 'No commit is recorded yet. Review the preview before trying again.')
    } catch (error) {
      setUncertainCommit(false)
      if (error instanceof ApiError && error.status === 410) setCommitMessage('This preview has expired. Upload a fresh CSV to continue.')
      else setCommitMessage(errorMessage(error))
    }
  }

  const totalPages = details ? Math.max(1, Math.ceil(details.rows.total / details.rows.size)) : 1
  const expired = detailsQuery.error instanceof ApiError && detailsQuery.error.status === 410
  const notFound = detailsQuery.error instanceof ApiError && detailsQuery.error.status === 404
  const canCommit = batch?.status === 'PREVIEWED' && batch.invalidCount === 0 && !stalePreview && !commitUnavailable && !uncertainCommit

  if (detailsQuery.isPending) return <section><PageHeader title="Import preview" description="Loading saved preview…" /><div className="import-detail-skeleton"><Skeleton /><Skeleton /><Skeleton /></div></section>
  if (notFound) return <EmptyState title="Import preview not found" description="This preview may have expired from storage or may not belong to your HR account." action={<Link className="button button-primary" to="/imports"><FileUp size={16} /> Upload a CSV</Link>} />
  if (expired) return <section className="import-expired-state"><div className="import-state-icon import-state-warning"><Clock3 size={24} /></div><p className="eyebrow">PREVIEW EXPIRED</p><h1>This preview is no longer available</h1><p>Uncommitted previews expire after 24 hours. Upload a fresh CSV to review the current employee data.</p><Link className="button button-primary" to="/imports"><FileUp size={16} /> Upload fresh CSV</Link></section>
  if (detailsQuery.isError && !details) return <section><Alert>{errorMessage(detailsQuery.error)} <button type="button" className="text-button" onClick={() => void detailsQuery.refetch()}>Retry</button></Alert><Link className="back-link" to="/imports"><ArrowLeft size={16} /> Back to import</Link></section>
  if (!details || !batch) return null

  return <section className="import-preview-page">
    <Link className="back-link" to="/imports"><ArrowLeft size={16} /> Back to import</Link>
    <PageHeader title={batch.status === 'COMMITTED' ? 'Import result' : 'Review import preview'} description={`Preview created ${dateTime(batch.createdAt)}`} actions={<StatusBadge tone={batch.status === 'COMMITTED' ? 'positive' : 'warning'}>{batch.status === 'COMMITTED' ? 'Committed' : 'Preview only'}</StatusBadge>} />
    {detailsQuery.isError && <Alert tone="info">Showing saved preview data. The latest refresh failed. <button type="button" className="text-button" onClick={() => void detailsQuery.refetch()}>Retry</button></Alert>}
    {commitMessage && <Alert tone={batch.status === 'COMMITTED' ? 'success' : stalePreview || commitUnavailable ? 'error' : 'info'}><span>{commitMessage}</span>{stalePreview && <Link className="text-button" to="/imports">Upload fresh CSV</Link>}{uncertainCommit && <button type="button" className="text-button" onClick={() => void checkStatus()}>Check batch status</button>}</Alert>}

    <ImportCounts batch={batch} />
    <div className="preview-meta"><span><Clock3 size={15} /> {batch.status === 'COMMITTED' ? `Committed ${batch.committedAt ? dateTime(batch.committedAt) : ''}` : `Expires ${dateTime(batch.expiresAt)}`}</span><span>Batch ID <code>{batch.id}</code></span></div>

    {batch.status === 'COMMITTED' && <div className="import-committed-banner"><CheckCircle2 size={18} /><div><strong>Import applied</strong><p>These saved counts are the final result. Reopening or committing this batch again will not apply its rows a second time.</p></div></div>}
    {batch.status === 'PREVIEWED' && batch.invalidCount > 0 && <Alert><AlertTriangle size={17} /><span>{batch.invalidCount.toLocaleString()} invalid {batch.invalidCount === 1 ? 'row needs' : 'rows need'} correction. Update the source CSV and upload it again; invalid rows cannot be committed.</span></Alert>}
    {batch.status === 'PREVIEWED' && details.rows.total === 0 && <Alert tone="info"><Info size={17} /> The staged row details have been cleaned up. Create a fresh preview if you need to review or commit the file.</Alert>}

    {details.rows.items.length > 0 && <>
      <div className="import-rows-heading"><div><h2>Proposed changes</h2><p>Values are normalized as the application will store them. Row numbers match the source CSV.</p></div><span>{details.rows.total.toLocaleString()} rows</span></div>
      <TableFrame label="CSV import preview rows"><table className="import-preview-table">
        <thead><tr><th scope="col">CSV row</th><th scope="col">Action</th><th scope="col">Employee code</th><th scope="col">Name</th><th scope="col">Country</th><th scope="col">Department</th><th scope="col">Job title</th><th scope="col">Status</th><th scope="col">Annual salary</th><th scope="col">Effective date</th><th scope="col">Validation</th></tr></thead>
        <tbody>{details.rows.items.map((row) => <tr key={row.rowNumber} className={row.action === 'INVALID' ? 'import-row-invalid' : undefined}>
          <td>{row.rowNumber}</td><td><ActionBadge action={row.action} /></td><td><code>{row.employeeCode || '—'}</code></td><td>{row.fullName || '—'}</td><td>{row.countryCode || '—'}</td><td>{row.department || '—'}</td><td>{row.jobTitle || '—'}</td><td>{row.status || '—'}</td><td>{row.annualBaseAmount && row.currencyCode ? row.action === 'INVALID' ? `${row.currencyCode} ${row.annualBaseAmount}` : formatMoney(row.annualBaseAmount, row.currencyCode) : '—'}</td><td>{row.effectiveDate || '—'}</td>
          <td>{row.errors.length ? <details className="row-errors"><summary>{row.errors.length} {row.errors.length === 1 ? 'issue' : 'issues'}</summary><ul>{row.errors.map((error, index) => <li key={`${error.field}-${index}`}><strong>{error.field}:</strong> {error.message}</li>)}</ul></details> : <span className="valid-row-label">Valid</span>}</td>
        </tr>)}</tbody>
      </table></TableFrame>
      <div className="import-pagination"><label>Rows per page <select value={size} onChange={(event) => updateParams(1, Number(event.target.value))}><option value={25}>25</option><option value={50}>50</option><option value={100}>100</option></select></label><span>Page {details.rows.page} of {totalPages}</span><div><button type="button" className="icon-button" aria-label="Previous import rows" disabled={page <= 1 || detailsQuery.isFetching} onClick={() => updateParams(page - 1)}><ChevronLeft size={18} /></button><button type="button" className="icon-button" aria-label="Next import rows" disabled={page >= totalPages || detailsQuery.isFetching} onClick={() => updateParams(page + 1)}><ChevronRight size={18} /></button></div></div>
    </>}

    {batch.status === 'COMMITTED' && <div className="import-next-actions"><Link className="button button-primary" to="/employees">Go to employees</Link><Link className="button button-secondary" to="/reports">View reports</Link></div>}
    {batch.status === 'PREVIEWED' && <div className="import-commit-bar"><div><strong>{stalePreview ? 'Preview is out of date' : batch.invalidCount === 0 ? 'Ready to commit' : 'Commit unavailable'}</strong><span>{stalePreview ? 'Employee data changed after preview. Upload a fresh CSV to continue.' : batch.invalidCount === 0 ? 'All rows are valid. Review the proposed changes before applying them.' : 'Correct invalid rows in the source file and create a fresh preview.'}</span></div>{stalePreview ? <Link className="button button-primary" to="/imports"><FileUp size={16} /> Upload fresh CSV</Link> : <Button variant="primary" disabled={!canCommit} busy={commitMutation.isPending} onClick={() => { setCommitMessage(null); setConfirmOpen(true) }}>Review and commit</Button>}</div>}

    <Dialog open={confirmOpen} title="Commit this import?" description="This applies every valid row in one transaction. If any row cannot be applied, no rows are changed." onClose={() => setConfirmOpen(false)}>
      <div className="commit-confirm-content"><p>Confirm the changes in this preview:</p><ul><li><strong>{batch.createCount.toLocaleString()}</strong> employees created</li><li><strong>{batch.updateCount.toLocaleString()}</strong> employees updated</li><li><strong>{batch.noOpCount.toLocaleString()}</strong> unchanged</li></ul><p className="commit-confirm-note">This action updates employee data. Invalid rows block commit. Review the preview before continuing.</p></div>
      <div className="dialog-actions"><Button type="button" onClick={() => setConfirmOpen(false)}>Go back</Button><Button variant="primary" busy={commitMutation.isPending} onClick={() => commitMutation.mutate()}>Commit import</Button></div>
    </Dialog>
  </section>
}
