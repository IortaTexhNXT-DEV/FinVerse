import { LogOut } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { useWorkspace } from '@/context/workspaceContext';
import { HeaderTools } from './HeaderTools';
import { shortCompanyName } from './companyName';
import { useRoleName } from '@/components/ui/useDisplayName';
import { humanize } from '@/utils/format';

function initials(name: string): string {
  return name
    .split(' ')
    .map((part) => part.charAt(0))
    .join('')
    .slice(0, 2)
    .toUpperCase();
}

/** Top bar: company/branch context selectors, notifications and tools, and the user menu. */
export function Header() {
  const { user, logout } = useAuth();
  const { companies, company, branches, branchId, setCompanyId, setBranchId } = useWorkspace();
  const roleName = useRoleName();

  return (
    <header className="app-header">
      <div className="header-context">
        <label className="visually-hidden" htmlFor="company-select">
          Company
        </label>
        <select
          id="company-select"
          className="select company-select"
          title={company === undefined ? undefined : `${company.code} – ${company.name}`}
          value={company?.id ?? ''}
          onChange={(e) => setCompanyId(Number(e.target.value))}
        >
          {companies.map((c) => (
            <option key={c.id} value={c.id}>
              {c.code} – {shortCompanyName(c.name)}
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
      <HeaderTools />
      {user !== null && (
        <div className="header-user">
          <div>
            <Link to="/profile" style={{ fontWeight: 600 }} title="My profile">
              {user.fullName}
            </Link>
            <div className="muted header-role" title={user.roles.map(humanize).join(', ')}>
              {roleName(user.username) ?? humanize(user.roles[0] ?? '')}
            </div>
          </div>
          <span className="avatar" aria-hidden="true">
            {initials(user.fullName)}
          </span>
          <button type="button" className="btn btn-ghost btn-sm" onClick={() => logout()}>
            <LogOut size={16} aria-hidden="true" /> Sign out
          </button>
        </div>
      )}
    </header>
  );
}
