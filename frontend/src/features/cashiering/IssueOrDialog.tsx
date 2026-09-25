import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { today } from '@/utils/format';
import { CodeSelect, TextField } from './CashFields';
import { cashieringApi } from './cashieringApi';
import type { PaymentMode } from './cashieringApi';
import { positive } from './cashieringLogic';

const MODES: readonly PaymentMode[] = [
  'CASH',
  'CHECK',
  'DIRECT_CREDIT',
  'CREDIT_TO_ACCOUNT',
  'NON_CASH',
];

interface OrForm {
  orType: string;
  payorName: string;
  payorCode: string;
  currency: string;
  mode: PaymentMode;
  checkNo: string;
  gross: string;
  vat: string;
  wtax: string;
  description: string;
  certificateRef: string;
}

const EMPTY: OrForm = {
  orType: '',
  payorName: '',
  payorCode: '',
  currency: 'PHP',
  mode: 'CHECK',
  checkNo: '',
  gross: '',
  vat: '0',
  wtax: '0',
  description: '',
  certificateRef: '',
};

function orErrors(f: OrForm): Record<string, string> {
  const e: Record<string, string> = {};
  if (f.orType === '') {
    e.orType = 'Select the OR type';
  }
  if (f.payorName.trim() === '') {
    e.payorName = 'Payor name is required';
  }
  if (!positive(f.gross)) {
    e.gross = 'Enter the gross amount';
  }
  if (Number(f.vat) < 0 || Number(f.wtax) < 0) {
    e.vat = 'VAT and withholding tax cannot be negative';
  }
  return e;
}

/**
 * Issue a Head Office official receipt (CSHID.002/011): service fee, profit share, commission,
 * incentive or other income, with its VAT and creditable withholding tax.
 */
export function IssueOrDialog({
  companyId,
  onClose,
}: Readonly<{ companyId: number; onClose: () => void }>) {
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<OrForm>(EMPTY);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const issue = useMutation({
    mutationFn: () =>
      cashieringApi.issueOr({
        companyId,
        orType: form.orType,
        receiptDate: today(),
        payorName: form.payorName.trim(),
        payorCode: form.payorCode || undefined,
        currency: form.currency,
        mode: form.mode,
        checkNo: form.checkNo || undefined,
        certificateRef: form.certificateRef || undefined,
        lines: [
          {
            gross: Number(form.gross),
            vat: Number(form.vat || 0),
            wtax: Number(form.wtax || 0),
            description: form.description || undefined,
          },
        ],
      }),
    onSuccess: async (r) => {
      toast.success(`${r.summary.receiptNo} issued`);
      await queryClient.invalidateQueries({ queryKey: ['cashiering'] });
      void navigate(`/cashiering/receipts/${r.summary.id}`);
    },
  });
  const set = (key: keyof OrForm) => (v: string) => setForm((f) => ({ ...f, [key]: v }));
  const submit = () => {
    const found = orErrors(form);
    setErrors(found);
    if (Object.keys(found).length === 0) {
      issue.mutate();
    }
  };
  return (
    <Modal
      open
      title="Issue Official Receipt"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={issue.isPending} onClick={submit}>
            Issue OR
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={issue.error} />
        <div className="form-grid">
          <Field label="OR Type" required error={errors.orType}>
            {(id) => (
              <LovSelect id={id} type="OR_TYPE" value={form.orType} onChange={set('orType')} />
            )}
          </Field>
          <TextField
            label="Payor Name"
            required
            value={form.payorName}
            onChange={set('payorName')}
            error={errors.payorName}
            maxLength={250}
          />
          <TextField
            label="Payor Code"
            value={form.payorCode}
            onChange={set('payorCode')}
            maxLength={30}
          />
          <TextField
            label="Currency"
            required
            value={form.currency}
            onChange={(v) => set('currency')(v.toUpperCase())}
            maxLength={3}
          />
          <CodeSelect
            label="Mode of Payment"
            value={form.mode}
            options={MODES}
            onChange={(v) => set('mode')(v)}
          />
          <TextField
            label="Check No."
            value={form.checkNo}
            onChange={set('checkNo')}
            maxLength={40}
          />
          <TextField
            label="Gross Amount"
            type="number"
            required
            value={form.gross}
            onChange={set('gross')}
            error={errors.gross}
          />
          <TextField
            label="VAT"
            type="number"
            value={form.vat}
            onChange={set('vat')}
            error={errors.vat}
          />
          <TextField
            label="Withholding Tax"
            type="number"
            value={form.wtax}
            onChange={set('wtax')}
          />
          <TextField
            label="2307 Certificate Ref."
            value={form.certificateRef}
            onChange={set('certificateRef')}
            maxLength={60}
          />
          <TextField
            label="Description"
            value={form.description}
            onChange={set('description')}
            maxLength={250}
          />
        </div>
      </div>
    </Modal>
  );
}
