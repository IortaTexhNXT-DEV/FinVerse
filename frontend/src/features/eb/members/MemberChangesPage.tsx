import { EbPlaceholder } from '../EbPlaceholder';

/**
 * Member Changes (BRID-013, 025): work list, entry and upload. Placeholder of the foundation;
 * built by wave E1-C.
 */
export default function MemberChangesPage() {
  return (
    <EbPlaceholder
      title="Member Changes"
      description="Additions, deletions and plan or data changes of members, relayed to the insurer, billed, validated by Processing and closed; entry on screen or by upload."
      cardTitle="Member Changes"
      emptyMessage="No member changes to display"
    />
  );
}
