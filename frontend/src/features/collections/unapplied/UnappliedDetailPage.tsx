import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CalendarDays, CreditCard, FileText, Landmark, UserRound, Wallet } from 'lucide-react';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { RecordSummary } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatAmount, formatDate, formatDateTime, humanize } from '@/utils/format';
import { RecordLoading } from '../plans/Parts';
import type {
  CollectorDisposition,
  DispositionDraft,
  HistoryEvent,
  UnappliedDetail,
  UnappliedRow,
} from './api';
import { unappliedApi } from './api';
import { REQUEST_COLUMNS } from './columns';
import { DispositionDialog } from './DispositionDialog';
import { ACTION_LABELS, TAB_LABELS, cashieringStatus, paymentFacts } from './labels';

type DetailTab = 'DETAILS' | 'DISPOSITIONS' | 'REQUESTS' | 'HISTORY';

const TABS: readonly { id: DetailTab; label: string }[] = [
  { id: 'DETAILS', label: 'Payment & Account' },
  { id: 'DISPOSITIONS', label: 'Collector Dispositions' },
  { id: 'REQUESTS', label: 'Requests to Cashiering' },
  { id: 'HISTORY', label: 'History' },
];

const DISPOSITION_COLUMNS: Column<CollectorDisposition>[] = [
  { key: 'at', header: 'Recorded', render: (d) => formatDateTime(d.createdAt) },
  { key: 'code', header: 'Disposition', render: (d) => humanize(d.dispositionCode) },
  { key: 'action', header: 'Cashiering', render: (d) => ACTION_LABELS[d.cashieringAction] },
  { key: 'invoice', header: 'Invoice No.', render: (d) => d.invoiceNo ?? '' },
  {
    key: 'amount',
    header: 'Amount',
    numeric: true,
    render: (d) => (d.amount === undefined ? 'Whole balance' : <Amount value={d.amount} />),
  },
  { key: 'by', header: 'By', render: (d) => d.createdBy },
  { key: 'remarks', header: 'Remarks', render: (d) => d.remarks ?? '' },
];

const HISTORY_COLUMNS: Column<HistoryEvent>[] = [
  { key: 'at', header: 'When', render: (e) => formatDateTime(e.at) },
  { key: 'event', header: 'Event', render: (e) => humanize(e.event) },
  { key: 'what', header: 'Description', render: (e) => e.description },
  {
    key: 'amount',
    header: 'Amount',
    numeric: true,
    render: (e) => (e.amount === undefined ? '' : <Amount value={e.amount} />),
  },
  { key: 'by', header: 'By', render: (e) => e.by },
  { key: 'ref', header: 'Reference', render: (e) => e.reference ?? '' },
];

function Facts({ row }: Readonly<{ row: UnappliedRow }>) {
  return (
    <dl className="detail-list">
      {paymentFacts(row).map(([label, value]) => (
        <div key={label} style={{ display: 'contents' }}>
          <dt>{label}</dt>
          <dd>{value ?? '—'}</dd>
        </div>
      ))}
    </dl>
  );
}

function Summary({ row }: Readonly<{ row: UnappliedRow }>) {
  const status = cashieringStatus(row.cashieringStatus);
  const disposition =
    row.dispositionCode === undefined ? 'None yet' : humanize(row.dispositionCode);
  return (
    <RecordSummary
      title={row.payor ?? 'Unknown payor'}
      chips={
        <>
          <ReferenceChip label="Unapplied" value={row.unappliedRef} />
          <span className="tag">{TAB_LABELS[row.cashieringTab]}</span>
          {status !== undefined && <StatusBadge status={status} />}
        </>
      }
      facts={[
        {
          icon: CalendarDays,
          label: 'Payment Date',
          value: `${formatDate(row.paymentDate)} (${row.ageDays} days)`,
        },
        { icon: Wallet, label: `Paid (${row.currency})`, value: formatAmount(row.amount) },
        {
          icon: CreditCard,
          label: `Unapplied (${row.currency})`,
          value: formatAmount(row.balance),
        },
        { icon: Landmark, label: 'Transaction', value: row.transactionNo ?? '—' },
        { icon: FileText, label: 'Collector Disposition', value: disposition },
        { icon: UserRound, label: 'Account Officer', value: row.account?.aoUsername ?? '—' },
      ]}
    />
  );
}

function DetailTabs({
  detail,
  companyId,
}: Readonly<{ detail: UnappliedDetail; companyId: number }>) {
  const [tab, setTab] = useState<DetailTab>('DETAILS');
  const ref = detail.row.unappliedRef;
  const history = useQuery({
    queryKey: ['collections', 'unapplied', 'history', companyId, ref],
    queryFn: () => unappliedApi.history(companyId, ref),
    enabled: companyId > 0 && tab === 'HISTORY',
  });
  return (
    <Card flush>
      <div>
        <Tabs tabs={TABS} active={tab} onChange={setTab} />
        <div className="card-body">
          {tab === 'DETAILS' && <Facts row={detail.row} />}
          {tab === 'DISPOSITIONS' && (
            <DataTable
              caption="Collector dispositions"
              columns={DISPOSITION_COLUMNS}
              rows={detail.dispositions}
              rowKey={(d) => d.id}
              emptyMessage="No collector disposition yet"
            />
          )}
          {tab === 'REQUESTS' && (
            <DataTable
              caption="Requests to Cashiering"
              columns={REQUEST_COLUMNS}
              rows={detail.requests}
              rowKey={(r) => r.id}
              emptyMessage="No request sent to Cashiering"
            />
          )}
          {tab === 'HISTORY' && (
            <DataTable
              caption="History"
              columns={HISTORY_COLUMNS}
              rows={history.data ?? []}
              rowKey={(e) => `${e.at ?? ''}-${e.event}-${e.reference ?? ''}-${e.description}`}
              loading={history.isLoading}
              emptyMessage="No history"
            />
          )}
        </div>
      </div>
    </Card>
  );
}

/**
 * An unapplied payment as the collector sees it (BRCLXN.034-036, 040): the payment and the
 * matched account, the collector dispositions, the requests sent to Cashiering with their status,
 * and the whole history kept after the payment is applied or refunded.
 */
export default function UnappliedDetailPage() {
  const ref = decodeURIComponent(useParams().ref ?? '');
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [disposing, setDisposing] = useState<{ code?: string }>();
  const detail = useQuery({
    queryKey: ['collections', 'unapplied', 'detail', companyId, ref],
    queryFn: () => unappliedApi.detail(companyId, ref),
    enabled: companyId > 0,
  });
  const dispose = useMutation({
    mutationFn: (draft: DispositionDraft) => unappliedApi.dispose(companyId, ref, draft),
    onSuccess: async () => {
      setDisposing(undefined);
      await queryClient.invalidateQueries({ queryKey: ['collections', 'unapplied'] });
      toast.success(`Disposition recorded on ${ref}`);
    },
  });
  if (detail.data === undefined) {
    return <RecordLoading error={detail.error} />;
  }
  const { row } = detail.data;
  const canWork = can('CLX_UNAPPLIED_WORK') && row.balance > 0;
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Collections · Unapplied Payments"
        backTo="/collections/unapplied"
        title={row.unappliedRef}
        description={`Unapplied payment of ${row.payor ?? 'an unknown payor'}`}
        actions={
          canWork && (
            <>
              <Button variant="secondary" onClick={() => setDisposing({})}>
                Record Disposition
              </Button>
              <Button
                variant="accent"
                onClick={() => setDisposing({ code: 'FOR_APPLICATION_TO_INVOICE' })}
              >
                Request Application
              </Button>
            </>
          )
        }
      />
      <Summary row={row} />
      <DetailTabs detail={detail.data} companyId={companyId} />
      {disposing !== undefined && (
        <DispositionDialog
          item={row}
          initialCode={disposing.code}
          busy={dispose.isPending}
          error={dispose.error}
          onClose={() => setDisposing(undefined)}
          onSave={(draft) => dispose.mutate(draft)}
        />
      )}
    </div>
  );
}
