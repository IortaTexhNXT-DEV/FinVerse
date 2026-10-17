import { Navigate } from 'react-router-dom';

/** The old Broking Setup route of the access requests: opens the Group Profiles of User Access. */
export function AccessRequestsRedirect() {
  return <Navigate to="/access/group-profiles" replace />;
}

/** The old Broking Setup route of the user access matrix. */
export function AccessMatrixRedirect() {
  return <Navigate to="/access/matrix" replace />;
}
