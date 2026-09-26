import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { RefreshCw } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { cashieringApi } from './cashieringApi';
import type { Prebooked } from './cashieringApi';
import { ageBucket } from './cashieringLogic';
import './cashiering.css';

const TABS = [
  { id: 'OPEN', label: 'Waiting for Booking' },
  { id: 'APPLIED', label: 'Applied' },
  { id: 'RELEASED', label: 'Released to Unapplied' },
] as const;
type Status = (typeof TABS)[number]['id'];

/**
 * Pre-booked queue (CSHID.020, OQ12): payments received for accounts not booked yet. The
 * PREBOOKED_REMATCH job applies them once the account is booked; ageing items raise the
 * PREBOOKED_AGEING alert and can be re-matched now or released to the unapplied workbench.
 */
export default function PrebookedPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<Status>('OPEN');
  const [page, setPage] = useState(0);
  const [releasing, setReleasing] = useState<Prebooked>();
  const list = useQuery({
    queryKey: ['cashiering', 'prebooked', companyId, status, page],
    queryFn: () => cashieringApi.prebooked(companyId, status, page),
    enabled: companyId > 0,
  });
  const act = useMutation({
    mutationFn: (fn: () => Promise<unknown>) => fn(),
    onSuccess: async (r) => {
      setReleasing(undefined);
      const p = r as Partial<Prebooked> & Record<string, number>;
      toast.success(
        p.arn ? `${p.arn}: ${p.status?.toLowerCase() ?? 'done'}` : 'Matching run finished',
      );
      await queryClient.invalidateQueries({ queryKey: ['cashiering'] });
    },
  });
  const apply = can('CASH_APPLY');
  const columns: Column<Prebooked>[] = [
    { key: 'arn', header: 'Account (ARN)', render: (p) => <strong>{p.arn}</strong> },
    { key: 'ref', header: 'Reference', render: (p) => p.reference },
    { key: 'amount', header: 'Amount', numeric: true, render: (p) => <Amount value={p.amount} /> },
    { key: 'first', header: 'Received', render: (p) => formatDate(p.firstSeen) },
    {
      key: 'age',
      header: 'Age',
      render: (p) => <span className={`csh-age-${ageBucket(p.ageDays)}`}>{p.ageDays} days</span>,
    },
    {
      key: 'rematch',
      header: 'Re-match Attempts',
      render: (p) =>
        [String(p.rematchCount), p.lastRematch && formatDateTime(p.lastRematch)]
          .filter(Boolean)
          .join(' · '),
    },
    {
      key: 'receipt',
      header: 'Receipt',
      render: (p) => <Link to={`/cashiering/receipts/${p.receiptId}`}>Open AR</Link>,
    },
    { key: 'status', header: 'Status', render: (p) => <StatusBadge status={p.status} /> },
    {
      key: 'actions',
      header: 'Actions',
      render: (p) =>
        p.status === 'OPEN' &&
        apply && (
          <div className="row">
            <Button
              size="sm"
              variant="secondary"
              onClick={() => act.mutate(() => cashieringApi.rematch(p.id))}
            >
              Re-match Now
            </Button>
            <Button size="sm" variant="ghost" onClick={() => setReleasing(p)}>
              Release
            </Button>
          </div>
        ),
    },
  ];
  return (
    <div className="stack">
      <PageHeader
        section="Cashiering"
        title="Pre-booked Payments"
        description="Payments received before the account was booked: applied automatically once it is booked."
        actions={
          apply && (
            <Button
              variant="accent"
              icon={<RefreshCw size={16} />}
              busy={act.isPending}
              onClick={() => act.mutate(cashieringApi.runMatching)}
            >
              Run Matching Now
            </Button>
          )
        }
      />
      <ErrorAlert error={list.error ?? act.error} />
      <Card flush>
        <Tabs
          tabs={TABS}
          active={status}
          onChange={(s) => {
            setStatus(s);
            setPage(0);
          }}
        />
        <DataTable
          caption="Pre-booked payments"
          columns={columns}
          rows={list.data?.content ?? []}
          rowKey={(p) => p.id}
          loading={list.isLoading}
          emptyMessage="No items to display"
        />
        <PageFooter data={list.data} noun="payments" onPage={setPage} />
      </Card>
      {releasing && (
        <ActionDialog
          title={`Release ${releasing.arn} to Unapplied`}
          confirmLabel="Release"
          busy={act.isPending}
          error={act.error}
          onConfirm={(note) =>
            act.mutate(() =>
              cashieringApi.releasePrebooked(
                releasing.id,
                (note.comment?.trim() ?? '') === '' ? 'Released by cashier' : (note.comment ?? ''),
              ),
            )
          }
          onClose={() => setReleasing(undefined)}
        />
      )}
    </div>
  );
}
