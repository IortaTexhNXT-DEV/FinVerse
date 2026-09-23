import { useQuery } from '@tanstack/react-query';
import { Bell, CircleHelp, Inbox, Info } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { alertsApi } from '@/api/alerts';
import { approvalsApi } from '@/api/approvals';
import { useAuth } from '@/auth/authContext';
import { useCompanyId } from '@/context/workspaceContext';
import { AboutDialog } from '@/features/help/AboutDialog';
import { SessionTimeoutGuard } from '@/session/SessionTimeoutGuard';

const REFRESH_MS = 60_000;

function CountBadge({ count }: Readonly<{ count: number }>) {
  if (count <= 0) {
    return null;
  }
  return (
    <span className="header-count" aria-hidden="true">
      {count > 99 ? '99+' : count}
    </span>
  );
}

/**
 * Header shortcuts: approvals inbox with its count, live alerts (for alert viewers), help and
 * about. Also hosts the inactivity sign-out guard for the authenticated shell.
 */
export function HeaderTools() {
  const { can } = useAuth();
  const companyId = useCompanyId();
  const [aboutOpen, setAboutOpen] = useState(false);
  const approvals = useQuery({
    queryKey: ['approvals', 'counts', companyId],
    queryFn: () => approvalsApi.counts(companyId || undefined),
    refetchInterval: REFRESH_MS,
  });
  const alerts = useQuery({
    queryKey: ['alerts', 'summary'],
    queryFn: alertsApi.summary,
    enabled: can('ALERT_VIEW'),
    refetchInterval: REFRESH_MS,
  });
  const pending = approvals.data?.total ?? 0;
  const live = alerts.data?.live ?? 0;

  return (
    <nav className="header-tools" aria-label="Shortcuts">
      <Link
        to="/approvals"
        className="btn btn-ghost btn-sm header-tool"
        aria-label={`My approvals: ${pending} waiting`}
        title="My approvals"
      >
        <Inbox size={18} aria-hidden="true" />
        <CountBadge count={pending} />
      </Link>
      {can('ALERT_VIEW') && (
        <Link
          to="/alerts"
          className="btn btn-ghost btn-sm header-tool"
          aria-label={`Alerts: ${live} open`}
          title="Alerts"
        >
          <Bell size={18} aria-hidden="true" />
          <CountBadge count={live} />
        </Link>
      )}
      <Link to="/help" className="btn btn-ghost btn-sm header-tool" aria-label="Help" title="Help">
        <CircleHelp size={18} aria-hidden="true" />
      </Link>
      <button
        type="button"
        className="btn btn-ghost btn-sm header-tool"
        aria-label="About iNXT FinVerse"
        title="About"
        onClick={() => setAboutOpen(true)}
      >
        <Info size={18} aria-hidden="true" />
      </button>
      <AboutDialog open={aboutOpen} onClose={() => setAboutOpen(false)} />
      <SessionTimeoutGuard />
    </nav>
  );
}
