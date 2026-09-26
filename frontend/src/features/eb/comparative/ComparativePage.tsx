import { EbPlaceholder } from '../EbPlaceholder';

/**
 * Comparative (BRID-010, 011, 016): matrix, recommendation, sign-off and threshold approval.
 * Placeholder of the foundation; built by wave E1-B.
 */
export default function ComparativePage() {
  return (
    <EbPlaceholder
      title="Comparative"
      description="The comparative matrix of the proposals with the recommendation, sign-off, threshold approval and the client's comments, in PDF and Excel."
      backTo="/eb/programmes"
      emptyMessage="The comparative comes with the marketing wave"
    />
  );
}
