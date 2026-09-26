import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Field } from '@/components/ui/Field';
import { useToast } from '@/components/ui/toastContext';
import { casesApi } from './api';
import type { CaseRow } from './api';
import type { CaseDialogProps } from './CaseDialogs';
import { LovField, StepDialog, TextField } from './StepDialog';

const OTHERS = 'OTHERS';

interface ReassignForm {
  assignee: string;
  reasonCode: string;
  comment: string;
}

function reassignErrors(form: ReassignForm, touched: boolean) {
  if (!touched) {
    return {};
  }
  return {
    assignee: form.assignee === '' ? 'Select the new assignee' : undefined,
    reasonCode: form.reasonCode === '' ? 'Select the reason for the re-assignment' : undefined,
    comment:
      form.reasonCode === OTHERS && form.comment.trim() === ''
        ? 'Enter a comment for reason Others'
        : undefined,
  };
}

function valid(form: ReassignForm): boolean {
  return (
    form.assignee !== '' &&
    form.reasonCode !== '' &&
    (form.reasonCode !== OTHERS || form.comment.trim() !== '')
  );
}

function ReassignFields({
  caseId,
  form,
  onChange,
  touched,
}: Readonly<{
  caseId: number;
  form: ReassignForm;
  onChange: (form: ReassignForm) => void;
  touched: boolean;
}>) {
  const eligible = useQuery({
    queryKey: ['screening', 'case', caseId, 'eligible'],
    queryFn: () => casesApi.eligible(caseId),
  });
  const errors = reassignErrors(form, touched);
  return (
    <>
      <Field label="New Assignee" required error={errors.assignee}>
        {(id) => (
          <select
            id={id}
            className="select"
            value={form.assignee}
            onChange={(e) => onChange({ ...form, assignee: e.target.value })}
          >
            <option value="">{eligible.isLoading ? 'Loading…' : 'Select…'}</option>
            {(eligible.data ?? []).map((u) => (
              <option key={u} value={u}>
                {u}
              </option>
            ))}
          </select>
        )}
      </Field>
      <LovField
        label="Reason"
        type="SCR_REASSIGN_REASON"
        value={form.reasonCode}
        onChange={(reasonCode) => onChange({ ...form, reasonCode })}
        error={errors.reasonCode}
      />
      <TextField
        label="Comment"
        value={form.comment}
        onChange={(comment) => onChange({ ...form, comment })}
        required={form.reasonCode === OTHERS}
        max={1000}
        error={errors.comment}
      />
    </>
  );
}

const EMPTY: ReassignForm = { assignee: '', reasonCode: '', comment: '' };

/**
 * Re-assign a case (SNSRP-404; FR-SS-043): only eligible users are offered (stage permission, not
 * the client's account officer); a reason is required, and a comment for Others.
 */
export function ReassignDialog({ detail, onDone, onClose }: Readonly<CaseDialogProps>) {
  const [form, setForm] = useState(EMPTY);
  const [touched, setTouched] = useState(false);
  const save = useMutation({
    mutationFn: () =>
      casesApi.reassign(detail.row.id, {
        assignee: form.assignee,
        reasonCode: form.reasonCode,
        comment: form.comment.trim() || undefined,
      }),
    onSuccess: onDone,
  });
  return (
    <StepDialog
      title="Re-assign Case"
      confirmLabel="Re-assign"
      busy={save.isPending}
      error={save.error}
      onClose={onClose}
      onConfirm={() => {
        setTouched(true);
        if (valid(form)) {
          save.mutate();
        }
      }}
    >
      <p className="muted">
        {detail.row.caseNo} is assigned to {detail.row.assignee ?? 'the stage queue'}. The data,
        stage and history do not change.
      </p>
      <ReassignFields caseId={detail.row.id} form={form} onChange={setForm} touched={touched} />
    </StepDialog>
  );
}

/** Re-assign the selected cases (the users eligible for the first case are offered). */
export function BulkReassignDialog({
  cases,
  onDone,
  onClose,
}: Readonly<{ cases: CaseRow[]; onDone: () => void; onClose: () => void }>) {
  const toast = useToast();
  const [form, setForm] = useState(EMPTY);
  const [touched, setTouched] = useState(false);
  const save = useMutation({
    mutationFn: async () => {
      const results = await Promise.allSettled(
        cases.map((c) =>
          casesApi.reassign(c.id, {
            assignee: form.assignee,
            reasonCode: form.reasonCode,
            comment: form.comment.trim() || undefined,
          }),
        ),
      );
      return results.filter((r) => r.status === 'fulfilled').length;
    },
    onSuccess: (done) => {
      const failed = cases.length - done;
      if (failed > 0) {
        toast.error(`${done} case(s) re-assigned; ${failed} refused (not eligible in their stage)`);
      } else {
        toast.success(`${done} case(s) re-assigned to ${form.assignee}`);
      }
      onDone();
    },
  });
  const first = cases[0];
  return (
    <StepDialog
      title="Re-assign Cases"
      confirmLabel="Re-assign"
      busy={save.isPending}
      error={save.error}
      onClose={onClose}
      onConfirm={() => {
        setTouched(true);
        if (valid(form)) {
          save.mutate();
        }
      }}
    >
      <p className="muted">{cases.map((c) => c.caseNo).join(', ')}</p>
      {first !== undefined && (
        <ReassignFields caseId={first.id} form={form} onChange={setForm} touched={touched} />
      )}
    </StepDialog>
  );
}

/**
 * Update Risk Tag (SNSRP-304; FR-SS-035): the client's rating and tags with a justification; the
 * case documents are the evidence.
 */
export function RiskTagDialog({ detail, onDone, onClose }: Readonly<CaseDialogProps>) {
  const [riskRating, setRiskRating] = useState('');
  const [endWatchlist, setEndWatchlist] = useState(false);
  const [addPep, setAddPep] = useState(false);
  const [justification, setJustification] = useState('');
  const [touched, setTouched] = useState(false);
  const save = useMutation({
    mutationFn: () =>
      casesApi.riskTag(detail.row.id, {
        riskRating,
        addTags: addPep ? ['PEP'] : [],
        removeTags: endWatchlist ? ['WATCHLIST_REVIEW'] : [],
        justification,
      }),
    onSuccess: () => onDone(),
  });
  return (
    <StepDialog
      title="Update Risk Tag"
      confirmLabel="Update Risk Tag"
      busy={save.isPending}
      error={save.error}
      onClose={onClose}
      onConfirm={() => {
        setTouched(true);
        if (riskRating !== '' && justification.trim() !== '') {
          save.mutate();
        }
      }}
    >
      <p className="muted">
        The documents of the case are attached as evidence; upload them on the Documents tab first.
        The change takes effect at once in the client master.
      </p>
      <LovField
        label="New Risk Rating"
        type="KYC_RISK_RATING"
        value={riskRating}
        onChange={setRiskRating}
        error={touched && riskRating === '' ? 'Select a valid risk rating' : undefined}
      />
      <label className="checkbox">
        <input
          type="checkbox"
          checked={endWatchlist}
          onChange={(e) => setEndWatchlist(e.target.checked)}
        />{' '}
        End the Watchlist Review tag
      </label>
      <label className="checkbox">
        <input type="checkbox" checked={addPep} onChange={(e) => setAddPep(e.target.checked)} /> Add
        the PEP tag
      </label>
      <TextField
        label="Justification"
        value={justification}
        onChange={setJustification}
        max={2000}
        error={
          touched && justification.trim() === ''
            ? 'Enter the justification of the change'
            : undefined
        }
      />
    </StepDialog>
  );
}
