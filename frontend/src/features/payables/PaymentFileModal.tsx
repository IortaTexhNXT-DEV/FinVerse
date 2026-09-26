import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { api, saveFile } from '@/api/client';
import { payablesApi } from '@/api/payables';
import type { BankAccount } from '@/api/payables';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { today } from '@/utils/format';

interface PaymentFileModalProps {
  open: boolean;
  banks: BankAccount[];
  onClose: () => void;
}

/**
 * Generates the payment notification file to the bank (FIN-BRS-PAYNOTIFY): approved transfers
 * (optionally cheques and PDCs) of a bank account in a date range, in the bank's layout.
 */
export function PaymentFileModal({ open, banks, onClose }: Readonly<PaymentFileModalProps>) {
  const toast = useToast();
  const [bankId, setBankId] = useState<number | undefined>(undefined);
  const [from, setFrom] = useState(today());
  const [to, setTo] = useState(today());
  const [cheques, setCheques] = useState(false);
  const selected = bankId ?? banks.find((b) => b.recordStatus === 'ACTIVE')?.id;
  const generate = useMutation({
    mutationFn: () =>
      api.download(payablesApi.notificationFileUrl(selected ?? 0, from, to, cheques), {}),
    onSuccess: ({ blob, fileName }) => {
      saveFile(blob, fileName);
      toast.success(`${fileName} generated`);
      onClose();
    },
  });
  return (
    <Modal
      title="Payment Notification to the Bank"
      open={open}
      onClose={onClose}
      footer={
        <Button
          variant="accent"
          disabled={selected === undefined}
          busy={generate.isPending}
          onClick={() => generate.mutate()}
        >
          Generate File
        </Button>
      }
    >
      <ErrorAlert error={generate.error} />
      <div className="form-grid">
        <Field label="Bank account" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={selected ?? ''}
              onChange={(e) => setBankId(Number(e.target.value))}
            >
              {banks
                .filter((b) => b.recordStatus === 'ACTIVE')
                .map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.code} – {b.name}
                  </option>
                ))}
            </select>
          )}
        </Field>
        <Field label="From" required>
          {(id) => (
            <input
              id={id}
              className="input"
              type="date"
              value={from}
              onChange={(e) => setFrom(e.target.value)}
            />
          )}
        </Field>
        <Field label="To" required>
          {(id) => (
            <input
              id={id}
              className="input"
              type="date"
              value={to}
              onChange={(e) => setTo(e.target.value)}
            />
          )}
        </Field>
        <Field label="Include cheques and PDCs (positive pay)">
          {(id) => (
            <input
              id={id}
              type="checkbox"
              checked={cheques}
              onChange={(e) => setCheques(e.target.checked)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
