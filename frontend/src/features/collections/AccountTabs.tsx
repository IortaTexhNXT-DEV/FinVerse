import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatAmount, formatDate, formatDateTime, humanize } from '@/utils/format';
import { collectionsApi } from './api';
import type { Account } from './api';
import { TIMELINE_KINDS } from './collectionsLogic';

function Total({
  label,
  value,
  strong,
}: Readonly<{ label: string; value: string; strong?: boolean }>) {
  return (
    <div className={strong === true ? 'clx-total clx-total-strong' : 'clx-total'}>
      <span className="clx-total-label">{label}</span>
      <span className="clx-total-value">{value}</span>
    </div>
  );
}

/** Net PR breakdown per component, live from the ledger (BRCLXN.046). */
export function SummaryTab({ account }: Readonly<{ account: Account }>) {
  const lines = account.breakdown;
  const sum = (pick: (l: (typeof lines)[number]) => number) =>
    lines.reduce((total, l) => total + pick(l), 0);
  const i = account.item;
  return (
    <div className="stack">
      <div className="clx-totals">
        <Total label="Booked" value={formatAmount(sum((l) => l.booked))} />
        <Total label="Endorsements / Adjustments" value={formatAmount(sum((l) => l.adjusted))} />
        <Total label="Payments Applied" value={formatAmount(sum((l) => l.applied))} />
        <Total label="Written Off / Reversed" value={formatAmount(sum((l) => l.writtenOff))} />
        <Total label="Net Outstanding" value={formatAmount(sum((l) => l.balance))} strong />
      </div>
      <Card title="Net PR Breakdown by Component">
        <DataTable
          caption="Breakdown"
          columns={[
            { key: 'c', header: 'Component', render: (l) => humanize(l.component) },
            { key: 'b', header: 'Booked', numeric: true, render: (l) => formatAmount(l.booked) },
            {
              key: 'a',
              header: 'Adjusted',
              numeric: true,
              render: (l) => formatAmount(l.adjusted),
            },
            { key: 'p', header: 'Applied', numeric: true, render: (l) => formatAmount(l.applied) },
            {
              key: 'w',
              header: 'Written Off',
              numeric: true,
              render: (l) => formatAmount(l.writtenOff),
            },
            {
              key: 'o',
              header: 'Outstanding',
              numeric: true,
              render: (l) => formatAmount(l.balance),
            },
          ]}
          rows={lines}
          rowKey={(l) => l.component}
          emptyMessage="The invoice is no longer in the ledger"
        />
      </Card>
      <Card title="Collection Work">
        <dl className="fact-grid">
          <div className="fact">
            <span>
              <span className="fact-label">Remarks</span>
              <span className="fact-value">{i.remarks ?? '—'}</span>
            </span>
          </div>
          <div className="fact">
            <span>
              <span className="fact-label">Last Effort</span>
              <span className="fact-value">
                {i.lastEffortCode === undefined
                  ? '—'
                  : `${i.lastEffortCode} · ${formatDateTime(i.lastEffortAt)}`}
              </span>
            </span>
          </div>
          <div className="fact">
            <span>
              <span className="fact-label">Promise / Escalation</span>
              <span className="fact-value">
                {[i.promiseStatus, i.escalationLevel].filter(Boolean).join(' · ') || '—'}
              </span>
            </span>
          </div>
        </dl>
      </Card>
    </div>
  );
}

/** Payments applied and reversed, one line per transaction (BRCLXN.054). */
export function PaymentsTab({ invoiceNo }: Readonly<{ invoiceNo: string }>) {
  const q = useQuery({
    queryKey: ['collections', 'payments', invoiceNo],
    queryFn: () => collectionsApi.payments(invoiceNo),
  });
  if (q.data === undefined) {
    return q.error ? (
      <ErrorAlert error={q.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  return (
    <div className="stack">
      <div className="clx-totals">
        <Total label="Premium Due" value={formatAmount(q.data.booked)} />
        <Total label="Paid" value={formatAmount(q.data.paid)} />
        <Total label="Outstanding" value={formatAmount(q.data.outstanding)} strong />
      </div>
      <Card title="Payment History">
        <DataTable
          caption="Payments"
          columns={[
            { key: 'd', header: 'Date', render: (l) => formatDate(l.date) },
            {
              key: 't',
              header: 'Type',
              render: (l) => (l.type === 'APPLIED' ? 'Applied' : 'Reversed'),
            },
            {
              key: 'ar',
              header: 'AR / OR',
              render: (l) => [l.arNo, l.orNo].filter(Boolean).join(' / '),
            },
            { key: 'r', header: 'Reference', render: (l) => `${l.sourceModule} ${l.reference}` },
            { key: 'a', header: 'Amount', numeric: true, render: (l) => formatAmount(l.amount) },
          ]}
          rows={q.data.lines}
          rowKey={(l) => `${l.type}-${l.reference}`}
          emptyMessage="No payment applied yet"
        />
      </Card>
    </div>
  );
}

/** Ledger movements and Collections actions, newest first (BRCLXN.057). */
export function TimelineTab({ invoiceNo }: Readonly<{ invoiceNo: string }>) {
  const q = useQuery({
    queryKey: ['collections', 'timeline', invoiceNo],
    queryFn: () => collectionsApi.timeline(invoiceNo),
  });
  if (q.data === undefined) {
    return q.error ? (
      <ErrorAlert error={q.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  if (q.data.length === 0) {
    return <EmptyState message="No history yet" />;
  }
  return (
    <Card>
      <ol className="clx-timeline">
        {q.data.map((e, index) => (
          <li key={`${e.at}-${index}`}>
            <span className="clx-muted">{formatDateTime(e.at)}</span>
            <span className="tag">{TIMELINE_KINDS[e.kind] ?? e.kind}</span>
            <span>
              <span className="clx-timeline-title">{e.title}</span>
              {e.detail !== undefined && <div className="clx-muted">{e.detail}</div>}
            </span>
            <span className="clx-muted">
              {[e.by, e.amount === undefined ? undefined : formatAmount(e.amount)]
                .filter(Boolean)
                .join(' · ')}
            </span>
          </li>
        ))}
      </ol>
    </Card>
  );
}

/** Policy, invoice family and co-insurance shares, read-only (BRCLXN.056). */
export function PolicyTab({ invoiceNo }: Readonly<{ invoiceNo: string }>) {
  const q = useQuery({
    queryKey: ['collections', 'policy', invoiceNo],
    queryFn: () => collectionsApi.policy(invoiceNo),
  });
  if (q.data === undefined) {
    return q.error ? (
      <ErrorAlert error={q.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const p = q.data;
  return (
    <div className="clx-grid-2">
      <Card title="Invoice Family">
        <DataTable
          caption="Invoice family"
          columns={[
            {
              key: 'no',
              header: 'Invoice No.',
              render: (f) => (
                <Link to={`/operations/invoices/${encodeURIComponent(f.invoiceNo)}`}>
                  {f.invoiceNo}
                </Link>
              ),
            },
            { key: 'k', header: 'Kind', render: (f) => humanize(f.kind) },
            { key: 'd', header: 'Booked', render: (f) => formatDate(f.bookingDate) },
            {
              key: 'g',
              header: 'Gross',
              numeric: true,
              render: (f) => formatAmount(f.grossPremium),
            },
            {
              key: 'b',
              header: 'Balance',
              numeric: true,
              render: (f) => formatAmount(f.premiumBalance),
            },
            { key: 's', header: 'Status', render: (f) => <StatusBadge status={f.paymentStatus} /> },
          ]}
          rows={p.family}
          rowKey={(f) => f.invoiceNo}
        />
      </Card>
      <Card title="Policy and Co-insurance">
        <div className="stack">
          <dl className="fact-grid">
            {[
              ['Policy', `${p.policyNo ?? '—'} (year ${p.policyYear})`],
              ['Product / Risk', [p.productLine, p.riskCode].filter(Boolean).join(' / ') || '—'],
              ['Period', `${formatDate(p.inceptionDate)} – ${formatDate(p.expiryDate)}`],
              ['Booked', formatDate(p.bookingDate)],
              ['Receipt Date (first AR)', formatDate(p.firstReceiptDate) || '—'],
              ['Delivery Date', 'To confirm with BDOI (CQ17)'],
            ].map(([label, value]) => (
              <div className="fact" key={label}>
                <span>
                  <span className="fact-label">{label}</span>
                  <span className="fact-value">{value}</span>
                </span>
              </div>
            ))}
          </dl>
          <DataTable
            caption="Insurer shares"
            columns={[
              { key: 'i', header: 'Insurer', render: (s) => s.insurerCode },
              {
                key: 'p',
                header: 'Share %',
                numeric: true,
                render: (s) => formatAmount(s.sharePct),
              },
              { key: 'l', header: 'Lead', render: (s) => (s.lead ? 'Lead' : '') },
            ]}
            rows={p.shares}
            rowKey={(s) => s.insurerCode}
          />
        </div>
      </Card>
    </div>
  );
}
