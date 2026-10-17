import { useState } from 'react';
import type { AccessReason } from '@/api/legacyInquiry';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Field } from '@/components/ui/Field';
import { REASONS } from './reason';

/** Asks the reason of the inquiry before the legacy archive is opened. */
export function ReasonCard({ onGiven }: Readonly<{ onGiven: (reason: AccessReason) => void }>) {
  const [code, setCode] = useState('');
  const [text, setText] = useState('');
  const incomplete = code === '' || (code === 'OTHER' && text.trim() === '');
  return (
    <Card title="Reason of the Inquiry">
      <p className="muted">
        The legacy archive holds closed client and financial records. Give the reason of this
        inquiry; it is kept for this session and written to the access log with each search, view,
        download and export.
      </p>
      <div className="form-grid">
        <Field label="Reason" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={code}
              onChange={(e) => setCode(e.target.value)}
            >
              <option value="">Choose a reason</option>
              {REASONS.map((r) => (
                <option key={r.value} value={r.value}>
                  {r.label}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Details" required={code === 'OTHER'} hint="Audit reference, request or case">
          {(id) => (
            <input
              id={id}
              className="input"
              value={text}
              maxLength={500}
              onChange={(e) => setText(e.target.value)}
            />
          )}
        </Field>
      </div>
      <Button
        variant="primary"
        disabled={incomplete}
        onClick={() => onGiven({ reasonCode: code, reasonText: text.trim() })}
      >
        Continue
      </Button>
    </Card>
  );
}
