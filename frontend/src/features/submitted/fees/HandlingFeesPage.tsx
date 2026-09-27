import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Tags, Upload } from 'lucide-react';
import { useState } from 'react';
import { submittedApi } from '@/api/submitted';
import type { AmbiguousView, FeeView } from '@/api/submitted';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ConfirmButton } from '@/components/ui/ConfirmButton';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Notice } from '@/components/ui/Notice';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useTabParam } from '@/components/ui/useTabParam';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatAmount, formatDate, formatDateTime } from '@/utils/format';
import { SUBMITTED_SECTION } from '../common/submittedCodes';
import { UploadPanel } from '../common/UploadPanel';

const TABS = [
  { id: 'BILLED', label: 'Billed', statuses: ['BILLED'] },
  { id: 'TAGGED', label: 'Tagged', statuses: ['TAGGED'] },
  { id: 'APPLIED', label: 'Applied', statuses: ['APPLIED'] },
  { id: 'CANCELLED', label: 'Cancelled', statuses: ['CANCELLED'] },
  { id: 'AMBIGUOUS', label: 'Several Matches', statuses: [] as string[] },
] as const;

type TabId = (typeof TABS)[number]['id'];

const CHANNELS: Record<string, string> = { CLPC: 'CLPC (collection)', OTC: 'Over the counter' };

const channelLabel = (c: string | null) => (c === null ? '—' : (CHANNELS[c] ?? c));

function Ambiguous({ onDone }: Readonly<{ onDone: () => void }>) {
  const companyId = useCompanyId();
  const toast = useToast();
  const list = useQuery({
    queryKey: ['submitted', 'fees-ambiguous', companyId],
    queryFn: () => submittedApi.ambiguous(companyId),
    enabled: companyId > 0,
  });
  const tag = useMutation({
    mutationFn: ({ feeId, ref }: { feeId: number; ref: string }) => submittedApi.tagFee(feeId, ref),
    onSuccess: (f) => {
      toast.success(`${f.feeNo} tagged`);
      onDone();
    },
  });
  return (
    <div className="stack">
      <ErrorAlert error={list.error ?? tag.error} />
      {(list.data ?? []).length === 0 && !list.isLoading && (
        <Notice tone="success">No payment matches several handling fees.</Notice>
      )}
      {(list.data ?? []).map((a: AmbiguousView) => (
        <Card
          key={a.unappliedRef}
          title={`Payment ${a.unappliedRef} · ${a.currency} ${formatAmount(a.amount)} · ${formatDate(a.paymentDate)}`}
          flush
        >
          <DataTable<FeeView>
            rows={a.fees}
            rowKey={(f) => f.id}
            columns={[
              { key: 'no', header: 'Fee No.', kind: 'code', render: (f) => f.feeNo },
              { key: 'pn', header: 'PN No.', kind: 'code', render: (f) => f.pnNo ?? '—' },
              { key: 'loc', header: 'Location', render: (f) => f.locationRef ?? '—' },
              {
                key: 'amount',
                header: 'Amount',
                kind: 'amount',
                render: (f) => formatAmount(f.amount),
              },
              {
                key: 'tag',
                header: 'Actions',
                render: (f) => (
                  <ConfirmButton
                    variant="ghost"
                    confirm={{
                      title: `Tag ${f.feeNo}`,
                      record: `Payment ${a.unappliedRef}`,
                      effect:
                        'The payment is recognised as handling fee income by Cashiering, which issues the official receipt.',
                    }}
                    onConfirm={() => tag.mutateAsync({ feeId: f.id, ref: a.unappliedRef })}
                  >
                    Tag This Fee
                  </ConfirmButton>
                ),
              },
            ]}
          />
        </Card>
      ))}
    </div>
  );
}

/**
 * Handling Fees (FR-SP-070 to 073): the fees billed from the billing file, their tagging to the
 * unapplied payments by PN (CLPC) or location (over the counter), the application by Cashiering
 * with an official receipt, and the payments matching several fees.
 */
export default function HandlingFeesPage() {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [tab, setTab] = useTabParam<TabId>(
    TABS.map((t) => t.id),
    'BILLED',
  );
  const [upload, setUpload] = useState(false);
  const [cancelling, setCancelling] = useState<FeeView>();
  const statuses = TABS.find((t) => t.id === tab)?.statuses ?? [];
  const list = useQuery({
    queryKey: ['submitted', 'fees', companyId, tab],
    queryFn: () => submittedApi.fees(companyId, statuses),
    enabled: companyId > 0 && tab !== 'AMBIGUOUS',
  });
  const refresh = () => void queryClient.invalidateQueries({ queryKey: ['submitted'] });
  const tagAll = useMutation({
    mutationFn: () => submittedApi.tagFees(companyId),
    onSuccess: (t) => {
      toast.success(
        `${String(t.tagged)} payments tagged, ${String(t.ambiguous.length)} with several matches`,
      );
      refresh();
    },
  });
  const cancel = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason: string }) =>
      submittedApi.cancelFee(id, reason),
    onSuccess: () => {
      setCancelling(undefined);
      refresh();
    },
  });
  return (
    <div className="stack">
      <PageHeader
        section={SUBMITTED_SECTION}
        title="Handling Fees"
        description="Handling fees billed to the borrowers and their payments in the unapplied collections."
        actions={
          <>
            <Button variant="secondary" icon={<Upload size={16} />} onClick={() => setUpload(true)}>
              Upload Billing File
            </Button>
            <Button
              icon={<Tags size={16} />}
              disabled={tagAll.isPending}
              onClick={() => tagAll.mutate()}
            >
              Tag Now
            </Button>
          </>
        }
      />
      {upload && (
        <UploadPanel
          label="Upload Handling Fee Billing"
          handler="SBM_HANDLING_FEE_BILLING"
          onCommitted={refresh}
          onClose={() => setUpload(false)}
        />
      )}
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      <ErrorAlert error={list.error ?? tagAll.error} />
      {tab === 'AMBIGUOUS' ? (
        <Ambiguous onDone={refresh} />
      ) : (
        <Card flush>
          <DataTable<FeeView>
            loading={list.isLoading}
            rows={list.data?.content ?? []}
            rowKey={(f) => f.id}
            emptyMessage="No handling fee in this tab"
            columns={[
              { key: 'no', header: 'Fee No.', kind: 'code', render: (f) => f.feeNo },
              { key: 'pn', header: 'PN No.', kind: 'code', render: (f) => f.pnNo ?? '—' },
              { key: 'loc', header: 'Location', render: (f) => f.locationRef ?? '—' },
              {
                key: 'date',
                header: 'Billing Date',
                kind: 'date',
                render: (f) => formatDate(f.billingDate),
              },
              {
                key: 'amount',
                header: 'Amount',
                kind: 'amount',
                render: (f) => formatAmount(f.amount),
              },
              {
                key: 'status',
                header: 'Status',
                kind: 'status',
                render: (f) => <StatusBadge status={f.status} />,
              },
              { key: 'channel', header: 'Channel', render: (f) => channelLabel(f.channel) },
              {
                key: 'payment',
                header: 'Payment',
                kind: 'code',
                render: (f) => f.unappliedRef ?? '—',
              },
              { key: 'or', header: 'OR No.', kind: 'code', render: (f) => f.orNo ?? '—' },
              {
                key: 'by',
                header: 'Tagged By',
                render: (f) => (f.taggedBy ? <UserName login={f.taggedBy} /> : '—'),
              },
              {
                key: 'at',
                header: 'Tagged',
                kind: 'datetime',
                render: (f) => formatDateTime(f.taggedAt),
              },
              {
                key: 'actions',
                header: 'Actions',
                render: (f) =>
                  f.status === 'BILLED' ? (
                    <Button variant="ghost" onClick={() => setCancelling(f)}>
                      Cancel
                    </Button>
                  ) : null,
              },
            ]}
          />
        </Card>
      )}
      {cancelling && (
        <ConfirmDialog
          title="Cancel Handling Fee"
          record={cancelling.feeNo}
          effect="The fee is no longer tagged to any payment."
          confirmLabel="Cancel Fee"
          reason="required"
          destructive
          busy={cancel.isPending}
          error={cancel.error}
          onConfirm={(reason) => cancel.mutate({ id: cancelling.id, reason })}
          onClose={() => setCancelling(undefined)}
        />
      )}
    </div>
  );
}
