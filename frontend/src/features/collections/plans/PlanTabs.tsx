import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { formatDate } from '@/utils/format';
import type { Statement } from '../billing/api';
import type { Installment, PaymentPromise, Plan } from './api';
import { plansApi } from './api';

/** Tabs of the installment plan record (BRCLXN.053/055/058). */

/** What the user may do on an installment. */
export interface InstallmentActions {
  billed: Set<number>;
  canBill: boolean;
  canPromise: boolean;
  busy: boolean;
  onBill: (seq: number) => void;
  onPromise: (installment: Installment) => void;
}

function actionsOf(i: Installment, a: InstallmentActions) {
  return (
    <span className="row">
      {a.canBill && !a.billed.has(i.seq) && (
        <Button size="sm" variant="secondary" busy={a.busy} onClick={() => a.onBill(i.seq)}>
          Generate SOA
        </Button>
      )}
      {a.canPromise && i.invoiceNo !== undefined && i.status !== 'PAID' && (
        <Button size="sm" variant="secondary" onClick={() => a.onPromise(i)}>
          Record Promise
        </Button>
      )}
    </span>
  );
}

/** The installments (billing cycles) with their allocation and status. */
export function InstallmentsTab({
  plan,
  actions,
}: Readonly<{ plan: Plan; actions: InstallmentActions }>) {
  const columns: Column<Installment>[] = [
    { key: 'seq', header: 'Cycle', render: (i) => i.seq },
    { key: 'year', header: 'Policy Year', render: (i) => i.policyYear },
    { key: 'inv', header: 'Invoice No.', render: (i) => i.invoiceNo ?? 'Scheduled' },
    {
      key: 'cycle',
      header: 'Coverage',
      render: (i) => `${formatDate(i.cycleFrom)} – ${formatDate(i.cycleTo)}`,
    },
    { key: 'due', header: 'Due Date', render: (i) => formatDate(i.dueDate) },
    { key: 'amount', header: 'Amount', numeric: true, render: (i) => <Amount value={i.amount} /> },
    { key: 'paid', header: 'Paid', numeric: true, render: (i) => <Amount value={i.paidAmount} /> },
    { key: 'bal', header: 'Balance', numeric: true, render: (i) => <Amount value={i.balance} /> },
    { key: 'status', header: 'Status', render: (i) => <StatusBadge status={i.status} /> },
  ];
  if (plan.status !== 'CANCELLED') {
    columns.push({ key: 'act', header: 'Actions', render: (i) => actionsOf(i, actions) });
  }
  return (
    <DataTable
      caption="Installments"
      columns={columns}
      rows={plan.installments}
      rowKey={(i) => i.id}
      emptyMessage="No installments"
    />
  );
}

const STATEMENT_COLUMNS: Column<Statement>[] = [
  { key: 'no', header: 'SOA No.', render: (s) => <strong>{s.soaNo}</strong> },
  { key: 'cycle', header: 'Cycle', render: (s) => s.cycleSeq },
  {
    key: 'period',
    header: 'Billing Cycle',
    render: (s) => `${formatDate(s.cycleFrom)} – ${formatDate(s.cycleTo)}`,
  },
  { key: 'due', header: 'Due Date', render: (s) => formatDate(s.dueDate) },
  { key: 'bal', header: 'Amount Due', numeric: true, render: (s) => <Amount value={s.balance} /> },
  { key: 'status', header: 'Status', render: (s) => <StatusBadge status={s.status} /> },
];

/** The statements of account of the plan. */
export function StatementsTab({ statements }: Readonly<{ statements: Statement[] }>) {
  const navigate = useNavigate();
  return (
    <DataTable
      caption="Statements of account"
      columns={STATEMENT_COLUMNS}
      rows={statements}
      rowKey={(s) => s.id}
      emptyMessage="No statements generated yet"
      onRowClick={(s) => void navigate(`/collections/billing/${s.id}`)}
    />
  );
}

const PROMISE_COLUMNS: Column<PaymentPromise>[] = [
  { key: 'inv', header: 'Invoice No.', render: (p) => p.invoiceNo },
  { key: 'on', header: 'Promised On', render: (p) => formatDate(p.promisedOn) },
  { key: 'date', header: 'Promised Date', render: (p) => formatDate(p.promisedDate) },
  {
    key: 'amount',
    header: 'Promised',
    numeric: true,
    render: (p) => <Amount value={p.promisedAmount} />,
  },
  {
    key: 'paid',
    header: 'Paid in Time',
    numeric: true,
    render: (p) => <Amount value={p.actualPaid} />,
  },
  { key: 'by', header: 'Recorded By', render: (p) => p.recordedBy },
  { key: 'status', header: 'Status', render: (p) => <StatusBadge status={p.status} /> },
];

/** The promises to pay on the plan's invoices. */
export function PromisesTab({ plan }: Readonly<{ plan: Plan }>) {
  const invoices = [
    ...new Set(plan.installments.flatMap((i) => (i.invoiceNo === undefined ? [] : [i.invoiceNo]))),
  ];
  const promises = useQuery({
    queryKey: ['collections', 'promises', 'plan', plan.id, invoices],
    queryFn: async () => (await Promise.all(invoices.map((n) => plansApi.promisesOf(n)))).flat(),
  });
  return (
    <DataTable
      caption="Promises to pay"
      columns={PROMISE_COLUMNS}
      rows={promises.data ?? []}
      rowKey={(p) => p.id}
      loading={promises.isLoading}
      emptyMessage="No promises to pay recorded"
    />
  );
}

type PlanTab = 'INSTALLMENTS' | 'STATEMENTS' | 'PROMISES';

const TABS: readonly { id: PlanTab; label: string }[] = [
  { id: 'INSTALLMENTS', label: 'Installments' },
  { id: 'STATEMENTS', label: 'Statements of Account' },
  { id: 'PROMISES', label: 'Promises to Pay' },
];

/** The tabs card of the plan record: installments, statements of account, promises. */
export function PlanTabsCard({
  plan,
  statements,
  actions,
}: Readonly<{
  plan: Plan;
  statements: Statement[];
  actions: Omit<InstallmentActions, 'billed'>;
}>) {
  const [tab, setTab] = useState<PlanTab>('INSTALLMENTS');
  const billed = new Set(statements.filter((s) => s.status !== 'CANCELLED').map((s) => s.cycleSeq));
  return (
    <Card flush>
      <div>
        <Tabs tabs={TABS} active={tab} onChange={setTab} />
        {tab === 'INSTALLMENTS' && <InstallmentsTab plan={plan} actions={{ ...actions, billed }} />}
        {tab === 'STATEMENTS' && <StatementsTab statements={statements} />}
        {tab === 'PROMISES' && <PromisesTab plan={plan} />}
      </div>
    </Card>
  );
}
