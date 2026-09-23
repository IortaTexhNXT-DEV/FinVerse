import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { receivablesApi } from '@/api/receivables';
import type { Reconciliation } from '@/api/receivables';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { formatDate, today } from '@/utils/format';
import { BrsView } from './BrsView';
import { MatchWorkbench } from './MatchWorkbench';
import { bankOptions, useReceivablesLookups } from './useReceivablesLookups';

const TABS = [
  { id: 'match', label: 'Match workbench' },
  { id: 'brs', label: 'Reconciliation statement' },
  { id: 'history', label: 'Reconciliations' },
] as const;
type Tab = (typeof TABS)[number]['id'];

/**
 * Bank reconciliation of a GL bank account: matching workbench, Bank Reconciliation Statement as of
 * a date and the saved / finalized reconciliations.
 */
export default function BankReconciliationPage() {
  const { companyId, bankAccounts } = useReceivablesLookups();
  const [bank, setBank] = useState('');
  const [asOf, setAsOf] = useState(today());
  const [tab, setTab] = useState<Tab>('match');
  const selectedBank = bank === '' ? (bankAccounts[0]?.code ?? '') : bank;
  const history = useQuery({
    queryKey: ['reconciliations', companyId],
    queryFn: () => receivablesApi.reconciliations(companyId),
    enabled: companyId > 0 && tab === 'history',
  });

  return (
    <div className="stack">
      <PageHeader
        section="Receivables & Banking"
        title="Bank Reconciliation"
        description="Match the book entries of a bank account with the bank statement and reconcile the balances."
      />
      <Card>
        <div className="form-grid">
          <Field label="Bank account" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={selectedBank}
                onChange={(e) => setBank(e.target.value)}
              >
                {bankOptions(bankAccounts).map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Statement date (as of)" required>
            {(id) => (
              <input
                id={id}
                type="date"
                className="input"
                value={asOf}
                onChange={(e) => setAsOf(e.target.value)}
              />
            )}
          </Field>
        </div>
      </Card>
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {selectedBank !== '' && tab === 'match' && (
        <MatchWorkbench companyId={companyId} bank={selectedBank} asOf={asOf} />
      )}
      {selectedBank !== '' && tab === 'brs' && (
        <BrsView companyId={companyId} bank={selectedBank} asOf={asOf} />
      )}
      {tab === 'history' && (
        <Card flush>
          <DataTable<Reconciliation>
            loading={history.isLoading}
            rows={history.data ?? []}
            rowKey={(r) => r.id}
            onRowClick={(r) => {
              setBank(r.bankAccountCode);
              setAsOf(r.asOfDate);
              setTab('brs');
            }}
            caption="Reconciliations"
            columns={[
              { key: 'bank', header: 'Bank', render: (r) => r.bankAccountCode },
              { key: 'date', header: 'As of', render: (r) => formatDate(r.asOfDate) },
              {
                key: 'book',
                header: 'Book balance',
                numeric: true,
                render: (r) => <Amount value={r.bookBalance} />,
              },
              {
                key: 'stmt',
                header: 'Statement balance',
                numeric: true,
                render: (r) => <Amount value={r.statementBalance} />,
              },
              {
                key: 'diff',
                header: 'Difference',
                numeric: true,
                render: (r) => <Amount value={r.difference} />,
              },
              {
                key: 'st',
                header: 'Status',
                render: (r) => (
                  <StatusBadge status={r.status === 'FINALIZED' ? 'RECONCILED' : r.status} />
                ),
              },
              { key: 'by', header: 'Finalized by', render: (r) => r.finalizedBy ?? '' },
            ]}
          />
        </Card>
      )}
    </div>
  );
}
