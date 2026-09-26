import { useState } from 'react';
import type { AccountSummary } from '@/api/accounts';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { PageHeader } from '@/components/ui/PageHeader';
import { formatDate } from '@/utils/format';
import { ACCOUNT_COLUMNS } from './accountColumns';
import { AccountTable } from './AccountTable';

const FFY_COLUMNS: Column<AccountSummary>[] = [
  ...ACCOUNT_COLUMNS.filter((c) => c.key !== 'o'),
  { key: 'fs', header: 'FFY Start', render: (a) => formatDate(a.ffyStart) },
  { key: 'fe', header: 'FFY End', render: (a) => formatDate(a.ffyEnd) },
  { key: 'o', header: 'Officer', render: (a) => a.accountOfficer ?? '' },
];

const CRITERIA = { ffy: true };

/**
 * Free First Year register (BRNB.101-104): accounts whose first-year premium is borne by the
 * bank, with the FFY period. Open an account to cancel its FFY with a reason.
 */
export default function FfyRegisterPage() {
  const [page, setPage] = useState(0);
  return (
    <div className="stack">
      <PageHeader
        section="Accounts & Placement"
        title="FFY Register"
        description="Accounts tagged Free First Year, with the FFY start and end."
      />
      <Card flush>
        <AccountTable
          criteria={CRITERIA}
          page={page}
          onPage={setPage}
          columns={FFY_COLUMNS}
          emptyMessage="No account is tagged Free First Year."
        />
      </Card>
    </div>
  );
}
