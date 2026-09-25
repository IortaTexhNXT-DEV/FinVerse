import { useState } from 'react';
import { LovSelect } from '@/components/broking/LovSelect';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { today } from '@/utils/format';
import type { Answer, DpBilling, DpItem } from './commissionApi';
import { answersOf, answersProblem } from './commissionLogic';
import type { Decision } from './commissionLogic';

function splitAddresses(value: string): string[] {
  return value
    .split(/[\s,;]+/)
    .map((a) => a.trim())
    .filter((a) => a !== '');
}

/** Sends a billing to the insurer (CMRID.008). */
export function SendBillingDialog({
  billing,
  busy,
  error,
  onClose,
  onSend,
}: Readonly<{
  billing: DpBilling;
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onSend: (to: string[], cc: string[]) => void;
}>) {
  const [to, setTo] = useState('');
  const [cc, setCc] = useState('');
  return (
    <Modal
      title={`Send ${billing.billingNo} to ${billing.insurerCode}`}
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={busy} onClick={() => onSend(splitAddresses(to), splitAddresses(cc))}>
            Send Billing
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <Field label="To" hint="Leave blank to use the insurer's billing contacts">
          {(id) => (
            <input id={id} className="input" value={to} onChange={(e) => setTo(e.target.value)} />
          )}
        </Field>
        <Field label="Cc">
          {(id) => (
            <input id={id} className="input" value={cc} onChange={(e) => setCc(e.target.value)} />
          )}
        </Field>
        <p className="muted">
          The billing workbook is sent password protected; the answer is due within the feedback
          working days.
        </p>
      </div>
    </Modal>
  );
}

function DecisionRow({
  item,
  decision,
  onChange,
}: Readonly<{ item: DpItem; decision: Decision; onChange: (d: Decision) => void }>) {
  const value = decision.approved === undefined ? '' : String(decision.approved);
  return (
    <tr>
      <th scope="row">
        {item.invoiceNo}
        <div className="muted">{item.assuredName}</div>
      </th>
      <td>
        <Amount value={item.amounts.net} />
      </td>
      <td>
        <select
          className="select"
          aria-label={`Decision on ${item.invoiceNo}`}
          value={value}
          onChange={(e) =>
            onChange({
              ...decision,
              approved: e.target.value === '' ? undefined : e.target.value === 'true',
            })
          }
        >
          <option value="">No answer yet</option>
          <option value="true">Approved</option>
          <option value="false">Rejected</option>
        </select>
      </td>
      <td>
        {decision.approved === false && (
          <LovSelect
            id={`reason-${String(item.id)}`}
            type="DP_FEEDBACK_REASON"
            value={decision.reason}
            onChange={(reason) => onChange({ ...decision, reason })}
          />
        )}
      </td>
    </tr>
  );
}

/** Records the insurer's answers on the accounts of a billing (CMRID.009). */
export function AnswersDialog({
  items,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<{
  items: DpItem[];
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onSave: (answers: Answer[]) => void;
}>) {
  const open = items.filter((i) => i.tag === 'BILLED');
  const [decisions, setDecisions] = useState<Record<string, Decision>>(() =>
    Object.fromEntries(
      open.map((i) => [i.invoiceNo, { approved: undefined, reason: '', comment: '' }]),
    ),
  );
  const [problem, setProblem] = useState<string>();
  const save = () => {
    const answers = answersOf(decisions);
    const p = answersProblem(answers);
    if (p === undefined) {
      onSave(answers);
    } else {
      setProblem(p);
    }
  };
  const approveAll = () =>
    setDecisions((d) =>
      Object.fromEntries(Object.entries(d).map(([k, v]) => [k, { ...v, approved: true }])),
    );
  return (
    <Modal
      title="Record Insurer Answers"
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="ghost" onClick={approveAll}>
            Approve All
          </Button>
          <Button busy={busy} onClick={save}>
            Save Answers
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        {problem && <p className="field-error">{problem}</p>}
        <table className="table">
          <caption className="visually-hidden">Insurer answers</caption>
          <thead>
            <tr>
              <th scope="col">Invoice</th>
              <th scope="col">Net Commission</th>
              <th scope="col">Decision</th>
              <th scope="col">Reason</th>
            </tr>
          </thead>
          <tbody>
            {open.map((item) => (
              <DecisionRow
                key={item.id}
                item={item}
                decision={
                  decisions[item.invoiceNo] ?? { approved: undefined, reason: '', comment: '' }
                }
                onChange={(d) => {
                  setProblem(undefined);
                  setDecisions((all) => ({ ...all, [item.invoiceNo]: d }));
                }}
              />
            ))}
          </tbody>
        </table>
      </div>
    </Modal>
  );
}

/** Records the collection of an approved billing (CMRID.010/011, MKTID.012). */
export function CollectDialog({
  billing,
  bank,
  busy,
  error,
  onClose,
  onCollect,
}: Readonly<{
  billing: DpBilling;
  bank: string | undefined;
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onCollect: (input: { receiptDate: string; bankAccount: string; certificateRef: string }) => void;
}>) {
  const [receiptDate, setReceiptDate] = useState(today());
  const [bankAccount, setBankAccount] = useState(bank ?? '');
  const [certificateRef, setCertificateRef] = useState('');
  return (
    <Modal
      title={`Collect ${billing.billingNo}`}
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={busy}
            onClick={() => onCollect({ receiptDate, bankAccount, certificateRef })}
          >
            Record Collection
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <div className="form-grid">
          <Field label="Collection Date" required>
            {(id) => (
              <input
                id={id}
                type="date"
                className="input"
                value={receiptDate}
                onChange={(e) => setReceiptDate(e.target.value)}
              />
            )}
          </Field>
          <Field
            label="Bank Account"
            required
            hint="GL bank account the commission was received in"
          >
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={30}
                value={bankAccount}
                onChange={(e) => setBankAccount(e.target.value)}
              />
            )}
          </Field>
          <Field label="BIR Certificate No." hint="Certificate of the tax withheld, if received">
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={60}
                value={certificateRef}
                onChange={(e) => setCertificateRef(e.target.value)}
              />
            )}
          </Field>
        </div>
        <p className="muted">
          The commission is posted to the bank, the premium receivable of each approved account is
          reversed and the official receipt is requested from the receipting system.
        </p>
      </div>
    </Modal>
  );
}
