import { CalendarDays } from 'lucide-react';
import { useRef, useState } from 'react';
import type { InputHTMLAttributes } from 'react';
import { formatMonth, parseMonthText } from '@/utils/dateText';

type MonthInputProps = Omit<InputHTMLAttributes<HTMLInputElement>, 'type'>;

function setNativeValue(el: HTMLInputElement, value: string): void {
  Reflect.set(HTMLInputElement.prototype, 'value', value, el);
}

/**
 * The month picker of BIBS, the month version of {@link DateInput}: a month typed and shown as
 * MMM-yyyy (Sep-2026; 09/2026 and 2026-09 are accepted too), with a calendar button. The value
 * stays ISO (yyyy-mm) and `onChange` receives the change event of the underlying month input.
 */
export function MonthInput({
  id,
  value,
  onChange,
  min,
  max,
  name,
  className = 'input',
  disabled,
  readOnly,
  required,
  placeholder = 'MMM-yyyy',
  ...rest
}: Readonly<MonthInputProps>) {
  const native = useRef<HTMLInputElement>(null);
  const [text, setText] = useState<string | null>(null);
  const iso = typeof value === 'string' ? value : undefined;
  const shown = text ?? formatMonth(iso ?? '');
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
          const parsed = parseMonthText(e.target.value);
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
        aria-label="Choose month"
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
        type="month"
        className="date-input-native"
        tabIndex={-1}
        aria-hidden="true"
        name={name}
        value={iso}
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
