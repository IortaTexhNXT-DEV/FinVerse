import { useState } from 'react';
import { LovSelect } from '@/components/broking/LovSelect';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { today } from '@/utils/format';
import { optionalText, parseInvoiceList } from '../plans/labels';
import { DialogFooter, InputField, TextAreaField } from '../plans/Parts';
import type { Basis, EscalateInput, Rule, RuleInput, TargetLevel } from './api';
import type { RuleForm } from './labels';
import { BASIS_LABELS, LEVEL_LABELS, ruleFormErrors, ruleFormOf, ruleInputOf } from './labels';

interface DialogProps {
  busy: boolean;
  error: unknown;
  onClose: () => void;
}

const LEVELS = Object.keys(LEVEL_LABELS) as TargetLevel[];
const BASES = Object.keys(BASIS_LABELS) as Basis[];

function LevelSelect({
  value,
  onChange,
}: Readonly<{ value: TargetLevel; onChange: (level: TargetLevel) => void }>) {
  return (
    <Field label="Escalate To" required>
      {(id) => (
        <select
          id={id}
          className="select"
          value={value}
          onChange={(e) => onChange(e.target.value as TargetLevel)}
        >
          {LEVELS.map((l) => (
            <option key={l} value={l}>
              {LEVEL_LABELS[l]}
            </option>
          ))}
        </select>
      )}
    </Field>
  );
}

/**
 * Manual escalation (BRCLXN.050): one or several invoices, regardless of the automatic rules; the
 * invoices of the same account are escalated together.
 */
export function EscalateDialog({
  companyId,
  busy,
  error,
  onClose,
  onEscalate,
}: Readonly<DialogProps & { companyId: number; onEscalate: (input: EscalateInput) => void }>) {
  const [invoices, setInvoices] = useState('');
  const [level, setLevel] = useState<TargetLevel>('TL');
  const [user, setUser] = useState('');
  const [reason, setReason] = useState('');
  const [remarks, setRemarks] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const escalate = () => {
    const invoiceNos = parseInvoiceList(invoices);
    const found: Record<string, string> = {};
    if (invoiceNos.length === 0) {
      found.invoices = 'Enter at least one invoice number';
    }
    if (level === 'USER' && user.trim() === '') {
      found.user = 'Name the user who receives the escalation';
    }
    if (reason === '') {
      found.reason = 'Choose the reason';
    }
    setErrors(found);
    if (Object.keys(found).length === 0) {
      onEscalate({
        companyId,
        invoiceNos,
        targetLevel: level,
        targetUsername: optionalText(user),
        reasonCode: reason,
        remarks: optionalText(remarks),
      });
    }
  };
  return (
    <Modal
      title="Escalate Accounts"
      open
      onClose={onClose}
      footer={<DialogFooter label="Escalate" busy={busy} onClose={onClose} onConfirm={escalate} />}
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <TextAreaField
          label="Invoice Nos."
          value={invoices}
          onChange={setInvoices}
          required
          error={errors.invoices}
          hint="One per line; one escalation per account"
        />
        <LevelSelect value={level} onChange={setLevel} />
        <InputField
          label="User"
          value={user}
          onChange={setUser}
          required={level === 'USER'}
          error={errors.user}
          hint="Optional for a level: without a user the escalation waits in the level's queue"
        />
        <Field label="Reason" required error={errors.reason}>
          {(id) => (
            <LovSelect
              id={id}
              type="CLX_ESCALATION_REASON"
              value={reason}
              onChange={setReason}
              required
            />
          )}
        </Field>
        <TextAreaField label="Remarks" value={remarks} onChange={setRemarks} />
      </div>
    </Modal>
  );
}

/** A new or changed escalation rule (BRCLXN.049); it applies once another user authorizes it. */
export function RuleDialog({
  rule,
  companyId,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<DialogProps & { rule?: Rule; companyId: number; onSave: (input: RuleInput) => void }>) {
  const [form, setForm] = useState<RuleForm>(ruleFormOf(rule, today()));
  const [errors, setErrors] = useState<ReturnType<typeof ruleFormErrors>>({});
  const set = (key: keyof RuleForm, value: string) => setForm({ ...form, [key]: value });
  const save = () => {
    const found = ruleFormErrors(form, rule === undefined);
    setErrors(found);
    if (Object.keys(found).length === 0) {
      onSave(ruleInputOf(form, companyId));
    }
  };
  return (
    <Modal
      title={rule === undefined ? 'New Escalation Rule' : `Change ${rule.code}`}
      open
      onClose={onClose}
      footer={<DialogFooter label="Save Rule" busy={busy} onClose={onClose} onConfirm={save} />}
    >
      <div className="stack">
        <ErrorAlert error={error} />
        {rule === undefined && (
          <InputField
            label="Code"
            value={form.code}
            onChange={(v) => set('code', v)}
            required
            error={errors.code}
          />
        )}
        <InputField
          label="Name"
          value={form.name}
          onChange={(v) => set('name', v)}
          required
          error={errors.name}
        />
        <Field label="Basis" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={form.basis}
              onChange={(e) => set('basis', e.target.value)}
            >
              {BASES.map((b) => (
                <option key={b} value={b}>
                  {BASIS_LABELS[b]}
                </option>
              ))}
            </select>
          )}
        </Field>
        <InputField
          label="Threshold"
          type="number"
          value={form.threshold}
          onChange={(v) => set('threshold', v)}
          required
          error={errors.threshold}
          hint="Days, a number of promises or an amount, depending on the basis"
        />
        <InputField
          label="Segment"
          value={form.segment}
          onChange={(v) => set('segment', v)}
          hint="Empty = every segment"
        />
        <LevelSelect value={form.targetLevel} onChange={(l) => set('targetLevel', l)} />
        <InputField
          label="User"
          value={form.targetUsername}
          onChange={(v) => set('targetUsername', v)}
          error={errors.targetUsername}
        />
        <Field label="Reason" required error={errors.reasonCode}>
          {(id) => (
            <LovSelect
              id={id}
              type="CLX_ESCALATION_REASON"
              value={form.reasonCode}
              onChange={(code) => set('reasonCode', code)}
              required
            />
          )}
        </Field>
        <InputField
          label="SLA (Hours)"
          type="number"
          value={form.slaHours}
          onChange={(v) => set('slaHours', v)}
          required
          error={errors.slaHours}
        />
        <div className="grid-2">
          <InputField
            label="Effective From"
            type="date"
            value={form.effectiveFrom}
            onChange={(v) => set('effectiveFrom', v)}
            required
            error={errors.effectiveFrom}
          />
          <InputField
            label="Effective To"
            type="date"
            value={form.effectiveTo}
            onChange={(v) => set('effectiveTo', v)}
            error={errors.effectiveTo}
          />
        </div>
      </div>
    </Modal>
  );
}
