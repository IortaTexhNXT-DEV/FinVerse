import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { bookingApi } from '@/api/booking';
import type { EndorsementType, PeriodBasis } from '@/api/booking';
import { useAuth } from '@/auth/authContext';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { useShownErrors } from '@/components/ui/useShownErrors';
import { useToast } from '@/components/ui/toastContext';
import { today } from '@/utils/format';
import { JournalLines, PremiumTables } from './BookingParts';
import { endorsementErrors, endorsementRequest, isValid, labelOf } from './bookingForm';
import type { EndorsementForm, FieldErrors } from './bookingForm';
import { DateInput } from '@/components/ui/DateInput';

const BASES: PeriodBasis[] = ['PRO_RATA', 'SHORT_PERIOD'];

function typesFor(canProcess: boolean, canAdjust: boolean): EndorsementType[] {
  const types: EndorsementType[] = [];
  if (canProcess) {
    types.push('POSITIVE');
  }
  if (canAdjust) {
    types.push('NEGATIVE');
  }
  types.push('NON_FINANCIAL');
  return types;
}

function AmountFields({
  form,
  errors,
  onChange,
}: Readonly<{
  form: EndorsementForm;
  errors: FieldErrors;
  onChange: (f: EndorsementForm, field: keyof EndorsementForm) => void;
}>) {
  return (
    <>
      <Field label="Premium basis" required hint="Remaining term of the policy year.">
        {(id) => (
          <select
            id={id}
            className="select"
            value={form.basis}
            onChange={(e) => onChange({ ...form, basis: e.target.value as PeriodBasis }, 'basis')}
          >
            {BASES.map((b) => (
              <option key={b} value={b}>
                {labelOf(b)}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Field
        label="Sum insured change"
        required
        error={errors.sumInsuredChange}
        hint="Negative for a reduction (return premium)."
      >
        {(id) => (
          <input
            id={id}
            className="input num"
            type="number"
            step="0.01"
            value={form.sumInsuredChange}
            onChange={(e) =>
              onChange({ ...form, sumInsuredChange: e.target.value }, 'sumInsuredChange')
            }
          />
        )}
      </Field>
      <Field label="Premium rate (%)" error={errors.ratePercent} hint="Blank: the product rate.">
        {(id) => (
          <input
            id={id}
            className="input num"
            type="number"
            step="0.0001"
            value={form.ratePercent}
            onChange={(e) => onChange({ ...form, ratePercent: e.target.value }, 'ratePercent')}
          />
        )}
      </Field>
    </>
  );
}

/**
 * New endorsement on a booked account (BRNB.061/076/081): positive (additional premium),
 * negative (return premium) or non-financial, rated for the remaining term with the catalog
 * calculator; the premium, commission and journal are previewed live before posting.
 */
export default function EndorsementEntryPage() {
  const [params] = useSearchParams();
  const arn = params.get('arn') ?? '';
  const { can } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const types = typesFor(can('BOOKING_PROCESS'), can('BOOKING_ADJUST'));
  const [form, setForm] = useState<EndorsementForm>({
    type: types[0] ?? 'NON_FINANCIAL',
    effectiveDate: '',
    basis: 'PRO_RATA',
    sumInsuredChange: '',
    ratePercent: '',
    description: '',
    bookingDate: today(),
  });
  const errors = endorsementErrors(form);
  const valid = isValid(errors);
  const { shown, touch, attempt } = useShownErrors(errors);
  const change = (next: EndorsementForm, field: keyof EndorsementForm) => {
    touch(field);
    setForm(next);
  };
  const request = endorsementRequest(arn, form);
  const preview = useQuery({
    queryKey: ['booking', 'endorsement-preview', request],
    queryFn: () => bookingApi.previewEndorsement(request),
    enabled: valid && form.type !== 'NON_FINANCIAL',
    retry: false,
  });
  const post = useMutation({
    mutationFn: async () => {
      const result = await bookingApi.postEndorsement(request);
      return result.invoiceNo === undefined ? undefined : bookingApi.invoiceByNo(result.invoiceNo);
    },
    onSuccess: async (invoice) => {
      await queryClient.invalidateQueries({ queryKey: ['booking'] });
      toast.success(
        invoice ? `Endorsement booked as ${invoice.invoiceNo ?? ''}` : 'Endorsement recorded',
      );
      void navigate(invoice ? `/booking/invoices/${String(invoice.id)}` : '/booking/endorsements');
    },
  });
  const draft = preview.data?.invoice;
  return (
    <div className="stack">
      <PageHeader
        section="Booking · Endorsement"
        backTo="/booking/endorsements"
        title="New Endorsement"
        description="Enter the change; the premium, commission and journal are computed as you type."
        actions={
          <>
            <ReferenceChip label="ARN" value={arn} />
            <Button
              busy={post.isPending}
              disabled={arn === ''}
              onClick={() => {
                if (attempt()) {
                  post.mutate();
                }
              }}
            >
              Post Endorsement
            </Button>
          </>
        }
      />
      <ErrorAlert error={post.error} />
      <Card title="Endorsement">
        <div className="form-grid">
          <Field label="Type" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={form.type}
                onChange={(e) => setForm({ ...form, type: e.target.value as EndorsementType })}
              >
                {types.map((t) => (
                  <option key={t} value={t}>
                    {labelOf(t)}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Effective date" required error={shown.effectiveDate}>
            {(id) => (
              <DateInput
                id={id}
                value={form.effectiveDate}
                onChange={(e) =>
                  change({ ...form, effectiveDate: e.target.value }, 'effectiveDate')
                }
              />
            )}
          </Field>
          <Field label="Booking date" hint="Blank: today. Its accounting period must be open.">
            {(id) => (
              <DateInput
                id={id}
                max={today()}
                value={form.bookingDate}
                onChange={(e) => setForm({ ...form, bookingDate: e.target.value })}
              />
            )}
          </Field>
          {form.type !== 'NON_FINANCIAL' && (
            <AmountFields form={form} errors={shown} onChange={change} />
          )}
        </div>
        <Field label="Description" required error={shown.description}>
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={2}
              maxLength={1000}
              value={form.description}
              onChange={(e) => change({ ...form, description: e.target.value }, 'description')}
            />
          )}
        </Field>
      </Card>
      <ErrorAlert error={preview.error} />
      {draft && (
        <>
          <Card title={`Invoice preview · policy year ${String(preview.data?.policyYear ?? '')}`}>
            <PremiumTables premium={draft.premium} commission={draft.commission} />
          </Card>
          <Card title="Journal preview">
            <JournalLines lines={preview.data?.journal ?? []} />
          </Card>
        </>
      )}
    </div>
  );
}
