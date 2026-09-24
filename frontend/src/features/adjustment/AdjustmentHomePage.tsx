import { OperationsSectionHome } from '@/features/operations/OperationsSectionHome';

/** Adjustment Workbench: the section's work queues from the Operations ledger (ADJID.001-028, MKTID.008). */
export default function AdjustmentHomePage() {
  return (
    <OperationsSectionHome
      section="ADJUSTMENT"
      description="Endorsement and cancellation requests on booked invoices."
    />
  );
}
