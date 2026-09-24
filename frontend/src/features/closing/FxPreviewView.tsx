import type { FxPreview, FxRun, OpenItemRevaluation, RevaluationItem } from '@/api/closing';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { Kpi } from '@/components/ui/Kpi';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useWorkspace } from '@/context/workspaceContext';
import { formatAmount, formatDate } from '@/utils/format';

function reversalText(r: FxRun): string {
  if (r.reversalBatchNo) {
    return r.reversalBatchNo;
  }
  return r.reversalPending ? `pending (${formatDate(r.reversalDate)})` : '–';
}

/** Revaluation preview: KPIs, GL balances at the closing rate and FC open items. */
export function FxPreviewView({
  preview,
  loading,
}: Readonly<{ preview?: FxPreview; loading: boolean }>) {
  const { branches } = useWorkspace();
  const branch = (id: number) => branches.find((b) => b.id === id)?.code ?? String(id);
  const missing = preview?.missingRates ?? [];
  return (
    <>
      {missing.length > 0 && (
        <div className="alert danger">
          No CLOSING rate for {missing.join(', ')}: posting is blocked.
        </div>
      )}
      {preview && (
        <div className="grid-4">
          <Kpi label="Revaluation date" value={formatDate(preview.revaluationDate)} />
          <Kpi label="Balances revalued" value={preview.items.length} />
          <Kpi
            label="Net unrealized gain / (loss)"
            value={formatAmount(preview.totalDifference)}
            accent
          />
          <Kpi label="FC open items" value={preview.openItems.length} hint="Information only" />
        </div>
      )}
      <Card title="GL balances (preview)" flush>
        <DataTable<RevaluationItem>
          loading={loading}
          rows={preview?.items ?? []}
          rowKey={(i) => `${i.accountCode}-${i.branchId}-${i.currency}`}
          emptyMessage="No foreign currency balances on revaluation accounts."
          columns={[
            { key: 'b', header: 'Branch', render: (i) => branch(i.branchId) },
            {
              key: 'a',
              header: 'Account',
              render: (i) => `${i.accountCode} ${i.accountName ?? ''}`,
            },
            { key: 'c', header: 'Ccy', render: (i) => i.currency },
            {
              key: 'f',
              header: 'FC Balance',
              numeric: true,
              render: (i) => <Amount value={i.fcBalance} />,
            },
            {
              key: 'k',
              header: 'Booked Base',
              numeric: true,
              render: (i) => <Amount value={i.bookedBase} />,
            },
            {
              key: 'r',
              header: 'Closing Rate',
              numeric: true,
              render: (i) => i.closingRate.toFixed(6),
            },
            {
              key: 'v',
              header: 'Revalued Base',
              numeric: true,
              render: (i) => <Amount value={i.revaluedBase} />,
            },
            {
              key: 'd',
              header: 'Gain / (loss)',
              numeric: true,
              render: (i) => <Amount value={i.difference} />,
            },
          ]}
        />
      </Card>
      <Card title="Foreign currency open items (information only)" flush>
        <DataTable<OpenItemRevaluation>
          rows={preview?.openItems ?? []}
          rowKey={(o) => `${o.partyCode}-${o.documentNo}`}
          emptyMessage="No outstanding foreign currency open items."
          columns={[
            { key: 'p', header: 'Party', render: (o) => o.partyCode },
            { key: 'd', header: 'Document', render: (o) => `${o.documentNo} (${o.direction})` },
            { key: 'c', header: 'Ccy', render: (o) => o.currency },
            {
              key: 'o',
              header: 'Outstanding',
              numeric: true,
              render: (o) => <Amount value={o.outstanding} />,
            },
            {
              key: 'k',
              header: 'Booked Base',
              numeric: true,
              render: (o) => <Amount value={o.bookedBase} />,
            },
            {
              key: 'v',
              header: 'Revalued Base',
              numeric: true,
              render: (o) => <Amount value={o.revaluedBase} />,
            },
            {
              key: 'g',
              header: 'Gain / (loss)',
              numeric: true,
              render: (o) => <Amount value={o.gainLoss} />,
            },
          ]}
        />
      </Card>
    </>
  );
}

/** History of revaluation runs with their journals and reversals. */
export function FxRunsTable({ runs, loading }: Readonly<{ runs: FxRun[]; loading: boolean }>) {
  return (
    <Card title="Revaluation runs" flush>
      <DataTable<FxRun>
        loading={loading}
        rows={runs}
        rowKey={(r) => r.id}
        columns={[
          { key: 'p', header: 'Period', render: (r) => <strong>{r.periodName}</strong> },
          { key: 's', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
          { key: 'j', header: 'Journal', render: (r) => r.journalBatchNo ?? 'nothing to post' },
          { key: 'r', header: 'Reversal', render: reversalText },
          {
            key: 'g',
            header: 'Gain',
            numeric: true,
            render: (r) => <Amount value={r.totalGain} />,
          },
          {
            key: 'l',
            header: 'Loss',
            numeric: true,
            render: (r) => <Amount value={r.totalLoss} />,
          },
          { key: 'u', header: 'By', render: (r) => r.createdBy },
        ]}
      />
    </Card>
  );
}
