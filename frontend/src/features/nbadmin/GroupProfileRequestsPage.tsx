import { AccessRequestWorklist } from './AccessRequestWorklist';

/**
 * Group Profile Requests (BRD 3.002; FR-UA-040 to FR-UA-045): requests to create, change,
 * deactivate and reactivate group profiles, decided by the approvers in order and implemented by
 * the System Administrator.
 */
export default function GroupProfileRequestsPage() {
  return (
    <AccessRequestWorklist
      title="Group Profile Requests"
      description="New, changed, deactivated and reactivated group profiles. Approvers decide in order; the System Administrator implements the approved request."
      groupProfiles
      newLabel="New Group Profile Request"
      newPath="/user-access/requests/new?kind=group"
      newPermissions={['UAM_GROUP_REQUEST', 'ACCESS_REQUEST']}
    />
  );
}
