import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCheck, FilePlus2 } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { LovSelect } from '@/components/broking/LovSelect';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import type { RowSelection } from '@/components/broking/rowSelection';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Kpi } from '@/components/ui/Kpi';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { disbursementApi } from './api';
import type { PaymentRequest, Summary } from './api';
import { WORKBENCH_TABS, workbenchTab, workbenchTabOf } from './labels';
import type { WorkbenchTab } from './labels';
import { REQUEST_COLUMNS, VOUCHER_COLUMNS } from './columns';
import { RequestDialog } from './WorkbenchParts';
import './disbursement.css';

interface Filters {
  type: string;
  from: string;
  to: string;
}

const NO_FILTERS: Filters = { type: '', from: '', to: '' };

function countOf(summary: Summary | undefined, tab: WorkbenchTab): number | undefined {
  if (summary === undefined) {
    return undefined;
  }
  const counts: Record<WorkbenchTab, number> = {
    REQUESTS: summary.requests,
    NO_PAYEE: summary.noPayee,
    IN_PROCESS: summary.inProcess,
    FOR_REVIEW: summary.forReview,
    FOR_APPROVAL: summary.forApproval,
    APPROVED: summary.approved,
    CLOSED: summary.closed,
  };
  return counts[tab];
}

function tabLabel(summary: Summary | undefined, tab: WorkbenchTab, label: string): string {
  const n = countOf(summary, tab);
  return n === undefined ? label : `${label} (${n})`;
}

function FilterPanel({
  filters,
  requests,
  onChange,
}: Readonly<{ filters: Filters; requests: boolean; onChange: (f: Filters) => void }>) {
  return (
    <div className="worklist-filters dsb-form">
      <Field label="Disbursement Type">
        {(id) => (
          <LovSelect
            id={id}
            type="DISBURSEMENT_TYPE"
            value={filters.type}
            placeholder="All types"
            onChange={(type) => onChange({ ...filters, type })}
          />
        )}
      </Field>
      {requests && (
        <>
          <Field label="Received From">
            {(id) => (
              <input
                id={id}
                type="date"
                className="input"
                value={filters.from}
                onChange={(e) => onChange({ ...filters, from: e.target.value })}
              />
            )}
          </Field>
          <Field label="Received To">
            {(id) => (
              <input
                id={id}
                type="date"
                className="input"
                value={filters.to}
                onChange={(e) => onChange({ ...filters, to: e.target.value })}
              />
            )}
          </Field>
        </>
      )}
    </div>
  );
}

/** Approves the selected vouchers, one result each (DIS 2.19.0). */
function useBulkApprove(reset: () => void) {
  const toast = useToast();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (ids: number[]) => disbursementApi.approveAll(ids, 'Approved from the workbench'),
    onSuccess: async (results) => {
      reset();
      await queryClient.invalidateQueries({ queryKey: ['disbursement'] });
      const ok = results.filter((r) => r.ok).length;
      const failed = results.filter((r) => !r.ok);
      if (failed.length === 0) {
        toast.success(`${ok} voucher(s) approved and posted`);
      } else {
        toast.error(`${ok} approved; ${failed.length} failed: ${failed[0]?.message ?? ''}`);
      }
    },
  });
}

function Kpis({ summary }: Readonly<{ summary: Summary | undefined }>) {
  const value = (n: number | undefined) => n ?? '—';
  const noPayee = summary?.noPayee ?? 0;
  const unregularized = summary?.unregularized ?? 0;
  return (
    <div className="grid-4">
      <Kpi label="For Approval" value={value(summary?.forApproval)} />
      <Kpi label="In Process" value={value(summary?.inProcess)} />
      <Kpi label="Waiting for Payee" value={value(summary?.noPayee)} accent={noPayee > 0} />
      <Kpi label="Unregularised" value={value(summary?.unregularized)} accent={unregularized > 0} />
    </div>
  );
}

interface ListProps {
  companyId: number;
  tab: WorkbenchTab;
  query: string;
  filters: Filters;
  page: number;
  onPage: (page: number) => void;
}

function RequestList({
  onOpen,
  ...p
}: Readonly<ListProps & { onOpen: (r: PaymentRequest) => void }>) {
  const statuses = workbenchTab(p.tab).requestStatuses ?? [];
  const requests = useQuery({
    queryKey: ['disbursement', 'requests', p.companyId, p.tab, p.query, p.filters, p.page],
    queryFn: () =>
      disbursementApi.requests(
        p.companyId,
        statuses,
        { q: p.query, from: p.filters.from, to: p.filters.to },
        p.page,
      ),
    enabled: p.companyId > 0,
  });
  return (
    <>
      <ErrorAlert error={requests.error} />
      <DataTable
        caption="Payment requests"
        columns={REQUEST_COLUMNS}
        rows={requests.data?.content ?? []}
        rowKey={(r) => r.id}
        loading={requests.isLoading}
        onRowClick={onOpen}
      />
      <PageFooter data={requests.data} noun="requests" onPage={p.onPage} />
    </>
  );
}

function VoucherList({ bulk, ...p }: Readonly<ListProps & { bulk?: RowSelection }>) {
  const navigate = useNavigate();
  const stages = workbenchTab(p.tab).stages ?? [];
  const vouchers = useQuery({
    queryKey: ['disbursement', 'vouchers', p.companyId, p.tab, p.query, p.filters.type, p.page],
    queryFn: () =>
      disbursementApi.vouchers(p.companyId, stages, { q: p.query, type: p.filters.type }, p.page),
    enabled: p.companyId > 0,
  });
  const rows = vouchers.data?.content ?? [];
  const columns =
    bulk === undefined
      ? VOUCHER_COLUMNS
      : [
          selectionColumn(
            rows,
            (v) => String(v.id),
            bulk,
            (v) => v.dvNo,
          ),
          ...VOUCHER_COLUMNS,
        ];
  return (
    <>
      <ErrorAlert error={vouchers.error} />
      <DataTable
        caption="Disbursement vouchers"
        columns={columns}
        rows={rows}
        rowKey={(v) => v.id}
        loading={vouchers.isLoading}
        onRowClick={(v) => void navigate(`/disbursement/vouchers/${v.id}`)}
      />
      <PageFooter data={vouchers.data} noun="vouchers" onPage={p.onPage} />
    </>
  );
}

function BulkApprove({ selection }: Readonly<{ selection: RowSelection }>) {
  const bulk = useBulkApprove(selection.clear);
  const ids = selection.keys.map(Number);
  return (
    <>
      <Button
        icon={<CheckCheck size={16} />}
        disabled={ids.length === 0}
        busy={bulk.isPending}
        onClick={() => bulk.mutate(ids)}
      >
        Approve Selected
      </Button>
      <ErrorAlert error={bulk.error} />
    </>
  );
}

/**
 * Disbursement Workbench (DIS 2.4.0-2.4.4, 2.6.2, 2.13.0, 2.19.0): the payment requests received
 * without voucher and those waiting for their payee, then the vouchers by stage. Processors encode
 * e-mail requests and open requests to create their voucher or return them; approvers approve the
 * selected vouchers in one go.
 */
export default function DisbursementHomePage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [tab, setTab] = useState<WorkbenchTab>(() => workbenchTabOf(params.get('tab')));
  const [query, setQuery] = useState('');
  const [filters, setFilters] = useState<Filters>(NO_FILTERS);
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [page, setPage] = useState(0);
  const [opened, setOpened] = useState<PaymentRequest>();
  const selection = useRowSelection();
  const isRequests = workbenchTab(tab).requestStatuses !== undefined;
  const summary = useQuery({
    queryKey: ['disbursement', 'summary', companyId],
    queryFn: () => disbursementApi.summary(companyId),
    enabled: companyId > 0,
  });
  const bulkEnabled = tab === 'FOR_APPROVAL' && can('DISB_APPROVE');
  const list: ListProps = { companyId, tab, query, filters, page, onPage: setPage };
  return (
    <div className="stack">
      <PageHeader
        section="Finance"
        title="Disbursement Workbench"
        description="Payment requests and disbursement vouchers by stage: system requests, in process, for review, for approval, approved and cancelled."
        actions={
          can('DISB_PROCESS') ? (
            <Button
              variant="accent"
              icon={<FilePlus2 size={16} />}
              onClick={() => void navigate('/disbursement/requests/new')}
            >
              Encode Request
            </Button>
          ) : undefined
        }
      />
      <Kpis summary={summary.data} />
      <ErrorAlert error={summary.error} />
      <Card flush>
        <div>
          <Tabs
            tabs={WORKBENCH_TABS.map((t) => ({
              id: t.id,
              label: tabLabel(summary.data, t.id, t.label),
            }))}
            active={tab}
            onChange={(t) => {
              setTab(t);
              setPage(0);
              selection.clear();
            }}
          />
          <WorklistToolbar
            placeholder={
              isRequests ? 'Search Request, RFP or Payee' : 'Search DV, Payee or Invoice'
            }
            onSearch={(text) => {
              setQuery(text);
              setPage(0);
            }}
            filters={{ open: filtersOpen, onToggle: () => setFiltersOpen(!filtersOpen) }}
          >
            {bulkEnabled && <BulkApprove selection={selection} />}
          </WorklistToolbar>
          {filtersOpen && (
            <FilterPanel
              filters={filters}
              requests={isRequests}
              onChange={(f) => {
                setFilters(f);
                setPage(0);
              }}
            />
          )}
          {isRequests ? (
            <RequestList {...list} onOpen={setOpened} />
          ) : (
            <VoucherList {...list} bulk={bulkEnabled ? selection : undefined} />
          )}
        </div>
      </Card>
      {opened !== undefined && (
        <RequestDialog
          request={opened}
          canProcess={can('DISB_PROCESS')}
          onClose={() => setOpened(undefined)}
        />
      )}
    </div>
  );
}
