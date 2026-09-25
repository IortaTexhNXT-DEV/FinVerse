import { useState } from 'react';
import { LovSelect } from '@/components/broking/LovSelect';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate, humanize } from '@/utils/format';
import type { DpItem } from './commissionApi';

/** A reason asked before an action (exclusion, cancellation). */
export function ReasonDialog({
  title,
  action,
  label,
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<{
  title: string;
  action: string;
  label: string;
  busy: boolean;
  error?: unknown;
  onClose: () => void;
  onConfirm: (reason: string) => void;
}>) {
  const [reason, setReason] = useState('');
  const [invalid, setInvalid] = useState<string>();
  return (
    <Modal
      title={title}
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={busy}
            onClick={() =>
              reason.trim() === '' ? setInvalid(`${label} is required`) : onConfirm(reason.trim())
            }
          >
            {action}
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <Field label={label} required error={invalid}>
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={3}
              maxLength={500}
              value={reason}
              onChange={(e) => {
                setReason(e.target.value);
                setInvalid(undefined);
              }}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

function Rules({ item }: Readonly<{ item: DpItem }>) {
  return (
    <table className="table">
      <caption className="visually-hidden">Validation rules</caption>
      <thead>
        <tr>
          <th scope="col">Rule</th>
          <th scope="col">Result</th>
          <th scope="col">Detail</th>
        </tr>
      </thead>
      <tbody>
        {item.rules.map((r) => (
          <tr key={r.rule}>
            <th scope="row">{humanize(r.rule)}</th>
            <td>
              <StatusBadge status={r.passed ? 'PASSED' : 'FAILED'} />
            </td>
            <td>{r.message ?? ''}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

type Mode = 'view' | 'reinstate';

/** What happened to an account, in words. */
function progressOf(item: DpItem): string {
  const f = item.feedback;
  const parts = [
    f.confirmedBy ? `Confirmed by ${f.confirmedBy}` : '',
    f.feedbackReason ? `Insurer: ${humanize(f.feedbackReason)} ${f.feedbackComment ?? ''}` : '',
    f.collectedOn ? `Collected ${formatDate(f.collectedOn)} (OR ${f.orNo ?? 'to follow'})` : '',
    f.returnedRef ? `Returned to collection as ${f.returnedRef}` : '',
    f.remarks ?? '',
  ];
  return parts.filter((p) => p !== '').join(' · ');
}

function Amounts({ item }: Readonly<{ item: DpItem }>) {
  const a = item.amounts;
  return (
    <dl className="grid-4">
      <div>
        <dt className="muted">Premium</dt>
        <dd>
          <Amount value={a.premium} />
        </dd>
      </div>
      <div>
        <dt className="muted">Commission</dt>
        <dd>
          <Amount value={a.commission} />
        </dd>
      </div>
      <div>
        <dt className="muted">VAT / Withholding Tax</dt>
        <dd>
          <Amount value={a.commissionVat} /> / <Amount value={a.wtax} />
        </dd>
      </div>
      <div>
        <dt className="muted">Net Commission</dt>
        <dd>
          <Amount value={a.net} />
        </dd>
      </div>
    </dl>
  );
}

function ViewFooter({
  item,
  editable,
  busy,
  onClose,
  onRevalidate,
  onReverse,
  onReinstate,
}: Readonly<{
  item: DpItem;
  editable: boolean;
  busy: boolean;
  onClose: () => void;
  onRevalidate: () => void;
  onReverse: () => void;
  onReinstate: () => void;
}>) {
  const tag = editable ? item.tag : undefined;
  return (
    <>
      <Button variant="secondary" onClick={onClose}>
        Close
      </Button>
      {(tag === 'DP_FOR_CONFIRMATION' || tag === 'EXCLUDED') && (
        <Button variant="secondary" busy={busy} onClick={onRevalidate}>
          Validate Again
        </Button>
      )}
      {tag === 'PR_REVERSED' && (
        <Button variant="secondary" onClick={onReinstate}>
          Reinstate PR
        </Button>
      )}
      {tag === 'COLLECTED' && (
        <Button busy={busy} onClick={onReverse}>
          Reverse PR Again
        </Button>
      )}
    </>
  );
}

function ReinstateFields({
  reason,
  comment,
  onReason,
  onComment,
}: Readonly<{
  reason: string;
  comment: string;
  onReason: (v: string) => void;
  onComment: (v: string) => void;
}>) {
  return (
    <>
      <Field label="Reinstatement Reason" required>
        {(id) => (
          <LovSelect
            id={id}
            type="REINSTATEMENT_REASON"
            parentCode="DIRECT_PAYMENT"
            value={reason}
            onChange={onReason}
          />
        )}
      </Field>
      <Field label="Comment">
        {(id) => (
          <textarea
            id={id}
            className="textarea"
            rows={2}
            maxLength={500}
            value={comment}
            onChange={(e) => onComment(e.target.value)}
          />
        )}
      </Field>
      <p className="muted">
        Due to Cancellation also returns the collected commission to the collection team as an
        unapplied amount.
      </p>
    </>
  );
}

/**
 * One direct payment account: the ledger amounts, the validation rules and what happened to it;
 * revalidation, and reversal or reinstatement of the premium receivable (CMRID.007-013).
 */
export function DpItemDialog({
  item,
  editable,
  busy,
  error,
  onClose,
  onRevalidate,
  onReverse,
  onReinstate,
}: Readonly<{
  item: DpItem;
  editable: boolean;
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onRevalidate: () => void;
  onReverse: () => void;
  onReinstate: (reasonCode: string, comment: string) => void;
}>) {
  const [mode, setMode] = useState<Mode>('view');
  const [reason, setReason] = useState('');
  const [comment, setComment] = useState('');
  const reinstating = mode === 'reinstate';
  const footer = reinstating ? (
    <>
      <Button variant="secondary" onClick={() => setMode('view')}>
        Cancel
      </Button>
      <Button busy={busy} disabled={reason === ''} onClick={() => onReinstate(reason, comment)}>
        Reinstate PR
      </Button>
    </>
  ) : (
    <ViewFooter
      item={item}
      editable={editable}
      busy={busy}
      onClose={onClose}
      onRevalidate={onRevalidate}
      onReverse={onReverse}
      onReinstate={() => setMode('reinstate')}
    />
  );
  return (
    <Modal title={`Account ${item.invoiceNo}`} open onClose={onClose} footer={footer}>
      <div className="stack">
        <ErrorAlert error={error} />
        <div className="row">
          <StatusBadge status={item.tag} />
          <StatusBadge status={item.sanitation} />
          <span className="muted">
            {[item.insurerCode, item.assuredName, item.policyNo].filter(Boolean).join(' · ')}
          </span>
        </div>
        <Amounts item={item} />
        {reinstating ? (
          <ReinstateFields
            reason={reason}
            comment={comment}
            onReason={setReason}
            onComment={setComment}
          />
        ) : (
          <>
            <Rules item={item} />
            <p className="muted">{progressOf(item)}</p>
          </>
        )}
      </div>
    </Modal>
  );
}
