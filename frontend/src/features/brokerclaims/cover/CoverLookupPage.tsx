import { ClaimsPlaceholder } from '../ClaimsPlaceholder';

/**
 * Cover Lookup (BRCLM.002/003/042): read-only account, endorsements, invoices, the claims of the
 * cover and its insurer location references. Placeholder of the foundation; built by CL1-A.
 */
export default function CoverLookupPage() {
  return (
    <ClaimsPlaceholder
      title="Cover Lookup"
      description="Look up a cover by ARN, policy number or assured: policy, versions, premium status, claims and insurer location references (read only)."
      cardTitle="Covers"
      emptyMessage="Search a cover to see its details"
    />
  );
}
