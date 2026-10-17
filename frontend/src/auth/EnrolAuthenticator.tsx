import { useEffect, useState } from 'react';
import type { MfaEnrolled, MfaEnrolment } from '@/api/mfa';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { CodeInput } from './CodeInput';

interface EnrolProps {
  /** Starts the enrolment and gives the secret. */
  start: () => Promise<MfaEnrolment>;
  /** Confirms the enrolment with the first code of the app. */
  confirm: (code: string) => Promise<MfaEnrolled>;
  onEnrolled: (enrolled: MfaEnrolled) => void;
  submitLabel?: string;
}

/**
 * Enrolment of an authenticator app: the QR code to scan (or the key to type), then the first
 * code of the app to confirm it. Used at the first sign-in when the second factor is required and
 * on My Profile.
 */
export function EnrolAuthenticator({
  start,
  confirm,
  onEnrolled,
  submitLabel = 'Confirm',
}: Readonly<EnrolProps>) {
  const [enrolment, setEnrolment] = useState<MfaEnrolment | null>(null);
  const [code, setCode] = useState('');
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let current = true;
    start()
      .then((e) => {
        if (current) {
          setEnrolment(e);
        }
      })
      .catch((e: unknown) => {
        if (current) {
          setError(e);
        }
      });
    return () => {
      current = false;
    };
  }, [start]);

  const submit = () => {
    setBusy(true);
    setError(null);
    confirm(code.trim())
      .then(onEnrolled)
      .catch(setError)
      .finally(() => setBusy(false));
  };

  if (enrolment === null) {
    return error === null ? (
      <span className="spinner" aria-label="Preparing the enrolment" />
    ) : (
      <ErrorAlert error={error} />
    );
  }
  return (
    <form
      className="stack"
      onSubmit={(e) => {
        e.preventDefault();
        submit();
      }}
    >
      <p className="login-notice">
        Scan the code with your authenticator app, or type the key into it, then enter the six-digit
        code the app shows.
      </p>
      <img className="mfa-qr" src={enrolment.qrCode} alt="QR code of the authenticator app key" />
      <p className="login-notice">
        Key: <code className="mfa-key">{enrolment.secret}</code>
      </p>
      <ErrorAlert error={error} />
      <Field label="Code from the app" required>
        {(id) => <CodeInput id={id} value={code} onChange={setCode} />}
      </Field>
      <Button type="submit" variant="accent" busy={busy} disabled={code.trim().length < 6}>
        {submitLabel}
      </Button>
    </form>
  );
}
