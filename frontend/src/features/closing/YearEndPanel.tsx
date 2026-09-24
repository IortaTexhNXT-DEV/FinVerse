import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Lock } from 'lucide-react';
import { closingApi } from '@/api/closing';
import type { ClosingBalance, YearEndClose } from '@/api/closing';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { useToast } from '@/components/ui/toastContext';
import { useWorkspace } from '@/context/workspaceContext';
import { formatAmount, formatDateTime } from '@/utils/format';
import { ChecklistView } from './ChecklistView';
import { PeriodSelectors } from './PeriodSelectors';
import type { usePeriodPicker } from './usePeriodPicker';

/** Income and expense balances the closing journal will transfer to retained earnings. */
function ClosingPreview({ companyId, yearId }: Readonly<{ companyId: number; yearId: number }>) {
  const { branches } = useWorkspace();
  const preview = useQuery({
    queryKey: ['year-preview', companyId, yearId],
    queryFn: () => closingApi.yearPreview(companyId, yearId),
    enabled: companyId > 0 && yearId > 0,
  });
  const net = -(preview.data ?? []).reduce((acc, b) => acc + b.netDebit, 0);
  const branch = (id: number) => branches.find((x) => x.id === id)?.code ?? String(id);
  return (
    <Card title={`Closing entries preview · net result ${formatAmount(net)}`} flush>
      <ErrorAlert error={preview.error} />
      <DataTable<ClosingBalance>
        loading={preview.isLoading}
        rows={preview.data ?? []}
        rowKey={(b) =>
          `${b.branchId}-${b.accountCode}-${b.costCenter ?? ''}-${b.businessLine ?? ''}`
        }
        emptyMessage="No income or expense balances to close."
        columns={[
          { key: 'b', header: 'Branch', render: (b) => branch(b.branchId) },
          { key: 'a', header: 'Account', render: (b) => b.accountCode },
          { key: 'c', header: 'Cost Centre', render: (b) => b.costCenter ?? '' },
          { key: 'l', header: 'Line of Business', render: (b) => b.businessLine ?? '' },
          {
            key: 'n',
            header: 'Balance (Dr +)',
            numeric: true,
            render: (b) => <Amount value={b.netDebit} />,
          },
        ]}
      />
      <p className="muted" style={{ padding: '0 16px' }}>
        Balance sheet balances are cumulative and carry forward automatically; no opening balance
        journal is needed.
      </p>
    </Card>
  );
}

function CloseRecord({ record }: Readonly<{ record: YearEndClose }>) {
  return (
    <div className="alert success">
      FY {record.yearCode} closed by {record.closedBy} on {formatDateTime(record.closedAt)}: net
      result {formatAmount(record.netResult)} transferred to {record.retainedEarningsAccount}{' '}
      (journals {record.closingBatches}).
    </div>
  );
}

/**
 * Year-end close: pre-close checklist, preview of the transfer to retained earnings, and the close
 * itself (closing journal per branch, fiscal year locked, next year opened).
 */
export function YearEndPanel({ picker }: Readonly<{ picker: ReturnType<typeof usePeriodPicker> }>) {
  const { companyId } = picker;
  const yearId = picker.year?.id ?? 0;
  const yearCode = picker.year?.yearCode ?? 0;
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const closer = can('YEAR_END_CLOSE');

  const checklist = useQuery({
    queryKey: ['year-checklist', companyId, yearId],
    queryFn: () => closingApi.yearChecklist(companyId, yearId),
    enabled: companyId > 0 && yearId > 0,
  });
  const record = useQuery({
    queryKey: ['year-close', yearId],
    queryFn: () => closingApi.closeRecord(yearId),
    enabled: yearId > 0,
  });
  const close = useMutation({
    mutationFn: () => closingApi.closeYear(companyId, yearId),
    onSuccess: async (r) => {
      await queryClient.invalidateQueries();
      toast.success(`FY ${r.yearCode} closed, net result ${formatAmount(r.netResult)}`);
    },
  });
  const confirmClose = () => {
    if (globalThis.confirm(`Close fiscal year ${yearCode}? This cannot be undone.`)) {
      close.mutate();
    }
  };

  return (
    <div className="stack">
      <div className="form-grid">
        <PeriodSelectors picker={picker} withPeriod={false} />
        {closer && (
          <Button
            variant="accent"
            icon={<Lock size={16} />}
            busy={close.isPending}
            disabled={checklist.data?.ready !== true}
            onClick={confirmClose}
            style={{ alignSelf: 'end' }}
          >
            Close Fiscal Year
          </Button>
        )}
      </div>
      <ErrorAlert error={checklist.error ?? close.error} />
      {record.data && <CloseRecord record={record.data} />}
      <Card title="Pre-close checklist" flush>
        <ChecklistView checklist={checklist.data} loading={checklist.isLoading} />
      </Card>
      {closer && <ClosingPreview companyId={companyId} yearId={yearId} />}
    </div>
  );
}
