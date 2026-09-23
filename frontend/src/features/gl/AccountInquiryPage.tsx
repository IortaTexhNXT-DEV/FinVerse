import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { glApi } from '@/api/gl';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Kpi } from '@/components/ui/Kpi';
import { PageHeader } from '@/components/ui/PageHeader';
import { useWorkspace } from '@/context/workspaceContext';
import { formatAmount, formatDate, today } from '@/utils/format';
import { useGlLookups } from './useLookups';

/** GL inquiry: account statement with opening, running and closing balance and drill-down. */
export default function AccountInquiryPage() {
  const { postableAccounts } = useGlLookups();
  const { branchId } = useWorkspace();
  const navigate = useNavigate();
  const [code, setCode] = useState('');
  const [from, setFrom] = useState(`${today().slice(0, 7)}-01`);
  const [to, setTo] = useState(today());
  const account = postableAccounts.find((a) => a.code === code);

  const statement = useQuery({
    queryKey: ['statement', account?.id, from, to, branchId],
    queryFn: () => glApi.statement(account?.id ?? 0, from, to, branchId),
    enabled: account !== undefined,
  });
  const s = statement.data;

  return (
    <div className="stack">
      <PageHeader
        section="General Ledger"
        title="Account Inquiry"
        description="Statement of any GL account with drill-down to the voucher."
      />
      <Card>
        <datalist id="inquiry-accounts">
          {postableAccounts.map((a) => (
            <option key={a.id} value={a.code}>
              {a.name}
            </option>
          ))}
        </datalist>
        <div className="form-grid">
          <Field label="Account" required hint={account?.name}>
            {(id) => (
              <input
                id={id}
                className="input"
                list="inquiry-accounts"
                value={code}
                onChange={(e) => setCode(e.target.value.trim())}
              />
            )}
          </Field>
          <Field label="From">
            {(id) => (
              <input
                id={id}
                className="input"
                type="date"
                value={from}
                onChange={(e) => setFrom(e.target.value)}
              />
            )}
          </Field>
          <Field label="To">
            {(id) => (
              <input
                id={id}
                className="input"
                type="date"
                value={to}
                onChange={(e) => setTo(e.target.value)}
              />
            )}
          </Field>
        </div>
      </Card>
      <ErrorAlert error={statement.error} />
      {s !== undefined && (
        <>
          <div className="grid-4">
            <Kpi
              label="Opening balance"
              value={formatAmount(s.openingBalance)}
              hint="Debit positive"
            />
            <Kpi label="Total debits" value={formatAmount(s.totalDebit)} />
            <Kpi label="Total credits" value={formatAmount(s.totalCredit)} />
            <Kpi label="Closing balance" value={formatAmount(s.closingBalance)} accent />
          </div>
          <Card title={`${s.accountCode} – ${s.accountName}`} flush>
            <DataTable
              rows={s.lines}
              rowKey={(l) => `${l.batchId}-${l.batchNo}-${l.runningBalance}`}
              onRowClick={(l) => void navigate(`/gl/journals/${l.batchId}`)}
              columns={[
                { key: 'd', header: 'Date', render: (l) => formatDate(l.valueDate) },
                { key: 'b', header: 'Batch', render: (l) => l.batchNo },
                { key: 't', header: 'Type', render: (l) => l.journalType },
                { key: 'n', header: 'Narration', render: (l) => l.narration ?? '' },
                { key: 'r', header: 'Reference', render: (l) => l.reference ?? '' },
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
                  render: (l) => <Amount value={l.runningBalance} />,
                },
              ]}
            />
          </Card>
        </>
      )}
    </div>
  );
}
