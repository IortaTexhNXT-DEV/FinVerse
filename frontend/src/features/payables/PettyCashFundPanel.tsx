import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { payablesApi } from '@/api/payables';
import type { Disbursement, DisbursementRequest, Fund, Reimbursement } from '@/api/payables';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useGlLookups } from '@/features/gl/useLookups';
import { formatDate, today } from '@/utils/format';

type Tab = 'vouchers' | 'claims';
const TABS: { id: Tab; label: string }[] = [
  { id: 'vouchers', label: 'Disbursement vouchers' },
  { id: 'claims', label: 'Reimbursement claims' },
];

const EMPTY: DisbursementRequest = {
  date: today(),
  payee: '',
  expenseAccountCode: '',
  costCenter: '',
  description: '',
  receiptRef: '',
  amount: 0,
};

/** Vouchers and reimbursement claims of one petty cash fund, with maker and checker actions. */
export function PettyCashFundPanel({ fund }: Readonly<{ fund: Fund }>) {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const { postableAccounts, costCenters } = useGlLookups();
  const [tab, setTab] = useState<Tab>('vouchers');
  const [form, setForm] = useState<DisbursementRequest | null>(null);
  const vouchers = useQuery({
    queryKey: ['petty-cash-vouchers', fund.id],
    queryFn: () => payablesApi.disbursements(fund.id),
  });
  const claims = useQuery({
    queryKey: ['petty-cash-claims', fund.id],
    queryFn: () => payablesApi.reimbursements(fund.id),
  });
  const refresh = async (message: string) => {
    await queryClient.invalidateQueries({ queryKey: ['petty-cash-vouchers', fund.id] });
    await queryClient.invalidateQueries({ queryKey: ['petty-cash-claims', fund.id] });
    await queryClient.invalidateQueries({ queryKey: ['petty-cash-funds'] });
    toast.success(message);
  };
  const disburse = useMutation({
    mutationFn: (f: DisbursementRequest) => payablesApi.disburse(fund.id, f),
    onSuccess: async (d) => {
      setForm(null);
      await refresh(`${d.documentNo} captured – pending approval`);
    },
  });
  const decide = useMutation({
    mutationFn: ({ d, approve }: { d: Disbursement; approve: boolean }) =>
      approve
        ? payablesApi.approveDisbursement(d.id)
        : payablesApi.rejectDisbursement(d.id, 'Rejected by checker'),
    onSuccess: (d) => refresh(`${d.documentNo} ${d.status.toLowerCase()}`),
  });
  const claim = useMutation({
    mutationFn: () => payablesApi.claimReimbursement(fund.id, today()),
    onSuccess: (c) => refresh(`${c.documentNo} claims ${c.amount.toFixed(2)}`),
  });
  const decideClaim = useMutation({
    mutationFn: ({ c, approve }: { c: Reimbursement; approve: boolean }) =>
      approve
        ? payablesApi.approveReimbursement(c.id)
        : payablesApi.rejectReimbursement(c.id, 'Rejected by checker'),
    onSuccess: (c) => refresh(`${c.documentNo} ${c.status.toLowerCase()}`),
  });
  const checker = can('RECEIPT_PAYMENT_AUTHORIZE');
  const decisionButtons = (onApprove: () => void, onReject: () => void) => (
    <div className="row">
      <Button size="sm" variant="secondary" onClick={onApprove}>
        Approve
      </Button>
      <Button size="sm" variant="ghost" onClick={onReject}>
        Reject
      </Button>
    </div>
  );

  return (
    <Card
      title={`${fund.code} – ${fund.name}`}
      actions={
        can('RECEIPT_PAYMENT_MAINTAIN') && (
          <div className="row">
            <Button
              size="sm"
              variant="secondary"
              busy={claim.isPending}
              onClick={() => claim.mutate()}
            >
              Claim reimbursement
            </Button>
            <Button
              size="sm"
              variant="accent"
              disabled={fund.establishedOn === undefined}
              onClick={() => setForm({ ...EMPTY })}
            >
              New voucher
            </Button>
          </div>
        )
      }
    >
      <div className="stack">
        <ErrorAlert error={decide.error ?? claim.error ?? decideClaim.error} />
        <Tabs tabs={TABS} active={tab} onChange={setTab} />
        {tab === 'vouchers' ? (
          <DataTable<Disbursement>
            loading={vouchers.isLoading}
            rows={vouchers.data ?? []}
            rowKey={(d) => d.id}
            columns={[
              { key: 'n', header: 'Voucher', render: (d) => d.documentNo },
              { key: 'd', header: 'Date', render: (d) => formatDate(d.date) },
              { key: 'p', header: 'Payee', render: (d) => d.payee },
              {
                key: 'a',
                header: 'Account',
                render: (d) => `${d.expenseAccountCode} ${d.costCenter ?? ''}`,
              },
              { key: 'x', header: 'Description', render: (d) => d.description },
              {
                key: 'm',
                header: 'Amount',
                numeric: true,
                render: (d) => <Amount value={d.amount} />,
              },
              {
                key: 'r',
                header: 'Claimed',
                render: (d) => (d.reimbursementId === undefined ? '' : 'Yes'),
              },
              { key: 's', header: 'Status', render: (d) => <StatusBadge status={d.status} /> },
              {
                key: 'k',
                header: 'Actions',
                render: (d) =>
                  checker &&
                  d.status === 'PENDING_APPROVAL' &&
                  decisionButtons(
                    () => decide.mutate({ d, approve: true }),
                    () => decide.mutate({ d, approve: false }),
                  ),
              },
            ]}
          />
        ) : (
          <DataTable<Reimbursement>
            loading={claims.isLoading}
            rows={claims.data ?? []}
            rowKey={(c) => c.id}
            columns={[
              { key: 'n', header: 'Claim', render: (c) => c.documentNo },
              { key: 'd', header: 'Date', render: (c) => formatDate(c.claimDate) },
              {
                key: 'm',
                header: 'Amount',
                numeric: true,
                render: (c) => <Amount value={c.amount} />,
              },
              { key: 'j', header: 'Journal', render: (c) => c.journalBatchNo ?? '' },
              { key: 's', header: 'Status', render: (c) => <StatusBadge status={c.status} /> },
              {
                key: 'k',
                header: 'Actions',
                render: (c) =>
                  checker &&
                  c.status === 'PENDING_APPROVAL' &&
                  decisionButtons(
                    () => decideClaim.mutate({ c, approve: true }),
                    () => decideClaim.mutate({ c, approve: false }),
                  ),
              },
            ]}
          />
        )}
      </div>
      <Modal
        title={`New voucher – ${fund.code} (cash ${fund.cashBalance.toFixed(2)})`}
        open={form !== null}
        onClose={() => setForm(null)}
        footer={
          <Button
            variant="accent"
            busy={disburse.isPending}
            onClick={() => form && disburse.mutate(form)}
          >
            Save for approval
          </Button>
        }
      >
        <ErrorAlert error={disburse.error} />
        {form !== null && (
          <div className="form-grid">
            <Field label="Date" required>
              {(id) => (
                <input
                  id={id}
                  className="input"
                  type="date"
                  value={form.date}
                  onChange={(e) => setForm({ ...form, date: e.target.value })}
                />
              )}
            </Field>
            <Field label="Payee" required>
              {(id) => (
                <input
                  id={id}
                  className="input"
                  value={form.payee}
                  onChange={(e) => setForm({ ...form, payee: e.target.value })}
                />
              )}
            </Field>
            <Field label="Expense account" required>
              {(id) => (
                <select
                  id={id}
                  className="select"
                  value={form.expenseAccountCode}
                  onChange={(e) => setForm({ ...form, expenseAccountCode: e.target.value })}
                >
                  <option value="">Select…</option>
                  {postableAccounts
                    .filter((a) => a.accountClass === 'EXPENSE')
                    .map((a) => (
                      <option key={a.code} value={a.code}>
                        {a.code} – {a.name}
                      </option>
                    ))}
                </select>
              )}
            </Field>
            <Field label="Cost centre">
              {(id) => (
                <select
                  id={id}
                  className="select"
                  value={form.costCenter ?? ''}
                  onChange={(e) => setForm({ ...form, costCenter: e.target.value })}
                >
                  <option value="">—</option>
                  {costCenters.map((c) => (
                    <option key={c.code} value={c.code}>
                      {c.code} – {c.name}
                    </option>
                  ))}
                </select>
              )}
            </Field>
            <Field label="Description" required>
              {(id) => (
                <input
                  id={id}
                  className="input"
                  value={form.description}
                  onChange={(e) => setForm({ ...form, description: e.target.value })}
                />
              )}
            </Field>
            <Field label="Receipt / OR no.">
              {(id) => (
                <input
                  id={id}
                  className="input"
                  value={form.receiptRef ?? ''}
                  onChange={(e) => setForm({ ...form, receiptRef: e.target.value })}
                />
              )}
            </Field>
            <Field label="Amount" required>
              {(id) => (
                <input
                  id={id}
                  className="input"
                  type="number"
                  step="0.01"
                  value={form.amount === 0 ? '' : form.amount}
                  onChange={(e) => setForm({ ...form, amount: Number(e.target.value) })}
                />
              )}
            </Field>
          </div>
        )}
      </Modal>
    </Card>
  );
}
