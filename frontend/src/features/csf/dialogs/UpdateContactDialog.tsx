import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { csfApi } from '@/api/csf';
import type { ContactDetails, Verification } from '@/api/csf';
import { lovApi } from '@/api/lov';
import { LovSelect } from '@/components/broking/LovSelect';
import { StageStepper } from '@/components/broking/StageStepper';
import type { Step } from '@/components/broking/stageSteps';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { Notice } from '@/components/ui/Notice';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';
import { CONTACT_FIELDS, CSF_LOV, contactError, minutesLeft } from '../csfCodes';
import type { ContactProp } from '../csfCodes';
import '../csf.css';

interface UpdateContactDialogProps {
  companyId: number;
  clientId: number;
  clientName: string;
  contact: ContactDetails;
  /** A passed verification still valid: the dialog opens at the change step. */
  verification: Verification | null;
  onClose: () => void;
}

function steps(step: 1 | 2): Step[] {
  return [
    { code: 'VERIFY', name: 'Verify Caller', state: step === 1 ? 'current' : 'done', near: true },
    {
      code: 'CHANGE',
      name: 'Change Contact',
      state: step === 2 ? 'current' : 'upcoming',
      near: true,
    },
  ];
}

function VerifyStep({
  companyId,
  clientId,
  onPassed,
}: Readonly<{ companyId: number; clientId: number; onPassed: (v: Verification) => void }>) {
  const [channel, setChannel] = useState('');
  const [answers, setAnswers] = useState<Record<string, boolean>>({});
  const [failed, setFailed] = useState<Verification | null>(null);
  const checks = useQuery({
    queryKey: ['lov', CSF_LOV.check],
    queryFn: () => lovApi.options(CSF_LOV.check),
    staleTime: 5 * 60_000,
  });
  const list = checks.data ?? [];
  const complete = channel !== '' && list.length > 0 && list.every((c) => c.code in answers);
  const verify = useMutation({
    mutationFn: () =>
      csfApi.verify(companyId, clientId, {
        channel,
        checks: list.map((c) => ({ code: c.code, matched: answers[c.code] ?? false })),
      }),
    onSuccess: (v) => {
      if (v.result === 'PASSED') {
        onPassed(v);
      } else {
        setFailed(v);
      }
    },
  });
  return (
    <div className="stack">
      <ErrorAlert error={verify.error ?? checks.error} title="Cannot record the verification" />
      {failed && (
        <Notice tone="error" title="Caller not verified">
          The caller could not be verified ({failed.matches} of {failed.required} checks needed
          matched). The contact details cannot be changed.
        </Notice>
      )}
      <Field label="Channel" required>
        {(id) => (
          <LovSelect
            id={id}
            type={CSF_LOV.channel}
            value={channel}
            onChange={setChannel}
            required
          />
        )}
      </Field>
      <fieldset className="csf-checklist" aria-label="Verification checks">
        {list.map((c) => (
          <div key={c.code} className="csf-check-row" role="radiogroup" aria-label={c.label}>
            <span>{c.label}</span>
            <label className="checkbox">
              <input
                type="radio"
                name={`check-${c.code}`}
                checked={answers[c.code] === true}
                onChange={() => setAnswers({ ...answers, [c.code]: true })}
              />
              Matched
            </label>
            <label className="checkbox">
              <input
                type="radio"
                name={`check-${c.code}`}
                checked={answers[c.code] === false}
                onChange={() => setAnswers({ ...answers, [c.code]: false })}
              />
              Not Matched
            </label>
          </div>
        ))}
      </fieldset>
      <div className="form-actions">
        <Button
          variant="accent"
          disabled={!complete}
          busy={verify.isPending}
          onClick={() => verify.mutate()}
        >
          Record Verification
        </Button>
      </div>
    </div>
  );
}

type Values = Record<ContactProp, string>;

function ChangeStep({
  companyId,
  clientId,
  clientName,
  contact,
  verification,
  onClose,
}: Readonly<Omit<UpdateContactDialogProps, 'verification'> & { verification: Verification }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const initial = Object.fromEntries(
    CONTACT_FIELDS.map((f) => [f.prop, contact[f.prop] ?? '']),
  ) as Values;
  const [values, setValues] = useState<Values>(initial);
  const [reason, setReason] = useState('');
  const [remarks, setRemarks] = useState('');
  const errors = Object.fromEntries(
    CONTACT_FIELDS.map((f) => [f.prop, contactError(f.key, values[f.prop])]),
  ) as Record<ContactProp, string | undefined>;
  const changed = CONTACT_FIELDS.filter((f) => values[f.prop].trim() !== initial[f.prop]);
  const invalid = Object.values(errors).some((e) => e !== undefined);
  const save = useMutation({
    mutationFn: () =>
      csfApi.change(companyId, clientId, {
        verificationId: verification.id,
        reasonCode: reason,
        remarks: remarks.trim() || undefined,
        values: Object.fromEntries(changed.map((f) => [f.key, values[f.prop].trim()])),
      }),
    onSuccess: (c) => {
      toast.success(`Contact details of ${clientName} changed (${c.changeNo})`);
      void queryClient.invalidateQueries({ queryKey: ['csf'] });
      onClose();
    },
  });
  return (
    <div className="stack">
      <Notice tone="success" title="Caller verified">
        Verified at {formatDateTime(verification.verifiedAt)}; valid for{' '}
        {minutesLeft(verification.validUntil)} more minutes.
      </Notice>
      <ErrorAlert error={save.error} title="Cannot change the contact details" />
      <div className="form-grid">
        {CONTACT_FIELDS.map((f) => (
          <Field key={f.key} label={f.label} hint={f.hint} error={errors[f.prop]}>
            {(id) => (
              <input
                id={id}
                className="input"
                value={values[f.prop]}
                aria-invalid={errors[f.prop] !== undefined}
                onChange={(e) => setValues({ ...values, [f.prop]: e.target.value })}
              />
            )}
          </Field>
        ))}
        <Field label="Reason" required>
          {(id) => (
            <LovSelect id={id} type={CSF_LOV.reason} value={reason} onChange={setReason} required />
          )}
        </Field>
        <Field label="Remarks">
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={2}
              maxLength={500}
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
            />
          )}
        </Field>
      </div>
      <div className="form-actions">
        <Button variant="secondary" onClick={onClose}>
          Cancel
        </Button>
        <Button
          variant="accent"
          disabled={changed.length === 0 || reason === '' || invalid}
          busy={save.isPending}
          onClick={() => save.mutate()}
        >
          Save
        </Button>
      </div>
    </div>
  );
}

/**
 * Update Contact (FR-CSF-020, 021; BRCSF-004): step 1 records the verification checklist of the
 * caller and the channel; after a pass, step 2 changes the contact details only - e-mail, mobile,
 * phone and address - with a reason. The change is saved in the client master and listed in the
 * Contact History.
 */
export function UpdateContactDialog(props: Readonly<UpdateContactDialogProps>) {
  const [verification, setVerification] = useState<Verification | null>(
    props.verification?.result === 'PASSED' ? props.verification : null,
  );
  const step = verification === null ? 1 : 2;
  return (
    <Modal title={`Update Contact - ${props.clientName}`} open onClose={props.onClose}>
      <div className="stack">
        <StageStepper steps={steps(step)} label="Update contact steps" />
        {verification === null ? (
          <VerifyStep
            companyId={props.companyId}
            clientId={props.clientId}
            onPassed={setVerification}
          />
        ) : (
          <ChangeStep {...props} verification={verification} />
        )}
      </div>
    </Modal>
  );
}
