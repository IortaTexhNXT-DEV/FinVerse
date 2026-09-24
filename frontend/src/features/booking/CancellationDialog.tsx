import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { bookingApi } from '@/api/booking';
import type { BookedInvoice, CancellationKind, PeriodBasis } from '@/api/booking';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { formatAmount, today } from '@/utils/format';
import { cancellationErrors, cancellationRequest, isValid, labelOf } from './bookingForm';
import type { CancellationForm } from './bookingForm';

const KINDS: CancellationKind[] = ['FLAT', 'FLAT_RETAIN_DST', 'PARTIAL'];
const BASES: PeriodBasis[] = ['PRO_RATA', 'SHORT_PERIOD'];

function KindFields({
  form,
  onChange,
}: Readonly<{ form: CancellationForm; onChange: (f: CancellationForm) => void }>) {
  return (
    <div className="form-grid">
      <Field label="Cancellation" required>
        {(id) => (
          <select
            id={id}
            className="select"
            value={form.kind}
            onChange={(e) => onChange({ ...form, kind: e.target.value as CancellationKind })}
          >
            {KINDS.map((k) => (
              <option key={k} value={k}>
                {labelOf(k)}
              </option>
            ))}
          </select>
        )}
      </Field>
      {form.kind === 'PARTIAL' && (
        <Field label="Return basis" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={form.basis}
              onChange={(e) => onChange({ ...form, basis: e.target.value as PeriodBasis })}
            >
              {BASES.map((b) => (
                <option key={b} value={b}>
                  {labelOf(b)}
                </option>
              ))}
            </select>
          )}
        </Field>
      )}
    </div>
  );
}

/**
 * Post-issuance cancellation of a booked account (BRNB.094): kind, date and reason, with the
 * return premium and commission computed live before confirming. The cancellation reverses the
 * booking entry and credits the service invoice.
 */
export function CancellationDialog({
  invoice,
  onClose,
}: Readonly<{ invoice: BookedInvoice; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<CancellationForm>({
    kind: 'FLAT',
    basis: 'PRO_RATA',
    effectiveDate: invoice.inceptionDate,
    reasonCode: '',
    description: '',
    bookingDate: today(),
  });
  const errors = cancellationErrors(form);
  const valid = isValid(errors);
  const request = cancellationRequest(invoice.arn, form);
  const preview = useQuery({
    queryKey: ['booking', 'cancel-preview', request],
    queryFn: () => bookingApi.previewEndorsement(request),
    enabled: form.effectiveDate !== '',
    retry: false,
  });
  const post = useMutation({
    mutationFn: () => bookingApi.postEndorsement(request),
    onSuccess: async (result) => {
      await queryClient.invalidateQueries({ queryKey: ['booking'] });
      toast.success(`Booking cancelled: ${result.endorsementNo} (${result.invoiceNo ?? ''})`);
      onClose();
    },
  });
  const returned = preview.data?.invoice;
  return (
    <Modal
      open
      title="Cancel Booking"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="danger"
            busy={post.isPending}
            disabled={!valid}
            onClick={() => post.mutate()}
          >
            Cancel Booking
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={post.error} />
        <KindFields form={form} onChange={setForm} />
        <div className="form-grid">
          <Field label="Cancellation date" required error={errors.effectiveDate}>
            {(id) => (
              <input
                id={id}
                className="input"
                type="date"
                value={form.effectiveDate}
                onChange={(e) => setForm({ ...form, effectiveDate: e.target.value })}
              />
            )}
          </Field>
          <Field label="Reason" required error={errors.reasonCode}>
            {(id) => (
              <LovSelect
                id={id}
                type="CANCELLATION_REASON"
                value={form.reasonCode}
                onChange={(reasonCode) => setForm({ ...form, reasonCode })}
                required
              />
            )}
          </Field>
        </div>
        <Field label="Description" required error={errors.description}>
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={2}
              maxLength={1000}
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            />
          )}
        </Field>
        <ErrorAlert error={preview.error} />
        {returned && (
          <div className="alert info" role="status">
            Return premium <strong>{formatAmount(returned.premium.total)}</strong>, commission{' '}
            <strong>{formatAmount(returned.commission.commission)}</strong> (policy year{' '}
            {preview.data?.policyYear}).
          </div>
        )}
      </div>
    </Modal>
  );
}
