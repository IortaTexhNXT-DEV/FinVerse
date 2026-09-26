import { AccessRequestWorklist } from './AccessRequestWorklist';

const REQUEST_PERMISSIONS = [
  'ACCESS_REQUEST',
  'UAM_ENROLL',
  'UAM_MODIFY',
  'UAM_DEACTIVATE',
  'UAM_REACTIVATE',
];

/**
 * Access Requests (BRD 1.002-1.008, 2.002; FR-UA-010 to FR-UA-034): every request the user may
 * see, by tab; a row opens the request with its history.
 */
export default function AccessRequestsPage() {
  return (
    <AccessRequestWorklist
      title="Access Requests"
      description="Requests to enrol, modify, deactivate and reactivate users and to change group profiles. Nothing changes until the chosen approver approves; the requester and the user concerned never decide."
      newLabel="New Request"
      newPath="/user-access/requests/new"
      newPermissions={REQUEST_PERMISSIONS}
    />
  );
}
