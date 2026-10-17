import { KeyRound } from 'lucide-react';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { passwordProblems } from './passwordRules';

interface PasswordForm {
  current: string;
  next: string;
  confirm: string;
}

const EMPTY: PasswordForm = { current: '', next: '', confirm: '' };

interface PasswordChangeFormProps {
  /** Ask for the current password (not with a reset link). */
  requireCurrent: boolean;
  submitLabel: string;
  busy: boolean;
  error: unknown;
  onSubmit: (current: string, next: string) => Promise<unknown>;
  /** Rules of the server shown under the fields (history, minimum age). */
  hint?: string;
  /** Lay the fields out in a grid (profile page) instead of a column (sign-in pages). */
  grid?: boolean;
}

/**
 * Current, new and confirmed password with the policy checked as the user types (UAM-NFR-31: 10+
 * characters with upper and lower case, a digit and a symbol); the server adds the history and the
 * minimum age (UAM-NFR-36). The fields are cleared after a successful change.
 */
export function PasswordChangeForm({
  requireCurrent,
  submitLabel,
  busy,
  error,
  onSubmit,
  hint,
  grid = false,
}: Readonly<PasswordChangeFormProps>) {
  const [form, setForm] = useState<PasswordForm>(EMPTY);
  const problems = passwordProblems(form.next, form.confirm);
  const fields: [keyof PasswordForm, string, string][] = [
    ['next', 'New password', 'new-password'],
    ['confirm', 'Confirm new password', 'new-password'],
  ];
  if (requireCurrent) {
    fields.unshift(['current', 'Current password', 'current-password']);
  }
  const submit = () => {
    onSubmit(form.current, form.next)
      .then(() => setForm(EMPTY))
      .catch(() => undefined);
  };
  return (
    <form
      className="stack"
      onSubmit={(e) => {
        e.preventDefault();
        submit();
      }}
    >
      <ErrorAlert error={error} />
      <div className={grid ? 'form-grid' : 'stack'}>
        {fields.map(([key, label, autoComplete]) => (
          <Field key={key} label={label} required>
            {(id) => (
              <input
                id={id}
                className="input"
                type="password"
                autoComplete={autoComplete}
                value={form[key]}
                onChange={(e) => setForm({ ...form, [key]: e.target.value })}
              />
            )}
          </Field>
        ))}
      </div>
      {form.next !== '' && problems.length > 0 && (
        <div className="muted" role="status">
          Still needed:
          <ul>
            {problems.map((p) => (
              <li key={p}>{p}</li>
            ))}
          </ul>
        </div>
      )}
      {hint !== undefined && <p className="muted">{hint}</p>}
      <div>
        <Button
          type="submit"
          variant="accent"
          icon={<KeyRound size={16} />}
          busy={busy}
          disabled={(requireCurrent && form.current === '') || problems.length > 0}
        >
          {submitLabel}
        </Button>
      </div>
    </form>
  );
}
