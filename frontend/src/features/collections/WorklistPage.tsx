import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FileSpreadsheet } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
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
import { formatAmount, formatDate } from '@/utils/format';
import { collectionsApi } from './api';
import type { CollectionItem, DispositionInput, EffortInput, ReassignInput } from './api';
import { WORKLIST_TABS, filtersFromSearch, tabFromSearch, worklistQuery } from './collectionsLogic';
import type { WorklistFilters, WorklistTab } from './collectionsLogic';
import { DispositionDialog, EffortDialog, ReassignDialog } from './WorkDialogs';
import { FilterPanel, TotalsCard } from './WorklistParts';
import './collections.css';

const COLUMNS: Column<CollectionItem>[] = [
  {
    key: 'inv',
    header: 'Invoice No.',
    render: (i) => (
      <>
        <strong>{i.invoiceNo}</strong>
        <div className="clx-muted">{i.policyNo ?? i.arn}</div>
      </>
    ),
  },
  {
    key: 'client',
    header: 'Client / Assured',
    render: (i) => (
      <>
        <Link
          to={`/collections/clients/${encodeURIComponent(i.clientCode)}`}
          onClick={(e) => e.stopPropagation()}
        >
          {i.clientCode}
        </Link>
        <div className="clx-muted">{i.assuredName}</div>
      </>
    ),
  },
  {
    key: 'segment',
    header: 'Segment / Unit',
    render: (i) => [i.segment, i.salesUnit].filter(Boolean).join(' / '),
  },
  { key: 'booked', header: 'Booked', render: (i) => formatDate(i.bookingDate) },
  { key: 'aging', header: 'Aging', render: (i) => `${i.agingDays} d · ${i.agingBracket ?? ''}` },
  {
    key: 'net',
    header: 'Outstanding',
    numeric: true,
    render: (i) => `${i.currency} ${formatAmount(i.netOutstanding)}`,
  },
  { key: 'handler', header: 'Handler', render: (i) => i.currentHandler ?? 'Unassigned' },
  {
    key: 'disp',
    header: 'Disposition',
    render: (i) => (
      <>
        {i.dispositionCode ?? '—'}
        {i.category !== undefined && <span className="tag">Cat. {i.category}</span>}
      </>
    ),
  },
  { key: 'status', header: 'Status', render: (i) => <StatusBadge status={i.status} /> },
];

type Dialog = 'disposition' | 'effort' | 'reassign';

/** The bulk actions of the selection, each refreshing the list with a toast. */
function useBulkActions(companyId: number, done: () => void) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const onSuccess = async (message: string) => {
    done();
    await queryClient.invalidateQueries({ queryKey: ['collections'] });
    toast.success(message);
  };
  return {
    dispose: useMutation({
      mutationFn: (input: DispositionInput) => collectionsApi.dispose(companyId, input),
      onSuccess: (r) => onSuccess(`Disposition recorded on ${r.length} account(s)`),
    }),
    effort: useMutation({
      mutationFn: (input: EffortInput) => collectionsApi.effort(companyId, input),
      onSuccess: (r) => onSuccess(`Effort logged on ${r.length} account(s)`),
    }),
    reassign: useMutation({
      mutationFn: (input: ReassignInput) => collectionsApi.reassign(companyId, input),
      onSuccess: (r) => onSuccess(`${r.moved} account(s) reassigned`),
    }),
    exportList: useMutation({
      mutationFn: (f: WorklistFilters) =>
        collectionsApi.export(companyId, f.segment ?? '', f.salesUnit ?? ''),
      onSuccess: () => onSuccess('Export ready on the Files screen'),
    }),
  };
}

function BulkButtons({
  mayWork,
  mayAssign,
  noSelection,
  onDialog,
}: Readonly<{
  mayWork: boolean;
  mayAssign: boolean;
  noSelection: boolean;
  onDialog: (d: Dialog) => void;
}>) {
  return (
    <>
      {mayWork && (
        <>
          <Button variant="secondary" disabled={noSelection} onClick={() => onDialog('effort')}>
            Log Effort
          </Button>
          <Button disabled={noSelection} onClick={() => onDialog('disposition')}>
            Record Disposition
          </Button>
        </>
      )}
      {mayAssign && (
        <Button variant="secondary" disabled={noSelection} onClick={() => onDialog('reassign')}>
          Reassign
        </Button>
      )}
    </>
  );
}

function WorklistDialogs({
  dialog,
  invoiceNos,
  actions,
  onClose,
}: Readonly<{
  dialog?: Dialog;
  invoiceNos: string[];
  actions: ReturnType<typeof useBulkActions>;
  onClose: () => void;
}>) {
  const error = actions.dispose.error ?? actions.effort.error ?? actions.reassign.error;
  switch (dialog) {
    case 'disposition':
      return (
        <DispositionDialog
          invoiceNos={invoiceNos}
          busy={actions.dispose.isPending}
          error={error}
          onClose={onClose}
          onSave={(input) => actions.dispose.mutate(input)}
        />
      );
    case 'effort':
      return (
        <EffortDialog
          invoiceNos={invoiceNos}
          busy={actions.effort.isPending}
          error={error}
          onClose={onClose}
          onSave={(input) => actions.effort.mutate(input)}
        />
      );
    case 'reassign':
      return (
        <ReassignDialog
          invoiceNos={invoiceNos}
          total={invoiceNos.length}
          busy={actions.reassign.isPending}
          error={error}
          onClose={onClose}
          onSave={(input) => actions.reassign.mutate(input)}
        />
      );
    default:
      return null;
  }
}

/**
 * PR worklist (BRCLXN.001-012, 050-052): the outstanding premium receivables per invoice with
 * server-side filters (segment, unit, UH, handler, AO, aging bracket, category, disposition,
 * amount, promise, escalation), status tabs and the bulk actions of the selection -
 * disposition, effort and reassignment.
 */
export default function WorklistPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const { can } = useAuth();
  const [search] = useSearchParams();
  const [tab, setTab] = useState<WorklistTab>(tabFromSearch(search));
  const [text, setText] = useState('');
  const [filters, setFilters] = useState<WorklistFilters>(filtersFromSearch(search));
  const [showFilters, setShowFilters] = useState(false);
  const [page, setPage] = useState(0);
  const [dialog, setDialog] = useState<Dialog>();
  const selection = useRowSelection();
  const query = worklistQuery(tab, text, filters);
  const rows = useQuery({
    queryKey: ['collections', 'worklist', companyId, query, page],
    queryFn: () => collectionsApi.worklist(companyId, query, page),
    enabled: companyId > 0,
  });
  const actions = useBulkActions(companyId, () => {
    setDialog(undefined);
    selection.clear();
  });
  const items = rows.data?.content ?? [];
  const bulk = selection.keys.length > 1;
  const mayWork = can('CLX_WORK') && (!bulk || can('CLX_BULK_UPDATE'));
  const noSelection = selection.keys.length === 0;
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Collections"
        backTo="/collections"
        title="PR Worklist"
        description="Invoices with outstanding premium receivable above the threshold, refreshed nightly from the invoice ledger."
        actions={
          can('CLX_EXPORT') ? (
            <Button
              variant="secondary"
              icon={<FileSpreadsheet size={16} />}
              busy={actions.exportList.isPending}
              onClick={() => actions.exportList.mutate(filters)}
            >
              Export to Files
            </Button>
          ) : undefined
        }
      />
      <ErrorAlert error={rows.error ?? actions.exportList.error} />
      <Card flush>
        <div>
          <Tabs
            tabs={WORKLIST_TABS}
            active={tab}
            onChange={(t) => {
              setTab(t);
              setPage(0);
              selection.clear();
            }}
          />
          <WorklistToolbar
            placeholder="Search Invoice, ARN, Policy or Client"
            onSearch={(t) => {
              setText(t);
              setPage(0);
            }}
            filters={{ open: showFilters, onToggle: () => setShowFilters(!showFilters) }}
            extra={
              <label className="checkbox">
                <input
                  type="checkbox"
                  checked={filters.mine === true}
                  onChange={(e) => {
                    setFilters({ ...filters, mine: e.target.checked || undefined });
                    setPage(0);
                  }}
                />
                Only mine
              </label>
            }
          >
            <BulkButtons
              mayWork={mayWork}
              mayAssign={can('CLX_ASSIGN')}
              noSelection={noSelection}
              onDialog={setDialog}
            />
          </WorklistToolbar>
          {showFilters && (
            <FilterPanel
              initial={filters}
              onApply={(f) => {
                setFilters({ ...f, mine: filters.mine });
                setPage(0);
              }}
            />
          )}
          <DataTable
            caption="Collection accounts"
            columns={[
              selectionColumn(
                items,
                (i) => i.invoiceNo,
                selection,
                (i) => i.invoiceNo,
              ),
              ...COLUMNS,
            ]}
            rows={items}
            rowKey={(i) => i.id}
            loading={rows.isLoading}
            emptyMessage="No accounts match the filters"
            onRowClick={(i) =>
              void navigate(`/collections/items/${encodeURIComponent(i.invoiceNo)}`)
            }
          />
          <PageFooter data={rows.data} noun="accounts" onPage={setPage} />
        </div>
      </Card>
      <TotalsCard companyId={companyId} query={query} />
      <WorklistDialogs
        dialog={dialog}
        invoiceNos={selection.keys}
        actions={actions}
        onClose={() => setDialog(undefined)}
      />
    </div>
  );
}
