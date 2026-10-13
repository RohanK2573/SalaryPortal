import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { ArrowRight, BadgeDollarSign, BriefcaseBusiness, CircleHelp, FileUp, LockKeyhole, Menu, Users, X } from 'lucide-react'
import { Link, Navigate, NavLink, Outlet, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthProvider'
import { EmployeeDirectoryPage } from '../employees/EmployeeDirectoryPage'
import { EmployeeFormPage } from '../employees/EmployeeFormPage'
import { EmployeeDetailPage } from '../employees/EmployeeDetailPage'
import { ReportsPage } from '../reports/ReportsPage'
import { ImportUploadPage } from '../imports/ImportUploadPage'
import { ImportPreviewPage } from '../imports/ImportPreviewPage'

const signInSchema = z.object({
  email: z.string().trim().email('Enter a valid email address.').max(254),
  password: z.string().min(1, 'Enter your password.').max(128),
})
type SignInValues = z.infer<typeof signInSchema>

function safeReturnTo(value: string | null): string {
  if (!value || !value.startsWith('/') || value.startsWith('//') || value.startsWith('/signin') || value.includes('\\') || /[\u0000-\u001f]/.test(value)) return '/employees'
  return value
}

function LoadingScreen() {
  return <main className="full-screen" aria-live="polite"><div className="spinner" aria-hidden="true" /><p>Checking your session…</p></main>
}

function SignInPage() {
  const { session, loading, error, refresh, signIn } = useAuth()
  const [message, setMessage] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const { register, handleSubmit, resetField, formState: { errors } } = useForm<SignInValues>({
    resolver: zodResolver(signInSchema),
    defaultValues: { email: '', password: '' },
  })
  const location = useLocation()
  const navigate = useNavigate()
  const returnTo = safeReturnTo(new URLSearchParams(location.search).get('returnTo'))
  const sessionExpired = (location.state as { sessionExpired?: boolean } | null)?.sessionExpired === true

  useEffect(() => {
    if (session?.authenticated) navigate(returnTo, { replace: true })
  }, [session, navigate, returnTo])

  if (loading) return <LoadingScreen />
  if (session?.authenticated) return <LoadingScreen />

  async function submit(values: SignInValues) {
    setBusy(true)
    setMessage(null)
    try {
      await signIn(values.email, values.password)
      resetField('password')
      navigate(returnTo, { replace: true })
    } catch (cause) {
      resetField('password')
      if (cause instanceof ApiError && cause.status === 429) setMessage('Too many sign-in attempts. Try again later.')
      else if (cause instanceof ApiError && cause.status === 401) setMessage('Invalid email or password.')
      else setMessage(cause instanceof Error ? cause.message : 'Sign-in could not be completed. Try again.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="signin-layout">
      <section className="signin-brand" aria-label="Salary Management">
        <div className="brand-mark"><BadgeDollarSign size={25} strokeWidth={2.2} /></div>
        <div><p className="eyebrow">PEOPLE OPERATIONS</p><h1>Salary<br />Management</h1><p className="brand-copy">A clear view of your people and compensation.</p></div>
        <div className="signin-footnote"><span className="dot" /> Secure HR workspace</div>
      </section>
      <section className="signin-panel">
        <div className="signin-form-wrap">
          <div className="mobile-brand"><div className="brand-mark"><BadgeDollarSign size={23} /></div><span>Salary Management</span></div>
          <span className="eyebrow">WELCOME BACK</span>
          <h2>Sign in to continue</h2>
          <p className="muted">Use your HR account to access employee compensation.</p>
          {error && <div className="alert alert-error" role="alert"><span>{error}</span><button className="text-button" type="button" onClick={() => void refresh()}>Retry</button></div>}
          {sessionExpired && <div className="alert alert-info" role="status">Your session expired. Sign in again to continue.</div>}
          {message && <div className="alert alert-error" role="alert">{message}</div>}
          <form className="form-stack" onSubmit={handleSubmit(submit)}>
            <label className="field"><span>Email address</span><input autoComplete="username" type="email" {...register('email')} placeholder="name@company.com" maxLength={254} disabled={busy || Boolean(error)} aria-invalid={Boolean(errors.email)} aria-describedby={errors.email ? 'email-error' : undefined} />{errors.email && <span className="field-error" id="email-error">{errors.email.message}</span>}</label>
            <label className="field"><span>Password</span><input autoComplete="current-password" type="password" {...register('password')} placeholder="Enter your password" maxLength={128} disabled={busy || Boolean(error)} aria-invalid={Boolean(errors.password)} aria-describedby={errors.password ? 'password-error' : undefined} />{errors.password && <span className="field-error" id="password-error">{errors.password.message}</span>}</label>
            <button className="button button-primary button-wide" type="submit" disabled={busy || Boolean(error)}>{busy ? 'Signing in…' : <>Sign in <ArrowRight size={17} /></>}</button>
          </form>
          <div className="signin-security"><LockKeyhole size={15} /><span>Your session is private to this browser.</span></div>
        </div>
        <footer className="signin-footer">Salary Management <span>·</span> HR workspace</footer>
      </section>
    </main>
  )
}

const navigation = [
  { to: '/employees', label: 'Employees', icon: Users },
  { to: '/reports', label: 'Reports', icon: BriefcaseBusiness },
  { to: '/imports', label: 'Import', icon: FileUp },
]

function AppShell() {
  const { session, signOut } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)
  const [signingOut, setSigningOut] = useState(false)

  useEffect(() => { setMenuOpen(false) }, [location.pathname])

  async function handleSignOut() {
    setSigningOut(true)
    try { await signOut() } finally { setSigningOut(false); navigate('/signin', { replace: true }) }
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <Link className="brand-lockup" to="/employees"><span className="brand-mark brand-mark-small"><BadgeDollarSign size={20} /></span><span>Salary Management</span></Link>
        <button className="mobile-menu-toggle" type="button" aria-expanded={menuOpen} aria-controls="primary-navigation" aria-label={menuOpen ? 'Close navigation' : 'Open navigation'} onClick={() => setMenuOpen((open) => !open)}>{menuOpen ? <X /> : <Menu />}</button>
        <nav id="primary-navigation" className={`primary-nav ${menuOpen ? 'primary-nav-open' : ''}`} aria-label="Main navigation">
          {navigation.map(({ to, label, icon: Icon }) => <NavLink key={to} to={to} className={({ isActive }) => `nav-link${isActive ? ' nav-link-active' : ''}`}><Icon size={17} /><span>{label}</span></NavLink>)}
        </nav>
        <div className="account-menu"><span className="account-avatar" aria-hidden="true">{session?.email?.slice(0, 1).toUpperCase() ?? 'H'}</span><span className="account-email">{session?.email}</span><button className="text-button signout-button" type="button" onClick={() => void handleSignOut()} disabled={signingOut}>{signingOut ? 'Signing out…' : 'Sign out'}</button></div>
      </header>
      <main className="main-content"><Outlet /></main>
    </div>
  )
}

function ProtectedLayout() {
  const { session, loading, error } = useAuth()
  const location = useLocation()
  if (loading) return <LoadingScreen />
  if (!session?.authenticated) return <Navigate to={`/signin?returnTo=${encodeURIComponent(location.pathname + location.search)}`} state={{ sessionExpired: true }} replace />
  if (error) return <main className="full-screen"><div className="alert alert-error" role="alert">{error}</div></main>
  return <AppShell />
}

function NotFoundPage() {
  return <section className="page-placeholder"><div className="placeholder-icon"><CircleHelp size={24} /></div><h1>Page not found</h1><p>This page may have moved or the address may be incorrect.</p><Link className="button button-secondary" to="/employees">Go to employees</Link></section>
}

export function App() {
  return <Routes>
    <Route path="/signin" element={<SignInPage />} />
    <Route element={<ProtectedLayout />}>
      <Route index element={<Navigate to="/employees" replace />} />
      <Route path="employees" element={<EmployeeDirectoryPage />} />
      <Route path="employees/new" element={<EmployeeFormPage />} />
      <Route path="employees/:id" element={<EmployeeDetailPage />} />
      <Route path="reports" element={<ReportsPage />} />
      <Route path="imports" element={<ImportUploadPage />} />
      <Route path="imports/:id" element={<ImportPreviewPage />} />
    </Route>
    <Route path="*" element={<NotFoundPage />} />
  </Routes>
}
