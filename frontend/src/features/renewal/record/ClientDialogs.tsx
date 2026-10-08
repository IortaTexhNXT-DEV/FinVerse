import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { attachmentsApi } from '@/api/attachments';
import { LovSelect } from '@/components/broking/LovSelect';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { RNW_LOV } from '../common/renewalCodes';
import { DialogFooter, TextArea } from './dialogParts';
import type { DialogProps } from './dialogParts';

/** A client acceptance (FR-RN-084). */
export interface AcceptanceInput {
  method: string;
  attachmentId?: number;
  reference?: string;
  acceptedOn?: string;
  remarks?: string;
  financialImpactAck: boolean;
}

/** Record Acceptance (FR-RN-084): by e-mail or signed RA (file on the renewal) or by payment. */
export function AcceptanceDialog({
  entityId,
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<DialogProps<AcceptanceInput> & { entityId: number }>) {
  const [method, setMethod] = useState('EMAIL');
  const [file, setFile] = useState('');
  const [reference, setReference] = useState('');
  const [on, setOn] = useState('');
  const [remarks, setRemarks] = useState('');
  const [ack, setAck] = useState(false);
  const files = useQuery({
    queryKey: ['attachments', 'RenewalCandidate', String(entityId)],
    queryFn: () => attachmentsApi.list('RenewalCandidate', String(entityId)),
  });
  const payment = method === 'PAYMENT';
  const ready = payment ? reference.trim() !== '' : file !== '';
  return (
    <Modal
      open
      title="Record Acceptance"
      onClose={onClose}
      footer={
        <DialogFooter
          label="Record"
          busy={busy}
          disabled={!ready}
          onClose={onClose}
          onConfirm={() =>
            onConfirm({
              method,
              attachmentId: payment ? undefined : Number(file),
              reference: reference.trim() || undefined,
              acceptedOn: on || undefined,
              remarks: remarks.trim() || undefined,
              financialImpactAck: ack,
            })
          }
        />
      }
    >
      <ErrorAlert error={error ?? files.error} />
      <div className="form-grid">
        <Field label="Method" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={method}
              onChange={(e) => setMethod(e.target.value)}
            >
              <option value="EMAIL">E-mail from the client</option>
              <option value="SIGNED_RA">Signed Renewal Advice</option>
              <option value="PAYMENT">Payment</option>
            </select>
          )}
        </Field>
        {!payment && (
          <Field label="Evidence" required hint="Upload the file in the Documents tab first">
            {(id) => (
              <select
                id={id}
                className="select"
                value={file}
                onChange={(e) => setFile(e.target.value)}
              >
                <option value="">Select the document</option>
                {(files.data ?? []).map((f) => (
                  <option key={f.id} value={String(f.id)}>
                    {f.fileName}
                  </option>
                ))}
              </select>
            )}
          </Field>
        )}
        <Field label={payment ? 'Payment Reference' : 'Reference'} required={payment}>
          {(id) => (
            <input
              id={id}
              className="input"
              value={reference}
              onChange={(e) => setReference(e.target.value)}
            />
          )}
        </Field>
        <Field label="Accepted On">
          {(id) => <DateInput id={id} value={on} onChange={(e) => setOn(e.target.value)} />}
        </Field>
      </div>
      <label className="checkbox rnw-ack">
        <input type="checkbox" checked={ack} onChange={(e) => setAck(e.target.checked)} />
        The client acknowledged the change of premium from the expiring terms
      </label>
      <TextArea label="Remarks" value={remarks} onChange={setRemarks} />
    </Modal>
  );
}

/** A follow-up call or e-mail (FR-RN-085). */
export interface FollowupInput {
  channel: string;
  outcome: string;
  remarks: string;
  nextActionDate?: string;
}

/** Add Follow-up (FR-RN-085). */
export function FollowupDialog({
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<DialogProps<FollowupInput>>) {
  const [channel, setChannel] = useState('');
  const [outcome, setOutcome] = useState('');
  const [remarks, setRemarks] = useState('');
  const [next, setNext] = useState('');
  return (
    <Modal
      open
      title="Add Follow-up"
      onClose={onClose}
      footer={
        <DialogFooter
          label="Save"
          busy={busy}
          disabled={channel === '' || outcome === '' || remarks.trim() === ''}
          onClose={onClose}
          onConfirm={() =>
            onConfirm({ channel, outcome, remarks, nextActionDate: next || undefined })
          }
        />
      }
    >
      <ErrorAlert error={error} />
      <div className="form-grid">
        <Field label="Channel" required>
          {(id) => (
            <LovSelect
              id={id}
              type={RNW_LOV.followupChannel}
              value={channel}
              onChange={setChannel}
              required
            />
          )}
        </Field>
        <Field label="Outcome" required>
          {(id) => (
            <LovSelect
              id={id}
              type={RNW_LOV.followupOutcome}
              value={outcome}
              onChange={setOutcome}
              required
            />
          )}
        </Field>
        <Field label="Next Action Date">
          {(id) => <DateInput id={id} value={next} onChange={(e) => setNext(e.target.value)} />}
        </Field>
      </div>
      <TextArea label="Remarks" value={remarks} onChange={setRemarks} required />
    </Modal>
  );
}

/** A package choice for a migrated policy (DMQ36). */
export interface PackageInput {
  productCode: string;
  productVersionNo: number;
  reason: string;
}

function validVersion(value: string): boolean {
  const n = Number(value);
  return Number.isInteger(n) && n >= 1;
}

/** Propose Package (DMQ36): the product version a migrated policy renews on, for approval. */
export function PackageDialog({
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<DialogProps<PackageInput>>) {
  const [product, setProduct] = useState('');
  const [version, setVersion] = useState('');
  const [reason, setReason] = useState('');
  return (
    <Modal
      open
      title="Propose Package"
      onClose={onClose}
      footer={
        <DialogFooter
          label="Submit for Approval"
          busy={busy}
          disabled={product.trim() === '' || !validVersion(version) || reason.trim() === ''}
          onClose={onClose}
          onConfirm={() =>
            onConfirm({
              productCode: product.trim(),
              productVersionNo: Number(version),
              reason: reason.trim(),
            })
          }
        />
      }
    >
      <ErrorAlert error={error} />
      <div className="form-grid">
        <Field label="Product Code" required>
          {(id) => (
            <input
              id={id}
              className="input"
              value={product}
              onChange={(e) => setProduct(e.target.value)}
            />
          )}
        </Field>
        <Field label="Product Version" required>
          {(id) => (
            <input
              id={id}
              className="input"
              inputMode="numeric"
              value={version}
              onChange={(e) => setVersion(e.target.value)}
            />
          )}
        </Field>
      </div>
      <TextArea label="Reason" value={reason} onChange={setReason} required />
    </Modal>
  );
}
