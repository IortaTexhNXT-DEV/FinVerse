import { useState } from 'react';
import type { AccountSummary } from '@/api/accounts';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { PageHeader } from '@/components/ui/PageHeader';
import { periodColumn } from '@/components/ui/periodColumn';
import { ACCOUNT_COLUMNS } from './accountColumns';
import { AccountTable } from './AccountTable';
import { UserName } from '@/components/ui/UserName';

/**
 * The register's columns: the account columns without the flags (every account here is FFY), the
 * FFY period in the period cell next to the policy period, and the officer.
 */
const FFY_COLUMNS: Column<AccountSummary>[] = [
  ...ACCOUNT_COLUMNS.filter((c) => c.key !== 'o' && c.key !== 'f' && c.key !== 's'),
  periodColumn<AccountSummary>(
    'ffy',
    'FFY Period',
    (a) => a.ffyStart,
    (a) => a.ffyEnd,
  ),
  { key: 'o', header: 'Officer', render: (a) => <UserName login={a.accountOfficer} empty="" /> },
  ...ACCOUNT_COLUMNS.filter((c) => c.key === 's'),
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
