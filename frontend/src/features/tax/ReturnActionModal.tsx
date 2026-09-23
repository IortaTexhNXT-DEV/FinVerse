import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { payablesApi } from '@/api/payables';
import { taxApi } from '@/api/tax';
import type { TaxReturn } from '@/api/tax';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { formatAmount, today } from '@/utils/format';

/** Action performed by the modal. */
export type ReturnAction = 'FILE' | 'PAY' | 'CANCEL';

interface Props {
  action: ReturnAction;
  taxReturn: TaxReturn;
  onClose: () => void;
}

const TITLES: Record<ReturnAction, string> = {
  FILE: 'File return',
  PAY: 'Pay return',
  CANCEL: 'Cancel draft',
};

/**
 * Files (date and eFPS / eBIRForms reference), pays (date, bank account, payment reference – the
 * remittance is posted) or cancels (reason) a return.
 */
export function ReturnActionModal({ action, taxReturn, onClose }: Readonly<Props>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [date, setDate] = useState(today());
  const [reference, setReference] = useState('');
  const [bank, setBank] = useState('');
  const banks = useQuery({
    queryKey: ['bank-accounts', taxReturn.companyId],
    queryFn: () => payablesApi.bankAccounts(taxReturn.companyId),
    enabled: action === 'PAY',
  });
  const submit = useMutation({
    mutationFn: () => {
      switch (action) {
        case 'FILE':
          return taxApi.fileReturn(taxReturn.id, date, reference);
        case 'PAY':
          return taxApi.payReturn(taxReturn.id, date, bank, reference);
        default:
          return taxApi.cancelReturn(taxReturn.id, reference);
      }
    },
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: ['tax-returns'] });
      await queryClient.invalidateQueries({ queryKey: ['tax-calendar'] });
      toast.success(`Return ${r.returnNo} is now ${r.status.toLowerCase()}`);
      onClose();
    },
  });
  const needsBank = action === 'PAY' && taxReturn.amountPayable > 0;
  const ready = reference.trim() !== '' && (!needsBank || bank !== '');

  return (
    <Modal
      title={`${TITLES[action]} ${taxReturn.returnNo}`}
      open
      onClose={onClose}
      footer={
        <Button
          variant={action === 'CANCEL' ? 'danger' : 'accent'}
          disabled={!ready}
          busy={submit.isPending}
          onClick={() => submit.mutate()}
        >
          {TITLES[action]}
        </Button>
      }
    >
      <ErrorAlert error={submit.error} />
      <p className="muted">
        {taxReturn.formCode} {taxReturn.periodLabel} – amount payable{' '}
        {formatAmount(taxReturn.amountPayable)}
      </p>
      <div className="form-grid">
        {action !== 'CANCEL' && (
          <Field label={action === 'FILE' ? 'Filing date' : 'Payment date'} required>
            {(id) => (
              <input
                id={id}
                className="input"
                type="date"
                value={date}
                onChange={(e) => setDate(e.target.value)}
              />
            )}
          </Field>
        )}
        {needsBank && (
          <Field label="Paid from bank account" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={bank}
                onChange={(e) => setBank(e.target.value)}
              >
                <option value="">Select…</option>
                {(banks.data ?? [])
                  .filter((b) => b.recordStatus === 'ACTIVE')
                  .map((b) => (
                    <option key={b.code} value={b.code}>
                      {b.code} – {b.name}
                    </option>
                  ))}
              </select>
            )}
          </Field>
        )}
        <Field label={action === 'CANCEL' ? 'Reason' : 'Reference'} required>
          {(id) => (
            <input
              id={id}
              className="input"
              maxLength={action === 'CANCEL' ? 200 : 60}
              value={reference}
              onChange={(e) => setReference(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
