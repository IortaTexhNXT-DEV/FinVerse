import { OperationsSectionHome } from '@/features/operations/OperationsSectionHome';

/** Reconciliation Workbench: the section's work queues from the Operations ledger (PRCID.001-039). */
export default function ProdReconHomePage() {
  return (
    <OperationsSectionHome
      section="PRODRECON"
      description="Production registers, insurer feedback and matching of booked accounts."
    />
  );
}
