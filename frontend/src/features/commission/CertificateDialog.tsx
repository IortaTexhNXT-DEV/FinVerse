import { useMutation } from '@tanstack/react-query';
import { Plus, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { formatAmount } from '@/utils/format';
import { commissionApi } from './commissionApi';
import type { Certificate, CertificateInput, OrLink } from './commissionApi';
import { certificateProblem, receiptsTotal } from './commissionLogic';

function blank(companyId: number): CertificateInput {
  return {
    companyId,
    insurerCode: '',
    form: '2307',
    number: '',
    periodFrom: '',
    periodTo: '',
    taxWithheld: 0,
    receipts: [],
  };
}

function Receipts({
  receipts,
  onChange,
}: Readonly<{ receipts: OrLink[]; onChange: (r: OrLink[]) => void }>) {
  const set = (index: number, patch: Partial<OrLink>) =>
    onChange(receipts.map((r, i) => (i === index ? { ...r, ...patch } : r)));
  return (
    <table className="table">
      <caption className="visually-hidden">Official receipts covered</caption>
      <thead>
        <tr>
          <th scope="col">OR No.</th>
          <th scope="col">Commission Amount</th>
          <th scope="col">
            <span className="visually-hidden">Remove</span>
          </th>
        </tr>
      </thead>
      <tbody>
        {receipts.map((r, index) => (
          <tr key={index}>
            <td>
              <input
                className="input"
                aria-label={`OR number ${String(index + 1)}`}
                maxLength={40}
                value={r.orNo}
                onChange={(e) => set(index, { orNo: e.target.value })}
              />
            </td>
            <td>
              <input
                className="input"
                type="number"
                step="0.01"
                aria-label={`Amount of OR ${String(index + 1)}`}
                value={r.amount}
                onChange={(e) => set(index, { amount: Number(e.target.value) })}
              />
            </td>
            <td>
              <Button
                size="sm"
                variant="ghost"
                icon={<Trash2 size={14} />}
                aria-label={`Remove OR ${String(index + 1)}`}
                onClick={() => onChange(receipts.filter((_, i) => i !== index))}
              />
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

/**
 * Submits a BIR certificate of the tax withheld on commission, tagged to the official receipts
 * it covers (CMRID.015); a rejected submission is corrected and resubmitted here.
 */
export function CertificateDialog({
  companyId,
  certificate,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<{
  companyId: number;
  certificate: Certificate | undefined;
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onSave: (input: CertificateInput) => void;
}>) {
  const [form, setForm] = useState<CertificateInput>(
    certificate === undefined
      ? blank(companyId)
      : { ...certificate.certificate, companyId, insurerCode: certificate.insurerCode },
  );
  const [problem, setProblem] = useState<string>();
  const set = (patch: Partial<CertificateInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    setProblem(undefined);
  };
  const load = useMutation({
    mutationFn: () => commissionApi.receipts(companyId, form.insurerCode ?? ''),
    onSuccess: (found) => {
      const known = new Set(form.receipts.map((r) => r.orNo));
      set({ receipts: [...form.receipts, ...found.filter((r) => !known.has(r.orNo))] });
    },
  });
  const save = () => {
    const p = certificateProblem(form);
    if (p === undefined) {
      onSave(form);
    } else {
      setProblem(p);
    }
  };
  return (
    <Modal
      title={certificate ? `Resubmit ${certificate.submissionNo}` : 'Submit BIR Certificate'}
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={busy} onClick={save}>
            {certificate ? 'Resubmit' : 'Submit to Comptrollership'}
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error ?? load.error} />
        {problem && <p className="field-error">{problem}</p>}
        <div className="form-grid">
          <Field label="Insurer Code" required>
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={30}
                disabled={certificate !== undefined}
                value={form.insurerCode ?? ''}
                onChange={(e) => set({ insurerCode: e.target.value.toUpperCase() })}
              />
            )}
          </Field>
          <Field label="BIR Form" required>
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={20}
                value={form.form}
                onChange={(e) => set({ form: e.target.value })}
              />
            )}
          </Field>
          <Field label="Certificate No." required>
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={60}
                value={form.number}
                onChange={(e) => set({ number: e.target.value })}
              />
            )}
          </Field>
          <Field label="Tax Withheld" required>
            {(id) => (
              <input
                id={id}
                type="number"
                step="0.01"
                className="input"
                value={form.taxWithheld}
                onChange={(e) => set({ taxWithheld: Number(e.target.value) })}
              />
            )}
          </Field>
          <Field label="Period From" required>
            {(id) => (
              <input
                id={id}
                type="date"
                className="input"
                value={form.periodFrom}
                onChange={(e) => set({ periodFrom: e.target.value })}
              />
            )}
          </Field>
          <Field label="Period To" required>
            {(id) => (
              <input
                id={id}
                type="date"
                className="input"
                value={form.periodTo}
                onChange={(e) => set({ periodTo: e.target.value })}
              />
            )}
          </Field>
        </div>
        <Receipts receipts={form.receipts} onChange={(receipts) => set({ receipts })} />
        <div className="row">
          <Button
            size="sm"
            variant="secondary"
            icon={<Plus size={14} />}
            onClick={() => set({ receipts: [...form.receipts, { orNo: '', amount: 0 }] })}
          >
            Add OR
          </Button>
          <Button
            size="sm"
            variant="ghost"
            busy={load.isPending}
            disabled={(form.insurerCode ?? '') === ''}
            onClick={() => load.mutate()}
          >
            Add ORs of the Insurer
          </Button>
          <span className="muted">Total {formatAmount(receiptsTotal(form))}</span>
        </div>
        <p className="muted">Attach the scanned certificate on the submission once saved.</p>
      </div>
    </Modal>
  );
}
