import type {
  AllocationMethod,
  BankAccount,
  PartyOption,
  PayerType,
  ReceiptMode,
} from '@/api/receivables';
import { Field } from '@/components/ui/Field';
import { humanize } from '@/utils/format';
import type { ReceiptForm } from './receiptForm';
import { hasParty, isCheque } from './receiptForm';
import { bankOptions } from './useReceivablesLookups';

const PAYER_TYPES: PayerType[] = ['POLICYHOLDER', 'INTERMEDIARY', 'REINSURER', 'OTHER'];
const MODES: ReceiptMode[] = ['CHEQUE', 'BANK_TRANSFER', 'CASH', 'CARD', 'PDC'];
const METHODS: AllocationMethod[] = ['MANUAL', 'FIFO', 'NONE'];

interface FieldsProps {
  form: ReceiptForm;
  set: (patch: Partial<ReceiptForm>) => void;
  parties: PartyOption[];
  bankAccounts: BankAccount[];
  currency: string;
}

function PayerFields({
  form,
  set,
  parties,
}: Readonly<Pick<FieldsProps, 'form' | 'set' | 'parties'>>) {
  if (hasParty(form)) {
    return (
      <Field label="Payer" required>
        {(id) => (
          <select
            id={id}
            className="select"
            value={form.partyCode}
            onChange={(e) => set({ partyCode: e.target.value })}
          >
            <option value="">Select...</option>
            {parties.map((p) => (
              <option key={p.code} value={p.code}>{`${p.code} - ${p.name}`}</option>
            ))}
          </select>
        )}
      </Field>
    );
  }
  return (
    <>
      <Field label="Payer name" required>
        {(id) => (
          <input
            id={id}
            className="input"
            value={form.payerName}
            onChange={(e) => set({ payerName: e.target.value })}
          />
        )}
      </Field>
      <Field label="Income account" required hint="Other income is credited to this account">
        {(id) => (
          <input
            id={id}
            className="input"
            value={form.incomeAccountCode}
            onChange={(e) => set({ incomeAccountCode: e.target.value })}
          />
        )}
      </Field>
    </>
  );
}

function ChequeFields({ form, set }: Readonly<Pick<FieldsProps, 'form' | 'set'>>) {
  if (!isCheque(form)) {
    return null;
  }
  return (
    <>
      <Field label={form.mode === 'PDC' ? 'Cheque (due) date' : 'Cheque date'} required>
        {(id) => (
          <input
            id={id}
            type="date"
            className="input"
            value={form.instrumentDate}
            onChange={(e) => set({ instrumentDate: e.target.value })}
          />
        )}
      </Field>
      <Field label="Drawee bank" required>
        {(id) => (
          <input
            id={id}
            className="input"
            value={form.draweeBank}
            onChange={(e) => set({ draweeBank: e.target.value })}
          />
        )}
      </Field>
    </>
  );
}

/** Header fields of the receipt entry form (payer, instrument, bank account, amount). */
export function ReceiptFormFields({
  form,
  set,
  parties,
  bankAccounts,
  currency,
}: Readonly<FieldsProps>) {
  const party = hasParty(form);
  return (
    <div className="form-grid">
      <Field label="Receipt date" required>
        {(id) => (
          <input
            id={id}
            type="date"
            className="input"
            value={form.receiptDate}
            onChange={(e) => set({ receiptDate: e.target.value })}
          />
        )}
      </Field>
      <Field label="Payer type" required>
        {(id) => (
          <select
            id={id}
            className="select"
            value={form.payerType}
            onChange={(e) => set({ payerType: e.target.value as PayerType, partyCode: '' })}
          >
            {PAYER_TYPES.map((t) => (
              <option key={t} value={t}>
                {humanize(t)}
              </option>
            ))}
          </select>
        )}
      </Field>
      <PayerFields form={form} set={set} parties={parties} />
      <Field label="Mode" required>
        {(id) => (
          <select
            id={id}
            className="select"
            value={form.mode}
            onChange={(e) => set({ mode: e.target.value as ReceiptMode })}
          >
            {MODES.filter((m) => party || m !== 'PDC').map((m) => (
              <option key={m} value={m}>
                {humanize(m)}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Field label={isCheque(form) ? 'Cheque no.' : 'Reference'} required={isCheque(form)}>
        {(id) => (
          <input
            id={id}
            className="input"
            value={form.instrumentNo}
            onChange={(e) => set({ instrumentNo: e.target.value })}
          />
        )}
      </Field>
      <ChequeFields form={form} set={set} />
      <Field label="Deposit to bank account" required>
        {(id) => (
          <select
            id={id}
            className="select"
            value={form.bankAccountCode}
            onChange={(e) => set({ bankAccountCode: e.target.value })}
          >
            <option value="">Select...</option>
            {bankOptions(bankAccounts).map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Field label={`Amount (${currency})`} required>
        {(id) => (
          <input
            id={id}
            type="number"
            min={0}
            step="0.01"
            className="input num"
            value={form.amount || ''}
            onChange={(e) => set({ amount: Number(e.target.value) })}
          />
        )}
      </Field>
      {party && form.mode !== 'PDC' && (
        <Field label="Allocation">
          {(id) => (
            <select
              id={id}
              className="select"
              value={form.method}
              onChange={(e) => set({ method: e.target.value as AllocationMethod })}
            >
              {METHODS.map((m) => (
                <option key={m} value={m}>
                  {m === 'NONE' ? 'Keep on account' : humanize(m)}
                </option>
              ))}
            </select>
          )}
        </Field>
      )}
      <Field label="Narration">
        {(id) => (
          <input
            id={id}
            className="input"
            value={form.narration}
            onChange={(e) => set({ narration: e.target.value })}
          />
        )}
      </Field>
    </div>
  );
}
