import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { productCatalogApi } from '@/api/productCatalog';
import type { Quotation } from '@/api/quotations';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { DateInput } from '@/components/ui/DateInput';

interface ExceptionForm {
  rate: string;
  reason: string;
  validUntil: string;
}

function formErrors(f: ExceptionForm): Record<string, string> {
  const errors: Record<string, string> = {};
  const rate = Number(f.rate);
  if (f.rate.trim() === '' || Number.isNaN(rate) || rate < 0 || rate > 100) {
    errors.rate = 'Enter the item rate between 0 and 100';
  }
  if (f.reason.trim() === '') {
    errors.reason = 'Explain why the scheme rate cannot be used';
  }
  return errors;
}

/**
 * "Request Rate Exception" (BRPM.007): asks to price this quotation on an item rate other than the
 * current package scheme rate. The request goes to My Approvals (PRODUCT_AUTHORIZE); once
 * approved, the quotation is priced and submitted with it.
 */
export function RateExceptionDialog({
  quotation,
  onClose,
}: Readonly<{ quotation: Quotation; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const firstRate = quotation.content.items.find((i) => i.ratePercent !== undefined)?.ratePercent;
  const [form, setForm] = useState<ExceptionForm>({
    rate: firstRate === undefined ? '' : String(firstRate),
    reason: '',
    validUntil: quotation.content.validUntil,
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const request = useMutation({
    mutationFn: () =>
      productCatalogApi.requestRateException({
        productCode: quotation.productCode,
        requestedRate: Number(form.rate),
        transactionRef: quotation.quotationNo,
        reason: form.reason.trim(),
        validUntil: form.validUntil || undefined,
      }),
    onSuccess: async (e) => {
      await queryClient.invalidateQueries({ queryKey: ['rate-exceptions', quotation.quotationNo] });
      toast.success(`Rate exception ${e.referenceNo} requested – pending approval`);
      onClose();
    },
  });
  const submit = () => {
    const found = formErrors(form);
    setErrors(found);
    if (Object.keys(found).length === 0) {
      request.mutate();
    }
  };
  return (
    <Modal
      open
      title="Request Rate Exception"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="accent" busy={request.isPending} onClick={submit}>
            Request Exception
          </Button>
        </>
      }
    >
      <p className="muted">
        New business is priced on the current rate scheme of {quotation.productCode}. An approver
        must accept a different item rate before the quotation can be submitted.
      </p>
      <ErrorAlert error={request.error} />
      <div className="form-grid">
        <Field label="Item rate %" required error={errors.rate}>
          {(id) => (
            <input
              id={id}
              className="input"
              type="number"
              step="any"
              value={form.rate}
              onChange={(e) => setForm({ ...form, rate: e.target.value })}
            />
          )}
        </Field>
        <Field label="Valid until">
          {(id) => (
            <DateInput
              id={id}
              value={form.validUntil}
              onChange={(e) => setForm({ ...form, validUntil: e.target.value })}
            />
          )}
        </Field>
      </div>
      <Field label="Reason" required error={errors.reason}>
        {(id) => (
          <textarea
            id={id}
            className="input"
            rows={3}
            value={form.reason}
            onChange={(e) => setForm({ ...form, reason: e.target.value })}
          />
        )}
      </Field>
    </Modal>
  );
}
