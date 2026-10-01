import { Lock } from 'lucide-react';
import { TypedInput } from '@/components/ui/DateInput';
import { UserName } from '@/components/ui/UserName';
import { formatDateTime } from '@/utils/format';
import { useDisplayName } from '@/components/ui/useDisplayName';
import type { DetailField } from './collectionsLogic';

/** Inputs of the collector's dialogs: handlers by name, hand-off details by label. */

/** The handlers of a drop-down by name (the login is the value, never shown). */
export function HandlerOptions({ handlers }: Readonly<{ handlers: readonly string[] }>) {
  const name = useDisplayName();
  return (
    <>
      {handlers.map((h) => (
        <option key={h} value={h}>
          {name(h)}
        </option>
      ))}
    </>
  );
}

/** A hand-off detail: a drop-down of labels, or a text, number or date input (dates on the BIBS date picker, dd-MMM-yyyy). */
export function DetailInput({
  id,
  field,
  value,
  onChange,
}: Readonly<{ id: string; field: DetailField; value: string; onChange: (v: string) => void }>) {
  if (field.type === 'select') {
    return (
      <select id={id} className="select" value={value} onChange={(e) => onChange(e.target.value)}>
        <option value="">Select…</option>
        {(field.options ?? []).map((o) => (
          <option key={o.code} value={o.code}>
            {o.label}
          </option>
        ))}
      </select>
    );
  }
  return (
    <TypedInput
      id={id}
      className="input"
      type={field.type}
      value={value}
      onChange={(e) => onChange(e.target.value)}
    />
  );
}

/** The "<user> is editing since" banner of an account another user has open (one text, one space). */
export function LockBanner({ login, since }: Readonly<{ login?: string; since?: string }>) {
  return (
    <div className="clx-lock-banner" role="status">
      <Lock size={16} aria-hidden="true" />
      <span>
        <UserName login={login} /> is editing since {formatDateTime(since ?? '')}
      </span>
    </div>
  );
}
