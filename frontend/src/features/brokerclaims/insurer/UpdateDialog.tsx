import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { attachmentsApi } from '@/api/attachments';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { formatDate, today } from '@/utils/format';
import { CLAIM_ENTITY } from '../record/api';
import { DialogFooter, InputField, LovField, TextAreaField } from '../record/FormParts';
import type { InsurerLine, InsurerUpdate, NewUpdate } from './api';
import type { UpdateErrors } from './insurerLogic';
import { updateErrors } from './insurerLogic';

/**
 * Record Insurer Update (BRCLM.041, FR-CM-022): date, source, insurer's reference, remarks, the
 * insurer line and the claim documents carrying the insurer's letter; a correction refers to the
 * update it corrects. Updates cannot be edited afterwards.
 */
export function UpdateDialog({
  claimId,
  lines,
  updates,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<{
  claimId: number;
  lines: InsurerLine[];
  updates: InsurerUpdate[];
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onSave: (update: NewUpdate) => void;
}>) {
  const files = useQuery({
    queryKey: ['attachments', CLAIM_ENTITY, String(claimId)],
    queryFn: () => attachmentsApi.list(CLAIM_ENTITY, String(claimId)),
  });
  const [lineId, setLineId] = useState('');
  const [date, setDate] = useState(today());
  const [source, setSource] = useState('');
  const [reference, setReference] = useState('');
  const [remarks, setRemarks] = useState('');
  const [attachment, setAttachment] = useState('');
  const [corrects, setCorrects] = useState('');
  const [errors, setErrors] = useState<UpdateErrors>({});
  const save = () => {
    const found = updateErrors(date, source, remarks, today());
    setErrors(found);
    if (Object.values(found).every((e) => e === undefined)) {
      onSave({
        insurerClaimId: lineId === '' ? undefined : Number(lineId),
        updateDate: date,
        source,
        reference: reference.trim() || undefined,
        remarks: remarks.trim(),
        attachmentIds: attachment === '' ? [] : [Number(attachment)],
        correctsUpdateId: corrects === '' ? undefined : Number(corrects),
      });
    }
  };
  return (
    <Modal
      title="Record Insurer Update"
      open
      onClose={onClose}
      footer={<DialogFooter label="Record Update" busy={busy} onClose={onClose} onConfirm={save} />}
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <Field label="Insurer Claim Line">
          {(id) => (
            <select
              id={id}
              className="select"
              value={lineId}
              onChange={(e) => setLineId(e.target.value)}
            >
              <option value="">Whole claim</option>
              {lines.map((l) => (
                <option key={l.id} value={l.id}>
                  {l.insurerName ?? l.insurerCode} {l.insurerClaimNo ?? ''}
                </option>
              ))}
            </select>
          )}
        </Field>
        <div className="form-grid">
          <InputField
            label="Update Date"
            type="date"
            required
            value={date}
            error={errors.date}
            onChange={setDate}
          />
          <LovField
            label="Source"
            type="BCL_UPDATE_SOURCE"
            required
            value={source}
            error={errors.source}
            onChange={setSource}
          />
          <InputField
            label="Insurer Reference"
            maxLength={100}
            value={reference}
            onChange={setReference}
          />
        </div>
        <TextAreaField
          label="Remarks"
          required
          value={remarks}
          error={errors.remarks}
          onChange={setRemarks}
        />
        <Field label="Insurer's Document" hint="Attach it on the Documents tab first">
          {(id) => (
            <select
              id={id}
              className="select"
              value={attachment}
              onChange={(e) => setAttachment(e.target.value)}
            >
              <option value="">None</option>
              {(files.data ?? []).map((f) => (
                <option key={f.id} value={f.id}>
                  {f.fileName}
                </option>
              ))}
            </select>
          )}
        </Field>
        {updates.length > 0 && (
          <Field label="Corrects Update">
            {(id) => (
              <select
                id={id}
                className="select"
                value={corrects}
                onChange={(e) => setCorrects(e.target.value)}
              >
                <option value="">None</option>
                {updates.map((u) => (
                  <option key={u.id} value={u.id}>
                    {formatDate(u.updateDate)} {u.source} {u.reference ?? ''}
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
