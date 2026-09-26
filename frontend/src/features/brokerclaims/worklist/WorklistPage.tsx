import { ClaimsPlaceholder } from '../ClaimsPlaceholder';

/**
 * Claims worklist (BRCLM.034, 043 AC5): tabs My Claims, Open, Temporarily Closed, Closed and
 * Follow-ups Due, search and bulk reassignment. Placeholder of the foundation; built by CL1-B.
 */
export default function WorklistPage() {
  return (
    <ClaimsPlaceholder
      title="Claims Worklist"
      description="Every claim you may see, by phase, with search by claim number, insurer claim number, ARN, policy number or assured."
      emptyMessage="No claims to display"
    />
  );
}
