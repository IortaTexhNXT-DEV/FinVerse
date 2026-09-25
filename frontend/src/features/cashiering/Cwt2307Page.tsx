import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
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
import { useCompanyId, useDefaultBranchId } from '@/context/workspaceContext';
import { formatDateTime, humanize } from '@/utils/format';
import { cashieringApi } from './cashieringApi';
import type { CwtBatch, CwtTag } from './cashieringApi';
import { CwtTagDialog } from './CwtTagDialog';

const TABS = [
  { id: 'tagged', label: 'Tagged by Marketing' },
  { id: 'validating', label: 'Validation' },
  { id: 'batches', label: 'Reports and Routing' },
  { id: 'closed', label: 'Released / Settled' },
] as const;
type TabId = (typeof TABS)[number]['id'];

const STAGES: Record<Exclude<TabId, 'batches'>, string[]> = {
  tagged: ['TAGGED'],
  validating: ['VALIDATING'],
  closed: ['REPORT_POSTED', 'WITH_DISBURSEMENT', 'RELEASED', 'SETTLED_CASH', 'CANCELLED'],
};

const keyOf = (t: CwtTag) => String(t.id);

const TAG_COLUMNS: Column<CwtTag>[] = [
  {
    key: 'ref',
    header: 'Reference',
    render: (t) => (
      <>
        <strong>{t.reference}</strong>
        <span className="cell-sub">
          {t.invoiceNo} · {t.arn}
        </span>
      </>
    ),
  },
  {
    key: 'client',
    header: 'Client / Insurer',
    render: (t) => `${t.clientCode ?? ''} · ${t.insurerCode ?? ''}`,
  },
  {
    key: 'path',
    header: 'Path',
    render: (t) => [humanize(t.path), t.certificateNo].filter(Boolean).join(' '),
  },
  { key: 'amount', header: 'Amount', numeric: true, render: (t) => <Amount value={t.amount} /> },
  {
    key: 'flags',
    header: 'Checklist',
    render: (t) => (
      <span className="tag-list">
        {t.cwtCopyReceived && <span className="tag">CWT Copy</span>}
        {t.remitted && <span className="tag">Remitted</span>}
      </span>
    ),
  },
  { key: 'by', header: 'Tagged', render: (t) => `${t.taggedBy} · ${formatDateTime(t.taggedAt)}` },
  { key: 'stage', header: 'Stage', render: (t) => <StatusBadge status={t.stage} /> },
];

function BatchList({
  companyId,
  onChange,
}: Readonly<{ companyId: number; onChange: (fn: () => Promise<unknown>) => void }>) {
  const { can } = useAuth();
  const batches = useQuery({
    queryKey: ['cashiering', 'cwt', 'batches', companyId],
    queryFn: () => cashieringApi.cwtBatches(companyId),
    enabled: companyId > 0,
  });
  const columns: Column<CwtBatch>[] = [
    { key: 'no', header: 'Batch No.', render: (b) => <strong>{b.batchNo}</strong> },
    { key: 'insurer', header: 'Insurer', render: (b) => b.insurerCode },
    { key: 'count', header: 'Certificates', render: (b) => b.tagCount },
    {
      key: 'amount',
      header: 'Total',
      numeric: true,
      render: (b) => <Amount value={b.totalAmount} />,
    },
    { key: 'dr', header: 'Disbursement', render: (b) => b.disbursementRequestNo ?? '' },
    { key: 'status', header: 'Status', render: (b) => <StatusBadge status={b.status} /> },
    {
      key: 'actions',
      header: '',
      render: (b) => (
        <div className="row">
          {b.status === 'REPORT_POSTED' && can('CWT_PROCESS') && (
            <Button size="sm" onClick={() => onChange(() => cashieringApi.routeCwt(b.id))}>
              Route to Disbursement
            </Button>
          )}
          {b.status === 'WITH_DISBURSEMENT' && can('DISB_PROCESS') && (
            <Button
              size="sm"
              variant="secondary"
              onClick={() => onChange(() => cashieringApi.releaseCwt(b.id))}
            >
              Release to Insurer
            </Button>
          )}
        </div>
      ),
    },
  ];
  return (
    <DataTable
      caption="2307 report batches"
      columns={columns}
      rows={batches.data ?? []}
      rowKey={(b) => b.id}
      loading={batches.isLoading}
      emptyMessage="No 2307 report yet"
    />
  );
}

/**
 * BIR 2307 (CSHID.026/027, MKTID.010/013, DBMID.001): Marketing tags the client certificates,
 * Cashiering receives them, ticks the CWT-copy checklist, settles cash 2307s, and validates the
 * certificates into a report per insurer (reclass to PR2307) that is routed to Disbursement and
 * released to the insurer (DTIP offset).
 */
export default function Cwt2307Page() {
  const companyId = useCompanyId();
  const branchId = useDefaultBranchId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const selection = useRowSelection();
  const [tab, setTab] = useState<TabId>('tagged');
  const [page, setPage] = useState(0);
  const [tagging, setTagging] = useState(false);
  const stages = tab === 'batches' ? [] : STAGES[tab];
  const list = useQuery({
    queryKey: ['cashiering', 'cwt', companyId, tab, page],
    queryFn: () => cashieringApi.cwtTags(companyId, stages, page),
    enabled: companyId > 0 && tab !== 'batches',
  });
  const act = useMutation({
    mutationFn: (fn: () => Promise<unknown>) => fn(),
    onSuccess: async () => {
      selection.clear();
      toast.success('BIR 2307 updated');
      await queryClient.invalidateQueries({ queryKey: ['cashiering', 'cwt'] });
    },
  });
  const process = can('CWT_PROCESS');
  const rows = list.data?.content ?? [];
  const rowActions: Column<CwtTag> = {
    key: 'actions',
    header: '',
    render: (t) =>
      process && (
        <div className="row">
          {t.stage === 'TAGGED' && (
            <Button
              size="sm"
              variant="secondary"
              onClick={() => act.mutate(() => cashieringApi.receiveCwt(t.id, false))}
            >
              Receive
            </Button>
          )}
          {t.stage === 'VALIDATING' && (
            <label className="checkbox">
              <input
                type="checkbox"
                checked={t.cwtCopyReceived}
                onChange={(e) => act.mutate(() => cashieringApi.checklist(t.id, e.target.checked))}
              />
              CWT copy received
            </label>
          )}
          {t.stage === 'VALIDATING' && t.path === 'CASH' && (
            <Button
              size="sm"
              variant="secondary"
              onClick={() => act.mutate(() => cashieringApi.settleCash(t.id, branchId))}
            >
              Settle in Cash
            </Button>
          )}
        </div>
      ),
  };
  const columns =
    tab === 'validating'
      ? [selectionColumn(rows, keyOf, selection, (t) => t.reference), ...TAG_COLUMNS, rowActions]
      : [...TAG_COLUMNS, rowActions];
  return (
    <div className="stack">
      <PageHeader
        section="Cashiering"
        title="BIR 2307"
        description="Creditable withholding tax certificates of 2% CWT clients: tagging, validation, report and routing to the insurer."
        actions={
          can('CWT_TAG') && (
            <Button variant="accent" icon={<Plus size={16} />} onClick={() => setTagging(true)}>
              Tag 2307
            </Button>
          )
        }
      />
      <ErrorAlert error={list.error ?? act.error} />
      <Card flush>
        <Tabs
          tabs={TABS}
          active={tab}
          onChange={(t) => {
            setTab(t);
            setPage(0);
            selection.clear();
          }}
        />
        {tab === 'validating' && process && (
          <div className="worklist-toolbar">
            <span className="muted">
              Select the validated certificates of one insurer to post the 2307 report.
            </span>
            <div className="worklist-actions">
              <Button
                disabled={selection.keys.length === 0}
                busy={act.isPending}
                onClick={() =>
                  act.mutate(() => cashieringApi.validateCwt(companyId, selection.keys.map(Number)))
                }
              >
                Validate and Post Report
              </Button>
            </div>
          </div>
        )}
        {tab === 'batches' ? (
          <BatchList companyId={companyId} onChange={(fn) => act.mutate(fn)} />
        ) : (
          <>
            <DataTable
              caption="BIR 2307 tags"
              columns={columns}
              rows={rows}
              rowKey={(t) => t.id}
              loading={list.isLoading}
              emptyMessage="No items to display"
            />
            <PageFooter data={list.data} noun="certificates" onPage={setPage} />
          </>
        )}
      </Card>
      {tagging && <CwtTagDialog companyId={companyId} onClose={() => setTagging(false)} />}
    </div>
  );
}
