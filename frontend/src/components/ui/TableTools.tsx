import { ArrowDown, ArrowUp, Columns3, Rows3, Rows4 } from 'lucide-react';
import { useEffect, useId, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { moveColumn, toggleColumn } from './tableColumns';
import type { ColumnChoice } from './tableColumns';

interface ToolColumn {
  key: string;
  header: ReactNode;
}

interface TableToolsProps {
  columns: readonly ToolColumn[];
  choice: ColumnChoice;
  locked: ReadonlySet<string>;
  onChange: (choice: ColumnChoice) => void;
  onReset: () => void;
}

/** The chooser: every column with a show / hide box and move up / down buttons, and Reset. */
function Chooser({
  columns,
  choice,
  locked,
  onChange,
  onReset,
  onClose,
  id,
}: Readonly<TableToolsProps & { onClose: () => void; id: string }>) {
  const byKey = new Map(columns.map((c) => [c.key, c]));
  const keys = choice.order.filter((k) => byKey.has(k) && k !== 'select');
  return (
    <div className="column-chooser" role="dialog" aria-label="Columns of the list" id={id}>
      <ul>
        {keys.map((key, i) => {
          const column = byKey.get(key);
          const fixed = locked.has(key);
          const label = column?.header === '' ? 'Actions' : column?.header;
          const text = typeof label === 'string' ? label : key;
          return (
            <li key={key}>
              <label className={fixed ? 'checkbox fixed' : 'checkbox'}>
                <input
                  type="checkbox"
                  checked={fixed || !choice.hidden.includes(key)}
                  disabled={fixed}
                  onChange={() => onChange(toggleColumn(choice, key))}
                />
                <span className="column-chooser-name">{label}</span>
              </label>
              <span className="column-chooser-move">
                <button
                  type="button"
                  className="btn btn-ghost btn-sm"
                  aria-label={`Move ${text} up`}
                  title="Move up"
                  disabled={i === 0}
                  onClick={() => onChange(moveColumn(choice, key, -1))}
                >
                  <ArrowUp size={14} aria-hidden="true" />
                </button>
                <button
                  type="button"
                  className="btn btn-ghost btn-sm"
                  aria-label={`Move ${text} down`}
                  title="Move down"
                  disabled={i === keys.length - 1}
                  onClick={() => onChange(moveColumn(choice, key, 1))}
                >
                  <ArrowDown size={14} aria-hidden="true" />
                </button>
              </span>
            </li>
          );
        })}
      </ul>
      <footer>
        <button type="button" className="link-button" onClick={onReset}>
          Reset to Default
        </button>
        <button type="button" className="btn btn-secondary btn-sm" onClick={onClose}>
          Done
        </button>
      </footer>
    </div>
  );
}

/**
 * The tools of a wide list, right-aligned above it: Compact / Comfortable rows and the column
 * chooser (show, hide and order the columns; saved per user in the browser, with a reset).
 */
export function TableTools(props: Readonly<TableToolsProps>) {
  const { choice, onChange, columns, locked } = props;
  const [open, setOpen] = useState(false);
  const id = useId();
  const root = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (!open) {
      return undefined;
    }
    const close = (e: MouseEvent) => {
      if (root.current !== null && !root.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    const escape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', close);
    document.addEventListener('keydown', escape);
    return () => {
      document.removeEventListener('mousedown', close);
      document.removeEventListener('keydown', escape);
    };
  }, [open]);
  const hidden = columns.filter((c) => !locked.has(c.key) && choice.hidden.includes(c.key)).length;
  const compact = choice.density === 'compact';
  return (
    <div className="table-tools" ref={root}>
      <button
        type="button"
        className="btn btn-ghost btn-sm"
        aria-pressed={compact}
        title={compact ? 'Comfortable rows' : 'Compact rows'}
        onClick={() => onChange({ ...choice, density: compact ? 'comfortable' : 'compact' })}
      >
        {compact ? <Rows3 size={16} aria-hidden="true" /> : <Rows4 size={16} aria-hidden="true" />}
        {compact ? 'Comfortable' : 'Compact'}
      </button>
      <button
        type="button"
        className="btn btn-ghost btn-sm"
        aria-expanded={open}
        aria-controls={open ? id : undefined}
        onClick={() => setOpen((o) => !o)}
      >
        <Columns3 size={16} aria-hidden="true" />
        Columns{hidden > 0 ? ` (${String(hidden)} hidden)` : ''}
      </button>
      {open && <Chooser {...props} id={id} onClose={() => setOpen(false)} />}
    </div>
  );
}
