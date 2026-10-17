import { Navigate } from 'react-router-dom';

/** Earlier link of an IAAF in a notification or work item (/submitted/iaaf/:id): the reviews list. */
export function IaafRedirect() {
  return <Navigate to="/submitted/reviews" replace />;
}

/** Earlier link of a TOR in a notification or work item (/submitted/tor/:id): the TOR list. */
export function TorRedirect() {
  return <Navigate to="/submitted/tors" replace />;
}
