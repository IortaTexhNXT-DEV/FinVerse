import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { remittanceApi } from './api';
import type { Batch } from './api';
import { TotalsStrip } from './RemittanceParts';
import { parseEmails } from './remittanceLabels';
import './remittance.css';

interface DialogProps {
  busy: boolean;
  error: unknown;
  onClose: () => void;
}

function Footer({
  label,
  busy,
  disabled = false,
  onClose,
  onConfirm,
}: Readonly<{
  label: string;
  busy: boolean;
  disabled?: boolean;
  onClose: () => void;
  onConfirm: () => void;
}>) {
  return (
    <>
      <Button variant="secondary" onClick={onClose}>
        Cancel
      </Button>
      <Button busy={busy} disabled={disabled} onClick={onConfirm}>
        {label}
      </Button>
    </>
  );
}

function CommentField({
  value,
  onChange,
}: Readonly<{ value: string; onChange: (value: string) => void }>) {
  return (
    <Field label="Comment" hint="Kept in the status history and sent with the notification.">
      {(id) => (
        <textarea
          id={id}
          className="textarea"
          rows={3}
          maxLength={1000}
          value={value}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
    </Field>
  );
}

/**
 * Preview before submission (RMTID.002 addendum, RMTID.010/019): the accounts kept, the totals and
 * the problems that block the submission.
 */
export function PreviewDialog({
  batch,
  busy,
  error,
  onClose,
  onSubmit,
}: Readonly<DialogProps & { batch: Batch; onSubmit: (comment?: string) => void }>) {
  const [comment, setComment] = useState('');
  const preview = useQuery({
    queryKey: ['remittance', 'preview', batch.summary.id],
    queryFn: () => remittanceApi.preview(batch.summary.id),
  });
  const p = preview.data;
  const blocked = p === undefined || p.problems.length > 0 || p.lineCount === 0;
  return (
    <Modal
      title={`Preview Submission of ${batch.summary.batchNo}`}
      open
      onClose={onClose}
      footer={
        <Footer
          label="Submit for Approval"
          busy={busy}
          disabled={blocked}
          onClose={onClose}
          onConfirm={() => onSubmit(comment.trim() || undefined)}
        />
      }
    >
      <div className="stack">
        <ErrorAlert error={error ?? preview.error} />
        {p === undefined ? (
          <span className="spinner" aria-label="Loading" />
        ) : (
          <>
            <p>
              {p.lineCount} account(s) will be remitted; {p.excludedCount} excluded account(s) stay
              out of the batch. Amounts cannot be changed.
            </p>
            <TotalsStrip totals={p.totals} currency={batch.summary.currency} />
            {p.problems.length > 0 && (
              <ul className="remit-problems" aria-label="Problems">
                {p.problems.map((problem) => (
                  <li key={problem}>{problem}</li>
                ))}
              </ul>
            )}
          </>
        )}
        <CommentField value={comment} onChange={setComment} />
      </div>
    </Modal>
  );
}

/** Approval by the team leader (four eyes): posting and push to Disbursement (RMTID.010/011). */
export function ApproveDialog({
  batch,
  busy,
  error,
  onClose,
  onApprove,
}: Readonly<DialogProps & { batch: Batch; onApprove: (comment?: string) => void }>) {
  const [comment, setComment] = useState('');
  return (
    <Modal
      title={`Approve ${batch.summary.batchNo}`}
      open
      onClose={onClose}
      footer={
        <Footer
          label="Approve and Push"
          busy={busy}
          onClose={onClose}
          onConfirm={() => onApprove(comment.trim() || undefined)}
        />
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <p>
          The remittance is posted to the ledger and the payment request is sent to Disbursement.
          The commission and incentive ORs are requested from Cashiering.
        </p>
        <TotalsStrip totals={batch.summary.totals} currency={batch.summary.currency} />
        <CommentField value={comment} onChange={setComment} />
      </div>
    </Modal>
  );
}

/** Exclusion of the selected accounts with a reason (RMTID.002 addendum). */
export function ExcludeDialog({
  count,
  busy,
  error,
  onClose,
  onExclude,
}: Readonly<
  DialogProps & { count: number; onExclude: (reason: string, comment?: string) => void }
>) {
  const [reason, setReason] = useState('');
  const [comment, setComment] = useState('');
  const [missing, setMissing] = useState<string>();
  return (
    <Modal
      title={`Exclude ${count} Account(s)`}
      open
      onClose={onClose}
      footer={
        <Footer
          label="Exclude Accounts"
          busy={busy}
          onClose={onClose}
          onConfirm={() => {
            if (reason === '') {
              setMissing('Select a reason');
              return;
            }
            onExclude(reason, comment.trim() || undefined);
          }}
        />
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <p>
          Excluded accounts keep their amounts and can be restored until the batch is submitted.
        </p>
        <Field label="Reason" required error={missing}>
          {(id) => (
            <LovSelect
              id={id}
              type="REMIT_EXCLUSION_REASON"
              value={reason}
              onChange={(code) => {
                setReason(code);
                setMissing(undefined);
              }}
              required
            />
          )}
        </Field>
        <CommentField value={comment} onChange={setComment} />
      </div>
    </Modal>
  );
}

/** The schedule e-mailed to the insurer, password-protected, once (MKTID.001). */
export function SendScheduleDialog({
  batch,
  busy,
  error,
  onClose,
  onSend,
}: Readonly<
  DialogProps & {
    batch: Batch;
    onSend: (mail: { to: string[]; subject: string; body: string }) => void;
  }
>) {
  const [to, setTo] = useState('');
  const [subject, setSubject] = useState(`Remittance schedule ${batch.summary.batchNo}`);
  const [body, setBody] = useState(
    `Please find attached our remittance schedule ${batch.summary.batchNo}. Kindly return it with your official receipt number and date for each account.`,
  );
  const [errors, setErrors] = useState<{ to?: string; subject?: string }>({});
  const send = () => {
    const emails = parseEmails(to);
    const next = {
      to:
        emails.valid.length === 0 || emails.invalid.length > 0
          ? 'Enter valid e-mail addresses separated by commas'
          : undefined,
      subject: subject.trim() === '' ? 'Subject is required' : undefined,
    };
    setErrors(next);
    if (next.to === undefined && next.subject === undefined) {
      onSend({ to: emails.valid, subject: subject.trim(), body });
    }
  };
  return (
    <Modal
      title="Send Schedule to Insurer"
      open
      onClose={onClose}
      footer={<Footer label="Send via Email" busy={busy} onClose={onClose} onConfirm={send} />}
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <Field
          label="To"
          required
          error={errors.to}
          hint="The Excel schedule is password protected; the password follows in a separate e-mail."
        >
          {(id) => (
            <input id={id} className="input" value={to} onChange={(e) => setTo(e.target.value)} />
          )}
        </Field>
        <Field label="Subject" required error={errors.subject}>
          {(id) => (
            <input
              id={id}
              className="input"
              maxLength={250}
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
            />
          )}
        </Field>
        <Field label="Message">
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={4}
              maxLength={4000}
              value={body}
              onChange={(e) => setBody(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
