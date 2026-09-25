import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Building2, CalendarRange, CheckCheck, Inbox, Send, Sigma } from 'lucide-react';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { Attachments } from '@/components/attachments/Attachments';
import { RecordSummary } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { CycleItemsTab } from './CycleItemsTab';
import { SendExtractDialog } from './ExtractParts';
import { extractColumns } from './extractColumns';
import { CYCLE_ENTITY, prodreconApi } from './prodreconApi';
import type { EarlyIncentiveLine, ReconCycle, ReconExtract } from './prodreconApi';
import { matchedPercent, monthLabel } from './prodreconLogic';

const TABS = [
  { id: 'items', label: 'Items' },
  { id: 'extracts', label: 'Registers Sent' },
  { id: 'incentive', label: 'Early Incentive' },
  { id: 'documents', label: 'Documents' },
] as const;

type TabId = (typeof TABS)[number]['id'];

const INCENTIVE_COLUMNS: Column<EarlyIncentiveLine>[] = [
  { key: 'invoice', header: 'Invoice No.', render: (l) => <strong>{l.invoiceNo}</strong> },
  { key: 'assured', header: 'Assured', render: (l) => l.assuredName ?? '' },
  { key: 'inception', header: 'Inception', render: (l) => formatDate(l.inceptionDate) },
  { key: 'remitted', header: 'Remitted', render: (l) => formatDate(l.remittedOn) },
  { key: 'days', header: 'Days', numeric: true, render: (l) => l.days ?? '' },
  {
    key: 'expected',
    header: 'Expected',
    numeric: true,
    render: (l) => <Amount value={l.expected} />,
  },
  {
    key: 'insurer',
    header: 'Per Insurer',
    numeric: true,
    render: (l) => <Amount value={l.insurerIncentive} />,
  },
  { key: 'outcome', header: 'Outcome', render: (l) => <StatusBadge status={l.outcome} /> },
];

function ExtractsTab({ cycle, maySend }: Readonly<{ cycle: ReconCycle; maySend: boolean }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [sending, setSending] = useState<ReconExtract>();
  const extracts = useQuery({
    queryKey: ['prodrecon', 'cycle-extracts', cycle.id],
    queryFn: () => prodreconApi.cycleExtracts(cycle.id),
  });
  const send = useMutation({
    mutationFn: (v: { id: number; to: string[]; cc: string[] }) =>
      prodreconApi.send(v.id, v.to, v.cc),
    onSuccess: async (e) => {
      setSending(undefined);
      await queryClient.invalidateQueries({ queryKey: ['prodrecon'] });
      toast.success(`${e.fileName} sent`);
    },
  });
  return (
    <Card>
      <div className="stack">
        <ErrorAlert error={extracts.error ?? send.error} />
        <DataTable
          caption="Registers of the cycle"
          columns={extractColumns(maySend && !cycle.closed ? setSending : undefined)}
          rows={extracts.data ?? []}
          rowKey={(e) => e.id}
          loading={extracts.isLoading}
          emptyMessage="No items to display"
        />
      </div>
      {sending !== undefined && (
        <SendExtractDialog
          extract={sending}
          busy={send.isPending}
          onClose={() => setSending(undefined)}
          onSend={(to, cc) => send.mutate({ id: sending.id, to, cc })}
        />
      )}
    </Card>
  );
}

function IncentiveTab({ cycle }: Readonly<{ cycle: ReconCycle }>) {
  const lines = useQuery({
    queryKey: ['prodrecon', 'early-incentive', cycle.id],
    queryFn: () => prodreconApi.earlyIncentive(cycle.id),
  });
  return (
    <Card title="Early Incentive Validation">
      <div className="stack">
        <p className="muted">
          The insurer&apos;s early remittance incentive checked against the remittance date of each
          booked account. Rates and windows come from the remittance rules when they are maintained.
        </p>
        <ErrorAlert error={lines.error} />
        <DataTable
          caption="Early incentive validation"
          columns={INCENTIVE_COLUMNS}
          rows={lines.data ?? []}
          rowKey={(l) => l.invoiceNo}
          loading={lines.isLoading}
          emptyMessage="No items to display"
        />
      </div>
    </Card>
  );
}

function CloseDialog({
  busy,
  onClose,
  onConfirm,
}: Readonly<{ busy: boolean; onClose: () => void; onConfirm: (comment: string) => void }>) {
  const [comment, setComment] = useState('');
  return (
    <Modal
      title="Close Reconciliation Cycle"
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={busy} onClick={() => onConfirm(comment)}>
            Close Cycle
          </Button>
        </>
      }
    >
      <Field label="Comment" hint="Why the cycle is closed with items still open, if any">
        {(id) => (
          <textarea
            id={id}
            className="textarea"
            rows={3}
            maxLength={500}
            value={comment}
            onChange={(e) => setComment(e.target.value)}
          />
        )}
      </Field>
    </Modal>
  );
}

function Summary({ cycle: c }: Readonly<{ cycle: ReconCycle }>) {
  return (
    <RecordSummary
      title={c.insurerCode}
      chips={
        <>
          <ReferenceChip label="Cycle" value={c.cycleNo} />
          <StatusBadge status={c.stage} />
        </>
      }
      facts={[
        { icon: CalendarRange, label: 'Production Month', value: monthLabel(c.productionMonth) },
        { icon: Sigma, label: 'Items', value: c.counts.total },
        {
          icon: CheckCheck,
          label: 'Matched',
          value: `${String(matchedPercent(c.counts))}% (${String(c.counts.discrepancy)} with differences)`,
        },
        {
          icon: Building2,
          label: 'BDOI Only / Insurer Only',
          value: `${String(c.counts.bdoiOnly)} / ${String(c.counts.insurerOnly)}`,
        },
        { icon: Send, label: 'Sent to Insurer', value: formatDateTime(c.sentAt) },
        { icon: Inbox, label: 'Last Feedback', value: formatDateTime(c.lastUploadAt) },
      ]}
    />
  );
}

/**
 * A reconciliation cycle (PRCID.009-033): the insurer and month, where it stands, the items by
 * bucket with their review and feedback, the registers sent, the early incentive validation and
 * the documents.
 */
export default function ReconCyclePage() {
  const id = Number(useParams().id);
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<TabId>('items');
  const [closing, setClosing] = useState(false);
  const cycle = useQuery({
    queryKey: ['prodrecon', 'cycle', id],
    queryFn: () => prodreconApi.cycle(id),
  });
  const act = useMutation({
    mutationFn: (fn: () => Promise<ReconCycle>) => fn(),
    onSuccess: async (c) => {
      setClosing(false);
      await queryClient.invalidateQueries({ queryKey: ['prodrecon'] });
      toast.success(`${c.cycleNo} is now ${c.stage.toLowerCase().replaceAll('_', ' ')}`);
    },
  });
  if (cycle.data === undefined) {
    return cycle.error ? (
      <ErrorAlert error={cycle.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const c = cycle.data;
  const editable = can('RECON_PROCESS') && !c.closed;
  return (
    <div className="stack">
      <PageHeader
        section="Product Reconciliation · Cycles"
        backTo="/prodrecon/cycles"
        title={c.cycleNo}
        description={`${c.insurerCode} · ${monthLabel(c.productionMonth)}`}
        actions={
          editable ? (
            <>
              <Button variant="secondary" onClick={() => setClosing(true)}>
                Close Cycle
              </Button>
              <Button
                busy={act.isPending}
                onClick={() => act.mutate(() => prodreconApi.automatch(id))}
              >
                Run Matching
              </Button>
            </>
          ) : undefined
        }
      />
      <ErrorAlert error={act.error} />
      <Summary cycle={c} />
      <WorkflowPanel
        entityType={CYCLE_ENTITY}
        entityId={c.id}
        onChanged={() => void queryClient.invalidateQueries({ queryKey: ['prodrecon'] })}
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'items' && <CycleItemsTab cycle={c} editable={editable} />}
      {tab === 'extracts' && <ExtractsTab cycle={c} maySend={can('RECON_SEND')} />}
      {tab === 'incentive' && <IncentiveTab cycle={c} />}
      {tab === 'documents' && (
        <Attachments entityType={CYCLE_ENTITY} entityId={c.id} reference={c.cycleNo} />
      )}
      {closing && (
        <CloseDialog
          busy={act.isPending}
          onClose={() => setClosing(false)}
          onConfirm={(comment) => act.mutate(() => prodreconApi.close(id, comment))}
        />
      )}
    </div>
  );
}
