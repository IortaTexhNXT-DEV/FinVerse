import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { receivablesApi } from '@/api/receivables';
import type { StatementLineItem } from '@/api/receivables';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Kpi } from '@/components/ui/Kpi';
import { PageHeader } from '@/components/ui/PageHeader';
import { formatAmount, formatDate, today } from '@/utils/format';
import { useReceivablesLookups } from './useReceivablesLookups';

const DEBTOR_TYPES = ['INDIVIDUAL_CLIENT', 'CORPORATE_CLIENT', 'AGENT', 'BROKER', 'REINSURER'];

function Lines({
  title,
  rows,
  net,
}: Readonly<{ title: string; rows: StatementLineItem[]; net: number }>) {
  return (
    <Card
      title={title}
      actions={
        <span>
          Net balance <strong>{formatAmount(net)}</strong>
        </span>
      }
      flush
    >
      <DataTable<StatementLineItem>
        rows={rows}
        rowKey={(l) => l.itemId}
        caption={title}
        emptyMessage="No documents."
        columns={[
          { key: 'date', header: 'Document date', render: (l) => formatDate(l.documentDate) },
          { key: 'ref', header: 'Document reference', render: (l) => l.reference ?? '' },
          { key: 'tc', header: 'Transaction code', render: (l) => l.transactionCode },
          { key: 'chq', header: 'Cheque no.', render: (l) => l.chequeNo ?? '' },
          { key: 'chqd', header: 'Cheque date', render: (l) => formatDate(l.chequeDate) },
          { key: 'ccy', header: 'Ccy', render: (l) => l.currency },
          {
            key: 'dr',
            header: 'Debit',
            numeric: true,
            render: (l) => <Amount value={l.debit || null} />,
          },
          {
            key: 'cr',
            header: 'Credit',
            numeric: true,
            render: (l) => <Amount value={l.credit || null} />,
          },
          {
            key: 'bal',
            header: 'Balance',
            numeric: true,
            render: (l) => <Amount value={l.balance} />,
          },
        ]}
      />
    </Card>
  );
}

/**
 * Statement of account of a customer, intermediary or reinsurer with matched (knocked off) and
 * unmatched documents of a period - the basis of the next month's statement reconciliation.
 */
export default function PartyStatementPage() {
  const { companyId } = useReceivablesLookups();
  const [partyCode, setPartyCode] = useState('');
  const [from, setFrom] = useState(`${today().slice(0, 4)}-01-01`);
  const [to, setTo] = useState(today());
  const [foreign, setForeign] = useState(true);
  const parties = useQuery({
    queryKey: ['parties', companyId, 'debtors'],
    queryFn: () => receivablesApi.parties(companyId, DEBTOR_TYPES),
    enabled: companyId > 0,
  });
  const statement = useQuery({
    queryKey: ['party-statement', companyId, partyCode, from, to, foreign],
    queryFn: () => receivablesApi.partyStatement(companyId, partyCode, from, to, foreign),
    enabled: companyId > 0 && partyCode !== '',
  });
  const s = statement.data?.[0];

  return (
    <div className="stack">
      <PageHeader
        section="Receivables & Banking"
        title="Statement of Account"
        description="Documents of a period split into matched and unmatched details."
      />
      <Card>
        <div className="form-grid">
          <Field label="Party" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={partyCode}
                onChange={(e) => setPartyCode(e.target.value)}
              >
                <option value="">Select...</option>
                {(parties.data ?? []).map((p) => (
                  <option key={p.code} value={p.code}>{`${p.code} - ${p.name}`}</option>
                ))}
              </select>
            )}
          </Field>
          <Field label="From">
            {(id) => (
              <input
                id={id}
                type="date"
                className="input"
                value={from}
                onChange={(e) => setFrom(e.target.value)}
              />
            )}
          </Field>
          <Field label="To">
            {(id) => (
              <input
                id={id}
                type="date"
                className="input"
                value={to}
                onChange={(e) => setTo(e.target.value)}
              />
            )}
          </Field>
          <Field label="Currency">
            {(id) => (
              <select
                id={id}
                className="select"
                value={foreign ? 'FOREIGN' : 'BASE'}
                onChange={(e) => setForeign(e.target.value === 'FOREIGN')}
              >
                <option value="FOREIGN">Document currency (as is)</option>
                <option value="BASE">Base currency (converted)</option>
              </select>
            )}
          </Field>
        </div>
      </Card>
      <ErrorAlert error={statement.error} />
      {partyCode !== '' && statement.data !== undefined && s === undefined && (
        <p className="muted">No documents for this party in the period.</p>
      )}
      {s !== undefined && (
        <>
          <div className="grid-4">
            <Kpi label="Party" value={s.partyCode} hint={s.partyName} />
            <Kpi label="Matched documents" value={s.matched.length} />
            <Kpi label="Unmatched documents" value={s.unmatched.length} />
            <Kpi label="Grand total (Dr+/Cr-)" value={formatAmount(s.unmatchedNet)} accent />
          </div>
          <Lines title="Unmatched details" rows={s.unmatched} net={s.unmatchedNet} />
          <Lines title="Matched details" rows={s.matched} net={s.matchedNet} />
        </>
      )}
    </div>
  );
}
