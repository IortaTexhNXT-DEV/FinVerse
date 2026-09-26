import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { crmApi } from '@/api/crm';
import type { ClientSummary } from '@/api/crm';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';

interface ClientPickerProps {
  id: string;
  /** Selected client id. */
  value: number | undefined;
  onChange: (client: ClientSummary | undefined) => void;
  /** Allow prospects (quotations); false requires confirmed clients (placement, booking). */
  allowProspect?: boolean;
  disabled?: boolean;
}

const MIN_TERM = 2;

/**
 * Searchable client picker (code or name). Shows the client's status so the user sees at once
 * whether a prospect still needs onboarding (BRNB.029/101).
 */
export function ClientPicker({
  id,
  value,
  onChange,
  allowProspect = true,
  disabled = false,
}: Readonly<ClientPickerProps>) {
  const companyId = useCompanyId();
  const [term, setTerm] = useState('');
  const selected = useQuery({
    queryKey: ['crm', 'summary', value],
    queryFn: () => crmApi.summary(value ?? 0),
    enabled: value !== undefined && value > 0,
  });
  const results = useQuery({
    queryKey: ['crm', 'lookup', companyId, term],
    queryFn: () => crmApi.lookup(companyId, term),
    enabled: term.trim().length >= MIN_TERM && companyId > 0,
  });
  const options = (results.data ?? []).filter((c) => allowProspect || c.status === 'CONFIRMED');
  if (selected.data && value !== undefined) {
    return (
      <div className="client-picked">
        <span>
          <strong>{selected.data.displayName}</strong>{' '}
          <span className="muted mono">{selected.data.code}</span>{' '}
          <StatusBadge status={selected.data.status} />
        </span>
        {!disabled && (
          <button
            type="button"
            className="btn btn-ghost btn-sm"
            onClick={() => onChange(undefined)}
          >
            Change
          </button>
        )}
      </div>
    );
  }
  return (
    <div className="client-picker">
      <input
        id={id}
        className="input"
        placeholder="Type a client code or name…"
        value={term}
        disabled={disabled}
        autoComplete="off"
        onChange={(e) => setTerm(e.target.value)}
      />
      {term.trim().length >= MIN_TERM && (
        <ul className="client-options" aria-label="Matching clients">
          {options.map((c) => (
            <li key={c.id}>
              <button
                type="button"
                className="client-option"
                onClick={() => {
                  setTerm('');
                  onChange(c);
                }}
              >
                <span>{c.displayName}</span>
                <span className="muted mono">{c.code}</span>
                <StatusBadge status={c.status} />
              </button>
            </li>
          ))}
          {results.isSuccess && options.length === 0 && (
            <li className="muted client-option-empty">No matching client.</li>
          )}
        </ul>
      )}
    </div>
  );
}
