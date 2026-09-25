import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Save } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { disbursementApi } from './api';
import { encodeErrors } from './labels';
import './disbursement.css';

interface Form {
  disbursementType: string;
  payeeCode: string;
  payeeName: string;
  currency: string;
  amount: string;
  purpose: string;
  rfpNo: string;
  rootInvoiceNo: string;
  expenseAccount: string;
  costCenter: string;
}

const EMPTY: Form = {
  disbursementType: '',
  payeeCode: '',
  payeeName: '',
  currency: 'PHP',
  amount: '',
  purpose: '',
  rfpNo: '',
  rootInvoiceNo: '',
  expenseAccount: '',
  costCenter: '',
};

const blank = (value: string) => (value.trim() === '' ? undefined : value.trim());

/**
 * Manual payment request (DIS 2.6.1, 2.7.4): a request encoded by the disbursement processor for
 * payments that do not come from another module (suppliers, government, employees). Its voucher
 * is created at once when the payee is maintained.
 */
export default function EncodeRequestPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<Form>(EMPTY);
  const [touched, setTouched] = useState(false);
  const errors = encodeErrors(form);
  const set = (patch: Partial<Form>) => setForm({ ...form, ...patch });
  const save = useMutation({
    mutationFn: () =>
      disbursementApi.encode({
        companyId,
        disbursementType: form.disbursementType,
        payeeCode: form.payeeCode.trim(),
        payeeName: blank(form.payeeName),
        currency: form.currency,
        amount: Number(form.amount),
        purpose: form.purpose.trim(),
        rfpNo: blank(form.rfpNo),
        rootInvoiceNo: blank(form.rootInvoiceNo),
        expenseAccount: blank(form.expenseAccount),
        costCenter: blank(form.costCenter),
      }),
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: ['disbursement'] });
      toast.success(`Request ${r.requestNo} encoded`);
      void navigate(
        r.voucherId === undefined ? '/disbursement' : `/disbursement/vouchers/${r.voucherId}`,
      );
    },
  });
  const submit = () => {
    setTouched(true);
    if (Object.keys(errors).length === 0) {
      save.mutate();
    }
  };
  const err = (key: keyof Form) => (touched ? errors[key] : undefined);
  const text = (key: keyof Form, label: string, required = false) => (
    <Field label={label} required={required} error={err(key)}>
      {(id) => (
        <input
          id={id}
          className="input"
          value={form[key]}
          onChange={(e) => set({ [key]: e.target.value })}
        />
      )}
    </Field>
  );
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Disbursement"
        backTo="/disbursement"
        title="Encode Payment Request"
        description="A payment that does not come from another module; the voucher is created at once when the payee is maintained."
        actions={
          <Button icon={<Save size={16} />} busy={save.isPending} onClick={submit}>
            Save Request
          </Button>
        }
      />
      <ErrorAlert error={save.error} />
      <Card title="Request">
        <div className="dsb-form">
          <Field label="Disbursement Type" required error={err('disbursementType')}>
            {(id) => (
              <LovSelect
                id={id}
                type="DISBURSEMENT_TYPE"
                value={form.disbursementType}
                onChange={(disbursementType) => set({ disbursementType })}
              />
            )}
          </Field>
          {text('payeeCode', 'Payee Code', true)}
          {text('payeeName', 'Payee Name')}
          <Field label="Currency" required error={err('currency')}>
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={3}
                value={form.currency}
                onChange={(e) => set({ currency: e.target.value.toUpperCase() })}
              />
            )}
          </Field>
          <Field label="Amount" required error={err('amount')}>
            {(id) => (
              <input
                id={id}
                type="number"
                step="0.01"
                min="0.01"
                className="input"
                value={form.amount}
                onChange={(e) => set({ amount: e.target.value })}
              />
            )}
          </Field>
          {text('rfpNo', 'RFP No.')}
          {text('rootInvoiceNo', 'Invoice No.')}
          {text('expenseAccount', 'Expense Account')}
          {text('costCenter', 'Cost Centre')}
          <div className="dsb-form-wide">
            <Field label="Purpose" required error={err('purpose')}>
              {(id) => (
                <textarea
                  id={id}
                  className="textarea"
                  rows={3}
                  maxLength={500}
                  value={form.purpose}
                  onChange={(e) => set({ purpose: e.target.value })}
                />
              )}
            </Field>
          </div>
        </div>
      </Card>
    </div>
  );
}
