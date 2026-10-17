import { PageHeader } from '@/components/ui/PageHeader';
import { CandidateList } from '../common/CandidateList';
import type { QuickFilter } from '../common/CandidateList';
import { RENEWAL_SECTION } from '../common/renewalCodes';
import type { RenewalTabDef } from '../common/renewalCodes';
import '../renewal.css';

const TABS: RenewalTabDef[] = [
  { id: 'RA_SENT', label: 'RA Sent' },
  { id: 'NRNS', label: 'NRNS' },
  { id: 'NB_PATH', label: 'Quotation / Proposal' },
];

const QUICK: QuickFilter[] = [
  { id: 'due30', label: 'Due in 30 days', filters: { dueWithin: 30 } },
  { id: 'kyc', label: 'KYC due', filters: { kycDue: true } },
];

/**
 * Follow-ups (FR-RN-083, 085): the renewals waiting for the client's reply after the Renewal
 * Advice, for the Contact Center. Open a renewal to record a call or e-mail with its outcome and
 * next action date, or the client's acceptance.
 */
export default function FollowupsPage() {
  return (
    <div className="stack">
      <PageHeader
        section={RENEWAL_SECTION}
        title="Follow-ups"
        description="Renewals waiting for the client's reply."
      />
      <CandidateList tabs={TABS} quickFilters={QUICK} emptyMessage="No renewals to follow up" />
    </div>
  );
}
