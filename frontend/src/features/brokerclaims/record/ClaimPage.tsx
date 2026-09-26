import { ClaimsPlaceholder } from '../ClaimsPlaceholder';

/**
 * Claim record (BRCLM.001-043): summary card with the claim number, status and flags, the workflow
 * panel, the actions and the tabs Details, Locations, Insurers & Updates, Reserve & Settlement,
 * Diary, Documents and History. Placeholder of the foundation; built by CL1-A (record, details,
 * locations, insurers, reserve, documents) with the status actions and the Diary / History tabs of
 * CL1-B.
 */
export default function ClaimPage() {
  return (
    <ClaimsPlaceholder
      title="Claim"
      description="The claim case file: cover, loss, locations, insurers, status, settlement, diary, documents and history."
      cardTitle="Details"
      backTo="/claims-handling/worklist"
      emptyMessage="Claim details are not available yet"
    />
  );
}
