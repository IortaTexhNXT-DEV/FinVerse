import { LovLabel } from '@/components/broking/LovLabel';
import { selectionColumn } from '@/components/broking/rowSelection';
import type { RowSelection } from '@/components/broking/rowSelection';
import { Amount } from '@/components/ui/Amount';
import type { Column } from '@/components/ui/DataTable';
import { RowActions } from '@/components/ui/RowActions';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { clientShortName } from '@/context/clientNames';
import type { ReconItem } from './prodreconApi';
import { differenceLabels, mayPair } from './prodreconLogic';

/** Columns of the items; the selection and Pair action only when the cycle can be worked. */
export function itemColumns(
  rows: ReconItem[],
  selection: RowSelection | undefined,
  onPair: (item: ReconItem) => void,
): Column<ReconItem>[] {
  return [
    ...(selection === undefined
      ? []
      : [
          selectionColumn(
            rows,
            (r) => String(r.id),
            selection,
            (r) => r.invoiceNo ?? String(r.id),
          ),
        ]),
    {
      key: 'invoice',
      header: 'Invoice / Policy',
      render: (r) => (
        <>
          <strong>{r.invoiceNo ?? r.insurer?.referenceNo ?? '—'}</strong>
          <div className="muted">{r.broker?.policyNo ?? r.insurer?.policyNo}</div>
        </>
      ),
    },
    {
      key: 'assured',
      header: 'Assured',
      render: (r) => r.broker?.assuredName ?? r.insurer?.assuredName ?? '',
    },
    {
      // One line cut at the column width (full name in the tooltip), so the list fits its card.
      key: 'ao',
      header: 'AO',
      width: '180px',
      truncate: true,
      render: (r) => <UserName login={r.aoUsername} truncate />,
    },
    {
      key: 'gross',
      header: `Gross Premium (${clientShortName()} / Insurer)`,
      numeric: true,
      render: (r) => (
        <>
          <Amount value={r.broker?.grossPremium} />
          <div className="muted">
            <Amount value={r.insurer?.grossPremium} />
          </div>
        </>
      ),
    },
    {
      key: 'diff',
      header: 'Differences',
      render: (r) => differenceLabels(r.discrepancies),
    },
    {
      key: 'disposition',
      header: 'Disposition',
      render: (r) => <LovLabel type="RECON_DISPOSITION" code={r.feedback?.disposition} />,
    },
    {
      key: 'status',
      header: 'Status',
      kind: 'status',
      render: (r) => <StatusBadge status={r.status} />,
    },
    {
      key: 'actions',
      header: '',
      render: (r) => (
        <RowActions
          record={r.invoiceNo ?? r.insurer?.policyNo ?? `Item ${String(r.id)}`}
          actions={[
            {
              label: 'Pair with Another Item',
              hidden: !(selection !== undefined && mayPair(r)),
              onSelect: () => onPair(r),
            },
          ]}
        />
      ),
    },
  ];
}
