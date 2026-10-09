import { ChevronDown, X } from 'lucide-react';
import { useEffect, useId, useMemo, useRef, useState } from 'react';
import type { KeyboardEvent, RefObject } from 'react';
import { filterOptions } from './comboOptions';
import type { ComboOption } from './comboOptions';

export interface ComboboxProps {
  id?: string;
  value: string;
  onChange: (value: string) => void;
  options: readonly ComboOption[];
  /** Text shown when nothing is chosen ("All" in a filter, "Select…" in a form). */
  placeholder?: string;
  /** In a filter: the empty choice is a value of its own ("All"), offered first in the list. */
  emptyLabel?: string;
  loading?: boolean;
  disabled?: boolean;
  required?: boolean;
  'aria-invalid'?: boolean;
  'aria-label'?: string;
}

interface ListState {
  open: boolean;
  text: string;
  active: number;
}

const CLOSED: ListState = { open: false, text: '', active: 0 };

/** Arrow keys move through the list; Enter chooses; Escape closes the list (not the dialog). */
function useListKeys(
  state: ListState,
  setState: (s: ListState) => void,
  shown: readonly ComboOption[],
  choose: (o: ComboOption) => void,
) {
  return (e: KeyboardEvent<HTMLInputElement>) => {
    const last = shown.length - 1;
    const moves: Record<string, number> = {
      ArrowDown: Math.min(state.active + 1, last),
      ArrowUp: Math.max(state.active - 1, 0),
      Home: 0,
      End: last,
    };
    if (e.key in moves) {
      e.preventDefault();
      setState({ ...state, open: true, active: state.open ? (moves[e.key] ?? 0) : 0 });
      return;
    }
    if (e.key === 'Enter' && state.open) {
      e.preventDefault();
      const option = shown[state.active];
      if (option !== undefined) {
        choose(option);
      }
      return;
    }
    if (e.key === 'Escape' && state.open) {
      e.preventDefault();
      e.stopPropagation();
      setState(CLOSED);
    }
  };
}

/** The open list of a Combobox: options are chosen with the mouse or from the field's keys. */
function OptionList({
  id,
  options,
  active,
  value,
  onChoose,
}: Readonly<{
  id: string;
  options: readonly ComboOption[];
  active: number;
  value: string;
  onChoose: (o: ComboOption) => void;
}>) {
  return (
    <div className="combobox-list" role="listbox" id={id}>
      {options.length === 0 && <div className="combobox-none">No match</div>}
      {options.map((o, i) => (
        <div
          key={o.value}
          id={`${id}-${String(i)}`}
          role="option"
          tabIndex={-1}
          aria-selected={o.value === value}
          className={i === active ? 'combobox-option active' : 'combobox-option'}
          onMouseDown={(e) => {
            e.preventDefault();
            onChoose(o);
          }}
        >
          <span>{o.label}</span>
          {o.hint !== undefined && <span className="combobox-hint">{o.hint}</span>}
        </div>
      ))}
    </div>
  );
}

/** Closes a popup on a mouse press outside its root. */
function useOutsideClose(open: boolean, root: RefObject<HTMLElement | null>, close: () => void) {
  useEffect(() => {
    if (!open) {
      return undefined;
    }
    const onDown = (e: MouseEvent) => {
      if (root.current !== null && !root.current.contains(e.target as Node)) {
        close();
      }
    };
    document.addEventListener('mousedown', onDown);
    return () => document.removeEventListener('mousedown', onDown);
  }, [open, root, close]);
}

/** The options with the empty choice of a filter ("All") first. */
function withEmpty(options: readonly ComboOption[], emptyLabel: string | undefined): ComboOption[] {
  return emptyLabel === undefined ? [...options] : [{ value: '', label: emptyLabel }, ...options];
}

/** The id of the highlighted option, read by screen readers. */
function activeIdOf(state: ListState, count: number, listId: string): string | undefined {
  return state.open && count > 0 ? `${listId}-${String(state.active)}` : undefined;
}

/** The text of the empty field. */
function placeholderOf(loading: boolean, emptyLabel: string | undefined, placeholder: string) {
  return loading ? 'Loading…' : (emptyLabel ?? placeholder);
}

/** A chosen value may be emptied in a filter ("All") or in an optional field. */
function clearable(value: string, filter: boolean, required: boolean, disabled: boolean): boolean {
  return value !== '' && (filter || !required) && !disabled;
}

/** The clear button of a chosen value that may be emptied, else the drop-down caret. */
function ClearOrCaret({
  clearable,
  onClear,
}: Readonly<{ clearable: boolean; onClear: () => void }>) {
  return clearable ? (
    <button
      type="button"
      className="combobox-clear"
      aria-label="Clear"
      title="Clear"
      onClick={onClear}
    >
      <X size={14} aria-hidden="true" />
    </button>
  ) : (
    <ChevronDown className="combobox-caret" size={16} aria-hidden="true" />
  );
}

/**
 * Searchable choice from a known list (list of values, units, users, products, insurers,
 * branches, statuses, codes, currencies): shows the name, searches name and code, and stores the
 * code. Keyboard: type to search, arrows to move, Enter to choose, Escape to close.
 */
export function Combobox({
  id,
  value,
  onChange,
  options,
  placeholder = 'Select…',
  emptyLabel,
  loading = false,
  disabled = false,
  required = false,
  ...aria
}: Readonly<ComboboxProps>) {
  const ownId = useId();
  const inputId = id ?? ownId;
  const listId = `${inputId}-list`;
  const [state, setState] = useState<ListState>(CLOSED);
  const root = useRef<HTMLDivElement>(null);
  const all = useMemo(() => withEmpty(options, emptyLabel), [options, emptyLabel]);
  const shown = useMemo(() => filterOptions(all, state.text), [all, state.text]);
  const selected = all.find((o) => o.value === value);
  const choose = (o: ComboOption) => {
    setState(CLOSED);
    onChange(o.value);
  };
  const onKeyDown = useListKeys(state, setState, shown, choose);
  useOutsideClose(state.open, root, () => setState(CLOSED));
  const activeId = activeIdOf(state, shown.length, listId);
  return (
    <div className="combobox" ref={root}>
      <input
        id={inputId}
        className="input combobox-input"
        role="combobox"
        aria-expanded={state.open}
        aria-controls={listId}
        aria-autocomplete="list"
        aria-activedescendant={activeId}
        autoComplete="off"
        disabled={disabled}
        required={required}
        placeholder={placeholderOf(loading, emptyLabel, placeholder)}
        value={state.open ? state.text : (selected?.label ?? value)}
        onFocus={(e) => e.currentTarget.select()}
        onClick={() => setState({ open: true, text: '', active: 0 })}
        onChange={(e) => setState({ open: true, text: e.target.value, active: 0 })}
        onBlur={() => setState(CLOSED)}
        onKeyDown={onKeyDown}
        {...aria}
      />
      <ClearOrCaret
        clearable={clearable(value, emptyLabel !== undefined, required, disabled)}
        onClear={() => onChange('')}
      />
      {state.open && (
        <OptionList
          id={listId}
          options={shown}
          active={state.active}
          value={value}
          onChoose={choose}
        />
      )}
    </div>
  );
}
