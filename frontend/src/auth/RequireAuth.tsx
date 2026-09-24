import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { mayOpen } from '@/navigation/access';
import { useAuth } from './authContext';

/** Redirects anonymous users to the login page. */
export function RequireAuth({ children }: Readonly<{ children: ReactNode }>) {
  const { user, loading } = useAuth();
  if (loading) {
    return <span className="spinner" aria-label="Loading" />;
  }
  return user === null ? <Navigate to="/login" replace /> : <>{children}</>;
}

/** Guards a screen by permission; users without it see an explanatory message. */
export function RequirePermission({
  permission,
  alsoPermissions,
  children,
}: Readonly<{ permission?: string; alsoPermissions?: string[]; children: ReactNode }>) {
  const { can } = useAuth();
  if (!mayOpen({ permission, alsoPermissions }, can)) {
    return (
      <div className="alert warning" role="alert">
        You do not have access to this screen. Contact your administrator if you need it.
      </div>
    );
  }
  return <>{children}</>;
}
