import { EbPlaceholder } from '../EbPlaceholder';

/**
 * EB Setup (BRID-016, 017): threshold rules, required documents, EB parameters (maker-checker).
 * Placeholder of the foundation; built by wave E1-B.
 */
export default function EbSetupPage() {
  return (
    <EbPlaceholder
      title="EB Setup"
      description="Value threshold rules, required documents per process and benefit line, and the EB parameters; changes wait for another user's authorization."
      cardTitle="Set-up"
      emptyMessage="The set-up screens come with the marketing wave"
    />
  );
}
