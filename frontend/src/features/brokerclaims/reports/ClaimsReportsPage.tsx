import { ClaimsPlaceholder } from '../ClaimsPlaceholder';

/**
 * Claims Handling reports (BRCLM.026-034/038/040): the Report Centre filtered to the Claims Handling
 * category. Placeholder of the foundation; built by CL1-B.
 */
export default function ClaimsReportsPage() {
  return (
    <ClaimsPlaceholder
      title="Claims Reports"
      description="Outstanding, past due, settled, ageing, loss experience, loss ratio, pending actions, claims-prone locations, insurer claim numbers, activity log and data extract."
      cardTitle="Reports"
      emptyMessage="No Claims Handling reports to display"
    />
  );
}
