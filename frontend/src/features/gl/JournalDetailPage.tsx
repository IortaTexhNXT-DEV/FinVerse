import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { glApi } from '@/api/gl';
import type { Journal } from '@/api/gl';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate, formatDateTime } from '@/utils/format';
import { JournalActions } from './JournalActions';

function Summary({ journal: j }: Readonly<{ journal: Journal }>) {
  return (
    <div className="grid-4">
      <Card>
        <div className="kpi-label">Type</div>
        <strong>{j.journalType}</strong>
      </Card>
      <Card>
        <div className="kpi-label">Value date</div>
        <strong>{formatDate(j.valueDate)}</strong>
      </Card>
      <Card>
        <div className="kpi-label">Total debit / credit</div>
        <strong>
          <Amount value={j.totalDebit} /> / <Amount value={j.totalCredit} />
        </strong>
      </Card>
      <Card>
        <div className="kpi-label">Source</div>
        <strong>{j.sourceModule ?? 'Manual entry'}</strong>
        {j.reference !== undefined && <div className="muted">Ref {j.reference}</div>}
      </Card>
    </div>
  );
}

function AuditPanel({ journal }: Readonly<{ journal: Journal }>) {
  const rows: [string, string | undefined, string | undefined][] = [
    ['Input by', journal.createdBy, journal.createdAt],
    ['Submitted by', journal.submittedBy, journal.submittedAt],
    ['Authorized by', journal.authorizedBy, journal.authorizedAt],
    ['Posted', journal.postedAt === undefined ? undefined : 'Posting engine', journal.postedAt],
  ];
  return (
    <Card title="Audit">
      <dl className="form-grid" style={{ margin: 0 }}>
        {rows.map(([label, who, when]) => (
          <div key={label}>
            <dt className="muted">{label}</dt>
            <dd style={{ margin: 0, fontWeight: 600 }}>
              {who ?? '—'} <span className="muted">{formatDateTime(when)}</span>
            </dd>
          </div>
        ))}
        {journal.rejectionReason !== undefined && (
          <div>
            <dt className="muted">Rejected by {journal.rejectedBy}</dt>
            <dd style={{ margin: 0 }}>{journal.rejectionReason}</dd>
          </div>
        )}
      </dl>
    </Card>
  );
}

/** Journal voucher view: header, lines, audit and the permitted workflow actions. */
export default function JournalDetailPage() {
  const id = Number(useParams().id);
  const journal = useQuery({ queryKey: ['journal', id], queryFn: () => glApi.journal(id) });

  if (journal.data === undefined) {
    return journal.error ? (
      <ErrorAlert error={journal.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const j = journal.data;
  return (
    <div className="stack">
      <PageHeader
        section="General Ledger · Journal"
        title={j.batchNo}
        description={j.narration}
        actions={
          <>
            <StatusBadge status={j.status} />
            <JournalActions journal={j} />
          </>
        }
      />
      <Summary journal={j} />
      <Card title="Lines" flush>
        <DataTable
          rows={j.lines}
          rowKey={(l) => l.lineNo}
          columns={[
            { key: 'no', header: '#', render: (l) => l.lineNo },
            { key: 'acc', header: 'Account', render: (l) => `${l.accountCode} – ${l.accountName}` },
            { key: 'cc', header: 'Cost centre', render: (l) => l.costCenter ?? '' },
            { key: 'lob', header: 'LOB', render: (l) => l.businessLine ?? '' },
            { key: 'ccy', header: 'Ccy', render: (l) => l.currency },
            {
              key: 'dr',
              header: 'Debit',
              numeric: true,
              render: (l) => (l.side === 'DEBIT' ? <Amount value={l.baseAmount} /> : ''),
            },
            {
              key: 'cr',
              header: 'Credit',
              numeric: true,
              render: (l) => (l.side === 'CREDIT' ? <Amount value={l.baseAmount} /> : ''),
            },
            { key: 'nar', header: 'Narration', render: (l) => l.narration ?? l.reference ?? '' },
          ]}
        />
      </Card>
      <AuditPanel journal={j} />
    </div>
  );
}
