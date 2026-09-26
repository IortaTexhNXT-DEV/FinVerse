import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { formatAmount, today } from '@/utils/format';
import { frbsApi } from './api';
import type { ServiceFeeLine } from './api';

function dateError(value: string, now: string, label: string): string | undefined {
  if (value === '') {
    return `Enter the ${label}`;
  }
  return value > now ? 'The date cannot be in the future' : undefined;
}

/** Tags a line released: the date the recipient was credited (FRBS 2.10.2). */
export function ReleaseDialog({
  line,
  onDone,
  onClose,
}: Readonly<{ line: ServiceFeeLine; onDone: (l: ServiceFeeLine) => void; onClose: () => void }>) {
  const now = today();
  const [on, setOn] = useState(now);
  const [checked, setChecked] = useState(false);
  const error = checked ? dateError(on, now, 'release date') : undefined;
  const release = useMutation({
    mutationFn: () => frbsApi.release(line.id, on),
    onSuccess: onDone,
  });
  return (
    <Modal
      open
      title={`Tag Released · Line ${String(line.lineNo)}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={release.isPending}
            onClick={() => {
              setChecked(true);
              if (dateError(on, now, 'release date') === undefined) {
                release.mutate();
              }
            }}
          >
            Tag Released
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={release.error} />
        <p>
          {line.payeeName}: {line.amounts.currency} {formatAmount(line.amounts.fee)}
        </p>
        <Field label="Released On" required error={error} hint="Date the recipient was credited">
          {(id) => (
            <input
              id={id}
              type="date"
              className="input"
              value={on}
              onChange={(e) => setOn(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

/** Tags a line liquidated with the unit's liquidation report (FRBS 2.10.1-2.10.2). */
export function LiquidateDialog({
  line,
  onDone,
  onClose,
}: Readonly<{ line: ServiceFeeLine; onDone: (l: ServiceFeeLine) => void; onClose: () => void }>) {
  const now = today();
  const [on, setOn] = useState(now);
  const [remarks, setRemarks] = useState('');
  const [file, setFile] = useState<File>();
  const [checked, setChecked] = useState(false);
  const dateMessage = checked ? dateError(on, now, 'liquidation date') : undefined;
  const fileMessage = checked && file === undefined ? 'Attach the liquidation report' : undefined;
  const liquidate = useMutation({
    mutationFn: (report: File) => frbsApi.liquidate(line.id, on, remarks.trim(), report),
    onSuccess: onDone,
  });
  return (
    <Modal
      open
      title={`Tag Liquidated · Line ${String(line.lineNo)}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={liquidate.isPending}
            onClick={() => {
              setChecked(true);
              if (file !== undefined && dateError(on, now, 'liquidation date') === undefined) {
                liquidate.mutate(file);
              }
            }}
          >
            Tag Liquidated
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={liquidate.error} />
        <p>
          {line.payeeName}: released {line.tags.releasedOn ?? ''}
        </p>
        <div className="frbs-form">
          <Field label="Liquidated On" required error={dateMessage}>
            {(id) => (
              <input
                id={id}
                type="date"
                className="input"
                value={on}
                onChange={(e) => setOn(e.target.value)}
              />
            )}
          </Field>
          <Field
            label="Liquidation Report"
            required
            error={fileMessage}
            hint="PDF, image or spreadsheet"
          >
            {(id) => (
              <input
                id={id}
                type="file"
                className="input"
                onChange={(e) => setFile(e.target.files?.[0])}
              />
            )}
          </Field>
          <div className="frbs-form-wide">
            <Field label="Remarks">
              {(id) => (
                <input
                  id={id}
                  className="input"
                  maxLength={250}
                  value={remarks}
                  onChange={(e) => setRemarks(e.target.value)}
                />
              )}
            </Field>
          </div>
        </div>
      </div>
    </Modal>
  );
}
