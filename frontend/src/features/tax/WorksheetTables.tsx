import { Link } from 'react-router-dom';
import type { LedgerControl, TaxDocument, Worksheet, WorksheetLine } from '@/api/tax';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { formatDate } from '@/utils/format';
import { sourceLink } from './taxDisplay';

const EMPTY: Worksheet = {
  kind: 'NONE',
  from: '',
  to: '',
  periodLabel: '',
  lines: [],
  figures: { taxBase: 0, taxDue: 0, taxCredits: 0, amountPayable: 0, excessCredit: 0 },
  documents: [],
  controls: [],
  notes: [],
};

/** Return lines, ledger reconciliation (with notes) and source documents of a worksheet. */
export function WorksheetTables({
  worksheet,
  loading,
}: Readonly<{ worksheet: Worksheet | undefined; loading: boolean }>) {
  const w = worksheet ?? EMPTY;
  return (
    <>
      <Card title="Return lines" flush>
        <DataTable<WorksheetLine>
          rows={w.lines}
          loading={loading}
          rowKey={(l) => l.code}
          caption="Return lines"
          columns={[
            { key: 'c', header: 'Line', render: (l) => l.code },
            { key: 'd', header: 'Description', render: (l) => l.description },
            {
              key: 'b',
              header: 'Tax base',
              numeric: true,
              render: (l) => <Amount value={l.base} />,
            },
            {
              key: 'a',
              header: 'Amount',
              numeric: true,
              render: (l) => <Amount value={l.amount} />,
            },
          ]}
        />
      </Card>
      <Card title="Reconciliation with the ledger" flush>
        <DataTable<LedgerControl>
          rows={w.controls}
          rowKey={(c) => c.description}
          caption="Ledger reconciliation"
          emptyMessage="No tax code configured for this worksheet."
          columns={[
            { key: 'd', header: 'Reconciled', render: (c) => c.description },
            { key: 'a', header: 'GL account', render: (c) => c.accountCode },
            {
              key: 'p',
              header: 'Documents',
              numeric: true,
              render: (c) => <Amount value={c.perDocuments} />,
            },
            {
              key: 'l',
              header: 'Ledger',
              numeric: true,
              render: (c) => <Amount value={c.perLedger} />,
            },
            {
              key: 'x',
              header: 'Difference',
              numeric: true,
              render: (c) => <Amount value={c.difference} />,
            },
          ]}
        />
        {w.notes.length > 0 && (
          <div className="card-body">
            {w.notes.map((n) => (
              <p key={n} className="muted">
                {n}
              </p>
            ))}
          </div>
        )}
      </Card>
      <Card title={`Source documents (${w.documents.length})`} flush>
        <DataTable<TaxDocument>
          rows={w.documents}
          loading={loading}
          rowKey={(d) => `${d.section}-${d.sourceType}-${d.documentNo}`}
          caption="Source documents"
          columns={[
            { key: 's', header: 'Section', render: (d) => d.section },
            {
              key: 'n',
              header: 'Document',
              render: (d) => <Link to={sourceLink(d)}>{d.documentNo}</Link>,
            },
            { key: 'dt', header: 'Date', render: (d) => formatDate(d.documentDate) },
            { key: 'p', header: 'Party', render: (d) => d.partyName },
            { key: 'tin', header: 'TIN', render: (d) => d.tin },
            { key: 'code', header: 'Class / ATC', render: (d) => d.taxCode },
            {
              key: 'base',
              header: 'Base',
              numeric: true,
              render: (d) => <Amount value={d.taxableAmount} />,
            },
            {
              key: 'tax',
              header: 'Tax',
              numeric: true,
              render: (d) => <Amount value={d.taxAmount} />,
            },
            { key: 'r', header: 'Rate %', numeric: true, render: (d) => d.rate },
          ]}
        />
      </Card>
    </>
  );
}
