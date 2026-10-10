import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { renewalApprovalApi } from '@/api/renewalApproval';
import type { Disposition } from '@/api/renewal';
import { LovSelect } from '@/components/broking/LovSelect';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { UnitSelect } from '../common/ActionDialogs';
import { DISPOSITION_OPTIONS, RNW_LOV } from '../common/renewalCodes';
import { DialogFooter, TextArea } from './dialogParts';
import type { DialogProps } from './dialogParts';

const TRANSFER = 'TRANSFER_TO_OTHER_UNIT';
const NEW_INVOICE = 'BOOKED_TO_NEW_INVOICE';

/** The disposition input of the officer or the Team Leader (FR-RN-043). */
export interface DispositionInput {
  code: Disposition;
  reasonCode?: string;
  newInvoiceNo?: string;
  receivingUnit?: string;
  remarks?: string;
}

/** Whether the disposition form is complete. */
function dispositionReady(f: {
  code: string;
  reason: string;
  invoice: string;
  unit: string;
}): boolean {
  if (f.code === '') return false;
  if (f.code !== 'NOT_FOR_RENEWAL') return true;
  if (f.reason === NEW_INVOICE) return f.invoice.trim() !== '';
  if (f.reason === TRANSFER) return f.unit.trim() !== '';
  return f.reason !== '';
}

/** The input of a disposition from the form. */
function dispositionInput(f: {
  code: string;
  reason: string;
  invoice: string;
  unit: string;
  remarks: string;
}): DispositionInput {
  const nfr = f.code === 'NOT_FOR_RENEWAL';
  return {
    code: f.code as Disposition,
    reasonCode: nfr ? f.reason : undefined,
    newInvoiceNo: nfr && f.reason === NEW_INVOICE ? f.invoice.trim() : undefined,
    receivingUnit: nfr && f.reason === TRANSFER ? f.unit.trim() : undefined,
    remarks: f.remarks.trim() === '' ? undefined : f.remarks.trim(),
  };
}

/** The reason of Not for Renewal, with the new invoice or receiving unit some reasons need. */
function NotForRenewalFields({
  reason,
  invoice,
  unit,
  onReason,
  onInvoice,
  onUnit,
}: Readonly<{
  reason: string;
  invoice: string;
  unit: string;
  onReason: (v: string) => void;
  onInvoice: (v: string) => void;
  onUnit: (v: string) => void;
}>) {
  return (
    <>
      <Field label="Reason for Not for Renewal" required>
        {(id) => (
          <LovSelect
            id={id}
            type={RNW_LOV.nonRenewalReason}
            value={reason}
            onChange={onReason}
            required
          />
        )}
      </Field>
      {reason === NEW_INVOICE && (
        <Field label="New Invoice No." required>
          {(id) => (
            <input
              id={id}
              className="input"
              value={invoice}
              onChange={(e) => onInvoice(e.target.value)}
            />
          )}
        </Field>
      )}
      {reason === TRANSFER && (
        <Field label="Receiving Unit" required>
          {(id) => <UnitSelect id={id} value={unit} onChange={onUnit} />}
        </Field>
      )}
    </>
  );
}

/** Set Disposition (FR-RN-043): Not for Renewal needs a reason; some reasons need more. */
export function DispositionDialog({
  initial,
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<DialogProps<DispositionInput> & { initial: Disposition | null }>) {
  const [code, setCode] = useState<string>(initial ?? '');
  const [reason, setReason] = useState('');
  const [invoice, setInvoice] = useState('');
  const [unit, setUnit] = useState('');
  const [remarks, setRemarks] = useState('');
  const nfr = code === 'NOT_FOR_RENEWAL';
  const ready = dispositionReady({ code, reason, invoice, unit });
  const offered = useQuery({
    queryKey: ['renewal', 'dispositions'],
    queryFn: renewalApprovalApi.dispositions,
    staleTime: 60_000,
  });
  return (
    <Modal
      open
      title="Set Disposition"
      onClose={onClose}
      footer={
        <DialogFooter
          label="Save"
          busy={busy}
          disabled={!ready}
          onClose={onClose}
          onConfirm={() => onConfirm(dispositionInput({ code, reason, invoice, unit, remarks }))}
        />
      }
    >
      <ErrorAlert error={error} />
      <Field label="Disposition" required>
        {(id) => (
          <select id={id} className="select" value={code} onChange={(e) => setCode(e.target.value)}>
            <option value="">Select the disposition</option>
            {(offered.data ?? DISPOSITION_OPTIONS).map((o) => (
              <option key={o.code} value={o.code}>
                {o.label}
              </option>
            ))}
          </select>
        )}
      </Field>
      {nfr && (
        <NotForRenewalFields
          reason={reason}
          invoice={invoice}
          unit={unit}
          onReason={setReason}
          onInvoice={setInvoice}
          onUnit={setUnit}
        />
      )}
      <TextArea label="Remarks" value={remarks} onChange={setRemarks} />
    </Modal>
  );
}

/** A manual insurer response (FR-RN-071). */
export interface ResponseInput {
  response: string;
  insurerRef?: string;
  revisedPremium?: number;
  revisedSumInsured?: number;
  receivedOn?: string;
  remarks?: string;
  remarket: boolean;
}

const num = (v: string) => (v.trim() === '' ? undefined : Number(v));

/** Record Insurer Response (FR-RN-071). */
export function ResponseDialog({
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<DialogProps<ResponseInput>>) {
  const [response, setResponse] = useState('RENEW_AS_IS');
  const [insurerRef, setInsurerRef] = useState('');
  const [premium, setPremium] = useState('');
  const [si, setSi] = useState('');
  const [on, setOn] = useState('');
  const [remarks, setRemarks] = useState('');
  const [remarket, setRemarket] = useState(false);
  return (
    <Modal
      open
      title="Record Insurer Response"
      onClose={onClose}
      footer={
        <DialogFooter
          label="Record"
          busy={busy}
          disabled={response === 'REVISE' && premium.trim() === ''}
          onClose={onClose}
          onConfirm={() =>
            onConfirm({
              response,
              insurerRef: insurerRef.trim() || undefined,
              revisedPremium: num(premium),
              revisedSumInsured: num(si),
              receivedOn: on || undefined,
              remarks: remarks.trim() || undefined,
              remarket,
            })
          }
        />
      }
    >
      <ErrorAlert error={error} />
      <div className="form-grid">
        <Field label="Response" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={response}
              onChange={(e) => setResponse(e.target.value)}
            >
              <option value="RENEW_AS_IS">Renew as is</option>
              <option value="REVISE">Renew with revised terms</option>
              <option value="REJECT">Declined</option>
            </select>
          )}
        </Field>
        <Field label="Insurer Reference">
          {(id) => (
            <input
              id={id}
              className="input"
              value={insurerRef}
              onChange={(e) => setInsurerRef(e.target.value)}
            />
          )}
        </Field>
        {response === 'REVISE' && (
          <>
            <Field label="Revised Premium" required>
              {(id) => (
                <input
                  id={id}
                  className="input"
                  inputMode="decimal"
                  value={premium}
                  onChange={(e) => setPremium(e.target.value)}
                />
              )}
            </Field>
            <Field label="Revised Sum Insured">
              {(id) => (
                <input
                  id={id}
                  className="input"
                  inputMode="decimal"
                  value={si}
                  onChange={(e) => setSi(e.target.value)}
                />
              )}
            </Field>
          </>
        )}
        <Field label="Received On">
          {(id) => <DateInput id={id} value={on} onChange={(e) => setOn(e.target.value)} />}
        </Field>
      </div>
      {response === 'REJECT' && (
        <label className="checkbox">
          <input
            type="checkbox"
            checked={remarket}
            onChange={(e) => setRemarket(e.target.checked)}
          />
          Re-market to another insurer (otherwise the renewal goes to the No Advice Letter)
        </label>
      )}
      <TextArea label="Remarks" value={remarks} onChange={setRemarks} />
    </Modal>
  );
}
