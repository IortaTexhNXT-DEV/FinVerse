import { OperationsSectionHome } from '@/features/operations/OperationsSectionHome';

/** Remittance Workbench: the section's work queues from the Operations ledger (RMTID.001-040, MKTID.001-009). */
export default function RemittanceHomePage() {
  return (
    <OperationsSectionHome
      section="REMITTANCE"
      description="Collected premium due to insurers, remittance batches, holds and special remittances."
    />
  );
}
