import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { renewalApi } from '@/api/renewal';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { DISPOSITION_OPTIONS, RNW_LOV } from './renewalCodes';

interface DialogProps {
  busy: boolean;
  error: unknown;
  onClose: () => void;
}

function Actions({
  label,
  busy,
  disabled,
  onClose,
  onConfirm,
}: Readonly<{
  label: string;
  busy: boolean;
  disabled: boolean;
  onClose: () => void;
  onConfirm: () => void;
}>) {
  return (
    <>
      <Button variant="ghost" onClick={onClose}>
        Cancel
      </Button>
      <Button busy={busy} disabled={disabled} onClick={onConfirm}>
        {label}
      </Button>
    </>
  );
}

/** Generate Expiry List for an expiry range (FR-RN-011). */
export function ExtractDialog({
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<DialogProps & { onConfirm: (from: string, to: string) => void }>) {
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  return (
    <Modal
      open
      title="Generate Expiry List"
      onClose={onClose}
      footer={
        <Actions
          label="Generate"
          busy={busy}
          disabled={from === '' || to === '' || to < from}
          onClose={onClose}
          onConfirm={() => onConfirm(from, to)}
        />
      }
    >
      <p className="muted">
        Accounts expiring in the range are read from the booked accounts and the migrated policies;
        renewals already extracted are not created again.
      </p>
      <ErrorAlert error={error} />
      <div className="form-grid">
        <Field label="Expiry From" required>
          {(id) => <DateInput id={id} value={from} onChange={(e) => setFrom(e.target.value)} />}
        </Field>
        <Field label="Expiry To" required>
          {(id) => <DateInput id={id} value={to} onChange={(e) => setTo(e.target.value)} />}
        </Field>
      </div>
    </Modal>
  );
}

const OVERRIDE_KINDS = [
  { code: 'OUTSTANDING_BALANCE', label: 'Outstanding balance' },
  { code: 'CHECK', label: 'Failed check' },
  { code: 'BUCKET', label: 'Classification' },
  { code: 'DISPOSITION', label: 'Disposition' },
  { code: 'INSURER_MISMATCH', label: 'Insurer response mismatch' },
  { code: 'RA_UNLOCK', label: 'Unlock the Renewal Advice' },
];

const BUCKETS = [
  { code: 'CLEAN', label: 'Clean' },
  { code: 'REVIEW', label: 'Review' },
  { code: 'EXCEPTION', label: 'Exception' },
];

function OverrideTarget({
  kind,
  value,
  onChange,
}: Readonly<{ kind: string; value: string; onChange: (v: string) => void }>) {
  const checks = useQuery({
    queryKey: ['renewal', 'setup', 'checks'],
    queryFn: renewalApi.checkSettings,
    enabled: kind === 'CHECK',
  });
  let options: { code: string; label: string }[] = [];
  if (kind === 'BUCKET') options = BUCKETS;
  if (kind === 'DISPOSITION') options = DISPOSITION_OPTIONS;
  if (kind === 'CHECK') {
    options = (checks.data ?? []).map((c) => ({ code: c.checkCode, label: c.checkName }));
  }
  if (options.length === 0) {
    return null;
  }
  return (
    <Field label={kind === 'CHECK' ? 'Check' : 'New value'} required>
      {(id) => (
        <select id={id} className="select" value={value} onChange={(e) => onChange(e.target.value)}>
          <option value="">Select</option>
          {options.map((o) => (
            <option key={o.code} value={o.code}>
              {o.label}
            </option>
          ))}
        </select>
      )}
    </Field>
  );
}

/** Override by the Team Leader (FR-RN-051): the kind, the new value, reason and remarks. */
export function OverrideDialog({
  count,
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<
  DialogProps & {
    count: number;
    onConfirm: (input: {
      kind: string;
      target?: string;
      reasonCode: string;
      remarks: string;
    }) => void;
  }
>) {
  const [kind, setKind] = useState('');
  const [target, setTarget] = useState('');
  const [reason, setReason] = useState('');
  const [remarks, setRemarks] = useState('');
  const needsTarget = ['CHECK', 'BUCKET', 'DISPOSITION'].includes(kind);
  const ready =
    kind !== '' && (!needsTarget || target !== '') && reason !== '' && remarks.trim() !== '';
  return (
    <Modal
      open
      title={`Override (${String(count)})`}
      onClose={onClose}
      footer={
        <Actions
          label="Override"
          busy={busy}
          disabled={!ready}
          onClose={onClose}
          onConfirm={() =>
            onConfirm({
              kind,
              target: needsTarget ? target : undefined,
              reasonCode: reason,
              remarks,
            })
          }
        />
      }
    >
      <ErrorAlert error={error} />
      <Field label="Override" required>
        {(id) => (
          <select
            id={id}
            className="select"
            value={kind}
            onChange={(e) => {
              setKind(e.target.value);
              setTarget('');
            }}
          >
            <option value="">Select what to override</option>
            {OVERRIDE_KINDS.map((k) => (
              <option key={k.code} value={k.code}>
                {k.label}
              </option>
            ))}
          </select>
        )}
      </Field>
      <OverrideTarget kind={kind} value={target} onChange={setTarget} />
      <Field label="Reason" required>
        {(id) => (
          <LovSelect
            id={id}
            type={RNW_LOV.overrideReason}
            value={reason}
            onChange={setReason}
            required
          />
        )}
      </Field>
      <Field label="Remarks" required hint="Up to 200 characters">
        {(id) => (
          <textarea
            id={id}
            className="input"
            rows={3}
            maxLength={200}
            value={remarks}
            onChange={(e) => setRemarks(e.target.value)}
          />
        )}
      </Field>
    </Modal>
  );
}

/** Generate Renewal Advice (FR-RN-080): first or second notice; a late RA needs confirmation. */
export function GenerateRaDialog({
  count,
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<
  DialogProps & {
    count: number;
    onConfirm: (notice: 'FIRST' | 'SECOND', confirmLate: boolean) => void;
  }
>) {
  const [notice, setNotice] = useState<'FIRST' | 'SECOND'>('FIRST');
  const [late, setLate] = useState(false);
  return (
    <Modal
      open
      title={`Generate Renewal Advice (${String(count)})`}
      onClose={onClose}
      footer={
        <Actions
          label="Generate"
          busy={busy}
          disabled={false}
          onClose={onClose}
          onConfirm={() => onConfirm(notice, late)}
        />
      }
    >
      <ErrorAlert error={error} />
      <Field label="Notice" required>
        {(id) => (
          <select
            id={id}
            className="select"
            value={notice}
            onChange={(e) => setNotice(e.target.value === 'SECOND' ? 'SECOND' : 'FIRST')}
          >
            <option value="FIRST">First notice</option>
            <option value="SECOND">Second notice</option>
          </select>
        )}
      </Field>
      <label className="checkbox">
        <input type="checkbox" checked={late} onChange={(e) => setLate(e.target.checked)} />
        Generate also when the minimum notice before expiry has passed (the confirmation is
        recorded)
      </label>
    </Modal>
  );
}

/** Assign a Processing Officer (FR-RN-061). */
export function PoAssignDialog({
  count,
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<DialogProps & { count: number; onConfirm: (po: string) => void }>) {
  const [po, setPo] = useState('');
  const officers = useQuery({
    queryKey: ['renewal', 'processing-officers'],
    queryFn: renewalApi.processingOfficers,
  });
  return (
    <Modal
      open
      title={`Assign Processing Officer (${String(count)})`}
      onClose={onClose}
      footer={
        <Actions
          label="Assign"
          busy={busy}
          disabled={po === ''}
          onClose={onClose}
          onConfirm={() => onConfirm(po)}
        />
      }
    >
      <ErrorAlert error={error ?? officers.error} />
      <Field label="Processing Officer" required>
        {(id) => (
          <select id={id} className="select" value={po} onChange={(e) => setPo(e.target.value)}>
            <option value="">Select the officer</option>
            {(officers.data ?? []).map((o) => (
              <option key={o.username} value={o.username}>
                {o.fullName}
              </option>
            ))}
          </select>
        )}
      </Field>
    </Modal>
  );
}

/** Return to Marketing (FR-RN-065): to the officer or to the Team Leader. */
export function ReturnToMarketingDialog({
  count,
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<
  DialogProps & {
    count: number;
    onConfirm: (input: { toLeader: boolean; reasonCode: string; remarks: string }) => void;
  }
>) {
  const [toLeader, setToLeader] = useState(false);
  const [reason, setReason] = useState('');
  const [remarks, setRemarks] = useState('');
  return (
    <Modal
      open
      title={`Return to Marketing (${String(count)})`}
      onClose={onClose}
      footer={
        <Actions
          label="Return"
          busy={busy}
          disabled={reason === '' || remarks.trim() === ''}
          onClose={onClose}
          onConfirm={() => onConfirm({ toLeader, reasonCode: reason, remarks })}
        />
      }
    >
      <ErrorAlert error={error} />
      <Field label="Return to" required>
        {(id) => (
          <select
            id={id}
            className="select"
            value={toLeader ? 'TL' : 'AO'}
            onChange={(e) => setToLeader(e.target.value === 'TL')}
          >
            <option value="AO">Account Officer</option>
            <option value="TL">Team Leader</option>
          </select>
        )}
      </Field>
      <Field label="Reason" required>
        {(id) => (
          <LovSelect
            id={id}
            type={RNW_LOV.returnReason}
            value={reason}
            onChange={setReason}
            required
          />
        )}
      </Field>
      <Field label="Remarks" required hint="Up to 200 characters">
        {(id) => (
          <textarea
            id={id}
            className="input"
            rows={3}
            maxLength={200}
            value={remarks}
            onChange={(e) => setRemarks(e.target.value)}
          />
        )}
      </Field>
    </Modal>
  );
}
