import { Navigate, useSearchParams } from 'react-router-dom';

/** The old Broking Setup route of the access requests: opens the User Access screen. */
export function AccessRequestsRedirect() {
  const [params] = useSearchParams();
  const id = params.get('id');
  return (
    <Navigate to={id === null ? '/user-access/requests' : `/user-access/requests/${id}`} replace />
  );
}

/** The old Broking Setup route of the user access matrix. */
export function AccessMatrixRedirect() {
  return <Navigate to="/user-access/matrix" replace />;
}
