import { useEffect, useRef } from 'react'
import type { ButtonHTMLAttributes, HTMLAttributes, PropsWithChildren, ReactNode } from 'react'
import { X } from 'lucide-react'

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'quiet'
  busy?: boolean
}

export function Button({ variant = 'secondary', busy = false, disabled, children, ...props }: ButtonProps) {
  return <button {...props} className={`button button-${variant}${props.className ? ` ${props.className}` : ''}`} disabled={disabled || busy} aria-busy={busy || undefined}>{busy ? 'Please wait…' : children}</button>
}

export function Alert({ tone = 'error', children, ...props }: PropsWithChildren<HTMLAttributes<HTMLDivElement> & { tone?: 'error' | 'success' | 'info' }>) {
  return <div {...props} className={`alert alert-${tone}${props.className ? ` ${props.className}` : ''}`} role={tone === 'error' ? 'alert' : 'status'}>{children}</div>
}

export function StatusBadge({ children, tone = 'neutral' }: PropsWithChildren<{ tone?: 'neutral' | 'positive' | 'warning' | 'danger' }>) {
  return <span className={`status-badge status-badge-${tone}`}>{children}</span>
}

export function PageHeader({ title, description, actions }: { title: string; description?: string; actions?: ReactNode }) {
  return <header className="page-header"><div><h1>{title}</h1>{description && <p>{description}</p>}</div>{actions && <div className="page-header-actions">{actions}</div>}</header>
}

export function EmptyState({ title, description, action }: { title: string; description: string; action?: ReactNode }) {
  return <section className="empty-state"><h2>{title}</h2><p>{description}</p>{action}</section>
}

export function Skeleton({ className = '', label = 'Loading' }: { className?: string; label?: string }) {
  return <div className={`skeleton ${className}`} role="status" aria-label={label} />
}

export function Dialog({ open, title, description, onClose, children }: PropsWithChildren<{ open: boolean; title: string; description?: string; onClose: () => void }>) {
  const dialogRef = useRef<HTMLDialogElement>(null)
  const titleId = `dialog-title-${title.toLowerCase().replace(/[^a-z0-9]+/g, '-')}`
  const descriptionId = description ? `${titleId}-description` : undefined

  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])

  return <dialog ref={dialogRef} className="dialog" aria-labelledby={titleId} aria-describedby={descriptionId} onCancel={(event) => { event.preventDefault(); onClose() }} onClose={onClose}>
    <div className="dialog-heading"><div><h2 id={titleId}>{title}</h2>{description && <p id={descriptionId}>{description}</p>}</div><button className="icon-button" type="button" aria-label="Close dialog" onClick={onClose}><X size={19} /></button></div>
    <div className="dialog-content">{children}</div>
  </dialog>
}

export function TableFrame({ children, label }: PropsWithChildren<{ label: string }>) {
  return <div className="table-frame" role="region" aria-label={label} tabIndex={0}>{children}</div>
}
