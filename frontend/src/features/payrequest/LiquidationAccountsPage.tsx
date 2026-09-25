import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { humanize } from '@/utils/format';
import { payRequestApi } from './api';

const ROLES = ['PER_DIEM', 'REPRESENTATION', 'TRANSPORT', 'LODGING', 'OTHER', 'CASH'] as const;

interface Row {
  role: string;
  accountCode: string;
}

/**
 * Accounts of the cash-advance liquidation (event PRQ_CA_LIQUIDATION; AQ02, AQ18): the expense
 * account of each category and the cash account of returned excess, set by Comptrollership. GL
 * accounts are never chosen in code; until BDOI's chart is loaded these are demo accounts.
 */
export default function LiquidationAccountsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [edits, setEdits] = useState<Record<string, string>>({});
  const accounts = useQuery({
    queryKey: ['payrequest', 'accounts', companyId],
    queryFn: () => payRequestApi.accounts(companyId),
    enabled: companyId > 0,
  });
  const save = useMutation({
    mutationFn: (row: Row) => payRequestApi.assignAccount(companyId, row.role, row.accountCode),
    onSuccess: async (a) => {
      await queryClient.invalidateQueries({ queryKey: ['payrequest', 'accounts'] });
      toast.success(`${humanize(a.role)} posts to ${a.accountCode}`);
    },
  });
  const mayEdit = can('ACCOUNTING_RULE_MANAGE');
  const rows: Row[] = ROLES.map((role) => ({
    role,
    accountCode: accounts.data?.find((a) => a.role === role)?.accountCode ?? '',
  }));
  const columns: Column<Row>[] = [
    { key: 'role', header: 'Account Role', render: (r) => humanize(r.role) },
    {
      key: 'account',
      header: 'GL Account',
      render: (r) =>
        mayEdit ? (
          <input
            className="input"
            aria-label={`GL account of ${humanize(r.role)}`}
            maxLength={30}
            value={edits[r.role] ?? r.accountCode}
            onChange={(e) => setEdits({ ...edits, [r.role]: e.target.value })}
          />
        ) : (
          r.accountCode || '—'
        ),
    },
    {
      key: 'save',
      header: '',
      render: (r) =>
        mayEdit ? (
          <Button
            size="sm"
            variant="secondary"
            disabled={(edits[r.role] ?? '').trim() === ''}
            onClick={() => save.mutate({ role: r.role, accountCode: (edits[r.role] ?? '').trim() })}
          >
            Save
          </Button>
        ) : null,
    },
  ];
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Refund & Cash Advance Requests"
        title="Liquidation Accounts"
        description="The GL accounts a cash-advance liquidation posts to, by expense category, and the cash account of returned excess."
      />
      <ErrorAlert error={accounts.error ?? save.error} />
      <Card flush>
        <DataTable
          caption="Liquidation accounts"
          columns={columns}
          rows={rows}
          rowKey={(r) => r.role}
          loading={accounts.isLoading}
        />
      </Card>
    </div>
  );
}
