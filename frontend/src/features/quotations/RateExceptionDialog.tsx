import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { BadgePercent } from 'lucide-react';
import { useState } from 'react';
import { productCatalogApi } from '@/api/productCatalog';
import type { Quotation } from '@/api/quotations';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDate } from '@/utils/format';

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
            <input
              id={id}
              className="input"
              type="date"
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

/**
 * The package rate scheme of a quotation (BRPM.007): the version that priced it, a warning when an
 * item rate differs from the scheme rate, the rate exceptions requested for it and the action to
 * request one.
 */
export function RateSchemePanel({ quotation }: Readonly<{ quotation: Quotation }>) {
  const { can } = useAuth();
  const [open, setOpen] = useState(false);
  const exceptions = useQuery({
    queryKey: ['rate-exceptions', quotation.quotationNo],
    queryFn: () => productCatalogApi.rateExceptions(quotation.quotationNo),
    enabled: quotation.content.schemeVersion !== undefined,
  });
  if (!quotation.content.schemeVersion) {
    return null;
  }
  const deviation = quotation.content.schemeDeviation === true;
  const mayRequest = deviation && quotation.status === 'DRAFT' && can('QUOTE_MAINTAIN');
  const requested = exceptions.data ?? [];
  if (!deviation && requested.length === 0) {
    return null;
  }
  return (
    <div className={deviation ? 'alert warning' : 'alert success'} role="status">
      <div className="row">
        <span>
          Priced on package version {quotation.content.schemeVersion}.
          {deviation &&
            ' An item rate differs from the scheme rate: submission needs an approved rate exception.'}
        </span>
        {mayRequest && (
          <Button
            size="sm"
            variant="secondary"
            icon={<BadgePercent size={14} />}
            onClick={() => setOpen(true)}
          >
            Request Rate Exception
          </Button>
        )}
      </div>
      {requested.map((e) => (
        <div className="row" key={e.referenceNo}>
          <strong>{e.referenceNo}</strong>
          <span>
            rate {e.requestedRate ?? '–'}% until {formatDate(e.validUntil)}
          </span>
          <StatusBadge status={e.recordStatus} />
        </div>
      ))}
      {open && <RateExceptionDialog quotation={quotation} onClose={() => setOpen(false)} />}
    </div>
  );
}
