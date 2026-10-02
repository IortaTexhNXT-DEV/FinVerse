import { useId, useMemo, useState } from 'react';
import { parseSelection, selectionValue } from './multiSelect';

/** An option of a multi-select criterion. */
export interface MultiOption {
  code: string;
  label: string;
}

interface MultiSelectFilterProps {
  label: string;
  options: readonly MultiOption[];
  /** Wire value: '' for all, "A,B" for a list, "!A,B" for all except the list. */
  value: string;
  onChange: (value: string) => void;
}

/**
 * A multi-select criterion of the report and list filters (BRD 1.003.3.1: single, multiple, all
 * except and all, with a search in the list): tick the codes and choose whether they are included
 * or excluded.
 */
export function MultiSelectFilter({
  label,
  options,
  value,
  onChange,
}: Readonly<MultiSelectFilterProps>) {
  const id = useId();
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState('');
  const { except, codes } = parseSelection(value);
  const shown = useMemo(() => {
    const term = search.trim().toLowerCase();
    return term === ''
      ? options
      : options.filter(
          (o) => o.label.toLowerCase().includes(term) || o.code.toLowerCase().includes(term),
        );
  }, [options, search]);
  const toggle = (code: string) => {
    const next = codes.includes(code) ? codes.filter((c) => c !== code) : [...codes, code];
    onChange(selectionValue(except, next));
  };
  let summary = 'All';
  if (codes.length > 0) {
    summary = `${except ? 'All except ' : ''}${String(codes.length)} selected`;
  }
  return (
    <div className="multi-select">
      <label className="field-label" htmlFor={id}>
        {label}
      </label>
      <button
        id={id}
        type="button"
        className="select multi-select-toggle"
        aria-expanded={open}
        onClick={() => setOpen((o) => !o)}
      >
        {summary}
      </button>
      {open && (
        <div className="multi-select-panel" role="group" aria-label={label}>
          <input
            className="input"
            placeholder={`Search ${label.toLowerCase()}`}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <label className="checkbox">
            <input
              type="checkbox"
              checked={except}
              onChange={(e) => onChange(selectionValue(e.target.checked, codes))}
            />{' '}
            All except the ticked ones
          </label>
          <ul className="multi-select-options">
            {shown.map((o) => (
              <li key={o.code}>
                <label className="checkbox">
                  <input
                    type="checkbox"
                    checked={codes.includes(o.code)}
                    onChange={() => toggle(o.code)}
                  />{' '}
                  {o.label}
                </label>
              </li>
            ))}
          </ul>
          <button type="button" className="btn btn-ghost" onClick={() => onChange('')}>
            Clear
          </button>
        </div>
      )}
    </div>
  );
}
