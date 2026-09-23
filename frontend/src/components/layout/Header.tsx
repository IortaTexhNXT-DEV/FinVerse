import { LogOut } from 'lucide-react';
import { useAuth } from '@/auth/authContext';
import { useWorkspace } from '@/context/workspaceContext';
import { Brand } from './Brand';

function initials(name: string): string {
  return name
    .split(' ')
    .map((part) => part.charAt(0))
    .join('')
    .slice(0, 2)
    .toUpperCase();
}

/** Top bar: brand, company/branch context selectors and the user menu. */
export function Header() {
  const { user, logout } = useAuth();
  const { companies, company, branches, branchId, setCompanyId, setBranchId } = useWorkspace();

  return (
    <header className="app-header">
      <Brand />
      <div className="header-context">
        <label className="visually-hidden" htmlFor="company-select">
          Company
        </label>
        <select
          id="company-select"
          className="select"
          value={company?.id ?? ''}
          onChange={(e) => setCompanyId(Number(e.target.value))}
        >
          {companies.map((c) => (
            <option key={c.id} value={c.id}>
              {c.code} – {c.name}
            </option>
          ))}
        </select>
        <label className="visually-hidden" htmlFor="branch-select">
          Branch
        </label>
        <select
          id="branch-select"
          className="select"
          value={branchId ?? ''}
          onChange={(e) => setBranchId(e.target.value ? Number(e.target.value) : undefined)}
        >
          <option value="">All branches</option>
          {branches.map((b) => (
            <option key={b.id} value={b.id}>
              {b.code} – {b.name}
            </option>
          ))}
        </select>
      </div>
      <div className="spacer" />
      {user !== null && (
        <div className="header-user">
          <div>
            <div style={{ fontWeight: 600 }}>{user.fullName}</div>
            <div className="muted" style={{ fontSize: 12 }}>
              {user.roles.join(', ')}
            </div>
          </div>
          <span className="avatar" aria-hidden="true">
            {initials(user.fullName)}
          </span>
          <button type="button" className="btn btn-ghost btn-sm" onClick={logout}>
            <LogOut size={16} aria-hidden="true" /> Sign out
          </button>
        </div>
      )}
    </header>
  );
}
