import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Download, FilePlus2 } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { api, saveFile, toQuery } from '@/api/client';
import { useAuth } from '@/auth/authContext';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { CodeSelect, TextField } from './CashFields';
import { recordActions } from './recordLogic';
import { recordsApi } from './recordsApi';
import type {
  PostingOutcome,
  ReceiptRecord,
  RecordFilters,
  RecordKind,
  RecordStage,
} from './recordsApi';
import './cashiering.css';

const TABS = [
  { id: 'CREATION', label: 'AR/OR for Issuance Posting' },
  { id: 'CANCELLATION', label: 'Cancellation (AR/OR) for Posting' },
  { id: 'REINSTATEMENT', label: 'Reinstatement (AR/OR) for Posting' },
] as const;

const STAGES: readonly RecordStage[] = [
  'FOR_POSTING',
  'RETURNED',
  'CREATED',
  'POSTED',
  'RECORD_CANCELLED',
];
const STAGE_LABELS: Record<string, string> = {
  FOR_POSTING: 'For Posting',
  RETURNED: 'Returned',
  CREATED: 'Created',
  POSTED: 'Posted',
  RECORD_CANCELLED: 'Record Cancelled',
};

const NUMBER_HEADER: Record<RecordKind, string> = {
  CREATION: 'Creation Record Number',
  CANCELLATION: 'Cancellation Record Number',
  REINSTATEMENT: 'Reinstatement Record Number',
};

function columnsOf(kind: RecordKind): Column<ReceiptRecord>[] {
  const cols: Column<ReceiptRecord>[] = [
    { key: 'no', header: NUMBER_HEADER[kind], render: (r) => <strong>{r.recordNo}</strong> },
    { key: 'kind', header: 'Receipt Type', render: (r) => r.receiptKind },
    {
      key: 'type',
      header: 'Record Type',
      truncate: true,
      render: (r) => r.receiptTypeLabel ?? r.receiptType ?? '',
    },
  ];
  if (kind !== 'CREATION') {
    cols.push({ key: 'receipt', header: 'AR/OR Number', render: (r) => r.receiptNo ?? '' });
  }
  cols.push(
    { key: 'amount', header: 'Amount', numeric: true, render: (r) => <Amount value={r.total} /> },
    {
      key: 'accounts',
      header: 'Account Number / Invoice Number',
      truncate: true,
      render: (r) => r.accountsText,
    },
    {
      key: 'client',
      header: 'Client Name',
      truncate: true,
      render: (r) => r.party?.clientName ?? r.party?.payorName ?? '',
    },
    {
      key: 'insurer',
      header: 'Insurer Name',
      truncate: true,
      render: (r) => r.party?.insurerName ?? '',
    },
    { key: 'branch', header: 'Receipting Branch', render: (r) => r.branchName ?? '' },
    { key: 'creator', header: 'Created by', render: (r) => r.createdBy ?? '' },
    { key: 'created', header: 'Date Created', render: (r) => formatDate(r.createdAt) },
    {
      key: 'status',
      header: 'Record Status',
      render: (r) => <StatusBadge status={r.stage} label={r.statusLabel} />,
    },
  );
  return cols;
}

function RecordFilterBar({
  filters,
  onChange,
}: Readonly<{ filters: RecordFilters; onChange: (f: RecordFilters) => void }>) {
  const setFilters = onChange;
  return (
    <div className="worklist-filters csh-filters">
      <CodeSelect
        label="Record Status"
        value={filters.stages?.[0] ?? ''}
        options={STAGES}
        labelOf={(s) => STAGE_LABELS[s] ?? s}
        onChange={(v) => setFilters({ ...filters, stages: [v as RecordStage] })}
      />
      <CodeSelect
        label="Receipt Type"
        value={filters.receiptKind ?? ''}
        options={['AR', 'OR']}
        empty="All"
        labelOf={(c) => (c === 'AR' ? 'Acknowledgement Receipt' : 'Official Receipt')}
        onChange={(v) =>
          setFilters({ ...filters, receiptKind: v === '' ? undefined : (v as 'AR' | 'OR') })
        }
      />
      <TextField
        label="Record Number"
        value={filters.recordNo ?? ''}
        onChange={(v) => setFilters({ ...filters, recordNo: v || undefined })}
      />
      <TextField
        label="Client or Insurer Name"
        value={filters.name ?? ''}
        onChange={(v) => setFilters({ ...filters, name: v || undefined })}
      />
      <TextField
        label="Created From"
        type="date"
        value={filters.from ?? ''}
        onChange={(v) => setFilters({ ...filters, from: v || undefined })}
      />
      <TextField
        label="Created To"
        type="date"
        value={filters.to ?? ''}
        onChange={(v) => setFilters({ ...filters, to: v || undefined })}
      />
    </div>
  );
}

function ListActions({ onExport }: Readonly<{ onExport: () => void }>) {
  const navigate = useNavigate();
  const { can } = useAuth();
  return (
    <>
      {can('CASH_RECEIPT') && (
        <Button
          variant="secondary"
          icon={<FilePlus2 size={16} />}
          onClick={() => void navigate('/cashiering/records/new?kind=AR')}
        >
          Create AR
        </Button>
      )}
      {can('CASH_RECEIPT') && (
        <Button
          variant="secondary"
          icon={<FilePlus2 size={16} />}
          onClick={() => void navigate('/cashiering/records/new?kind=OR')}
        >
          Create OR
        </Button>
      )}
      <Button variant="secondary" icon={<Download size={16} />} onClick={onExport}>
        Export to Excel
      </Button>
    </>
  );
}

function initialFilters(params: URLSearchParams): RecordFilters {
  return {
    stages: [(params.get('stage') as RecordStage | null) ?? 'FOR_POSTING'],
    receiptKind: (params.get('receiptKind') as RecordFilters['receiptKind']) ?? undefined,
    receiptType: params.get('type') ?? undefined,
  };
}

/**
 * The lists For Posting and Returned of the creation, cancellation and reinstatement records
 * (FRS.CSH.01.03.04, 02.05.01 to 02.05.07, 03.01.08, 04.01.09): filtered by status (For Posting by
 * default), receipt type, branch, creator, dates, record number and name, exported to Excel; the
 * Approver/Poster selects one or several records and posts them or returns one with a reason.
 */
export default function PostingListPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const { user, can } = useAuth();
  const [params] = useSearchParams();
  const [kind, setKind] = useState<RecordKind>(
    (params.get('kind') as RecordKind | null) ?? 'CREATION',
  );
  const [filters, setFilters] = useState<RecordFilters>(() => initialFilters(params));
  const [page, setPage] = useState(0);
  const [returning, setReturning] = useState<ReceiptRecord | null>(null);
  const [outcomes, setOutcomes] = useState<PostingOutcome[]>([]);
  const selection = useRowSelection();
  const list = useQuery({
    queryKey: ['cashiering', 'records', companyId, kind, filters, page],
    queryFn: () => recordsApi.list(companyId, kind, filters, page),
    enabled: companyId > 0,
  });
  const rows = list.data?.content ?? [];
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['cashiering'] });
  const post = useMutation({
    mutationFn: (ids: number[]) => recordsApi.post(ids),
    onSuccess: async (result) => {
      setOutcomes(result);
      selection.clear();
      toast.success(`${result.filter((o) => o.posted).length} of ${result.length} posted`);
      await refresh();
    },
  });
  const giveBack = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason: string }) =>
      recordsApi.returnToCreator(id, reason),
    onSuccess: async (r) => {
      toast.success(`${r.recordNo} returned to its creator`);
      setReturning(null);
      await refresh();
    },
  });
  const exportList = async () => {
    const query = toQuery({ companyId, kind, ...filters, stages: filters.stages?.join(',') });
    const file = await api.getFile(`/cashiering/records/export${query}`);
    saveFile(file.blob, file.fileName);
  };
  const postable = (r: ReceiptRecord) => recordActions(r, user?.username, can).post;
  const columns = [
    selectionColumn(
      rows,
      (r) => String(r.id),
      selection,
      (r) => r.recordNo,
      (r) =>
        postable(r) ? undefined : 'Only a record For Posting created by another user can be posted',
    ),
    ...columnsOf(kind),
    {
      key: 'actions',
      header: '',
      width: '56px',
      render: (r: ReceiptRecord) => (
        <RowActionMenu
          label={r.recordNo}
          actions={[
            { label: 'Open', onSelect: () => void navigate(`/cashiering/records/${r.id}`) },
            { label: 'Post', onSelect: () => post.mutate([r.id]), disabled: !postable(r) },
            { label: 'Return', onSelect: () => setReturning(r), disabled: !postable(r) },
            {
              label: 'Edit',
              onSelect: () => void navigate(`/cashiering/records/${r.id}/edit`),
              disabled: !recordActions(r, user?.username, can).edit || r.recordKind !== 'CREATION',
            },
          ]}
        />
      ),
    },
  ];
  return (
    <div className="stack">
      <PageHeader
        section="Cashiering"
        title="Posting"
        description="Creation, cancellation and reinstatement records of ARs and ORs waiting for posting or returned."
        actions={<ListActions onExport={() => void exportList()} />}
      />
      <ErrorAlert error={list.error ?? post.error ?? giveBack.error} />
      <Card flush>
        <Tabs
          tabs={TABS}
          active={kind}
          onChange={(k) => {
            setKind(k);
            setPage(0);
            selection.clear();
          }}
        />
        <div className="stack">
          <RecordFilterBar filters={filters} onChange={setFilters} />
          <div className="worklist-actions">
            <Button
              variant="accent"
              disabled={selection.keys.length === 0}
              busy={post.isPending}
              onClick={() => post.mutate(selection.keys.map(Number))}
            >
              Post Selected ({selection.keys.length})
            </Button>
          </div>
          {outcomes.some((o) => !o.posted) && (
            <ul className="field-error" aria-label="Records not posted">
              {outcomes
                .filter((o) => !o.posted)
                .map((o) => (
                  <li key={o.id}>
                    {o.recordNo}: {o.message}
                  </li>
                ))}
            </ul>
          )}
          <DataTable
            caption="Records"
            list={`cashiering-posting-${kind}`}
            columns={columns}
            rows={rows}
            rowKey={(r) => r.id}
            loading={list.isLoading}
            emptyMessage="No records match the filters"
            onRowClick={(r) => void navigate(`/cashiering/records/${r.id}`)}
          />
          <PageFooter data={list.data} noun="records" onPage={setPage} />
        </div>
      </Card>
      {returning && (
        <ConfirmDialog
          title="Return to Creator"
          record={returning.recordNo}
          effect="The record goes back to its creator with the reason."
          confirmLabel="Return"
          reason="required"
          busy={giveBack.isPending}
          onConfirm={(reason) => giveBack.mutate({ id: returning.id, reason })}
          onClose={() => setReturning(null)}
        />
      )}
    </div>
  );
}
