import { useQuery } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { useState } from 'react';
import { renewalApi } from '@/api/renewal';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useCompanyId } from '@/context/workspaceContext';
import { RNW_LOV } from './renewalCodes';

interface BaseProps {
  count: number;
  busy: boolean;
  error: unknown;
  onClose: () => void;
}

function Footer({
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

function Remarks({
  label = 'Remarks',
  value,
  onChange,
  required = true,
}: Readonly<{ label?: string; value: string; onChange: (v: string) => void; required?: boolean }>) {
  return (
    <Field label={label} required={required} hint="Up to 200 characters">
      {(id) => (
        <textarea
          id={id}
          className="input"
          maxLength={200}
          rows={3}
          value={value}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
    </Field>
  );
}

/** Assign Disposition / Re-assign Officer (FR-RN-030). */
export function AssignDialog({
  count,
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<BaseProps & { onConfirm: (ao: string, reasonCode: string) => void }>) {
  const companyId = useCompanyId();
  const [ao, setAo] = useState('');
  const [reason, setReason] = useState('');
  const officers = useQuery({
    queryKey: ['renewal', 'officers', companyId],
    queryFn: () => renewalApi.officers(companyId),
  });
  return (
    <Modal
      open
      title={`Assign ${String(count)} renewal(s)`}
      onClose={onClose}
      footer={
        <Footer
          label="Assign and Push"
          busy={busy}
          disabled={ao === ''}
          onClose={onClose}
          onConfirm={() => onConfirm(ao, reason)}
        />
      }
    >
      <ErrorAlert error={error ?? officers.error} />
      <Field label="Account Officer" required>
        {(id) => (
          <select id={id} className="select" value={ao} onChange={(e) => setAo(e.target.value)}>
            <option value="">Select the officer</option>
            {(officers.data ?? []).map((o) => (
              <option key={o.username} value={o.username}>
                {o.fullName} ({o.unit})
              </option>
            ))}
          </select>
        )}
      </Field>
      <Field label="Reason (re-assignment)">
        {(id) => (
          <LovSelect id={id} type={RNW_LOV.transferReason} value={reason} onChange={setReason} />
        )}
      </Field>
    </Modal>
  );
}

/** A reason and remarks (Return, Return to Marketing, Override). */
export function ReasonDialog({
  title,
  lov,
  confirmLabel,
  count,
  busy,
  error,
  onClose,
  onConfirm,
  children,
}: Readonly<
  BaseProps & {
    title: string;
    lov: string;
    confirmLabel: string;
    onConfirm: (reasonCode: string, remarks: string) => void;
    children?: ReactNode;
  }
>) {
  const [reason, setReason] = useState('');
  const [remarks, setRemarks] = useState('');
  return (
    <Modal
      open
      title={`${title} (${String(count)})`}
      onClose={onClose}
      footer={
        <Footer
          label={confirmLabel}
          busy={busy}
          disabled={reason === '' || remarks.trim() === ''}
          onClose={onClose}
          onConfirm={() => onConfirm(reason, remarks)}
        />
      }
    >
      <ErrorAlert error={error} />
      {children}
      <Field label="Reason" required>
        {(id) => <LovSelect id={id} type={lov} value={reason} onChange={setReason} required />}
      </Field>
      <Remarks value={remarks} onChange={setRemarks} />
    </Modal>
  );
}

/** Transfer to another Marketing unit (FR-RN-031). */
export function TransferDialog({
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<
  Omit<BaseProps, 'count'> & {
    onConfirm: (toUnit: string, reasonCode: string, remarks: string) => void;
  }
>) {
  const companyId = useCompanyId();
  const [unit, setUnit] = useState('');
  const [reason, setReason] = useState('');
  const [remarks, setRemarks] = useState('');
  const units = useQuery({
    queryKey: ['renewal', 'code-set', 'renewal.unitsAll', companyId],
    queryFn: () => renewalApi.codeSet('renewal.unit', companyId),
  });
  return (
    <Modal
      open
      title="Transfer to another unit"
      onClose={onClose}
      footer={
        <Footer
          label="Submit and Push"
          busy={busy}
          disabled={unit.trim() === '' || remarks.trim() === ''}
          onClose={onClose}
          onConfirm={() => onConfirm(unit.trim(), reason, remarks)}
        />
      }
    >
      <ErrorAlert error={error} />
      <Field label="Receiving Unit" required hint="Sales unit code, e.g. T-CORP1">
        {(id) => (
          <input
            id={id}
            className="input"
            list="rnw-units"
            value={unit}
            onChange={(e) => setUnit(e.target.value)}
          />
        )}
      </Field>
      <datalist id="rnw-units">
        {(units.data ?? []).map((u) => (
          <option key={u.code} value={u.code} />
        ))}
      </datalist>
      <Field label="Reason">
        {(id) => (
          <LovSelect id={id} type={RNW_LOV.transferReason} value={reason} onChange={setReason} />
        )}
      </Field>
      <Remarks value={remarks} onChange={setRemarks} />
    </Modal>
  );
}

/** A simple text (re-open reason, remark, decline remarks). */
export function TextDialog({
  title,
  label,
  confirmLabel,
  required = true,
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<
  Omit<BaseProps, 'count'> & {
    title: string;
    label: string;
    confirmLabel: string;
    required?: boolean;
    onConfirm: (text: string) => void;
  }
>) {
  const [text, setText] = useState('');
  return (
    <Modal
      open
      title={title}
      onClose={onClose}
      footer={
        <Footer
          label={confirmLabel}
          busy={busy}
          disabled={required && text.trim() === ''}
          onClose={onClose}
          onConfirm={() => onConfirm(text)}
        />
      }
    >
      <ErrorAlert error={error} />
      <Remarks label={label} value={text} onChange={setText} required={required} />
    </Modal>
  );
}
