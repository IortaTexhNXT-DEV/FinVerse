import { ClaimsPlaceholder } from '../ClaimsPlaceholder';

/**
 * Record Claim (BRCLM.001/003/004/006/009/016/037/039): cover search, cover card with the premium
 * check, locations, loss details and insurers. Placeholder of the foundation; built by CL1-A.
 */
export default function RecordClaimPage() {
  return (
    <ClaimsPlaceholder
      title="Record Claim"
      description="Find the cover by ARN, policy number or assured, check the premium, then record the loss, the locations and the insurers."
      cardTitle="Cover"
      backTo="/claims-handling"
      emptyMessage="Search a cover to record a claim"
    />
  );
}
