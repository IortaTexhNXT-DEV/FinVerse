import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Send, Undo2 } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime, humanize } from '@/utils/format';
import { adjustmentApi } from './api';
import type { PostingBatch, RequestSummary } from './api';
import { BatchResultDialog } from './BatchResultDialog';
import { RequestFlags } from './RequestParts';

type View = 'READY' | 'BATCHES';

const VIEWS: readonly { id: View; label: string }[] = [
  { id: 'READY', label: 'Ready for Posting' },
  { id: 'BATCHES', label: 'Posted Batches' },
];

const READY_COLUMNS: Column<RequestSummary>[] = [
  { key: 'no', header: 'Request No.', render: (r) => <strong>{r.requestNo}</strong> },
  { key: 'invoice', header: 'Invoice No.', render: (r) => r.invoiceNo },
  { key: 'assured', header: 'Assured', render: (r) => r.assuredName },
  {
    key: 'type',
    header: 'Type',
    render: (r) => humanize(r.requestType ?? r.endorsementType),
  },
  { key: 'effective', header: 'Effective', render: (r) => formatDate(r.effectiveDate) },
  {
    key: 'flags',
    header: 'Flags',
    render: (r) => (
      <RequestFlags
        negative={r.negative}
        quotationRequired={r.quotationRequired}
        duplicateOverride={r.duplicateOverride}
      />
    ),
  },
];

const BATCH_COLUMNS: Column<PostingBatch>[] = [
  { key: 'no', header: 'Batch No.', render: (b) => <strong>{b.batchNo}</strong> },
  { key: 'at', header: 'Posted', render: (b) => formatDateTime(b.createdAt) },
  { key: 'by', header: 'Posted By', render: (b) => b.createdBy },
  { key: 'posted', header: 'Posted', numeric: true, render: (b) => b.postedCount },
  { key: 'pending', header: 'To Re-apply', numeric: true, render: (b) => b.pendingCount },
  { key: 'failed', header: 'Failed', numeric: true, render: (b) => b.failedCount },
  { key: 'remarks', header: 'Remarks', render: (b) => b.remarks ?? '—' },
];

function ReadyList({ onPosted }: Readonly<{ onPosted: (b: PostingBatch) => void }>) {
  const companyId = useCompanyId();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const selection = useRowSelection();
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [dialog, setDialog] = useState<'post' | 'return' | null>(null);
  const list = useQuery({
    queryKey: ['adjustment', 'requests', companyId, 'FOR_POSTING', q, page],
    queryFn: () => adjustmentApi.requests(companyId, 'FOR_POSTING', q, page),
    enabled: companyId > 0,
  });
  const done = async () => {
    setDialog(null);
    selection.clear();
    await queryClient.invalidateQueries({ queryKey: ['adjustment'] });
  };
  const ids = selection.keys.map(Number);
  const post = useMutation({
    mutationFn: (remarks?: string) => adjustmentApi.postBatch(companyId, ids, remarks),
    onSuccess: async (batch) => {
      await done();
      onPosted(batch);
    },
  });
  const giveBack = useMutation({
    mutationFn: ({ reason, comment }: { reason: string; comment?: string }) =>
      adjustmentApi.returnRequests(ids, reason, comment),
    onSuccess: async (result) => {
      await done();
      toast.success(`${String(result.returned)} request(s) returned to the requester`);
    },
  });
  const rows = list.data?.content ?? [];
  const columns = [
    selectionColumn(
      rows,
      (r) => String(r.id),
      selection,
      (r) => r.requestNo,
    ),
    ...READY_COLUMNS,
  ];
  return (
    <>
      <WorklistToolbar
        placeholder="Search Request No."
        onSearch={(text) => {
          setQ(text);
          setPage(0);
        }}
      >
        <Button
          variant="secondary"
          icon={<Undo2 size={16} />}
          disabled={ids.length === 0}
          onClick={() => setDialog('return')}
        >
          Return Selected
        </Button>
        <Button
          variant="accent"
          icon={<Send size={16} />}
          disabled={ids.length === 0}
          onClick={() => setDialog('post')}
        >
          Post Selected
        </Button>
      </WorklistToolbar>
      <ErrorAlert error={list.error} />
      <DataTable
        caption="Requests ready for posting"
        columns={columns}
        rows={rows}
        rowKey={(r) => r.id}
        loading={list.isLoading}
        emptyMessage="No items to display"
        onRowClick={(r) => void navigate(`/adjustment/requests/${String(r.id)}`)}
      />
      <PageFooter data={list.data} noun="requests" onPage={setPage} />
      {dialog === 'post' && (
        <ActionDialog
          title={`Post ${String(ids.length)} Request(s)`}
          confirmLabel="Post Batch"
          busy={post.isPending}
          error={post.error}
          onConfirm={(note) => post.mutate(note.comment)}
          onClose={() => setDialog(null)}
        />
      )}
      {dialog === 'return' && (
        <ActionDialog
          title={`Return ${String(ids.length)} Request(s)`}
          reasonLov="ADJ_RETURN_REASON"
          confirmLabel="Return"
          busy={giveBack.isPending}
          error={giveBack.error}
          onConfirm={(note) =>
            giveBack.mutate({ reason: note.reasonCode ?? '', comment: note.comment })
          }
          onClose={() => setDialog(null)}
        />
      )}
    </>
  );
}

function BatchList({ onOpen }: Readonly<{ onOpen: (batchNo: string) => void }>) {
  const companyId = useCompanyId();
  const [page, setPage] = useState(0);
  const list = useQuery({
    queryKey: ['adjustment', 'batches', companyId, page],
    queryFn: () => adjustmentApi.batches(companyId, page),
    enabled: companyId > 0,
  });
  return (
    <>
      <ErrorAlert error={list.error} />
      <DataTable
        caption="Posting batches"
        columns={BATCH_COLUMNS}
        rows={list.data?.content ?? []}
        rowKey={(b) => b.batchNo}
        loading={list.isLoading}
        emptyMessage="No items to display"
        onRowClick={(b) => onOpen(b.batchNo)}
      />
      <PageFooter data={list.data} noun="batches" onPage={setPage} />
    </>
  );
}

/**
 * Posting batches (ADJID.005/006/017): the requests ready for posting, to return with a reason
 * when they do not qualify or to post together as a validation batch; and the batches posted with
 * the outcome of each request.
 */
export default function PostingBatchesPage() {
  const [view, setView] = useState<View>('READY');
  const [shown, setShown] = useState<PostingBatch | string | null>(null);
  return (
    <div className="stack">
      <PageHeader
        backTo="/adjustment"
        section="Client & Policy · Adjustment"
        title="Posting Batches"
        description="Review the requests ready for posting, return those that do not qualify and post the others as one validation batch."
      />
      <Card flush>
        <div className="work-tabs">
          <Tabs<View> tabs={VIEWS} active={view} onChange={setView} />
        </div>
        {view === 'READY' ? <ReadyList onPosted={setShown} /> : <BatchList onOpen={setShown} />}
      </Card>
      {shown !== null && <BatchResultDialog batch={shown} onClose={() => setShown(null)} />}
    </div>
  );
}
