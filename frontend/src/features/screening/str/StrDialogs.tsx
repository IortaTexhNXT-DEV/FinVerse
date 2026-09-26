import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { DataTable } from '@/components/ui/DataTable';
import { Field } from '@/components/ui/Field';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime, today } from '@/utils/format';
import { StepDialog, TextField } from '../cases/StepDialog';
import { strApi } from './api';
import type { StrExtraction, StrRow } from './api';
import { extractionPeriodError, filingErrors, monthStart } from './strLogic';

/**
 * Record Filing (SNSRP-706; FR-SS-072): the AMLC reference (unique) and the filing date, not before
 * the extraction; the STR becomes FILED and the case closes.
 */
export function FilingDialog({
  str,
  onDone,
  onClose,
}: Readonly<{ str: StrRow; onDone: () => void; onClose: () => void }>) {
  const toast = useToast();
  const [reference, setReference] = useState('');
  const [filedOn, setFiledOn] = useState(today());
  const [touched, setTouched] = useState(false);
  const extractedOn = str.extractedAt?.slice(0, 10);
  const errors = touched ? filingErrors(reference, filedOn, extractedOn) : {};
  const save = useMutation({
    mutationFn: () => strApi.filing(str.id, { reference: reference.trim(), filedOn }),
    onSuccess: (filed) => {
      toast.success(`${filed.strNo} filed with AMLC reference ${filed.amlcReference ?? ''}`);
      onDone();
    },
  });
  return (
    <StepDialog
      title="Record Filing"
      confirmLabel="Record Filing"
      busy={save.isPending}
      error={save.error}
      onClose={onClose}
      onConfirm={() => {
        setTouched(true);
        if (Object.keys(filingErrors(reference, filedOn, extractedOn)).length === 0) {
          save.mutate();
        }
      }}
    >
      <p className="muted">
        {str.strNo} of {str.subjectName} was extracted on {formatDate(str.extractedAt)}. Filing on
        the AMLC portal is done outside BIBS; record its reference here.
      </p>
      <Field label="AMLC Reference" required error={errors.reference}>
        {(id) => (
          <input
            id={id}
            className="input"
            maxLength={60}
            value={reference}
            onChange={(e) => setReference(e.target.value)}
          />
        )}
      </Field>
      <Field label="Filing Date" required error={errors.filedOn}>
        {(id) => (
          <input
            id={id}
            type="date"
            className="input"
            max={today()}
            value={filedOn}
            onChange={(e) => setFiledOn(e.target.value)}
          />
        )}
      </Field>
    </StepDialog>
  );
}

/**
 * Extract Approved STRs (SNSRP-706; FR-SS-071): the committee-approved STRs of a period not yet
 * extracted are listed, then written in the AMLC layout and saved; a re-extraction needs a reason.
 */
export function ExtractDialog({
  onDone,
  onClose,
}: Readonly<{ onDone: (extraction: StrExtraction) => void; onClose: () => void }>) {
  const companyId = useCompanyId();
  const [from, setFrom] = useState(monthStart(today()));
  const [to, setTo] = useState(today());
  const [reason, setReason] = useState('');
  const periodError = extractionPeriodError(from, to);
  const request = { companyId, from, to, reason: reason.trim() || undefined };
  const preview = useMutation({ mutationFn: () => strApi.preview(request) });
  const extract = useMutation({ mutationFn: () => strApi.extract(request), onSuccess: onDone });
  const listed = preview.data;
  return (
    <StepDialog
      title="Extract Approved STRs"
      confirmLabel={listed === undefined ? 'List STRs' : 'Extract'}
      busy={preview.isPending || extract.isPending}
      error={preview.error ?? extract.error}
      onClose={onClose}
      onConfirm={() => {
        if (periodError !== undefined) {
          return;
        }
        if (listed === undefined) {
          preview.mutate();
        } else {
          extract.mutate();
        }
      }}
    >
      <div className="form-grid">
        <Field label="Committee Decision From" required>
          {(id) => (
            <input
              id={id}
              type="date"
              className="input"
              value={from}
              onChange={(e) => {
                setFrom(e.target.value);
                preview.reset();
              }}
            />
          )}
        </Field>
        <Field label="Committee Decision To" required error={periodError}>
          {(id) => (
            <input
              id={id}
              type="date"
              className="input"
              value={to}
              onChange={(e) => {
                setTo(e.target.value);
                preview.reset();
              }}
            />
          )}
        </Field>
      </div>
      <TextField
        label="Re-extraction Reason"
        value={reason}
        onChange={(value) => {
          setReason(value);
          preview.reset();
        }}
        required={false}
        max={1000}
        hint="Only to extract STRs already extracted again; the extraction is recorded as a re-extraction."
      />
      {listed !== undefined && (
        <DataTable<StrRow>
          caption="STRs to extract"
          rows={listed}
          rowKey={(s) => s.id}
          emptyMessage="No committee-approved STR to extract"
          columns={[
            {
              key: 'no',
              header: 'STR No.',
              render: (s) => <span className="mono">{s.strNo}</span>,
            },
            { key: 'subject', header: 'Subject', render: (s) => s.subjectName },
            {
              key: 'decided',
              header: 'Committee Decision',
              render: (s) => formatDateTime(s.committeeDecidedAt),
            },
            { key: 'status', header: 'Status', render: (s) => s.status },
          ]}
        />
      )}
    </StepDialog>
  );
}
