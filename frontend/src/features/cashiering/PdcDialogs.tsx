import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { CodeSelect, TextField } from './CashFields';
import type { PdcBody, PdcItem, PdcStatus } from './cashieringApi';
import { positive } from './cashieringLogic';

type PdcForm = Omit<PdcBody, 'companyId' | 'branchId' | 'amount'> & { amount: string };

const EMPTY: PdcForm = {
  payorName: '',
  reference: '',
  checkNo: '',
  bankCode: '',
  maturityDate: '',
  amount: '',
};

const REQUIRED: readonly [keyof PdcForm, string][] = [
  ['payorName', 'Payor name'],
  ['reference', 'Reference'],
  ['checkNo', 'Check number'],
  ['bankCode', 'Bank'],
  ['maturityDate', 'Maturity date'],
];

function pdcErrors(f: PdcForm): Record<string, string> {
  const e: Record<string, string> = {};
  REQUIRED.forEach(([k, label]) => {
    if ((f[k] ?? '').trim() === '') {
      e[k] = `${label} is required`;
    }
  });
  if (!positive(f.amount)) {
    e.amount = 'Enter an amount above zero';
  }
  return e;
}

/** Warehouses a post-dated check (CSHID.008 item 4). */
export function WarehouseDialog({
  busy,
  error,
  onSave,
  onClose,
}: Readonly<{
  busy: boolean;
  error: unknown;
  onSave: (body: Omit<PdcBody, 'companyId' | 'branchId'>) => void;
  onClose: () => void;
}>) {
  const [f, setF] = useState<PdcForm>(EMPTY);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const set = (k: keyof PdcForm) => (v: string) => setF((x) => ({ ...x, [k]: v }));
  const save = () => {
    const found = pdcErrors(f);
    setErrors(found);
    if (Object.keys(found).length === 0) {
      onSave({ ...f, amount: Number(f.amount) });
    }
  };
  return (
    <Modal
      open
      title="Warehouse Post-dated Check"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={busy} onClick={save}>
            Warehouse Check
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <div className="form-grid">
          <TextField
            label="Payor Name"
            required
            value={f.payorName}
            onChange={set('payorName')}
            error={errors.payorName}
            maxLength={250}
          />
          <TextField
            label="Client Code"
            value={f.clientCode ?? ''}
            onChange={set('clientCode')}
            maxLength={30}
          />
          <TextField
            label="ARN, Invoice, Policy or PN No."
            required
            value={f.reference}
            onChange={set('reference')}
            error={errors.reference}
            maxLength={80}
          />
          <TextField
            label="Check No."
            required
            value={f.checkNo}
            onChange={set('checkNo')}
            error={errors.checkNo}
            maxLength={40}
          />
          <TextField
            label="Bank"
            required
            value={f.bankCode}
            onChange={set('bankCode')}
            error={errors.bankCode}
            maxLength={30}
          />
          <TextField
            label="Bank Branch"
            value={f.checkBranch ?? ''}
            onChange={set('checkBranch')}
            maxLength={60}
          />
          <TextField
            label="Maturity Date"
            type="date"
            required
            value={f.maturityDate}
            onChange={set('maturityDate')}
            error={errors.maturityDate}
          />
          <TextField
            label="Amount"
            type="number"
            required
            value={f.amount}
            onChange={set('amount')}
            error={errors.amount}
          />
          <TextField
            label="Market Segment"
            value={f.segment ?? ''}
            onChange={set('segment')}
            maxLength={40}
          />
        </div>
      </div>
    </Modal>
  );
}

const OUTCOMES: readonly PdcStatus[] = ['RETURNED', 'REPLACED', 'PULLED_OUT'];

/** Takes a check out of the warehouse before maturity: returned, replaced or pulled out. */
export function ReleaseDialog({
  item,
  busy,
  error,
  onSave,
  onClose,
}: Readonly<{
  item: PdcItem;
  busy: boolean;
  error: unknown;
  onSave: (outcome: PdcStatus, reason: string) => void;
  onClose: () => void;
}>) {
  const [outcome, setOutcome] = useState<PdcStatus>('RETURNED');
  const [reason, setReason] = useState('');
  const [err, setErr] = useState<string>();
  return (
    <Modal
      open
      title={`Release ${item.warehouseNo}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={busy}
            onClick={() =>
              reason.trim() === '' ? setErr('Reason is required') : onSave(outcome, reason.trim())
            }
          >
            Release Check
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <CodeSelect
          label="Outcome"
          required
          value={outcome}
          options={OUTCOMES}
          onChange={(v) => setOutcome(v as PdcStatus)}
        />
        <TextField
          label="Reason"
          required
          value={reason}
          onChange={setReason}
          error={err}
          maxLength={250}
        />
      </div>
    </Modal>
  );
}
