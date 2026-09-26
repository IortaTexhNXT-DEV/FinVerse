import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { catalogApi } from '@/api/catalog';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { formatAmount, today } from '@/utils/format';
import { DialogFooter, InputField, LovField } from '../record/FormParts';
import { amountError, toAmount } from '../record/recordLogic';
import type { InsurerLine, NewInsurerLine } from './api';
import { isReuse } from './insurerLogic';

interface DialogProps {
  busy: boolean;
  error: unknown;
  onClose: () => void;
}

const FUTURE = 'The date cannot be in the future';

/** Add Insurer (BRCLM.043): an insurer of the incident with its share and number. */
export function AddInsurerDialog({
  companyId,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<
  DialogProps & { companyId: number; onSave: (line: NewInsurerLine, confirmReuse: boolean) => void }
>) {
  const insurers = useQuery({
    queryKey: ['catalog', 'insurers', companyId],
    queryFn: () => catalogApi.insurers(companyId),
  });
  const [insurer, setInsurer] = useState('');
  const [share, setShare] = useState('');
  const [number, setNumber] = useState('');
  const [reportedOn, setReportedOn] = useState('');
  const [errors, setErrors] = useState<{ insurer?: string; share?: string; date?: string }>({});
  const save = () => {
    const found = {
      insurer: insurer === '' ? 'Select the insurer' : undefined,
      share: amountError(share),
      date: reportedOn !== '' && reportedOn > today() ? FUTURE : undefined,
    };
    setErrors(found);
    if (Object.values(found).every((e) => e === undefined)) {
      onSave(
        {
          insurerCode: insurer,
          sharePct: toAmount(share),
          insurerClaimNo: number.trim() || undefined,
          reportedToInsurerOn: reportedOn || undefined,
        },
        isReuse(error),
      );
    }
  };
  return (
    <Modal
      title="Add Insurer"
      open
      onClose={onClose}
      footer={
        <DialogFooter
          label={isReuse(error) ? 'Confirm and Add' : 'Add Insurer'}
          busy={busy}
          onClose={onClose}
          onConfirm={save}
        />
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <Field label="Insurer" required error={errors.insurer}>
          {(id) => (
            <select
              id={id}
              className="select"
              value={insurer}
              onChange={(e) => setInsurer(e.target.value)}
            >
              <option value="">Select…</option>
              {(insurers.data ?? []).map((i) => (
                <option key={i.partyCode} value={i.partyCode}>
                  {i.name} ({i.partyCode})
                </option>
              ))}
            </select>
          )}
        </Field>
        <InputField
          label="Share %"
          type="number"
          value={share}
          error={errors.share}
          onChange={setShare}
        />
        <InputField label="Insurer Claim No." value={number} onChange={setNumber} />
        <InputField
          label="Reported to Insurer On"
          type="date"
          value={reportedOn}
          error={errors.date}
          onChange={setReportedOn}
        />
      </div>
    </Modal>
  );
}

/** Add Insurer Claim Number on a line (BRCLM.043, FR-CL-021). */
export function NumberDialog({
  line,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<
  DialogProps & {
    line: InsurerLine;
    onSave: (v: {
      insurerClaimNo: string;
      reportedToInsurerOn?: string;
      confirmReuse: boolean;
    }) => void;
  }
>) {
  const [number, setNumber] = useState('');
  const [reportedOn, setReportedOn] = useState(line.reportedToInsurerOn ?? today());
  const [errors, setErrors] = useState<{ number?: string; date?: string }>({});
  const save = () => {
    const found = {
      number: number.trim() === '' ? 'Enter the insurer claim number' : undefined,
      date: reportedOn > today() ? FUTURE : undefined,
    };
    setErrors(found);
    if (found.number === undefined && found.date === undefined) {
      onSave({
        insurerClaimNo: number.trim(),
        reportedToInsurerOn: reportedOn || undefined,
        confirmReuse: isReuse(error),
      });
    }
  };
  return (
    <Modal
      title={`Add Insurer Claim Number - ${line.insurerName ?? line.insurerCode}`}
      open
      onClose={onClose}
      footer={
        <DialogFooter
          label={isReuse(error) ? 'Confirm and Save' : 'Save Number'}
          busy={busy}
          onClose={onClose}
          onConfirm={save}
        />
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <InputField
          label="Insurer Claim No."
          required
          value={number}
          error={errors.number}
          onChange={setNumber}
        />
        <InputField
          label="Reported to Insurer On"
          type="date"
          value={reportedOn}
          error={errors.date}
          onChange={setReportedOn}
        />
      </div>
    </Modal>
  );
}

/** Assign Adjuster to an insurer line (BRCLM.018, FR-CL-031; BCL_ADJUSTER_ASSIGN). */
export function AdjusterDialog({
  line,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<DialogProps & { line: InsurerLine; onSave: (code: string) => void }>) {
  const [code, setCode] = useState(line.adjusterCode ?? '');
  const [missing, setMissing] = useState(false);
  return (
    <Modal
      title={`Assign Adjuster - ${line.insurerName ?? line.insurerCode}`}
      open
      onClose={onClose}
      footer={
        <DialogFooter
          label="Assign Adjuster"
          busy={busy}
          onClose={onClose}
          onConfirm={() => {
            setMissing(code === '');
            if (code !== '') {
              onSave(code);
            }
          }}
        />
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <LovField
          label="Adjuster / Appraiser"
          type="BCL_ADJUSTER"
          required
          value={code}
          error={missing ? 'Select the adjuster' : undefined}
          onChange={setCode}
        />
      </div>
    </Modal>
  );
}

/** Amend Reserve of an insurer line (BRCLM.023/024, FR-CL-032; BCL_RESERVE_AMEND). */
export function ReserveDialog({
  line,
  currency,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<
  DialogProps & {
    line: InsurerLine;
    currency: string;
    onSave: (amount: number, reason: string) => void;
  }
>) {
  const [amount, setAmount] = useState('');
  const [reason, setReason] = useState('');
  const [errors, setErrors] = useState<{ amount?: string; reason?: string }>({});
  const save = () => {
    const value = toAmount(amount);
    const found = {
      amount:
        value === undefined || Number.isNaN(value) || value < 0
          ? 'The reserve cannot be negative'
          : undefined,
      reason: reason.trim() === '' ? 'Enter the reason for the change' : undefined,
    };
    setErrors(found);
    if (found.amount === undefined && found.reason === undefined && value !== undefined) {
      onSave(value, reason.trim());
    }
  };
  return (
    <Modal
      title={`Amend Reserve - ${line.insurerName ?? line.insurerCode}`}
      open
      onClose={onClose}
      footer={<DialogFooter label="Amend Reserve" busy={busy} onClose={onClose} onConfirm={save} />}
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <p className="muted">
          Current reserve: {currency} {formatAmount(line.reserveAmount)}. The insurer reserve is
          information only; no journal is posted.
        </p>
        <InputField
          label={`New Reserve (${currency})`}
          type="number"
          required
          value={amount}
          error={errors.amount}
          onChange={setAmount}
        />
        <InputField
          label="Reason"
          required
          maxLength={500}
          value={reason}
          error={errors.reason}
          onChange={setReason}
        />
      </div>
    </Modal>
  );
}
