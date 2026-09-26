import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Landmark } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { receivablesApi } from '@/api/receivables';
import type { DepositSlip, ReceiptSummary } from '@/api/receivables';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDate, humanize, today } from '@/utils/format';
import { round2 } from './receivablesMath';
import { bankOptions, useReceivablesLookups } from './useReceivablesLookups';

/**
 * Cheques and cash received but not yet banked, grouped on deposit (pay-in) slips per bank account;
 * a slip is confirmed when the bank has accepted the deposit.
 */
export default function DepositsPage() {
  const { can } = useAuth();
  const maker = can('RECEIPT_PAYMENT_MAINTAIN');
  const toast = useToast();
  const queryClient = useQueryClient();
  const { companyId, branchId, bankAccounts } = useReceivablesLookups();
  const [bank, setBank] = useState('');
  const [slipDate, setSlipDate] = useState(today());
  const [selected, setSelected] = useState<number[]>([]);

  const undeposited = useQuery({
    queryKey: ['undeposited', companyId, bank],
    queryFn: () => receivablesApi.undeposited(companyId, bank || undefined),
    enabled: companyId > 0,
  });
  const slips = useQuery({
    queryKey: ['slips', companyId],
    queryFn: () => receivablesApi.slips(companyId),
    enabled: companyId > 0,
  });
  const refresh = async (message: string) => {
    setSelected([]);
    await queryClient.invalidateQueries({ queryKey: ['undeposited'] });
    await queryClient.invalidateQueries({ queryKey: ['slips'] });
    toast.success(message);
  };
  const create = useMutation({
    mutationFn: () =>
      receivablesApi.createSlip({
        companyId,
        branchId,
        bankAccountCode: bank,
        slipDate,
        receiptIds: selected,
      }),
    onSuccess: (slip) => refresh(`Deposit slip ${slip.slipNo} prepared`),
  });
  const slipAction = useMutation({
    mutationFn: (run: () => Promise<DepositSlip>) => run(),
    onSuccess: (slip) => refresh(`${slip.slipNo}: ${humanize(slip.status)}`),
  });
  const rows = undeposited.data ?? [];
  const total = round2(
    rows.filter((r) => selected.includes(r.id)).reduce((acc, r) => acc + r.amount, 0),
  );
  const toggle = (id: number) =>
    setSelected((s) => (s.includes(id) ? s.filter((x) => x !== id) : [...s, id]));

  return (
    <div className="stack">
      <PageHeader
        section="Receivables & Banking"
        title="Cheques & Deposits"
        description="Cheques and cash received but not yet deposited, and bank deposit slips."
      />
      <ErrorAlert error={create.error ?? slipAction.error} />
      <Card
        title="Not yet deposited"
        actions={
          maker && (
            <Button
              variant="accent"
              icon={<Landmark size={16} />}
              disabled={bank === '' || selected.length === 0}
              busy={create.isPending}
              onClick={() => create.mutate()}
            >
              Prepare Slip ({total.toFixed(2)})
            </Button>
          )
        }
      >
        <div className="form-grid">
          <Field label="Bank account" hint="Select the bank to prepare a slip">
            {(id) => (
              <select
                id={id}
                className="select"
                value={bank}
                onChange={(e) => {
                  setBank(e.target.value);
                  setSelected([]);
                }}
              >
                <option value="">All bank accounts</option>
                {bankOptions(bankAccounts).map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Slip date">
            {(id) => (
              <input
                id={id}
                type="date"
                className="input"
                value={slipDate}
                onChange={(e) => setSlipDate(e.target.value)}
              />
            )}
          </Field>
        </div>
        <DataTable<ReceiptSummary>
          loading={undeposited.isLoading}
          rows={rows}
          rowKey={(r) => r.id}
          caption="Undeposited receipts"
          emptyMessage="Everything received has been deposited."
          columns={[
            {
              key: 'sel',
              header: 'Select',
              render: (r) => (
                <input
                  type="checkbox"
                  aria-label={`Select ${r.receiptNo}`}
                  disabled={bank === ''}
                  checked={selected.includes(r.id)}
                  onChange={() => toggle(r.id)}
                />
              ),
            },
            { key: 'no', header: 'Receipt', render: (r) => r.receiptNo },
            { key: 'date', header: 'Date', render: (r) => formatDate(r.receiptDate) },
            { key: 'mode', header: 'Mode', render: (r) => humanize(r.mode) },
            { key: 'chq', header: 'Cheque No.', render: (r) => r.instrumentNo ?? '' },
            { key: 'payer', header: 'Payer', render: (r) => r.payerName },
            { key: 'bank', header: 'Bank', render: (r) => r.bankAccountCode },
            {
              key: 'amt',
              header: 'Amount',
              numeric: true,
              render: (r) => <Amount value={r.amount} />,
            },
          ]}
        />
      </Card>
      <Card title="Deposit slips" flush>
        <DataTable<DepositSlip>
          loading={slips.isLoading}
          rows={slips.data ?? []}
          rowKey={(s) => s.id}
          caption="Deposit slips"
          columns={[
            { key: 'no', header: 'Slip No.', render: (s) => <strong>{s.slipNo}</strong> },
            { key: 'date', header: 'Slip Date', render: (s) => formatDate(s.slipDate) },
            { key: 'bank', header: 'Bank', render: (s) => s.bankAccountCode },
            { key: 'n', header: 'Receipts', numeric: true, render: (s) => s.receiptCount },
            {
              key: 'amt',
              header: 'Total',
              numeric: true,
              render: (s) => <Amount value={s.totalAmount} />,
            },
            { key: 'dep', header: 'Deposited', render: (s) => formatDate(s.depositedOn) },
            { key: 'st', header: 'Status', render: (s) => <StatusBadge status={s.status} /> },
            {
              key: 'act',
              header: 'Actions',
              render: (s) =>
                maker &&
                s.status === 'PREPARED' && (
                  <div className="row">
                    <Button
                      size="sm"
                      onClick={() =>
                        slipAction.mutate(() => receivablesApi.confirmSlip(s.id, today()))
                      }
                    >
                      Confirm Deposit
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => slipAction.mutate(() => receivablesApi.cancelSlip(s.id))}
                    >
                      Cancel
                    </Button>
                  </div>
                ),
            },
          ]}
        />
      </Card>
    </div>
  );
}
