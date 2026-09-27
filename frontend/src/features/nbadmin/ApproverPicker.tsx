import { useQuery } from '@tanstack/react-query';
import { ArrowUp, X } from 'lucide-react';
import { nbadminApi } from '@/api/nbadmin';
import type { AccessUserType } from '@/api/nbadmin';
import { Button } from '@/components/ui/Button';
import { Field } from '@/components/ui/Field';

interface ApproverPickerProps {
  userType: AccessUserType;
  /** User the request is about (never an approver). */
  subject?: string;
  value: string[];
  onChange: (approvers: string[]) => void;
  /** Group-profile requests have approvers in order; user requests one. */
  multiple: boolean;
  error?: string;
}

/**
 * The approver drop-down (BRD 1.002.1.1.3, 3.002.x; FR-UA-015, FR-UA-044): eligible approvers
 * only, never the requester nor the user concerned; group-profile requests list several in order.
 */
export function ApproverPicker({
  userType,
  subject,
  value,
  onChange,
  multiple,
  error,
}: Readonly<ApproverPickerProps>) {
  const options = useQuery({
    queryKey: ['nbadmin', 'approvers', userType, subject],
    queryFn: () => nbadminApi.approvers(userType, subject === '' ? undefined : subject),
  });
  const eligible = options.data ?? [];
  const nameOf = (u: string) => eligible.find((o) => o.username === u)?.fullName ?? u;
  if (!multiple) {
    return (
      <Field label="Approver" required error={error} hint="The approver you choose is notified.">
        {(id) => (
          <select
            id={id}
            className="select"
            value={value[0] ?? ''}
            onChange={(e) => onChange(e.target.value === '' ? [] : [e.target.value])}
          >
            <option value="">Select the approver…</option>
            {eligible.map((o) => (
              <option key={o.username} value={o.username}>
                {o.fullName} ({o.username})
              </option>
            ))}
          </select>
        )}
      </Field>
    );
  }
  const move = (index: number) => {
    const next = [...value];
    const [moved] = next.splice(index, 1);
    next.splice(index - 1, 0, ...(moved === undefined ? [] : [moved]));
    onChange(next);
  };
  return (
    <Field
      label="Approvers in Order"
      required
      error={error}
      hint="Each approver decides in turn; the first is notified on submission."
    >
      {(id) => (
        <div className="stack">
          {value.length > 0 && (
            <ol className="stack">
              {value.map((u, i) => (
                <li key={u} className="row">
                  <span>
                    {nameOf(u)} ({u})
                  </span>
                  <span className="spacer" />
                  {i > 0 && (
                    <Button
                      size="sm"
                      variant="ghost"
                      icon={<ArrowUp size={14} />}
                      aria-label={`Move ${u} up`}
                      onClick={() => move(i)}
                    />
                  )}
                  <Button
                    size="sm"
                    variant="ghost"
                    icon={<X size={14} />}
                    aria-label={`Remove ${u}`}
                    onClick={() => onChange(value.filter((x) => x !== u))}
                  />
                </li>
              ))}
            </ol>
          )}
          <select
            id={id}
            className="select"
            value=""
            onChange={(e) => e.target.value !== '' && onChange([...value, e.target.value])}
          >
            <option value="">Add an approver…</option>
            {eligible
              .filter((o) => !value.includes(o.username))
              .map((o) => (
                <option key={o.username} value={o.username}>
                  {o.fullName} ({o.username})
                </option>
              ))}
          </select>
        </div>
      )}
    </Field>
  );
}
