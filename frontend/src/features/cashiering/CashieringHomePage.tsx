import { OperationsSectionHome } from '@/features/operations/OperationsSectionHome';

/** Cashiering Workbench: the section's work queues from the Operations ledger (CSHID.001-027, MKTID.010/013, DBMID.001). */
export default function CashieringHomePage() {
  return (
    <OperationsSectionHome
      section="CASHIERING"
      description="Payments, receipts, applications and unapplied items of the Operations invoice ledger."
    />
  );
}
