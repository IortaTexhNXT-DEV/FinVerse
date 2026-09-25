import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { today } from '@/utils/format';
import { collectionsApi } from './api';
import type { DispositionInput, EffortInput, ReassignInput } from './api';
import { cleanDetails, detailFields, dispositionErrors, handoffText } from './collectionsLogic';

interface DialogProps {
  /** Accounts the action applies to. */
  invoiceNos: string[];
  busy: boolean;
  error: unknown;
  onClose: () => void;
}

function Footer({
  label,
  busy,
  onClose,
  onConfirm,
}: Readonly<{ label: string; busy: boolean; onClose: () => void; onConfirm: () => void }>) {
  return (
    <>
      <Button variant="secondary" onClick={onClose}>
        Cancel
      </Button>
      <Button busy={busy} onClick={onConfirm}>
        {label}
      </Button>
    </>
  );
}

function Scope({ invoiceNos }: Readonly<{ invoiceNos: string[] }>) {
  return (
    <p className="clx-muted">
      {invoiceNos.length === 1
        ? `Account ${invoiceNos[0]}`
        : `${invoiceNos.length} accounts selected (one bulk reference)`}
    </p>
  );
}

function TextArea({
  label,
  value,
  onChange,
}: Readonly<{ label: string; value: string; onChange: (v: string) => void }>) {
  return (
    <Field label={label}>
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
 * Records a PR collector disposition (BRCLXN.016-023, 051): the active dispositions with their
 * rules, and the hand-off details the chosen one needs (check pick-up, BIR 2307).
 */
export function DispositionDialog({
  invoiceNos,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<DialogProps & { onSave: (input: DispositionInput) => void }>) {
  const rules = useQuery({
    queryKey: ['collections', 'disposition-rules'],
    queryFn: collectionsApi.dispositionRules,
    staleTime: 5 * 60_000,
  });
  const [code, setCode] = useState('');
  const [remarks, setRemarks] = useState('');
  const [details, setDetails] = useState<Record<string, string>>({});
  const [errors, setErrors] = useState<Record<string, string>>({});
  const rule = rules.data?.find((r) => r.code === code);
  const save = () => {
    const found = dispositionErrors(code, rule, details, today());
    setErrors(found);
    if (Object.keys(found).length === 0) {
      onSave({
        invoiceNos,
        code,
        remarks: remarks.trim() || undefined,
        details: cleanDetails(rule, details),
      });
    }
  };
  const handoff = handoffText(rule);
  return (
    <Modal
      title="Record Disposition"
      open
      onClose={onClose}
      footer={<Footer label="Record Disposition" busy={busy} onClose={onClose} onConfirm={save} />}
    >
      <div className="stack">
        <ErrorAlert error={error ?? rules.error} />
        <Scope invoiceNos={invoiceNos} />
        <Field label="Disposition" required error={errors.code}>
          {(id) => (
            <select
              id={id}
              className="select"
              value={code}
              onChange={(e) => {
                setCode(e.target.value);
                setDetails({});
              }}
            >
              <option value="">{rules.isLoading ? 'Loading…' : 'Select…'}</option>
              {(rules.data ?? []).map((r) => (
                <option key={r.code} value={r.code}>
                  {r.label}
                </option>
              ))}
            </select>
          )}
        </Field>
        {rule !== undefined && (
          <p className="clx-muted">
            {[
              rule.category && `Category ${rule.category}`,
              rule.taggingOwner && `Owner ${rule.taggingOwner}`,
              rule.allowedRoles.length > 0 && `Reserved to ${rule.allowedRoles.join(', ')}`,
            ]
              .filter(Boolean)
              .join(' · ')}
          </p>
        )}
        {handoff !== undefined && <div className="clx-info">{handoff}</div>}
        <div className="form-grid">
          {detailFields(rule?.opsAction).map((f) => (
            <Field
              key={f.key}
              label={f.label}
              required={f.required}
              error={errors[f.key]}
              hint={f.hint}
            >
              {(id) => (
                <input
                  id={id}
                  className="input"
                  type={f.type}
                  value={details[f.key] ?? ''}
                  onChange={(e) => setDetails({ ...details, [f.key]: e.target.value })}
                />
              )}
            </Field>
          ))}
        </div>
        <TextArea label="Remarks" value={remarks} onChange={setRemarks} />
      </div>
    </Modal>
  );
}

/** Logs a collection effort (p.43-46, BRCLXN.051): code, channel, contact and remarks. */
export function EffortDialog({
  invoiceNos,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<DialogProps & { onSave: (input: EffortInput) => void }>) {
  const [form, setForm] = useState({ code: '', channel: '', contactPerson: '', remarks: '' });
  const [codeError, setCodeError] = useState<string>();
  const save = () => {
    if (form.code === '') {
      setCodeError('Choose the effort');
      return;
    }
    onSave({
      invoiceNos,
      code: form.code,
      channel: form.channel.trim() || undefined,
      contactPerson: form.contactPerson.trim() || undefined,
      remarks: form.remarks.trim() || undefined,
    });
  };
  return (
    <Modal
      title="Log Collection Effort"
      open
      onClose={onClose}
      footer={<Footer label="Log Effort" busy={busy} onClose={onClose} onConfirm={save} />}
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <Scope invoiceNos={invoiceNos} />
        <Field label="Effort" required error={codeError}>
          {(id) => (
            <LovSelect
              id={id}
              type="CLX_EFFORT_CODE"
              value={form.code}
              onChange={(code) => setForm({ ...form, code })}
              required
            />
          )}
        </Field>
        <div className="form-grid">
          <Field label="Channel">
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={30}
                value={form.channel}
                onChange={(e) => setForm({ ...form, channel: e.target.value })}
              />
            )}
          </Field>
          <Field label="Contact Person">
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={120}
                value={form.contactPerson}
                onChange={(e) => setForm({ ...form, contactPerson: e.target.value })}
              />
            )}
          </Field>
        </div>
        <TextArea
          label="Remarks"
          value={form.remarks}
          onChange={(remarks) => setForm({ ...form, remarks })}
        />
      </div>
    </Modal>
  );
}

/** Reassigns accounts (BRCLXN.052): handler, permanent or temporary with an end date, reason. */
export function ReassignDialog({
  invoiceNos,
  total,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<
  Omit<DialogProps, 'invoiceNos'> & {
    invoiceNos?: string[];
    total: number;
    onSave: (input: ReassignInput) => void;
  }
>) {
  const handlers = useQuery({
    queryKey: ['collections', 'handlers'],
    queryFn: collectionsApi.handlers,
  });
  const [form, setForm] = useState({
    handler: '',
    kind: 'PERMANENT' as 'PERMANENT' | 'TEMPORARY',
    validTo: '',
    reason: '',
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const save = () => {
    const found: Record<string, string> = {};
    if (form.handler === '') {
      found.handler = 'Choose the new handler';
    }
    if (form.reason.trim() === '') {
      found.reason = 'Give the reason';
    }
    if (form.kind === 'TEMPORARY' && form.validTo <= today()) {
      found.validTo = 'A temporary reassignment ends after today';
    }
    setErrors(found);
    if (Object.keys(found).length === 0) {
      onSave({
        invoiceNos,
        handler: form.handler,
        kind: form.kind,
        validTo: form.kind === 'TEMPORARY' ? form.validTo : undefined,
        reason: form.reason.trim(),
      });
    }
  };
  return (
    <Modal
      title="Reassign Accounts"
      open
      onClose={onClose}
      footer={<Footer label="Reassign" busy={busy} onClose={onClose} onConfirm={save} />}
    >
      <div className="stack">
        <ErrorAlert error={error ?? handlers.error} />
        <p className="clx-muted">{total} account(s) will move to the new handler.</p>
        <div className="form-grid">
          <Field label="New Handler" required error={errors.handler}>
            {(id) => (
              <select
                id={id}
                className="select"
                value={form.handler}
                onChange={(e) => setForm({ ...form, handler: e.target.value })}
              >
                <option value="">Select…</option>
                {(handlers.data ?? []).map((h) => (
                  <option key={h} value={h}>
                    {h}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Kind" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={form.kind}
                onChange={(e) =>
                  setForm({ ...form, kind: e.target.value as 'PERMANENT' | 'TEMPORARY' })
                }
              >
                <option value="PERMANENT">Permanent</option>
                <option value="TEMPORARY">Temporary</option>
              </select>
            )}
          </Field>
          {form.kind === 'TEMPORARY' && (
            <Field label="Until" required error={errors.validTo}>
              {(id) => (
                <input
                  id={id}
                  className="input"
                  type="date"
                  value={form.validTo}
                  onChange={(e) => setForm({ ...form, validTo: e.target.value })}
                />
              )}
            </Field>
          )}
        </div>
        <Field label="Reason" required error={errors.reason}>
          {(id) => (
            <input
              id={id}
              className="input"
              maxLength={500}
              value={form.reason}
              onChange={(e) => setForm({ ...form, reason: e.target.value })}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

/** Changes the collector's remarks and tagging category A / B / C of an account (p.41). */
export function DetailsDialog({
  remarks,
  category,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<
  Omit<DialogProps, 'invoiceNos'> & {
    remarks?: string;
    category?: string;
    onSave: (remarks: string, category: string) => void;
  }
>) {
  const [text, setText] = useState(remarks ?? '');
  const [cat, setCat] = useState(category ?? '');
  return (
    <Modal
      title="Update Remarks and Category"
      open
      onClose={onClose}
      footer={
        <Footer label="Save" busy={busy} onClose={onClose} onConfirm={() => onSave(text, cat)} />
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <Field label="Tagging Category">
          {(id) => (
            <select id={id} className="select" value={cat} onChange={(e) => setCat(e.target.value)}>
              <option value="">None</option>
              <option value="A">A</option>
              <option value="B">B</option>
              <option value="C">C</option>
            </select>
          )}
        </Field>
        <TextArea label="Remarks" value={text} onChange={setText} />
      </div>
    </Modal>
  );
}
