import { useRef, useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { ArrowRight, Check, Download, FileSpreadsheet, Info, UploadCloud } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { Alert, Button, PageHeader } from '../components/primitives'
import { importTemplate, previewCsv } from './importApi'

const maxFileBytes = 10 * 1024 * 1024

function uploadError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 413) return 'This file exceeds the 10 MiB limit. Choose a smaller CSV file.'
    if (error.status === 422) {
      const fields = error.problem.errors?.map((item) => item.message).filter(Boolean) ?? []
      return fields.length ? `${error.problem.detail} ${fields.join(' ')}` : error.problem.detail
    }
    if (error.status === 403) return 'You do not have permission to preview imports.'
    return error.problem.detail
  }
  return error instanceof Error ? error.message : 'The CSV preview could not be created.'
}

export function ImportUploadPage() {
  const navigate = useNavigate()
  const fileInput = useRef<HTMLInputElement>(null)
  const [file, setFile] = useState<File | null>(null)
  const [fileMessage, setFileMessage] = useState<string | null>(null)
  const [templateMessage, setTemplateMessage] = useState<string | null>(null)
  const [templateBusy, setTemplateBusy] = useState(false)
  const previewMutation = useMutation({
    mutationFn: previewCsv,
    onSuccess: (batch) => navigate(`/imports/${batch.id}`),
  })

  function chooseFile(next: File | null) {
    setFileMessage(null)
    previewMutation.reset()
    if (!next) { setFile(null); return }
    if (!next.name.toLowerCase().endsWith('.csv')) {
      setFile(null)
      setFileMessage('Choose a .csv file. Excel files are not supported.')
      if (fileInput.current) fileInput.current.value = ''
      return
    }
    if (next.size > maxFileBytes) {
      setFile(null)
      setFileMessage('This file exceeds the 10 MiB limit. Choose a smaller CSV file.')
      if (fileInput.current) fileInput.current.value = ''
      return
    }
    if (next.size === 0) {
      setFile(null)
      setFileMessage('This file is empty. Choose a CSV with a header and at least one data row.')
      if (fileInput.current) fileInput.current.value = ''
      return
    }
    setFile(next)
  }

  async function downloadTemplate() {
    setTemplateBusy(true)
    setTemplateMessage(null)
    try { await importTemplate() } catch (error) { setTemplateMessage(uploadError(error)) } finally { setTemplateBusy(false) }
  }

  async function startPreview() {
    if (!file) return
    try { await previewMutation.mutateAsync(file) } catch { /* The mutation state below presents the error. */ }
  }

  const mutationError = previewMutation.error
  return <section className="import-upload-page">
    <PageHeader title="Import employees" description="Review proposed employee changes before applying them." actions={<Button onClick={() => void downloadTemplate()} busy={templateBusy}><Download size={16} /> Download CSV template</Button>} />
    <div className="import-purpose-note"><Info size={17} /><p>Previewing a CSV does not change employee data. You’ll review every proposed row before choosing to commit the valid file.</p></div>
    {(templateMessage || fileMessage || mutationError) && <Alert><span>{templateMessage ?? fileMessage ?? uploadError(mutationError)}</span>{mutationError && <button type="button" className="text-button" onClick={() => void startPreview()}>Retry preview</button>}</Alert>}

    <div className="import-steps" aria-label="Import steps"><span className="import-step import-step-active"><b>1</b> Select file</span><span className="import-step-line" /><span className="import-step"><b>2</b> Review preview</span><span className="import-step-line" /><span className="import-step"><b>3</b> Commit changes</span></div>

    <section className="upload-card" aria-labelledby="upload-title">
      <div className="upload-icon"><FileSpreadsheet size={25} /></div>
      <h2 id="upload-title">Choose a CSV file</h2>
      <p>Use the template to prepare employee and salary records.</p>
      <label className="file-picker" htmlFor="employee-csv-file"><UploadCloud size={18} /> Browse files<input ref={fileInput} id="employee-csv-file" type="file" accept=".csv,text/csv" onChange={(event) => chooseFile(event.target.files?.[0] ?? null)} /></label>
      {file ? <div className="selected-file"><FileSpreadsheet size={17} /><span><strong>{file.name}</strong><small>{(file.size / 1024).toFixed(1)} KB</small></span><Check size={16} /></div> : <span className="file-requirements">CSV format · UTF-8 · up to 10 MiB</span>}
      <Button variant="primary" className="preview-button" busy={previewMutation.isPending} disabled={!file} onClick={() => void startPreview()}>Create preview <ArrowRight size={16} /></Button>
    </section>

    <section className="csv-format-card" aria-labelledby="csv-format-title">
      <div><h2 id="csv-format-title">CSV format</h2><p>Include one header row followed by 1–10,000 employee rows. Column order can vary; use each column once.</p></div>
      <div className="csv-columns" aria-label="Required CSV columns">{['employee_code', 'full_name', 'country_code', 'department', 'job_title', 'status', 'annual_base_amount', 'currency_code', 'effective_date'].map((column) => <code key={column}>{column}</code>)}</div>
      <p className="csv-format-note">Save the file as UTF-8 CSV. Use plain decimal amounts (up to four decimal places) and dates in YYYY-MM-DD format.</p>
    </section>
  </section>
}
