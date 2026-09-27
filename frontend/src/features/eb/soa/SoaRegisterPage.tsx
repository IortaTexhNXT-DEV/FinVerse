import { EbPlaceholder } from '../EbPlaceholder';

/**
 * SOA Register (BRID-021): intake, validation, release. Placeholder of the foundation; built by
 * wave E1-C.
 */
export default function SoaRegisterPage() {
  return (
    <EbPlaceholder
      title="SOA Register"
      description="Insurer statements of account received, validated by Processing and released to the client and Collection, with the payment status of the linked invoices."
      cardTitle="Insurer SOAs"
      emptyMessage="No SOAs to display"
    />
  );
}
