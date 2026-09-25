import { useMutation } from '@tanstack/react-query';
import { Plus, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useCompanyId } from '@/context/workspaceContext';
import { formatAmount, today } from '@/utils/format';
import { draftErrors, draftTax, emptyDraft, receivedApi } from './receivedCertificates';
import type {
  CertificateDraft,
  CertificateKind,
  ReceivedCertificate,
} from './receivedCertificates';

type Line = CertificateDraft['lines'][number];

function Lines({
  lines,
  error,
  onChange,
}: Readonly<{ lines: Line[]; error?: string; onChange: (lines: Line[]) => void }>) {
  const set = (i: number, next: Partial<Line>) =>
    onChange(lines.map((l, j) => (j === i ? { ...l, ...next } : l)));
  return (
    <Field label="Income Payments" required error={error}>
      {() => (
        <div className="stack">
          {lines.map((l, i) => (
            <div key={String(i)} className="row">
              <select
                className="select"
                aria-label={`Kind ${String(i + 1)}`}
                value={l.kind}
                onChange={(e) => set(i, { kind: e.target.value as CertificateKind })}
              >
                <option value="COMMISSION">Commission</option>
                <option value="INCENTIVE">Incentive</option>
              </select>
              <input
                className="input"
                aria-label={`ATC ${String(i + 1)}`}
                maxLength={10}
                value={l.atc}
                onChange={(e) => set(i, { atc: e.target.value })}
              />
              <input
                className="input"
                aria-label={`Income ${String(i + 1)}`}
                type="number"
                step="0.01"
                placeholder="Income"
                value={l.income}
                onChange={(e) => set(i, { income: e.target.value })}
              />
              <input
                className="input"
                aria-label={`Tax withheld ${String(i + 1)}`}
                type="number"
                step="0.01"
                placeholder="Tax withheld"
                value={l.tax}
                onChange={(e) => set(i, { tax: e.target.value })}
              />
              <Button
                variant="ghost"
                size="sm"
                icon={<Trash2 size={14} />}
                disabled={lines.length === 1}
                onClick={() => onChange(lines.filter((_, j) => j !== i))}
              >
                Remove
              </Button>
            </div>
          ))}
          <div>
            <Button
              variant="secondary"
              size="sm"
              icon={<Plus size={14} />}
              onClick={() =>
                onChange([
                  ...lines,
                  { kind: 'INCENTIVE', atc: 'WC158', incomeNature: '', income: '', tax: '' },
                ])
              }
            >
              Add Income Payment
            </Button>
          </div>
        </div>
      )}
    </Field>
  );
}

function Text({
  label,
  value,
  error,
  required = false,
  type = 'text',
  onChange,
}: Readonly<{
  label: string;
  value: string;
  error?: string;
  required?: boolean;
  type?: string;
  onChange: (v: string) => void;
}>) {
  return (
    <Field label={label} required={required} error={error}>
      {(id) => (
        <input
          id={id}
          type={type}
          className="input"
          value={value}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
    </Field>
  );
}

/**
 * Records a BIR 2307 certificate received from an insurer (DIS 2.11.1-2.11.2): number, agent,
 * period covered, date received and the income payments with the tax withheld; recording posts
 * the move from AR-BIR on commission / incentives to AR-BIR on hand.
 */
export function RecordCertificateDialog({
  onDone,
  onClose,
}: Readonly<{ onDone: (c: ReceivedCertificate) => void; onClose: () => void }>) {
  const companyId = useCompanyId();
  const now = today();
  const [draft, setDraft] = useState<CertificateDraft>(emptyDraft(now));
  const [checked, setChecked] = useState(false);
  const errors = checked ? draftErrors(draft, now) : {};
  const change = (next: Partial<CertificateDraft>) => setDraft((d) => ({ ...d, ...next }));
  const record = useMutation({
    mutationFn: () => receivedApi.record(companyId, draft),
    onSuccess: onDone,
  });
  return (
    <Modal
      open
      title="Record Certificate Received"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={record.isPending}
            onClick={() => {
              setChecked(true);
              if (Object.keys(draftErrors(draft, now)).length === 0) {
                record.mutate();
              }
            }}
          >
            Record Certificate
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={record.error} />
        <div className="form-grid">
          <Text
            label="Certificate No."
            required
            value={draft.certificateNo}
            error={errors.certificateNo}
            onChange={(v) => change({ certificateNo: v })}
          />
          <Text
            label="Withholding Agent (Insurer Code)"
            required
            value={draft.agentCode}
            error={errors.agentCode}
            onChange={(v) => change({ agentCode: v })}
          />
          <Text
            label="Agent Name"
            required
            value={draft.agentName}
            error={errors.agentName}
            onChange={(v) => change({ agentName: v })}
          />
          <Text
            label="Agent TIN"
            value={draft.agentTin}
            onChange={(v) => change({ agentTin: v })}
          />
          <Text
            label="Period Covered From"
            type="date"
            required
            value={draft.periodFrom}
            onChange={(v) => change({ periodFrom: v })}
          />
          <Text
            label="Period Covered To"
            type="date"
            required
            value={draft.periodTo}
            error={errors.periodTo}
            onChange={(v) => change({ periodTo: v })}
          />
          <Text
            label="Date Received"
            type="date"
            required
            value={draft.receivedOn}
            error={errors.receivedOn}
            onChange={(v) => change({ receivedOn: v })}
          />
          <Text
            label="Reference (DV / Remittance)"
            value={draft.sourceRef}
            onChange={(v) => change({ sourceRef: v })}
          />
        </div>
        <Lines lines={draft.lines} error={errors.lines} onChange={(lines) => change({ lines })} />
        <p className="muted">Tax withheld: {formatAmount(draftTax(draft))}</p>
      </div>
    </Modal>
  );
}
