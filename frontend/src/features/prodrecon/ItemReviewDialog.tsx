import { useState } from 'react';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import type { ReconFeedback, ReconItem } from './prodreconApi';
import { sideBySide } from './prodreconLogic';
import './prodrecon.css';

const EMPTY: ReconFeedback = {
  companyConcerned: '',
  instruction: '',
  insurerFeedback: '',
  marketingFeedback: '',
  disposition: '',
  forClosure: false,
};

/** The BDOI and insurer values of an item, differences highlighted (PRCID.013). */
export function SideBySide({ item }: Readonly<{ item: ReconItem }>) {
  return (
    <table className="table prc-diff">
      <caption className="visually-hidden">BDOI and insurer values</caption>
      <thead>
        <tr>
          <th scope="col">Field</th>
          <th scope="col">BDOI</th>
          <th scope="col">Insurer</th>
        </tr>
      </thead>
      <tbody>
        {sideBySide(item).map((row) => (
          <tr key={row.key} className={row.differs ? 'prc-diff-row' : undefined}>
            <th scope="row">{row.label}</th>
            <td>{row.bdoi || '—'}</td>
            <td>{row.insurer || '—'}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function TextField({
  label,
  value,
  disabled,
  onChange,
}: Readonly<{ label: string; value: string; disabled: boolean; onChange: (v: string) => void }>) {
  return (
    <Field label={label}>
      {(id) => (
        <textarea
          id={id}
          className="textarea"
          rows={2}
          maxLength={500}
          disabled={disabled}
          value={value}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
    </Field>
  );
}

/**
 * One reconciliation item: both sides compared and the feedback that follows it to closure
 * (company concerned, instruction, insurer and marketing feedback, disposition - PRCID.015-018).
 */
export function ItemReviewDialog({
  item,
  editable,
  busy,
  error,
  onClose,
  onSave,
  onSplit,
}: Readonly<{
  item: ReconItem;
  editable: boolean;
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onSave: (feedback: ReconFeedback) => void;
  onSplit?: () => void;
}>) {
  const [form, setForm] = useState<ReconFeedback>({ ...EMPTY, ...item.feedback });
  const set = (patch: Partial<ReconFeedback>) => setForm((f) => ({ ...f, ...patch }));
  return (
    <Modal
      title={`Review ${item.invoiceNo ?? item.insurer?.policyNo ?? 'Item ' + String(item.id)}`}
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          {editable && onSplit !== undefined && (
            <Button variant="ghost" onClick={onSplit}>
              Split Pairing
            </Button>
          )}
          {editable && (
            <Button busy={busy} onClick={() => onSave(form)}>
              Save Feedback
            </Button>
          )}
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <div className="row">
          <StatusBadge status={item.status} />
          {item.discrepancies.length > 0 && (
            <span className="muted">Differences: {item.discrepancies.join(', ')}</span>
          )}
        </div>
        <SideBySide item={item} />
        {item.insurerRemarks && <p className="muted">Insurer remarks: {item.insurerRemarks}</p>}
        <div className="form-grid">
          <Field label="Company Concerned">
            {(id) => (
              <LovSelect
                id={id}
                type="RECON_COMPANY_CONCERNED"
                value={form.companyConcerned ?? ''}
                disabled={!editable}
                onChange={(companyConcerned) => set({ companyConcerned })}
              />
            )}
          </Field>
          <Field label="Disposition">
            {(id) => (
              <LovSelect
                id={id}
                type="RECON_DISPOSITION"
                value={form.disposition ?? ''}
                disabled={!editable}
                onChange={(disposition) => set({ disposition })}
              />
            )}
          </Field>
        </div>
        <TextField
          label="Instruction"
          value={form.instruction ?? ''}
          disabled={!editable}
          onChange={(instruction) => set({ instruction })}
        />
        <TextField
          label="Insurer Feedback"
          value={form.insurerFeedback ?? ''}
          disabled={!editable}
          onChange={(insurerFeedback) => set({ insurerFeedback })}
        />
        <TextField
          label="Marketing Feedback"
          value={form.marketingFeedback ?? ''}
          disabled={!editable}
          onChange={(marketingFeedback) => set({ marketingFeedback })}
        />
        <label className="row">
          <input
            type="checkbox"
            checked={form.forClosure}
            disabled={!editable}
            onChange={(e) => set({ forClosure: e.target.checked })}
          />{' '}
          Ready for Closure
        </label>
      </div>
    </Modal>
  );
}
