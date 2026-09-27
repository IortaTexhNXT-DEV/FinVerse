import { useState } from 'react';
import { Card } from '@/components/ui/Card';
import { PageHeader } from '@/components/ui/PageHeader';
import { AccountTable } from './AccountTable';

const CRITERIA = { directPayment: true };

/**
 * Direct payment accounts (BRNB.114): the client pays the insurer directly, so the account
 * skips BDOI collection and is released for placement on validation.
 */
export default function DirectPaymentPage() {
  const [page, setPage] = useState(0);
  return (
    <div className="stack">
      <PageHeader
        section="Accounts & Placement"
        title="Direct Payment Accounts"
        description="Accounts whose premium is paid directly to the insurer."
      />
      <Card flush>
        <AccountTable
          criteria={CRITERIA}
          page={page}
          onPage={setPage}
          emptyMessage="No direct payment account."
        />
      </Card>
    </div>
  );
}
