import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { StatusPage } from '@/components/ui/StatusPage';
import { mayOpen } from '@/navigation/access';
import { useAuth } from './authContext';
import { ForcedPasswordChange } from './ForcedPasswordChange';
import { LoadingPanel } from '@/components/ui/LoadingPanel';

/**
 * Redirects anonymous users to the login page, remembering the page so the user returns to it
 * after signing in again (session expiry); a user whose password must be changed first
 * (administrator reset, first use or expiry; UAM-NFR-36) sees the password change instead.
 */
export function RequireAuth({ children }: Readonly<{ children: ReactNode }>) {
  const { user, loading, passwordChange } = useAuth();
  const location = useLocation();
  if (loading) {
    return <LoadingPanel />;
  }
  if (user === null) {
    return (
      <Navigate to="/login" replace state={{ from: `${location.pathname}${location.search}` }} />
    );
  }
  return passwordChange === null ? (
    <>{children}</>
  ) : (
    <ForcedPasswordChange reason={passwordChange} />
  );
}

/**
 * Guards a screen by permission. Users without it are sent to `fallbackTo` when given (the home
 * page sends them to their own landing screen), otherwise they see an explanatory message.
 */
export function RequirePermission({
  permission,
  alsoPermissions,
  requiresAll,
  productModule,
  fallbackTo,
  children,
}: Readonly<{
  permission?: string;
  alsoPermissions?: string[];
  requiresAll?: string[];
  /** Product module of the screen: refused while the module is switched off. */
  productModule?: string;
  fallbackTo?: string;
  children: ReactNode;
}>) {
  const { can } = useAuth();
  if (!mayOpen({ permission, alsoPermissions, requiresAll, productModule }, can)) {
    if (fallbackTo !== undefined) {
      return <Navigate to={fallbackTo} replace />;
    }
    return <StatusPage kind="forbidden" />;
  }
  return <>{children}</>;
}
