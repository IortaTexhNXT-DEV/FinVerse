import type { LegacySnapshot } from '@/api/operationsLegacy';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import type { Definition } from '@/components/ui/DefinitionGrid';
import { formatAmount, formatDate, humanize } from '@/utils/format';

type Line = LegacySnapshot['lines'][number];

const amount = (key: keyof Omit<Line, 'component'>, header: string): Column<Line> => ({
  key,
  header,
  numeric: true,
  render: (l) => <Amount value={l[key]} />,
});

const COLUMNS: Column<Line>[] = [
  { key: 'component', header: 'Component', render: (l) => humanize(l.component) },
  amount('booked', 'Booked'),
  amount('adjusted', 'Adjusted'),
  amount('paid', 'Paid in Legacy'),
  amount('remitted', 'Remitted in Legacy'),
  amount('writtenOff', 'Written Off'),
  amount('openBalance', 'Open at Cut-over'),
];

/**
 * The legacy block of the invoice 360 of an open legacy invoice migrated at cut-over: source
 * system, legacy invoice number and reference, migration batch and the frozen original values -
 * header amounts and the position of each component at cut-over, never updated after the load.
 */
export function LegacyInvoiceBlock({ legacy }: Readonly<{ legacy: LegacySnapshot }>) {
  const facts: Definition[] = [
    { label: 'Source System', value: legacy.sourceSystem },
    { label: 'Legacy Invoice No.', value: legacy.legacyInvoiceNo },
    { label: 'Legacy Reference', value: legacy.legacyRef },
    { label: 'Legacy Service Invoice', value: legacy.legacyServiceInvoiceNo },
    { label: 'Invoice Date', value: formatDate(legacy.invoiceDate) },
    { label: 'Due Date', value: formatDate(legacy.dueDate) },
    { label: 'Original Gross Premium', value: formatAmount(legacy.grossPremium) },
    {
      label: 'Original Commission / VAT',
      value: `${formatAmount(legacy.commission)} / ${formatAmount(legacy.vatOnCommission)}`,
    },
    { label: 'Commission Realised in Legacy', value: formatAmount(legacy.commissionRealised) },
    { label: 'Deferred VAT Open', value: formatAmount(legacy.deferredVatOpen) },
    { label: 'Insurer Shares', value: legacy.shares },
    { label: 'Migration Batch', value: legacy.migrationBatch },
  ];
  return (
    <Card title="Legacy Invoice">
      <p className="muted">
        {legacy.openBalanceMode
          ? 'Loaded from open balances only; the original amounts were not available in legacy.'
          : 'Original values at cut-over, kept as loaded.'}
      </p>
      <DefinitionGrid items={facts} columns={2} label="Legacy invoice" />
      <DataTable
        caption="Legacy positions at cut-over"
        columns={COLUMNS}
        rows={legacy.lines}
        rowKey={(l) => l.component}
        emptyMessage="No items to display"
      />
    </Card>
  );
}
