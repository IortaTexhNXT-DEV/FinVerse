import { OperationsSectionHome } from '@/features/operations/OperationsSectionHome';

/** Commission Workbench: the section's work queues from the Operations ledger (CMRID.001-015, MKTID.012). */
export default function CommissionHomePage() {
  return (
    <OperationsSectionHome
      section="COMMISSION"
      description="Direct payment commission, insurer billing and feedback, incentives and BIR certificates."
    />
  );
}
