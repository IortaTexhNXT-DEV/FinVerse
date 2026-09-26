import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { attachmentsApi } from '@/api/attachments';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { today } from '@/utils/format';
import type { Claim } from './api';
import { CLAIM_ENTITY } from './api';
import { DialogFooter, InputField, LovField, TextAreaField } from './FormParts';
import { dateErrors } from './recordLogic';

interface DialogProps {
  busy: boolean;
  error: unknown;
  onClose: () => void;
}

const REASON_REQUIRED = 'Enter the reason for the change';

/** Correct Reported Date (BRCLM.004, FR-CL-012; BCL_STATUS_UPDATE). */
export function ReportedDateDialog({
  claim,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<
  DialogProps & {
    claim: Claim;
    onSave: (v: { reportedDate: string; reason: string; remark?: string }) => void;
  }
>) {
  const [date, setDate] = useState(claim.loss.reportedDate);
  const [reason, setReason] = useState('');
  const [remark, setRemark] = useState('');
  const [errors, setErrors] = useState<{ date?: string; reason?: string }>({});
  const save = () => {
    const found = {
      date: dateErrors(claim.loss.lossDate, date, today()).reportedDate,
      reason: reason === '' ? REASON_REQUIRED : undefined,
    };
    setErrors(found);
    if (found.date === undefined && found.reason === undefined) {
      onSave({ reportedDate: date, reason, remark: remark.trim() || undefined });
    }
  };
  return (
    <Modal
      title="Correct Reported Date"
      open
      onClose={onClose}
      footer={<DialogFooter label="Save Date" busy={busy} onClose={onClose} onConfirm={save} />}
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <InputField
          label="Reported Date"
          type="date"
          required
          value={date}
          error={errors.date}
          onChange={setDate}
        />
        <LovField
          label="Reason"
          type="BCL_OVERRIDE_REASON"
          required
          value={reason}
          error={errors.reason}
          onChange={setReason}
        />
        <TextAreaField label="Remark" value={remark} maxLength={500} onChange={setRemark} />
      </div>
    </Modal>
  );
}

/** Override Claimant (BRCLM.006, FR-CL-030; BCL_CLAIMANT_OVERRIDE). */
export function ClaimantDialog({
  claim,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<DialogProps & { claim: Claim; onSave: (name: string, reason: string) => void }>) {
  const [name, setName] = useState(claim.loss.claimantName ?? '');
  const [reason, setReason] = useState('');
  const [errors, setErrors] = useState<{ name?: string; reason?: string }>({});
  const save = () => {
    const found = {
      name: name.trim() === '' ? "Enter the claimant's name" : undefined,
      reason: reason === '' ? REASON_REQUIRED : undefined,
    };
    setErrors(found);
    if (found.name === undefined && found.reason === undefined) {
      onSave(name.trim(), reason);
    }
  };
  return (
    <Modal
      title="Override Claimant"
      open
      onClose={onClose}
      footer={<DialogFooter label="Save Claimant" busy={busy} onClose={onClose} onConfirm={save} />}
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <InputField
          label="Claimant Name"
          required
          maxLength={200}
          value={name}
          error={errors.name}
          onChange={setName}
        />
        <LovField
          label="Reason"
          type="BCL_OVERRIDE_REASON"
          required
          value={reason}
          error={errors.reason}
          onChange={setReason}
        />
      </div>
    </Modal>
  );
}

/**
 * Generate Authorization Code (BRCLM.001, FR-CL-016; BCL_AUTHORIZE). On a direct-payment cover
 * under BCL_AUTH_DP_POLICY = CONFIRM the insurer's payment evidence, attached to the claim first,
 * is chosen here.
 */
export function AuthorizeDialog({
  claim,
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<DialogProps & { claim: Claim; onConfirm: (evidenceAttachmentId?: number) => void }>) {
  const needsEvidence =
    claim.premium.live.status === 'DIRECT_PAYMENT' &&
    claim.premium.dpPolicy.toUpperCase() === 'CONFIRM';
  const files = useQuery({
    queryKey: ['attachments', CLAIM_ENTITY, String(claim.id)],
    queryFn: () => attachmentsApi.list(CLAIM_ENTITY, String(claim.id)),
    enabled: needsEvidence,
  });
  const [evidence, setEvidence] = useState('');
  const [missing, setMissing] = useState(false);
  const confirm = () => {
    setMissing(needsEvidence && evidence === '');
    if (!needsEvidence || evidence !== '') {
      onConfirm(evidence === '' ? undefined : Number(evidence));
    }
  };
  return (
    <Modal
      title="Generate Authorization Code"
      open
      onClose={onClose}
      footer={
        <DialogFooter label="Generate Code" busy={busy} onClose={onClose} onConfirm={confirm} />
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <p>
          The claims authorization code of {claim.claimNo} is issued once, on a paid premium check.
          Its use by BDOI is still to be confirmed (CLQ01).
        </p>
        {needsEvidence && (
          <Field
            label="Insurer Payment Evidence"
            required
            error={missing ? "Attach the insurer's payment evidence first" : undefined}
            hint="Attach the insurer's evidence on the Documents tab, then choose it here"
          >
            {(id) => (
              <select
                id={id}
                className="select"
                value={evidence}
                onChange={(e) => setEvidence(e.target.value)}
              >
                <option value="">Select…</option>
                {(files.data ?? []).map((f) => (
                  <option key={f.id} value={f.id}>
                    {f.fileName}
                  </option>
                ))}
              </select>
            )}
          </Field>
        )}
      </div>
    </Modal>
  );
}
