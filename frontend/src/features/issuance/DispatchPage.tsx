import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Send } from 'lucide-react';
import { useState } from 'react';
import { issuanceApi } from '@/api/issuance';
import { useAuth } from '@/auth/authContext';
import type { DispatchLog, IssuanceRow, Outcome } from '@/api/issuance';
import { ItemResultsDialog } from '@/components/broking/ItemResultsDialog';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import { SendEmailDialog } from '@/components/broking/SendEmailDialog';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';
import { DispatchManyDialog } from './IssuanceBulkBar';
import { rowKeyOf } from './issuanceLogic';

const TABS = [
  { id: 'ready', label: 'Ready to Dispatch' },
  { id: 'report', label: 'Dispatch Report' },
] as const;

type TabId = (typeof TABS)[number]['id'];

function DispatchOneDialog({
  row,
  onClose,
  onSent,
}: Readonly<{ row: IssuanceRow; onClose: () => void; onSent: () => void }>) {
  const toast = useToast();
  const id = row.epolicyId ?? 0;
  const draft = useQuery({
    queryKey: ['issuance', 'dispatch-draft', id],
    queryFn: () => issuanceApi.dispatchDraft(id),
  });
  const send = useMutation({
    mutationFn: (d: {
      to: string[];
      cc: string[];
      subject: string;
      body: string;
      passwordHint?: string;
    }) => issuanceApi.dispatch(id, d),
    onSuccess: (e) => {
      toast.success(`E-policy of ${e.arn} sent`);
      onSent();
    },
  });
  if (draft.data === undefined) {
    return <ErrorAlert error={draft.error} />;
  }
  return (
    <SendEmailDialog
      title={`Send E-policy of ${row.arn}`}
      initial={{
        ...draft.data,
        to: draft.data.to.join(', '),
        cc: draft.data.cc.join(', '),
        protect: true,
      }}
      attachmentNames={[row.epolicyFile ?? 'e-policy.pdf']}
      busy={send.isPending}
      error={send.error}
      onClose={onClose}
      onSend={(d) =>
        send.mutate({
          to: d.to,
          cc: d.cc,
          subject: d.subject,
          body: d.body,
          passwordHint: d.passwordHint,
        })
      }
    />
  );
}

function ReadyToDispatch({ companyId }: Readonly<{ companyId: number }>) {
  const { can } = useAuth();
  const sender = can('EPOLICY_SEND');
  const queryClient = useQueryClient();
  const selection = useRowSelection();
  const [page, setPage] = useState(0);
  const [single, setSingle] = useState<IssuanceRow | null>(null);
  const [many, setMany] = useState(false);
  const [outcomes, setOutcomes] = useState<Outcome[] | null>(null);
  const list = useQuery({
    queryKey: ['issuance', 'workbench', companyId, 'READY_TO_DISPATCH', '', page],
    queryFn: () => issuanceApi.workbench(companyId, 'READY_TO_DISPATCH', undefined, page),
    enabled: companyId > 0,
  });
  const rows = list.data?.content ?? [];
  const chosen = rows.filter((r) => selection.has(rowKeyOf(r)));
  const refresh = () => void queryClient.invalidateQueries({ queryKey: ['issuance'] });
  const dispatch = useMutation({
    mutationFn: (hint: string) =>
      issuanceApi.dispatchMany(
        chosen.flatMap((r) => (r.epolicyId === undefined ? [] : [r.epolicyId])),
        hint.trim() || undefined,
      ),
    onSuccess: (done) => {
      setMany(false);
      selection.clear();
      setOutcomes(done);
      refresh();
    },
  });
  return (
    <Card
      title="Ready to Dispatch"
      flush
      actions={
        sender && (
          <Button
            variant="primary"
            icon={<Send size={16} />}
            disabled={chosen.length === 0}
            onClick={() => setMany(true)}
          >
            Send Selected
          </Button>
        )
      }
    >
      <ErrorAlert error={list.error} />
      <DataTable<IssuanceRow>
        caption="E-policies ready to dispatch"
        loading={list.isLoading}
        rows={rows}
        rowKey={rowKeyOf}
        emptyMessage="No items to display"
        columns={[
          selectionColumn(rows, rowKeyOf, selection, (r) => r.arn),
          {
            key: 'client',
            header: 'Name / Client Code',
            render: (r) => (
              <span>
                {r.clientName}
                <span className="cell-sub">{r.clientCode}</span>
              </span>
            ),
          },
          { key: 'arn', header: 'Proposal No.', render: (r) => <code>{r.arn}</code> },
          { key: 'policy', header: 'Policy No.', render: (r) => r.policyNumbers.join(', ') },
          { key: 'file', header: 'E-policy', render: (r) => r.epolicyFile ?? '' },
          {
            key: 'send',
            header: '',
            render: (r) =>
              sender && (
                <Button
                  size="sm"
                  variant="secondary"
                  icon={<Send size={14} />}
                  onClick={() => setSingle(r)}
                >
                  Send
                </Button>
              ),
          },
        ]}
      />
      <PageFooter data={list.data} noun="e-policies" onPage={setPage} />
      {single && (
        <DispatchOneDialog
          row={single}
          onClose={() => setSingle(null)}
          onSent={() => {
            setSingle(null);
            refresh();
          }}
        />
      )}
      {many && (
        <DispatchManyDialog
          count={chosen.length}
          busy={dispatch.isPending}
          error={dispatch.error}
          onSend={(hint) => dispatch.mutate(hint)}
          onClose={() => setMany(false)}
        />
      )}
      {outcomes && (
        <ItemResultsDialog
          title="Send E-policies"
          results={outcomes.map((o) => ({ reference: o.reference, ok: o.ok, message: o.message }))}
          onClose={() => setOutcomes(null)}
        />
      )}
    </Card>
  );
}

function DispatchReport() {
  const [page, setPage] = useState(0);
  const log = useQuery({
    queryKey: ['issuance', 'dispatch-log', page],
    queryFn: () => issuanceApi.dispatchLog(undefined, page),
  });
  return (
    <Card title="Dispatch Report" flush>
      <ErrorAlert error={log.error} />
      <DataTable<DispatchLog>
        caption="E-policy dispatch report"
        loading={log.isLoading}
        rows={log.data?.content ?? []}
        rowKey={(m) => m.id}
        emptyMessage="No items to display"
        columns={[
          {
            key: 'when',
            header: 'Queued',
            render: (m) => `${formatDateTime(m.createdAt)} by ${m.createdBy}`,
          },
          { key: 'arn', header: 'Proposal No.', render: (m) => <code>{m.reference ?? ''}</code> },
          { key: 'to', header: 'To', render: (m) => m.recipients },
          { key: 'subject', header: 'Subject', render: (m) => m.subject },
          { key: 'status', header: 'Outcome', render: (m) => <StatusBadge status={m.status} /> },
          {
            key: 'sent',
            header: 'Delivered',
            render: (m) => (m.sentAt ? formatDateTime(m.sentAt) : (m.lastError ?? '—')),
          },
        ]}
      />
      <PageFooter data={log.data} noun="e-mails" onPage={setPage} />
    </Card>
  );
}

/**
 * E-policy dispatch (BRNB.077/035/078): send the confirmed e-policies to the clients, one by one
 * or in batch, encrypted with the password in a separate e-mail; the dispatch report lists every
 * e-policy e-mail with its outcome from the messaging log.
 */
export default function DispatchPage() {
  const companyId = useCompanyId();
  const [tab, setTab] = useState<TabId>('ready');
  return (
    <div className="stack">
      <PageHeader
        section="Policy Issuance"
        title="E-policy Dispatch"
        description="Send the e-policies to the clients, encrypted; the password follows in a separate e-mail. The report shows each delivery and its outcome."
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'ready' ? <ReadyToDispatch companyId={companyId} /> : <DispatchReport />}
    </div>
  );
}
