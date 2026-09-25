import { useMutation, useQuery } from '@tanstack/react-query';
import { Save } from 'lucide-react';
import { useState } from 'react';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, humanize } from '@/utils/format';
import { disbursementApi } from './api';
import type { Mode, TermsInput, Voucher } from './api';
import { termsErrors } from './forms';
import { MODE_LABELS, MODES } from './labels';
import './disbursement.css';

function termsOf(v: Voucher): TermsInput {
  return {
    mode: v.summary.mode ?? 'CHECK',
    bankAccountId: v.bankAccountId ?? 0,
    payeeAccountId: v.payeeAccountId,
    ewt: v.summary.ewt,
    purpose: v.purpose ?? '',
    valueDate: v.valueDate ?? '',
    costCenter: v.costCenter,
    expenseAccount: v.expenseAccount,
  };
}

/** Optional values separated by slashes, a dash for each missing one. */
function slashed(...values: (string | undefined)[]): string {
  return values.map((v) => v ?? '—').join(' / ');
}

function ReadOnly({ voucher }: Readonly<{ voucher: Voucher }>) {
  const s = voucher.summary;
  return (
    <dl className="detail-list">
      <dt>Mode of payment</dt>
      <dd>{s.mode ? MODE_LABELS[s.mode] : '—'}</dd>
      <dt>Paying account</dt>
      <dd>{voucher.bankAccount ?? '—'}</dd>
      <dt>Payee account</dt>
      <dd>{voucher.payeeAccount ?? '—'}</dd>
      <dt>Gross / withholding tax / net</dt>
      <dd>
        <Amount value={s.gross} /> / <Amount value={s.ewt} /> / <Amount value={s.net} />{' '}
        {s.currency}
      </dd>
      <dt>Purpose</dt>
      <dd>{voucher.purpose ?? '—'}</dd>
      <dt>Value date</dt>
      <dd>{formatDate(voucher.valueDate)}</dd>
      <dt>Cost centre / expense account</dt>
      <dd>{slashed(voucher.costCenter, voucher.expenseAccount)}</dd>
      <dt>Processor / checker / approver</dt>
      <dd>{slashed(voucher.submittedBy, voucher.reviewedBy, voucher.approvedBy)}</dd>
      <dt>Journal / reversal</dt>
      <dd>{slashed(voucher.journalNo, voucher.cancelJournalNo)}</dd>
      {voucher.cancelReason !== undefined && (
        <>
          <dt>Cancellation</dt>
          <dd>
            {voucher.cancelReason} ({voucher.cancelledBy})
          </dd>
        </>
      )}
      <dt>Request</dt>
      <dd>
        {voucher.request.requestNo} · {humanize(voucher.request.sourceModule)} ·{' '}
        {voucher.request.sourceRef}
      </dd>
    </dl>
  );
}

/**
 * The details of a voucher (DIS 2.7.1-2.7.4, 3.30.0): mode of payment, paying and payee accounts,
 * withholding tax, purpose, value date, cost centre and expense account; editable by the processor
 * while the voucher is in process, with field errors. Saving rebuilds the entry from the rule.
 */
export function VoucherDetailsTab({
  voucher,
  editable,
  onSaved,
}: Readonly<{ voucher: Voucher; editable: boolean; onSaved: (v: Voucher) => void }>) {
  const companyId = useCompanyId();
  const toast = useToast();
  const [terms, setTerms] = useState<TermsInput>(() => termsOf(voucher));
  const [touched, setTouched] = useState(false);
  const banks = useQuery({
    queryKey: ['disbursement', 'banks', companyId],
    queryFn: () => disbursementApi.banks(companyId),
    enabled: editable && companyId > 0,
  });
  const payee = useQuery({
    queryKey: ['disbursement', 'payee', voucher.request.payeeId],
    queryFn: () => disbursementApi.payee(voucher.request.payeeId ?? 0),
    enabled: editable && voucher.request.payeeId !== undefined,
  });
  const save = useMutation({
    mutationFn: () => disbursementApi.terms(voucher.summary.id, terms),
    onSuccess: (v) => {
      toast.success('Terms saved; the entry was rebuilt from the rule');
      onSaved(v);
    },
  });
  if (!editable) {
    return (
      <Card title="Details">
        <ReadOnly voucher={voucher} />
      </Card>
    );
  }
  const errors = termsErrors(terms, voucher.summary.gross, voucher.summary.disbursementType);
  const shown = (key: string) => (touched ? errors[key] : undefined);
  const set = (patch: Partial<TermsInput>) => setTerms({ ...terms, ...patch });
  const accounts = (payee.data?.accounts ?? []).filter((a) => a.active);
  const allowed: Mode[] = payee.data?.allowedModes ?? MODES;
  return (
    <Card
      title="Details"
      actions={
        <Button
          icon={<Save size={16} />}
          busy={save.isPending}
          onClick={() => {
            setTouched(true);
            if (Object.keys(errors).length === 0) {
              save.mutate();
            }
          }}
        >
          Save Terms
        </Button>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error ?? banks.error} />
        <div className="dsb-form">
          <Field label="Mode of Payment" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={terms.mode}
                onChange={(e) => set({ mode: e.target.value as Mode })}
              >
                {allowed.map((m) => (
                  <option key={m} value={m}>
                    {MODE_LABELS[m]}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Paying Account" required error={shown('bank')}>
            {(id) => (
              <select
                id={id}
                className="select"
                value={terms.bankAccountId}
                onChange={(e) => set({ bankAccountId: Number(e.target.value) })}
              >
                <option value={0}>Select…</option>
                {(banks.data ?? [])
                  .filter((b) => b.currency === voucher.summary.currency && b.status === 'ACTIVE')
                  .map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.code} - {b.bankName}
                    </option>
                  ))}
              </select>
            )}
          </Field>
          <Field label="Payee Account" error={shown('payeeAccount')}>
            {(id) => (
              <select
                id={id}
                className="select"
                value={terms.payeeAccountId ?? ''}
                onChange={(e) =>
                  set({
                    payeeAccountId: e.target.value === '' ? undefined : Number(e.target.value),
                  })
                }
              >
                <option value="">None</option>
                {accounts.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.bankName} {a.accountNo}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Withholding Tax" error={shown('ewt')}>
            {(id) => (
              <input
                id={id}
                type="number"
                step="0.01"
                min="0"
                className="input"
                value={terms.ewt}
                onChange={(e) => set({ ewt: Number(e.target.value) })}
              />
            )}
          </Field>
          <Field label="Value Date" required error={shown('valueDate')}>
            {(id) => (
              <input
                id={id}
                type="date"
                className="input"
                value={terms.valueDate}
                onChange={(e) => set({ valueDate: e.target.value })}
              />
            )}
          </Field>
          <Field label="Cost Centre">
            {(id) => (
              <input
                id={id}
                className="input"
                value={terms.costCenter ?? ''}
                onChange={(e) => set({ costCenter: e.target.value || undefined })}
              />
            )}
          </Field>
          <Field label="Expense Account" error={shown('expense')}>
            {(id) => (
              <input
                id={id}
                className="input"
                value={terms.expenseAccount ?? ''}
                onChange={(e) => set({ expenseAccount: e.target.value || undefined })}
              />
            )}
          </Field>
          <Field label="Purpose" required error={shown('purpose')}>
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={500}
                value={terms.purpose}
                onChange={(e) => set({ purpose: e.target.value })}
              />
            )}
          </Field>
        </div>
      </div>
    </Card>
  );
}
