import { LovSelect } from '@/components/broking/LovSelect';
import { Field } from '@/components/ui/Field';
import type { PayoutAccount } from './api';
import type { FieldErrors, PayoutDraft } from './requestForm';

interface PayoutFieldsProps {
  value: PayoutDraft;
  errors: FieldErrors;
  onChange: (next: PayoutDraft) => void;
  /** CA / SA information the client already has (MKT 2.25.0), offered to prefill. */
  known?: PayoutAccount[];
}

/**
 * Mode of payment and CA / SA information of a request (Appendix D; MKT 2.25.0, Addendum 1):
 * credit to account needs the BDO account number and account name, a check the payee name.
 */
export function PayoutFields({ value, errors, onChange, known = [] }: Readonly<PayoutFieldsProps>) {
  const live = known.filter((k) => k.active);
  const needsAccount = value.paymentMode === 'CTA';
  return (
    <div className="form-grid">
      <Field label="Mode of Payment" required error={errors.paymentMode}>
        {(id) => (
          <LovSelect
            id={id}
            type="PRQ_PAYMENT_MODE"
            value={value.paymentMode}
            required
            onChange={(code) => onChange({ ...value, paymentMode: code })}
          />
        )}
      </Field>
      {live.length > 0 && (
        <Field label="Known CA / SA" hint="Account captured from an earlier refund of this client">
          {(id) => (
            <select
              id={id}
              className="select"
              value=""
              onChange={(e) => {
                const picked = live.find((k) => String(k.id) === e.target.value);
                if (picked) {
                  onChange({
                    paymentMode: picked.mode,
                    accountNo: picked.accountNo ?? '',
                    accountName: picked.payeeName,
                  });
                }
              }}
            >
              <option value="">Use a known account…</option>
              {live.map((k) => (
                <option key={k.id} value={k.id}>
                  {k.mode === 'CTA'
                    ? `${k.accountNo ?? ''} · ${k.payeeName}`
                    : `Check · ${k.payeeName}`}
                </option>
              ))}
            </select>
          )}
        </Field>
      )}
      {needsAccount && (
        <Field label="BDO Account No." required error={errors.accountNo} hint="10 to 16 digits">
          {(id) => (
            <input
              id={id}
              className="input"
              inputMode="numeric"
              maxLength={16}
              aria-invalid={errors.accountNo !== undefined}
              value={value.accountNo}
              onChange={(e) => onChange({ ...value, accountNo: e.target.value })}
            />
          )}
        </Field>
      )}
      <Field
        label={needsAccount ? 'Account Name' : 'Check / Payee Name'}
        required={needsAccount || value.paymentMode === 'CHECK'}
        error={errors.accountName}
      >
        {(id) => (
          <input
            id={id}
            className="input"
            maxLength={250}
            aria-invalid={errors.accountName !== undefined}
            value={value.accountName}
            onChange={(e) => onChange({ ...value, accountName: e.target.value })}
          />
        )}
      </Field>
    </div>
  );
}
