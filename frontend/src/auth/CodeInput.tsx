/** Input of a one-time code (six digits of the app, or a recovery code). */
export function CodeInput({
  id,
  value,
  onChange,
  recovery = false,
}: Readonly<{ id: string; value: string; onChange: (value: string) => void; recovery?: boolean }>) {
  return (
    <input
      id={id}
      className="input mfa-code"
      autoComplete="one-time-code"
      inputMode={recovery ? 'text' : 'numeric'}
      maxLength={recovery ? 11 : 6}
      placeholder={recovery ? 'xxxxx-xxxxx' : '123456'}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      required
    />
  );
}
