import { EbPlaceholder } from '../EbPlaceholder';

/**
 * Pending Items (BRID-030): tracked items by programme or member, filters, manual update.
 * Placeholder of the foundation; built by wave E1-C.
 */
export default function PendingItemsPage() {
  return (
    <EbPlaceholder
      title="Pending Items"
      description="Contracts, HMO cards, card replacements and billings pending per programme or member, with due dates and follow-ups sent."
      cardTitle="Pending Items"
      emptyMessage="No pending items to display"
    />
  );
}
