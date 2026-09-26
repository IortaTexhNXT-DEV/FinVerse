import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId, useDefaultBranchId } from '@/context/workspaceContext';
import { today } from '@/utils/format';
import { CodeSelect, TextField } from './CashFields';
import { cashieringApi } from './cashieringApi';
import type { IntakeResult, PaymentMode } from './cashieringApi';
import { parseReferences, receiveErrors } from './cashieringLogic';
import { PaymentPreviewPane } from './PaymentPreviewPane';
import { useDebounced } from './useDebounced';
import './cashiering.css';

const OTC_MODES: readonly PaymentMode[] = [
  'CASH',
  'CHECK',
  'DIRECT_CREDIT',
  'ADA',
  'CREDIT_TO_ACCOUNT',
];

interface Form {
  references: string;
  payorCode: string;
  payorName: string;
  assuredName: string;
  currency: string;
  amount: string;
  paymentDate: string;
  mode: PaymentMode;
  checkNo: string;
  checkBank: string;
  arClass: string;
}

const EMPTY: Form = {
  references: '',
  payorCode: '',
  payorName: '',
  assuredName: '',
  currency: 'PHP',
  amount: '',
  paymentDate: today(),
  mode: 'CASH',
  checkNo: '',
  checkBank: '',
  arClass: 'PREMIUM',
};

function resultMessage(r: IntakeResult): string {
  if (r.prebookedId !== undefined) {
    return `${r.receiptNo} issued; the payment waits for the booking in the pre-booked queue`;
  }
  if (r.unappliedRef) {
    return `${r.receiptNo} issued; ${r.unappliedRef} left in unapplied collections`;
  }
  return `${r.receiptNo} issued and applied to ${r.applications.length} invoice(s)`;
}

/**
 * Receive Payment (CSHID.008/020/022): the over-the-counter payment of a walk-in payor. The
 * references are matched on ARN, invoice, policy or PN number, the preview shows the application
 * by component, and saving issues the acknowledgement receipt, applies the payment and leaves any
 * excess in unapplied collections.
 */
export default function ReceivePaymentPage() {
  const companyId = useCompanyId();
  const branchId = useDefaultBranchId();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<Form>(EMPTY);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const refs = parseReferences(form.references);
  const settledKey = useDebounced(JSON.stringify([refs, form.amount, form.currency]));
  const [settledRefs, settledAmount, settledCurrency] = JSON.parse(settledKey) as [
    string[],
    string,
    string,
  ];
  const settled = { refs: settledRefs, amount: settledAmount, currency: settledCurrency };
  const preview = useQuery({
    queryKey: ['cashiering', 'preview', companyId, settledKey],
    queryFn: () =>
      cashieringApi.preview(
        companyId,
        settled.refs,
        settled.amount === '' ? undefined : Number(settled.amount),
        settled.currency,
      ),
    enabled: companyId > 0 && settled.refs.length > 0,
  });
  const save = useMutation({
    mutationFn: () =>
      cashieringApi.receive({
        companyId,
        branchId,
        references: refs,
        payorCode: form.payorCode || undefined,
        payorName: form.payorName.trim(),
        assuredName: form.assuredName || undefined,
        currency: form.currency,
        amount: Number(form.amount),
        paymentDate: form.paymentDate,
        mode: form.mode,
        checkNo: form.checkNo || undefined,
        checkBank: form.checkBank || undefined,
        arClass: form.arClass,
      }),
    onSuccess: async (r) => {
      toast.success(resultMessage(r));
      await queryClient.invalidateQueries({ queryKey: ['cashiering'] });
      void navigate(`/cashiering/receipts/${r.receiptId}`);
    },
  });
  const set = (key: keyof Form) => (value: string) => {
    setForm((f) => ({ ...f, [key]: value }));
    setErrors((e) => ({ ...e, [key]: '' }));
  };
  const submit = () => {
    const found = receiveErrors({ ...form, references: refs });
    setErrors(found);
    if (Object.keys(found).length === 0) {
      save.mutate();
    }
  };
  const err = (key: string) => (errors[key] === '' ? undefined : errors[key]);
  return (
    <div className="stack">
      <PageHeader
        section="Cashiering"
        title="Receive Payment"
        description="Over-the-counter payment: match it to the booked invoices, check the application by component and issue the acknowledgement receipt."
        backTo="/cashiering"
      />
      <ErrorAlert error={save.error} />
      <div className="csh-two-pane">
        <Card title="Payor and Payment">
          <div className="stack">
            <Field
              label="ARN, Invoice, Policy or PN No."
              required
              error={err('references')}
              hint="Several references separated by commas or lines are applied oldest first."
            >
              {(id) => (
                <textarea
                  id={id}
                  className="input"
                  rows={3}
                  value={form.references}
                  aria-invalid={err('references') !== undefined}
                  onChange={(e) => set('references')(e.target.value)}
                />
              )}
            </Field>
            <div className="form-grid">
              <TextField
                label="Payor Name"
                required
                value={form.payorName}
                onChange={set('payorName')}
                error={err('payorName')}
                maxLength={250}
              />
              <TextField
                label="Payor / Client Code"
                value={form.payorCode}
                onChange={set('payorCode')}
                maxLength={30}
              />
              <TextField
                label="Assured Name"
                value={form.assuredName}
                onChange={set('assuredName')}
                maxLength={250}
              />
              <Field label="AR Class" required>
                {(id) => (
                  <LovSelect
                    id={id}
                    type="AR_CLASS"
                    value={form.arClass}
                    onChange={set('arClass')}
                  />
                )}
              </Field>
              <TextField
                label="Amount"
                type="number"
                required
                value={form.amount}
                onChange={set('amount')}
                error={err('amount')}
              />
              <TextField
                label="Currency"
                required
                value={form.currency}
                onChange={(v) => set('currency')(v.toUpperCase())}
                maxLength={3}
              />
              <TextField
                label="Payment Date"
                type="date"
                required
                value={form.paymentDate}
                onChange={set('paymentDate')}
              />
              <CodeSelect
                label="Mode of Payment"
                required
                value={form.mode}
                options={OTC_MODES}
                onChange={(v) => set('mode')(v)}
              />
              {form.mode === 'CHECK' && (
                <>
                  <TextField
                    label="Check No."
                    required
                    value={form.checkNo}
                    onChange={set('checkNo')}
                    error={err('checkNo')}
                    maxLength={40}
                  />
                  <TextField
                    label="Bank"
                    value={form.checkBank}
                    onChange={set('checkBank')}
                    maxLength={60}
                  />
                </>
              )}
            </div>
            <div className="row">
              <Button variant="accent" busy={save.isPending} onClick={submit}>
                Issue AR and Apply
              </Button>
              <Button variant="secondary" onClick={() => setForm(EMPTY)}>
                Clear
              </Button>
            </div>
          </div>
        </Card>
        <PaymentPreviewPane
          preview={refs.length > 0 ? preview.data : undefined}
          loading={preview.isFetching}
          error={preview.error}
        />
      </div>
    </div>
  );
}
