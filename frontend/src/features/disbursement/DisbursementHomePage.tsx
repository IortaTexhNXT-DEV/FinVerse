import { SectionLanding } from '@/components/broking/SectionLanding';

/** Disbursement workbench (DIS 2.6-2.21): requests and vouchers by stage once A1-DSB adds them. */
export default function DisbursementHomePage() {
  return (
    <SectionLanding
      section="Finance"
      title="Disbursement Workbench"
      description="Payment requests and disbursement vouchers by stage: system requests, in process, for review, for approval, approved and cancelled."
      emptyMessage="No payment requests to show yet"
    />
  );
}
