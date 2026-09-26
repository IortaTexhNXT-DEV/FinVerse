import { EbPlaceholder } from '../EbPlaceholder';

/**
 * Programmes work list (BRID-001-017; design 10.1): status tabs, search, bulk Send RA. Placeholder
 * of the foundation; built by wave E1-B.
 */
export default function ProgrammesPage() {
  return (
    <EbPlaceholder
      title="Programmes"
      description="Every EB programme you may see, by status: Renewal Due, In Progress, With Client, In Placement, Placed and Lost, with search by programme number or client."
      cardTitle="Programmes"
      emptyMessage="No programmes to display"
    />
  );
}
