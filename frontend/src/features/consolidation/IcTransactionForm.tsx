import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { intercompanyApi } from '@/api/consolidation';
import type { IcTransactionInput, IcTransactionType } from '@/api/consolidation';
import type { Company } from '@/api/types';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { useToast } from '@/components/ui/toastContext';
import { today } from '@/utils/format';

interface Props {
  companies: Company[];
  companyId: number;
}

type Form = Omit<IcTransactionInput, 'amount'> & { amount: string };

/**
 * Inter-company transaction entry. The creditor books due-from against its counter account; the
 * debtor books its counter account against due-to. Both journals post together or not at all.
 */
export function IcTransactionForm({ companies, companyId }: Readonly<Props>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const other = companies.find((c) => c.id !== companyId);
  const [form, setForm] = useState<Form>({
    type: 'CHARGE',
    creditorCompanyId: companyId,
    debtorCompanyId: other?.id ?? 0,
    valueDate: today(),
    currency: 'USD',
    amount: '',
    creditorAccount: '4700',
    debtorAccount: '5605',
    narration: '',
    costCenter: 'FIN',
  });
  const debtor = form.debtorCompanyId || (other?.id ?? 0);
  const post = useMutation({
    mutationFn: () =>
      intercompanyApi.post({ ...form, debtorCompanyId: debtor, amount: Number(form.amount) }),
    onSuccess: async (t) => {
      await queryClient.invalidateQueries({ queryKey: ['ic'] });
      toast.success(`${t.icReference} posted: ${t.creditorBatchNo} / ${t.debtorBatchNo}`);
      setForm({ ...form, amount: '', narration: '' });
    },
  });
  const set = (patch: Partial<Form>) => setForm({ ...form, ...patch });
  const companySelect = (label: string, value: number, onChange: (id: number) => void) => (
    <Field label={label} required>
      {(id) => (
        <select
          id={id}
          className="select"
          value={value}
          onChange={(e) => onChange(Number(e.target.value))}
        >
          {companies.map((c) => (
            <option key={c.id} value={c.id}>
              {c.code} – {c.name}
            </option>
          ))}
        </select>
      )}
    </Field>
  );
  const text = (label: string, key: keyof Form, required = true) => (
    <Field label={label} required={required}>
      {(id) => (
        <input
          id={id}
          className="input"
          value={String(form[key] ?? '')}
          onChange={(e) => set({ [key]: e.target.value })}
        />
      )}
    </Field>
  );

  return (
    <div className="stack">
      <ErrorAlert error={post.error} />
      <div className="form-grid">
        <Field label="Type" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={form.type}
              onChange={(e) => set({ type: e.target.value as IcTransactionType })}
            >
              <option value="CHARGE">Charge (creates due-from / due-to)</option>
              <option value="SETTLEMENT">Settlement (reduces balances)</option>
            </select>
          )}
        </Field>
        {companySelect('Creditor (due-from)', form.creditorCompanyId, (v) =>
          set({ creditorCompanyId: v }),
        )}
        {companySelect('Debtor (due-to)', debtor, (v) => set({ debtorCompanyId: v }))}
        <Field label="Value date" required>
          {(id) => (
            <input
              id={id}
              className="input"
              type="date"
              value={form.valueDate}
              onChange={(e) => set({ valueDate: e.target.value })}
            />
          )}
        </Field>
        {text('Currency', 'currency')}
        <Field label="Amount" required>
          {(id) => (
            <input
              id={id}
              className="input num"
              type="number"
              step="0.01"
              value={form.amount}
              onChange={(e) => set({ amount: e.target.value })}
            />
          )}
        </Field>
        {text('Creditor counter account', 'creditorAccount')}
        {text('Debtor counter account', 'debtorAccount')}
        {text('Cost centre', 'costCenter', false)}
        {text('Narration', 'narration')}
      </div>
      <div className="row">
        <Button
          variant="accent"
          busy={post.isPending}
          disabled={
            Number(form.amount) <= 0 ||
            form.narration.trim() === '' ||
            debtor === form.creditorCompanyId
          }
          onClick={() => post.mutate()}
        >
          Post Mirror Journals
        </Button>
      </div>
    </div>
  );
}
