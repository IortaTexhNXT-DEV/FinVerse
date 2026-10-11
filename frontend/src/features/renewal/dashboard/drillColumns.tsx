import { Link } from 'react-router-dom';
import type { Drill, DrillColumn, DrillRow } from '@/api/renewalDashboard';
import type { Column, ColumnKind } from '@/components/ui/DataTable';
import { UserName } from '@/components/ui/UserName';
import { formatAmount, formatDate } from '@/utils/format';

function kindOf(c: DrillColumn): ColumnKind | undefined {
  if (c.kind === 'AMOUNT' || c.kind === 'NUMBER') {
    return 'amount';
  }
  return c.kind === 'DATE' ? 'date' : undefined;
}

function extraValue(c: DrillColumn, r: DrillRow): string {
  const v = r.extra[c.key];
  if (v === null || v === undefined) {
    return '';
  }
  if (c.kind === 'AMOUNT') {
    return formatAmount(Number(v));
  }
  return c.kind === 'DATE' ? formatDate(String(v)) : String(v);
}

const text = (key: keyof DrillRow, header: string, kind?: ColumnKind): Column<DrillRow> => ({
  key,
  sortKey: key,
  header,
  kind,
  render: (r) => {
    const v = r[key];
    return typeof v === 'string' ? v : '';
  },
});

/** The columns of the marketing drill-down (FRRN.002.02) and the extra columns of the figure. */
export function drillColumns(drill: Drill | undefined): Column<DrillRow>[] {
  const base: Column<DrillRow>[] = [
    {
      key: 'ref',
      sortKey: 'ref',
      header: 'Reference Number',
      kind: 'code',
      render: (r) =>
        r.businessType === 'Renewal' ? (
          <Link to={`/renewal/candidates/${encodeURIComponent(r.ref)}`}>{r.ref}</Link>
        ) : (
          r.ref
        ),
    },
    text('invoiceNo', 'Invoice Number', 'code'),
    text('expiringInvoiceNo', 'Expiring Invoice Number', 'code'),
    text('assured', "Assured's Name"),
    text('productLine', 'Product Line'),
    text('riskCode', 'Risk Code', 'code'),
    {
      key: 'premium',
      sortKey: 'premium',
      header: 'Premium',
      kind: 'amount',
      render: (r) => formatAmount(r.premium),
    },
    {
      key: 'commission',
      sortKey: 'commission',
      header: 'Commission',
      kind: 'amount',
      render: (r) => formatAmount(r.commission),
    },
    text('status', 'Renewal Status'),
    {
      key: 'assignedUser',
      sortKey: 'assignedUser',
      header: 'Assigned User',
      render: (r) => (r.assignedUser === null ? '' : <UserName login={r.assignedUser} />),
    },
    text('insurerDisposition', 'Insurer Disposition'),
    text('insurerRemarks', 'Insurer Remarks'),
    {
      key: 'expiryDate',
      sortKey: 'expiryDate',
      header: 'Expiry Date',
      kind: 'date',
      render: (r) => formatDate(r.expiryDate),
    },
  ];
  const extra: Column<DrillRow>[] = (drill?.columns ?? []).map((c) => ({
    key: `x-${c.key}`,
    sortKey: c.key,
    header: c.label,
    kind: kindOf(c),
    render: (r: DrillRow) => extraValue(c, r),
  }));
  return [...base, ...extra];
}
