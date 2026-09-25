import { useState } from 'react';
import type { ReactNode } from 'react';
import type {
  AutoBookRule,
  IncentiveRule,
  ServiceInvoiceType,
  SiRecipient,
  SiTrigger,
} from '@/api/booking';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';

interface DialogProps<T> {
  value: T;
  busy: boolean;
  error: unknown;
  onSave: (value: T) => void;
  onClose: () => void;
}

function SetupModal({
  title,
  busy,
  disabled,
  error,
  onSave,
  onClose,
  children,
}: Readonly<{
  title: string;
  busy: boolean;
  disabled: boolean;
  error: unknown;
  onSave: () => void;
  onClose: () => void;
  children: ReactNode;
}>) {
  return (
    <Modal
      open
      title={title}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={busy} disabled={disabled} onClick={onSave}>
            Save
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        {children}
      </div>
    </Modal>
  );
}

function TextField({
  label,
  value,
  onChange,
  hint,
  error,
  required = false,
  type = 'text',
}: Readonly<{
  label: string;
  value: string | undefined;
  onChange: (v: string) => void;
  hint?: string;
  error?: string;
  required?: boolean;
  type?: 'text' | 'date';
}>) {
  return (
    <Field label={label} hint={hint} error={error} required={required}>
      {(id) => (
        <input
          id={id}
          className="input"
          type={type}
          value={value ?? ''}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
    </Field>
  );
}

function Check({
  label,
  checked,
  onChange,
}: Readonly<{ label: string; checked: boolean; onChange: (v: boolean) => void }>) {
  return (
    <label className="checkbox">
      <input type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} />
      {label}
    </label>
  );
}

function required(value: string | undefined, message: string): string | undefined {
  return value === undefined || value.trim() === '' ? message : undefined;
}

/** Add or change an auto-book rule (BRNB.076). */
export function AutoBookRuleDialog({
  value,
  busy,
  error,
  onSave,
  onClose,
}: Readonly<DialogProps<AutoBookRule>>) {
  const [rule, setRule] = useState(value);
  const missing = required(rule.description, 'Describe the rule');
  return (
    <SetupModal
      title={rule.id === undefined ? 'New Auto-book Rule' : 'Edit Auto-book Rule'}
      busy={busy}
      disabled={missing !== undefined}
      error={error}
      onSave={() => onSave(rule)}
      onClose={onClose}
    >
      <div className="form-grid">
        <TextField
          label="Product"
          hint="Blank: every product."
          value={rule.productCode}
          onChange={(v) => setRule({ ...rule, productCode: v.toUpperCase() })}
        />
        <TextField
          label="Market segment"
          hint="Blank: every segment."
          value={rule.marketSegment}
          onChange={(v) => setRule({ ...rule, marketSegment: v.toUpperCase() })}
        />
      </div>
      <TextField
        label="Description"
        required
        error={missing}
        value={rule.description}
        onChange={(v) => setRule({ ...rule, description: v })}
      />
      <Check
        label="Enabled"
        checked={rule.enabled}
        onChange={(enabled) => setRule({ ...rule, enabled })}
      />
    </SetupModal>
  );
}

/** Add or change an incentive eligibility rule (BRNB.107). */
export function IncentiveRuleDialog({
  value,
  busy,
  error,
  onSave,
  onClose,
}: Readonly<DialogProps<IncentiveRule>>) {
  const [rule, setRule] = useState(value);
  const missing = required(rule.description, 'Describe the rule');
  const noStart = required(rule.periodFrom, 'Enter the first booking date covered');
  const backwards =
    rule.periodTo !== undefined && rule.periodTo !== '' && rule.periodTo < rule.periodFrom
      ? 'The period end is before its start'
      : undefined;
  return (
    <SetupModal
      title={rule.id === undefined ? 'New Incentive Rule' : 'Edit Incentive Rule'}
      busy={busy}
      disabled={[missing, noStart, backwards].some((e) => e !== undefined)}
      error={error}
      onSave={() => onSave({ ...rule, periodTo: rule.periodTo === '' ? undefined : rule.periodTo })}
      onClose={onClose}
    >
      <div className="form-grid">
        <TextField
          label="Product"
          hint="Blank: every product."
          value={rule.productCode}
          onChange={(v) => setRule({ ...rule, productCode: v.toUpperCase() })}
        />
        <TextField
          label="Market segment"
          hint="Blank: every segment."
          value={rule.marketSegment}
          onChange={(v) => setRule({ ...rule, marketSegment: v.toUpperCase() })}
        />
        <TextField
          label="Source channel"
          hint="Blank: every channel."
          value={rule.sourceChannel}
          onChange={(v) => setRule({ ...rule, sourceChannel: v.toUpperCase() })}
        />
        <TextField
          label="Booked from"
          type="date"
          required
          error={noStart}
          value={rule.periodFrom}
          onChange={(periodFrom) => setRule({ ...rule, periodFrom })}
        />
        <TextField
          label="Booked until"
          type="date"
          error={backwards}
          value={rule.periodTo}
          onChange={(periodTo) => setRule({ ...rule, periodTo })}
        />
      </div>
      <TextField
        label="Description"
        required
        error={missing}
        value={rule.description}
        onChange={(v) => setRule({ ...rule, description: v })}
      />
      <Check
        label="Active"
        checked={rule.active}
        onChange={(active) => setRule({ ...rule, active })}
      />
    </SetupModal>
  );
}

const RECIPIENTS: SiRecipient[] = ['INSURER', 'INTERNAL'];
const TRIGGERS: SiTrigger[] = ['ON_BOOKING', 'ON_ENDORSEMENT', 'MANUAL', 'ON_INCENTIVE'];
const TRIGGER_LABELS: Record<SiTrigger, string> = {
  ON_BOOKING: 'On booking',
  ON_ENDORSEMENT: 'On endorsement',
  MANUAL: 'Manual',
  ON_INCENTIVE: 'On early incentive (remittance)',
};

/** Add or change a service invoice type (BRNB.100). */
export function ServiceInvoiceTypeDialog({
  value,
  busy,
  error,
  onSave,
  onClose,
}: Readonly<DialogProps<ServiceInvoiceType>>) {
  const [type, setType] = useState(value);
  const badCode = /^[A-Z0-9_]{2,40}$/.test(type.code)
    ? undefined
    : 'Use 2-40 capitals, digits or _';
  const missing =
    required(type.name, 'Name the type') ?? required(type.templateCode, 'Choose the template');
  return (
    <SetupModal
      title={type.id === undefined ? 'New Service Invoice Type' : `Edit ${type.code}`}
      busy={busy}
      disabled={badCode !== undefined || missing !== undefined}
      error={error}
      onSave={() => onSave(type)}
      onClose={onClose}
    >
      <div className="form-grid">
        <Field label="Code" required error={badCode}>
          {(id) => (
            <input
              id={id}
              className="input"
              disabled={type.id !== undefined}
              value={type.code}
              onChange={(e) => setType({ ...type, code: e.target.value.toUpperCase() })}
            />
          )}
        </Field>
        <TextField
          label="Name"
          required
          value={type.name}
          onChange={(name) => setType({ ...type, name })}
        />
        <Field label="Recipient" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={type.recipient}
              onChange={(e) => setType({ ...type, recipient: e.target.value as SiRecipient })}
            >
              {RECIPIENTS.map((r) => (
                <option key={r} value={r}>
                  {r === 'INSURER' ? 'Insurer (e-mailed)' : 'Internal'}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Trigger" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={type.trigger}
              onChange={(e) => setType({ ...type, trigger: e.target.value as SiTrigger })}
            >
              {TRIGGERS.map((t) => (
                <option key={t} value={t}>
                  {TRIGGER_LABELS[t]}
                </option>
              ))}
            </select>
          )}
        </Field>
        <TextField
          label="Owner team (permission)"
          hint="Its holders are notified of the dispatch."
          value={type.ownerPermission}
          onChange={(ownerPermission) =>
            setType({ ...type, ownerPermission: ownerPermission.toUpperCase() })
          }
        />
        <TextField
          label="Owner user"
          value={type.ownerUsername}
          onChange={(ownerUsername) => setType({ ...type, ownerUsername })}
        />
        <TextField
          label="Document template"
          required
          error={missing}
          value={type.templateCode}
          onChange={(templateCode) =>
            setType({ ...type, templateCode: templateCode.toUpperCase() })
          }
        />
      </div>
      <Check
        label="Active"
        checked={type.active}
        onChange={(active) => setType({ ...type, active })}
      />
    </SetupModal>
  );
}
