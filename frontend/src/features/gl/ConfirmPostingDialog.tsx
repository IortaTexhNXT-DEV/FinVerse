import { AlertTriangle } from 'lucide-react';
import type { ReactNode } from 'react';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';

/** One fact shown in the confirmation (label and value). */
export interface ConfirmFact {
  label: string;
  value: ReactNode;
}

interface Props {
  open: boolean;
  title: string;
  intro: string;
  facts: ConfirmFact[];
  warnings?: string[];
  confirmLabel: string;
  busy?: boolean;
  error?: unknown;
  onConfirm: () => void;
  onClose: () => void;
}

/**
 * Confirmation of the details before a voucher moves to the next step of its workflow (FRBS
 * 2.5.10, 2.8.3): the key facts (totals, lines, dates) and the non-blocking warnings of the
 * negative balance control (FRBS 2.5.5, 2.8.4).
 */
export function ConfirmPostingDialog(p: Readonly<Props>) {
  const warnings = p.warnings ?? [];
  return (
    <Modal
      title={p.title}
      open={p.open}
      onClose={p.onClose}
      footer={
        <>
          <Button variant="secondary" onClick={p.onClose}>
            Cancel
          </Button>
          <Button variant="accent" busy={p.busy} onClick={p.onConfirm}>
            {p.confirmLabel}
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={p.error} />
        <p>{p.intro}</p>
        <dl className="form-grid" style={{ margin: 0 }}>
          {p.facts.map((f) => (
            <div key={f.label}>
              <dt className="kpi-label">{f.label}</dt>
              <dd style={{ margin: 0 }}>{f.value}</dd>
            </div>
          ))}
        </dl>
        {warnings.length > 0 && (
          <div className="alert warning" role="status">
            <AlertTriangle size={16} aria-hidden="true" /> Please check before you continue:
            <ul>
              {warnings.map((w) => (
                <li key={w}>{w}</li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </Modal>
  );
}
