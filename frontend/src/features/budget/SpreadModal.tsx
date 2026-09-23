import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { SEASONALITY, spreadWeighted, sum } from './budgetMath';

interface Props {
  open: boolean;
  initialAnnual: number;
  onClose: () => void;
  onApply: (months: number[]) => void;
}

/** Spreads an annual amount over the twelve months evenly or by a seasonality profile. */
export function SpreadModal({ open, initialAnnual, onClose, onApply }: Readonly<Props>) {
  const [annual, setAnnual] = useState(String(initialAnnual));
  const [profile, setProfile] = useState('Even');
  const weights = SEASONALITY[profile] ?? [];
  const preview = spreadWeighted(Number(annual) || 0, weights);

  return (
    <Modal
      title="Spread annual amount"
      open={open}
      onClose={onClose}
      footer={
        <Button variant="accent" onClick={() => onApply(preview)}>
          Apply to row
        </Button>
      }
    >
      <div className="form-grid">
        <Field label="Annual amount" required>
          {(id) => (
            <input
              id={id}
              className="input num"
              type="number"
              step="0.01"
              value={annual}
              onChange={(e) => setAnnual(e.target.value)}
            />
          )}
        </Field>
        <Field label="Seasonality" hint="Relative monthly weights">
          {(id) => (
            <select
              id={id}
              className="select"
              value={profile}
              onChange={(e) => setProfile(e.target.value)}
            >
              {Object.keys(SEASONALITY).map((name) => (
                <option key={name}>{name}</option>
              ))}
            </select>
          )}
        </Field>
      </div>
      <p className="muted">
        Monthly: {preview.map((m) => m.toFixed(2)).join(' · ')} (total {sum(preview).toFixed(2)})
      </p>
    </Modal>
  );
}
