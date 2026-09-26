import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CalendarClock, Plus } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId, useDefaultBranchId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { cashieringApi } from './cashieringApi';
import type { PdcItem, PdcStatus } from './cashieringApi';
import { byMaturityMonth, monthLabel } from './cashieringLogic';
import { ReleaseDialog, WarehouseDialog } from './PdcDialogs';
import './cashiering.css';

const TABS: readonly { id: PdcStatus | ''; label: string }[] = [
  { id: 'WAREHOUSED', label: 'In Warehouse' },
  { id: 'APPLIED', label: 'Applied' },
  { id: 'MATURED', label: 'Matured (Unapplied)' },
  { id: '', label: 'All' },
];

/**
 * PDC Warehouse (CSHID.008 item 4): post-dated checks kept with a PDCW- number and shown by
 * maturity month. The PDC_MATURITY job turns a matured check into a payment with its AR; before
 * that a check can be returned, replaced or pulled out.
 */
export default function PdcWarehousePage() {
  const companyId = useCompanyId();
  const branchId = useDefaultBranchId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<PdcStatus | ''>('WAREHOUSED');
  const [adding, setAdding] = useState(false);
  const [releasing, setReleasing] = useState<PdcItem>();
  const list = useQuery({
    queryKey: ['cashiering', 'pdc', companyId, status],
    queryFn: () => cashieringApi.pdcs(companyId, status),
    enabled: companyId > 0,
  });
  const act = useMutation({
    mutationFn: (fn: () => Promise<unknown>) => fn(),
    onSuccess: async () => {
      setAdding(false);
      setReleasing(undefined);
      toast.success('PDC warehouse updated');
      await queryClient.invalidateQueries({ queryKey: ['cashiering'] });
    },
  });
  const upload = can('CASH_UPLOAD');
  const columns: Column<PdcItem>[] = [
    {
      key: 'no',
      header: 'Warehouse No.',
      render: (p) => (
        <>
          <strong>{p.warehouseNo}</strong>
          <span className="cell-sub">
            {p.bankCode} {p.checkNo}
          </span>
        </>
      ),
    },
    { key: 'payor', header: 'Payor', render: (p) => p.payorName },
    { key: 'ref', header: 'Reference', render: (p) => p.reference },
    { key: 'maturity', header: 'Maturity', render: (p) => formatDate(p.maturityDate) },
    { key: 'amount', header: 'Amount', numeric: true, render: (p) => <Amount value={p.amount} /> },
    { key: 'ar', header: 'AR', render: (p) => p.receiptNo ?? p.statusReason ?? '' },
    { key: 'status', header: 'Status', render: (p) => <StatusBadge status={p.status} /> },
    {
      key: 'actions',
      header: '',
      render: (p) =>
        p.status === 'WAREHOUSED' &&
        upload && (
          <Button size="sm" variant="ghost" onClick={() => setReleasing(p)}>
            Release
          </Button>
        ),
    },
  ];
  const months = byMaturityMonth(list.data?.content ?? []);
  return (
    <div className="stack">
      <PageHeader
        section="Cashiering"
        title="PDC Warehouse"
        description="Post-dated checks by maturity month. Matured checks become payments with an AR automatically."
        actions={
          upload && (
            <>
              <Button
                variant="secondary"
                icon={<CalendarClock size={16} />}
                busy={act.isPending}
                onClick={() => act.mutate(cashieringApi.matureNow)}
              >
                Process Maturities Now
              </Button>
              <Button variant="accent" icon={<Plus size={16} />} onClick={() => setAdding(true)}>
                Warehouse Check
              </Button>
            </>
          )
        }
      />
      <ErrorAlert error={list.error ?? act.error} />
      <Card flush>
        <Tabs tabs={TABS} active={status} onChange={setStatus} />
        {!list.isLoading && months.length === 0 && <EmptyState message="No post-dated checks" />}
        {months.map((m) => (
          <div key={m.month} className="stack">
            <div className="csh-month">
              <strong>{monthLabel(m.month)}</strong>
              <span>
                {m.items.length} check(s) · <Amount value={m.total} />
              </span>
            </div>
            <DataTable
              caption={`Checks maturing in ${monthLabel(m.month)}`}
              columns={columns}
              rows={m.items}
              rowKey={(p) => p.id}
            />
          </div>
        ))}
      </Card>
      {adding && (
        <WarehouseDialog
          busy={act.isPending}
          error={act.error}
          onSave={(body) =>
            act.mutate(() => cashieringApi.warehouse({ ...body, companyId, branchId }))
          }
          onClose={() => setAdding(false)}
        />
      )}
      {releasing && (
        <ReleaseDialog
          item={releasing}
          busy={act.isPending}
          error={act.error}
          onSave={(outcome, reason) =>
            act.mutate(() => cashieringApi.releasePdc(releasing.id, outcome, reason))
          }
          onClose={() => setReleasing(undefined)}
        />
      )}
    </div>
  );
}
