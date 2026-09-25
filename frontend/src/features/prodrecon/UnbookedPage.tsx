import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Amount } from '@/components/ui/Amount';
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
import { formatDate, humanize } from '@/utils/format';
import { ItemReviewDialog } from './ItemReviewDialog';
import { prodreconApi } from './prodreconApi';
import type { ReconItem, UnbookedStatus } from './prodreconApi';

const TABS: readonly { id: UnbookedStatus; label: string }[] = [
  { id: 'OPEN', label: 'Not Booked' },
  { id: 'PREBOOKED', label: 'Pre-booked' },
  { id: 'BOOKED', label: 'Booked Since' },
  { id: 'CLOSED', label: 'Closed' },
];

const COLUMNS: Column<ReconItem>[] = [
  {
    key: 'policy',
    header: 'Policy / Reference',
    render: (r) => (
      <>
        <strong>{r.insurer?.policyNo ?? '—'}</strong>
        <div className="muted">{r.insurer?.referenceNo}</div>
      </>
    ),
  },
  { key: 'assured', header: 'Assured', render: (r) => r.insurer?.assuredName ?? '' },
  {
    key: 'period',
    header: 'Period',
    render: (r) => `${formatDate(r.insurer?.periodFrom)} – ${formatDate(r.insurer?.periodTo)}`,
  },
  {
    key: 'gross',
    header: 'Gross Premium',
    numeric: true,
    render: (r) => <Amount value={r.insurer?.grossPremium} />,
  },
  { key: 'arn', header: 'Pre-booked ARN', render: (r) => r.prebookedArn ?? '' },
  { key: 'invoice', header: 'Booked As', render: (r) => r.invoiceNo ?? '' },
  {
    key: 'disposition',
    header: 'Disposition',
    render: (r) => (r.feedback?.disposition ? humanize(r.feedback.disposition) : ''),
  },
  {
    key: 'status',
    header: 'Status',
    render: (r) => <StatusBadge status={r.unbookedStatus ?? r.status} />,
  },
];

/**
 * Unbooked repository (PRCID.019-024): accounts the insurers reported that BDOI has not booked,
 * followed until they are booked (matched automatically on booking) or closed with a disposition.
 */
export default function UnbookedPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<UnbookedStatus>('OPEN');
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [review, setReview] = useState<ReconItem>();
  const items = useQuery({
    queryKey: ['prodrecon', 'unbooked', companyId, tab, q, page],
    queryFn: () => prodreconApi.unbooked(companyId, { status: tab, q: q || undefined }, page),
    enabled: companyId > 0,
  });
  const save = useMutation({
    mutationFn: (v: Parameters<typeof prodreconApi.feedback>) => prodreconApi.feedback(...v),
    onSuccess: async () => {
      setReview(undefined);
      await queryClient.invalidateQueries({ queryKey: ['prodrecon'] });
      toast.success('Feedback saved');
    },
  });
  return (
    <div className="stack">
      <PageHeader
        section="Product Reconciliation"
        title="Unbooked Accounts"
        description="Accounts reported by the insurers that are not booked by BDOI, followed to booking or closure."
      />
      <ErrorAlert error={items.error} />
      <Card>
        <div className="stack">
          <Tabs
            tabs={TABS}
            active={tab}
            onChange={(t) => {
              setTab(t);
              setPage(0);
            }}
          />
          <WorklistToolbar
            placeholder="Search Policy, Reference or Assured"
            onSearch={(text) => {
              setQ(text.trim());
              setPage(0);
            }}
          />
          <DataTable
            caption="Unbooked accounts"
            columns={COLUMNS}
            rows={items.data?.content ?? []}
            rowKey={(r) => r.id}
            loading={items.isLoading}
            onRowClick={setReview}
            emptyMessage="No items to display"
          />
          <PageFooter data={items.data} noun="accounts" onPage={setPage} />
        </div>
      </Card>
      {review !== undefined && (
        <ItemReviewDialog
          item={review}
          editable={can('RECON_PROCESS') && review.unbookedStatus !== 'CLOSED'}
          busy={save.isPending}
          error={save.error}
          onClose={() => setReview(undefined)}
          onSave={(feedback) => save.mutate([review.id, feedback])}
        />
      )}
    </div>
  );
}
