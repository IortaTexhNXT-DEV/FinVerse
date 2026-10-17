import { useCallback, useState } from 'react';
import { mfaApi } from '@/api/mfa';
import type { MfaEnrolled } from '@/api/mfa';
import type { LoginResponse } from '@/api/types';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { CodeInput } from './CodeInput';
import { EnrolAuthenticator } from './EnrolAuthenticator';
import { RecoveryCodes } from './RecoveryCodes';

interface StepProps {
  /** The answer of the first step (mfaStep and mfaChallenge set). */
  pending: LoginResponse;
  /** Opens the session once the second factor is accepted. */
  onSignedIn: (result: LoginResponse) => void;
  onBack: () => void;
}

/** The code of the authenticator app (or a recovery code) after the password. */
function VerifyCode({ pending, onSignedIn, onBack }: Readonly<StepProps>) {
  const [code, setCode] = useState('');
  const [recovery, setRecovery] = useState(false);
  const [remember, setRemember] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  const days = pending.rememberDeviceDays ?? 0;
  const submit = () => {
    setBusy(true);
    setError(null);
    mfaApi
      .verify(pending.mfaChallenge ?? '', code.trim(), remember)
      .then(onSignedIn)
      .catch(setError)
      .finally(() => setBusy(false));
  };
  return (
    <form
      className="stack"
      onSubmit={(e) => {
        e.preventDefault();
        submit();
      }}
    >
      <p className="login-notice">
        {recovery
          ? 'Enter one of your recovery codes. Each code works once.'
          : 'Enter the six-digit code of your authenticator app.'}
      </p>
      <ErrorAlert error={error} />
      <Field label={recovery ? 'Recovery code' : 'Code'} required>
        {(id) => <CodeInput id={id} value={code} onChange={setCode} recovery={recovery} />}
      </Field>
      {days > 0 && (
        <label className="checkbox">
          <input
            type="checkbox"
            checked={remember}
            onChange={(e) => setRemember(e.target.checked)}
          />
          Do not ask again on this device for {days} days
        </label>
      )}
      <div className="login-links">
        <Button
          variant="ghost"
          className="login-link"
          onClick={() => {
            setRecovery(!recovery);
            setCode('');
          }}
        >
          {recovery ? 'Use the authenticator app' : 'Use a recovery code'}
        </Button>
      </div>
      <Button type="submit" variant="accent" busy={busy} disabled={code.trim() === ''}>
        Verify
      </Button>
      <Button variant="secondary" onClick={onBack}>
        Back to Login
      </Button>
    </form>
  );
}

/** Enrolment of the authenticator app at the first sign-in, then the recovery codes. */
function EnrolAtSignIn({ pending, onSignedIn, onBack }: Readonly<StepProps>) {
  const [enrolled, setEnrolled] = useState<MfaEnrolled | null>(null);
  const challenge = pending.mfaChallenge ?? '';
  const start = useCallback(() => mfaApi.startAtSignIn(challenge), [challenge]);
  const confirm = useCallback(
    (code: string) => mfaApi.confirmAtSignIn(challenge, code),
    [challenge],
  );
  if (enrolled !== null) {
    return (
      <div className="stack">
        <RecoveryCodes codes={enrolled.recoveryCodes} />
        <Button
          variant="accent"
          onClick={() => {
            if (enrolled.signIn !== undefined) {
              onSignedIn(enrolled.signIn);
            }
          }}
        >
          Continue
        </Button>
      </div>
    );
  }
  return (
    <div className="stack">
      <p className="login-notice">
        Your account needs a second factor. Set up an authenticator app on your phone to continue.
      </p>
      <EnrolAuthenticator start={start} confirm={confirm} onEnrolled={setEnrolled} />
      <Button variant="secondary" onClick={onBack}>
        Back to Login
      </Button>
    </div>
  );
}

/** The second step of a sign-in: the code, or the enrolment of the app when none is set up. */
export function SecondFactorStep(props: Readonly<StepProps>) {
  return props.pending.mfaStep === 'ENROL' ? (
    <EnrolAtSignIn {...props} />
  ) : (
    <VerifyCode {...props} />
  );
}
