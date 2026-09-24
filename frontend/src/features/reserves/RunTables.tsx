import type { ReserveLine, ReserveMovement, ReserveTotal, TakafulItem } from '@/api/reserves';
import { Amount } from '@/components/ui/Amount';
import { DataTable } from '@/components/ui/DataTable';
import { formatDate, humanize } from '@/utils/format';
import { reserveLabel, summarizeLines } from './reserveMath';
import type { LineSummary } from './reserveMath';

const GROSS = 'Gross';
const RI_SHARE = 'RI share';
const NET = 'Net';

/** Totals per reserve type of a run (DAC: RI share = UCR). */
export function TotalsTable({ totals }: Readonly<{ totals: ReserveTotal[] }>) {
  return (
    <DataTable<ReserveTotal>
      rows={totals}
      rowKey={(t) => t.type}
      emptyMessage="No reserves in this run."
      columns={[
        { key: 't', header: 'Reserve', render: (t) => <strong>{t.label}</strong> },
        { key: 'g', header: GROSS, numeric: true, render: (t) => <Amount value={t.gross} /> },
        {
          key: 'r',
          header: 'RI Share / UCR',
          numeric: true,
          render: (t) => <Amount value={t.ri} />,
        },
        { key: 'n', header: NET, numeric: true, render: (t) => <Amount value={t.net} /> },
      ]}
    />
  );
}

/** Reserves per type and line of business (branches and products merged). */
export function LineSummaryTable({ lines }: Readonly<{ lines: ReserveLine[] }>) {
  return (
    <DataTable<LineSummary>
      rows={summarizeLines(lines)}
      rowKey={(s) => s.key}
      columns={[
        { key: 't', header: 'Reserve', render: (s) => reserveLabel(s.type) },
        { key: 'l', header: 'Line of Business', render: (s) => s.businessLine },
        { key: 'g', header: GROSS, numeric: true, render: (s) => <Amount value={s.gross} /> },
        { key: 'r', header: RI_SHARE, numeric: true, render: (s) => <Amount value={s.ri} /> },
        { key: 'n', header: NET, numeric: true, render: (s) => <Amount value={s.net} /> },
      ]}
    />
  );
}

/** Every reserve line: branch, line, product, channel, with the calculation base and rate. */
export function LinesTable({ lines }: Readonly<{ lines: ReserveLine[] }>) {
  return (
    <DataTable<ReserveLine>
      rows={lines}
      rowKey={(l) => `${l.type}|${l.branchId}|${l.businessLine}|${l.productCode}|${l.sourceType}`}
      columns={[
        { key: 't', header: 'Reserve', render: (l) => l.type },
        { key: 'b', header: 'Branch', render: (l) => l.branchCode },
        { key: 'l', header: 'Line', render: (l) => l.businessLine },
        { key: 'p', header: 'Product', render: (l) => l.productCode },
        { key: 's', header: 'Channel', render: (l) => humanize(l.sourceType) },
        { key: 'm', header: 'Method', render: (l) => (l.method ? humanize(l.method) : '') },
        { key: 'x', header: 'Base', numeric: true, render: (l) => <Amount value={l.base} /> },
        { key: 'r', header: 'Rate %', numeric: true, render: (l) => l.rate ?? '' },
        { key: 'g', header: GROSS, numeric: true, render: (l) => <Amount value={l.gross} /> },
        { key: 'i', header: RI_SHARE, numeric: true, render: (l) => <Amount value={l.ri} /> },
        { key: 'n', header: NET, numeric: true, render: (l) => <Amount value={l.net} /> },
      ]}
    />
  );
}

/** Journal amount components the run posts (closing − previous posted run). */
export function MovementsTable({ movements }: Readonly<{ movements: ReserveMovement[] }>) {
  return (
    <DataTable<ReserveMovement>
      rows={movements}
      rowKey={(m) => `${m.branchCode}|${m.businessLine}|${m.eventType}|${m.component}`}
      emptyMessage="No movement against the previous posted run."
      columns={[
        { key: 'b', header: 'Branch', render: (m) => m.branchCode },
        { key: 'l', header: 'Line', render: (m) => m.businessLine },
        { key: 'e', header: 'Accounting Event', render: (m) => m.eventType },
        { key: 'c', header: 'Component', render: (m) => m.component },
        { key: 'a', header: 'Movement', numeric: true, render: (m) => <Amount value={m.amount} /> },
      ]}
    />
  );
}

/** Takaful surplus of the policies expiring in the valuation month (PGIBR074). */
export function TakafulTable({
  items,
  loading,
}: Readonly<{ items: TakafulItem[]; loading: boolean }>) {
  return (
    <DataTable<TakafulItem>
      loading={loading}
      rows={items}
      rowKey={(i) => i.policyNo}
      emptyMessage="No takaful policy expired in the month (or takaful is not enabled)."
      columns={[
        { key: 'p', header: 'Policy', render: (i) => <strong>{i.policyNo}</strong> },
        { key: 'i', header: 'Insured', render: (i) => i.insuredName },
        { key: 'x', header: 'Expiry', render: (i) => formatDate(i.expiryDate) },
        { key: 'g', header: GROSS, numeric: true, render: (i) => <Amount value={i.gross} /> },
        { key: 'c', header: 'Claims', numeric: true, render: (i) => <Amount value={i.claims} /> },
        {
          key: 'a',
          header: 'Applicable',
          numeric: true,
          render: (i) => <Amount value={i.applicable} />,
        },
        {
          key: 'r',
          header: 'Retakaful',
          numeric: true,
          render: (i) => <Amount value={i.retakaful} />,
        },
        { key: 't', header: 'Tax', numeric: true, render: (i) => <Amount value={i.tax} /> },
        { key: 'y', header: 'Payable', numeric: true, render: (i) => <Amount value={i.payable} /> },
      ]}
    />
  );
}
