import { ClaimsPlaceholder } from '../ClaimsPlaceholder';

/**
 * Insurer location references (BRCLM.042): search, maintain and upload the reference each insurer
 * gives a location of a cover. Placeholder of the foundation; built by CL1-A.
 */
export default function LocationRefsPage() {
  return (
    <ClaimsPlaceholder
      title="Insurer Location References"
      description="The reference each insurer uses for a location of a cover, with its effective dates; maintained here or by bulk upload."
      cardTitle="Location References"
      emptyMessage="No location references to display"
    />
  );
}
