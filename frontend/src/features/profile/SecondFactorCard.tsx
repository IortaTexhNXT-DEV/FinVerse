import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useState } from 'react';
import { mfaApi } from '@/api/mfa';
import type { MfaEnrolled, MyMfaStatus } from '@/api/mfa';
import { CodeInput } from '@/auth/CodeInput';
import { EnrolAuthenticator } from '@/auth/EnrolAuthenticator';
import { RecoveryCodes } from '@/auth/RecoveryCodes';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { Notice } from '@/components/ui/Notice';
import { formatDateTime } from '@/utils/format';

type Dialog = 'enrol' | 'codes' | null;

/** New recovery codes after a code of the app. */
function NewCodes({ onDone }: Readonly<{ onDone: (codes: string[]) => void }>) {
  const [code, setCode] = useState('');
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  return (
    <form
      className="stack"
      onSubmit={(e) => {
        e.preventDefault();
        setBusy(true);
        setError(null);
        mfaApi
          .newRecoveryCodes(code.trim())
          .then(onDone)
          .catch(setError)
          .finally(() => setBusy(false));
      }}
    >
      <p className="muted">
        Enter a code of your authenticator app. The new codes replace all earlier recovery codes.
      </p>
      <ErrorAlert error={error} />
      <Field label="Code from the app" required>
        {(id) => <CodeInput id={id} value={code} onChange={setCode} />}
      </Field>
      <Button type="submit" variant="primary" busy={busy} disabled={code.trim().length < 6}>
        Get New Codes
      </Button>
    </form>
  );
}

/** What the card says of the user's second factor. */
function SecondFactorFacts({ status }: Readonly<{ status: MyMfaStatus }>) {
  const facts = status.enrolled
    ? `Set up ${formatDateTime(status.enrolledAt)}. Last used ${formatDateTime(status.lastUsedAt) || '—'}. ${status.recoveryCodesLeft} recovery codes left.`
    : 'No authenticator app is set up.';
  return (
    <div className="stack">
      {status.required && !status.enrolled && (
        <Notice tone="warning">
          Your account needs a second factor. Set up an authenticator app now; otherwise you are
          asked to at your next sign-in.
        </Notice>
      )}
      <p className="muted">{facts}</p>
    </div>
  );
}

/** The content of the dialog: the enrolment, the new codes form, then the codes. */
function DialogBody({
  dialog,
  codes,
  onCodes,
}: Readonly<{ dialog: Dialog; codes: string[] | null; onCodes: (codes: string[]) => void }>) {
  const start = useCallback(() => mfaApi.startMine(), []);
  const confirm = useCallback((code: string) => mfaApi.confirmMine(code), []);
  if (codes !== null) {
    return <RecoveryCodes codes={codes} />;
  }
  if (dialog === 'enrol') {
    return (
      <EnrolAuthenticator
        start={start}
        confirm={confirm}
        onEnrolled={(e: MfaEnrolled) => onCodes(e.recoveryCodes)}
      />
    );
  }
  return dialog === 'codes' ? <NewCodes onDone={onCodes} /> : null;
}

/**
 * My Profile: the second factor of the signed-in user (authenticator app): whether it is set up
 * and required, its enrolment or replacement, and new recovery codes.
 */
export function SecondFactorCard() {
  const queries = useQueryClient();
  const status = useQuery({ queryKey: ['my-mfa'], queryFn: mfaApi.me });
  const [dialog, setDialog] = useState<Dialog>(null);
  const [codes, setCodes] = useState<string[] | null>(null);
  const close = () => {
    setDialog(null);
    setCodes(null);
    void queries.invalidateQueries({ queryKey: ['my-mfa'] });
  };
  const data = status.data;
  if (!data?.available) {
    return null;
  }
  return (
    <Card
      title="Second factor"
      actions={
        <div className="row">
          {data.enrolled && (
            <Button size="sm" variant="secondary" onClick={() => setDialog('codes')}>
              New Recovery Codes
            </Button>
          )}
          <Button size="sm" variant="primary" onClick={() => setDialog('enrol')}>
            {data.enrolled ? 'Replace App' : 'Set Up App'}
          </Button>
        </div>
      }
    >
      <SecondFactorFacts status={data} />
      <Modal
        title={dialog === 'codes' ? 'New Recovery Codes' : 'Authenticator App'}
        open={dialog !== null}
        onClose={close}
      >
        <DialogBody dialog={dialog} codes={codes} onCodes={setCodes} />
      </Modal>
    </Card>
  );
}
