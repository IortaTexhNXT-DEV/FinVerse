import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Upload } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime, humanize, today } from '@/utils/format';
import { commissionApi } from './commissionApi';
import type { DpList, Submission } from './commissionApi';

const TABS = [
  { id: 'lists', label: 'Lists Received' },
  { id: 'tracker', label: 'Branch Submissions' },
] as const;

type TabId = (typeof TABS)[number]['id'];

const LIST_COLUMNS: Column<DpList>[] = [
  {
    key: 'no',
    header: 'List No.',
    render: (l) => (
      <>
        <strong>{l.listNo}</strong>
        <div className="muted">{l.fileName}</div>
      </>
    ),
  },
  { key: 'source', header: 'Source', render: (l) => humanize(l.source) },
  { key: 'branch', header: 'Branch', render: (l) => l.branchCode ?? '' },
  { key: 'date', header: 'Submitted', render: (l) => formatDate(l.submissionDate) },
  { key: 'items', header: 'Accounts', numeric: true, render: (l) => l.itemCount },
  { key: 'valid', header: 'Valid', numeric: true, render: (l) => l.validCount },
  { key: 'excluded', header: 'Excluded', numeric: true, render: (l) => l.excludedCount },
  {
    key: 'at',
    header: 'Taken In',
    render: (l) => `${formatDateTime(l.createdAt)} · ${l.createdBy}`,
  },
];

const TRACKER_COLUMNS: Column<Submission>[] = [
  { key: 'branch', header: 'Branch', render: (s) => <strong>{s.branchName}</strong> },
  { key: 'code', header: 'Code', render: (s) => s.branchCode },
  { key: 'lists', header: 'Lists', numeric: true, render: (s) => s.lists },
  { key: 'accounts', header: 'Accounts', numeric: true, render: (s) => s.accounts },
  { key: 'last', header: 'Last Submission', render: (s) => formatDate(s.lastSubmission) },
  {
    key: 'status',
    header: 'Status',
    render: (s) => <StatusBadge status={s.received ? 'RECEIVED' : 'MISSING'} />,
  },
];

function UploadDialog({
  busy,
  error,
  onClose,
  onUpload,
}: Readonly<{ busy: boolean; error: unknown; onClose: () => void; onUpload: (f: File) => void }>) {
  const [file, setFile] = useState<File>();
  return (
    <Modal
      title="Upload DP List"
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={busy} disabled={file === undefined} onClick={() => file && onUpload(file)}>
            Upload and Validate
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <Field
          label="DP List File"
          required
          hint="xlsx or csv named <Branch>_DP_<yyyyMMdd>, e.g. HO_DP_20260930.xlsx, with Invoice No., Policy No., Insurer, Premium and Remarks columns"
        >
          {(id) => (
            <input
              id={id}
              type="file"
              className="input"
              accept=".xlsx,.csv"
              onChange={(e) => setFile(e.target.files?.[0])}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

function Tracker({ companyId }: Readonly<{ companyId: number }>) {
  const [from, setFrom] = useState(`${today().slice(0, 7)}-01`);
  const [to, setTo] = useState(today());
  const tracker = useQuery({
    queryKey: ['commission', 'tracker', companyId, from, to],
    queryFn: () => commissionApi.submissions(companyId, from, to),
    enabled: companyId > 0 && from !== '' && to !== '',
  });
  return (
    <div className="stack">
      <div className="row">
        <Field label="From">
          {(id) => (
            <input
              id={id}
              type="date"
              className="input"
              value={from}
              onChange={(e) => setFrom(e.target.value)}
            />
          )}
        </Field>
        <Field label="To">
          {(id) => (
            <input
              id={id}
              type="date"
              className="input"
              value={to}
              onChange={(e) => setTo(e.target.value)}
            />
          )}
        </Field>
      </div>
      <ErrorAlert error={tracker.error} />
      <DataTable
        caption="Branch submissions"
        columns={TRACKER_COLUMNS}
        rows={tracker.data ?? []}
        rowKey={(s) => s.branchCode}
        loading={tracker.isLoading}
        emptyMessage="No items to display"
      />
    </div>
  );
}

/**
 * Direct payment lists (CMRID.001/002/012): lists uploaded by the branches and Head Office or
 * pulled from the collection feed, each account validated on arrival, and the branches that
 * have not submitted in a period.
 */
export default function DpListsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<TabId>('lists');
  const [page, setPage] = useState(0);
  const [uploading, setUploading] = useState(false);
  const lists = useQuery({
    queryKey: ['commission', 'lists', companyId, page],
    queryFn: () => commissionApi.lists(companyId, page),
    enabled: companyId > 0 && tab === 'lists',
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['commission'] });
  const upload = useMutation({
    mutationFn: (file: File) => commissionApi.uploadList(companyId, file),
    onSuccess: async (l) => {
      setUploading(false);
      await refresh();
      toast.success(
        `${l.listNo}: ${String(l.validCount)} of ${String(l.itemCount)} account(s) valid`,
      );
    },
  });
  const pull = useMutation({
    mutationFn: () => commissionApi.pull(companyId),
    onSuccess: async (taken) => {
      await refresh();
      toast.success(`${String(taken.length)} list(s) taken from the collection feed`);
    },
  });
  const mayProcess = can('COMMREC_PROCESS');
  return (
    <div className="stack">
      <PageHeader
        section="Commission Receivables"
        title="DP Lists"
        description="Direct payment accounts reported by the branches, Head Office and the collection feed."
        actions={
          mayProcess ? (
            <>
              <Button variant="secondary" busy={pull.isPending} onClick={() => pull.mutate()}>
                Pull from Collection
              </Button>
              <Button icon={<Upload size={16} />} onClick={() => setUploading(true)}>
                Upload DP List
              </Button>
            </>
          ) : undefined
        }
      />
      <ErrorAlert error={lists.error ?? pull.error} />
      <Card>
        <div className="stack">
          <Tabs tabs={TABS} active={tab} onChange={setTab} />
          {tab === 'lists' ? (
            <>
              <DataTable
                caption="DP lists"
                columns={LIST_COLUMNS}
                rows={lists.data?.content ?? []}
                rowKey={(l) => l.id}
                loading={lists.isLoading}
                onRowClick={(l) => void navigate(`/commission/dp/items?listId=${String(l.id)}`)}
                emptyMessage="No items to display"
              />
              <PageFooter data={lists.data} noun="lists" onPage={setPage} />
            </>
          ) : (
            <Tracker companyId={companyId} />
          )}
        </div>
      </Card>
      {uploading && (
        <UploadDialog
          busy={upload.isPending}
          error={upload.error}
          onClose={() => setUploading(false)}
          onUpload={(file) => upload.mutate(file)}
        />
      )}
    </div>
  );
}
