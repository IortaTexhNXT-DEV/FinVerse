import { CalendarDays } from 'lucide-react';
import { useRef, useState } from 'react';
import type { InputHTMLAttributes } from 'react';
import { formatDate } from '@/utils/format';
import { parseDateText } from '@/utils/dateText';
import { MonthInput } from './MonthInput';

type DateInputProps = Omit<InputHTMLAttributes<HTMLInputElement>, 'type'>;

/** Sets an input's value the way the browser does, so React sees the change event. */
function setNativeValue(el: HTMLInputElement, value: string): void {
  Reflect.set(HTMLInputElement.prototype, 'value', value, el);
}

/**
 * The one date picker of BIBS: a date typed and shown as dd-MMM-yyyy (23-Sep-2026; 23/09/2026 and
 * 2026-09-23 are accepted too), with a calendar button. A drop-in for `<input type="date">`: the
 * value stays ISO (yyyy-mm-dd) and `onChange` receives the change event of the underlying date
 * input, so `e.target.value` is the ISO date.
 */
export function DateInput({
  id,
  value,
  defaultValue,
  onChange,
  min,
  max,
  name,
  className = 'input',
  disabled,
  readOnly,
  required,
  placeholder = 'dd-MMM-yyyy',
  ...rest
}: Readonly<DateInputProps>) {
  const native = useRef<HTMLInputElement>(null);
  const [text, setText] = useState<string | null>(null);
  const iso = typeof value === 'string' ? value : undefined;
  const shown = text ?? formatDate(iso ?? (typeof defaultValue === 'string' ? defaultValue : ''));

  const emit = (next: string) => {
    const el = native.current;
    if (el === null || el.value === next) {
      return;
    }
    setNativeValue(el, next);
    el.dispatchEvent(new Event('input', { bubbles: true }));
  };

  return (
    <span className="date-input">
      <input
        {...rest}
        id={id}
        className={className}
        value={shown}
        placeholder={placeholder}
        disabled={disabled}
        readOnly={readOnly}
        required={required}
        inputMode="text"
        autoComplete="off"
        onChange={(e) => {
          setText(e.target.value);
          const parsed = parseDateText(e.target.value);
          if (parsed !== undefined) {
            emit(parsed);
          }
        }}
        onBlur={(e) => {
          setText(null);
          rest.onBlur?.(e);
        }}
      />
      <button
        type="button"
        className="date-input-button"
        aria-label="Choose date"
        disabled={disabled === true || readOnly === true}
        onClick={() => {
          try {
            native.current?.showPicker();
          } catch {
            native.current?.focus();
          }
        }}
      >
        <CalendarDays size={16} aria-hidden="true" />
      </button>
      <input
        ref={native}
        type="date"
        className="date-input-native"
        tabIndex={-1}
        aria-hidden="true"
        name={name}
        value={iso}
        defaultValue={iso === undefined ? defaultValue : undefined}
        min={min}
        max={max}
        disabled={disabled}
        onChange={(e) => {
          setText(null);
          onChange?.(e);
        }}
      />
    </span>
  );
}

/**
 * An input of any type where a date uses the BIBS date picker ({@link DateInput}); for the form
 * field wrappers that take the input type as a prop.
 */
export function TypedInput({ type, ...props }: Readonly<InputHTMLAttributes<HTMLInputElement>>) {
  if (type === 'date') {
    return <DateInput {...props} />;
  }
  if (type === 'month') {
    return <MonthInput {...props} />;
  }
  return <input type={type} {...props} />;
}
