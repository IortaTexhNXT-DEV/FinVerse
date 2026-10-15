import { useQuery } from '@tanstack/react-query';
import { Navigate, useParams } from 'react-router-dom';
import { nbadminApi } from '@/api/nbadmin';
import { LoadingPanel } from '@/components/ui/LoadingPanel';

/** The earlier Users screen. */
export function UsersRedirect() {
  return <Navigate to="/access/users" replace />;
}

/** The earlier Roles & Permissions, Access Requests and Group Profile Requests screens. */
export function GroupProfilesRedirect() {
  return <Navigate to="/access/group-profiles" replace />;
}

/** The earlier Identity Synchronisation screen. */
export function IdentitySyncRedirect() {
  return <Navigate to="/access/identity-sync" replace />;
}

/** The earlier User Access Matrix. */
export function MatrixRedirect() {
  return <Navigate to="/access/matrix" replace />;
}

/** The earlier Separation of Duties screen. */
export function SodRedirect() {
  return <Navigate to="/access/separation-of-duties" replace />;
}

/** The earlier User Access Reports. */
export function ReportsRedirect() {
  return <Navigate to="/access/reports" replace />;
}

/**
 * An earlier link to an access request: the pending change of its group profile, or the Users
 * screen for a request about a user (raised before UIDM-ISC maintained the users).
 */
export function RequestRedirect() {
  const { id } = useParams();
  const request = useQuery({
    queryKey: ['nbadmin', 'request', id],
    queryFn: () => nbadminApi.request(Number(id)),
    enabled: id !== undefined && /^\d+$/.test(id),
    retry: false,
  });
  if (request.isLoading) {
    return <LoadingPanel />;
  }
  const r = request.data;
  if (r?.roleCode !== undefined) {
    return (
      <Navigate
        to={`/access/group-profiles/${encodeURIComponent(r.roleCode)}?tab=pending`}
        replace
      />
    );
  }
  return <Navigate to={r === undefined ? '/access/group-profiles' : '/access/users'} replace />;
}
