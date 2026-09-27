import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { casesApi } from './api';
import type { CaseDetail, CaseOutcome } from './api';
import { LovField, StepDialog, TextField } from './StepDialog';

/** Props of a case action dialog. */
export interface CaseDialogProps {
  detail: CaseDetail;
  onDone: (outcome?: CaseOutcome) => void;
  onClose: () => void;
}

function required(value: string, message: string, touched: boolean): string | undefined {
  return touched && value.trim() === '' ? message : undefined;
}

/**
 * Submit (SNSRP-502, 701; FR-SS-051): a disposition of the investigation stage, the recommendation
 * and the STR flag; the case is validated and routed by the approval matrix.
 */
export function SubmitDialog({ detail, onDone, onClose }: Readonly<CaseDialogProps>) {
  const [disposition, setDisposition] = useState('');
  const [recommendation, setRecommendation] = useState(detail.recommendation ?? '');
  const [strRequired, setStrRequired] = useState(detail.strRequired);
  const [touched, setTouched] = useState(false);
  const save = useMutation({
    mutationFn: () => casesApi.submit(detail.row.id, { disposition, recommendation, strRequired }),
    onSuccess: onDone,
  });
  return (
    <StepDialog
      title="Submit Case"
      confirmLabel="Submit"
      busy={save.isPending}
      error={save.error}
      onClose={onClose}
      onConfirm={() => {
        setTouched(true);
        if (disposition !== '' && recommendation.trim() !== '') {
          save.mutate();
        }
      }}
    >
      <LovField
        label="Disposition"
        type="SCR_DISPOSITION"
        parentCode="INVESTIGATION"
        value={disposition}
        onChange={setDisposition}
        error={required(disposition, 'Select the disposition', touched)}
      />
      <TextField
        label="Recommendation"
        value={recommendation}
        onChange={setRecommendation}
        error={required(recommendation, 'Write the recommendation', touched)}
      />
      <label className="checkbox">
        <input
          type="checkbox"
          checked={strRequired}
          onChange={(e) => setStrRequired(e.target.checked)}
        />{' '}
        STR required
      </label>
    </StepDialog>
  );
}

/** Resubmit a returned case with a response (FR-SS-062). */
export function ResubmitDialog({ detail, onDone, onClose }: Readonly<CaseDialogProps>) {
  const [response, setResponse] = useState('');
  const [touched, setTouched] = useState(false);
  const save = useMutation({
    mutationFn: () =>
      casesApi.resubmit(detail.row.id, { response, strRequired: detail.strRequired }),
    onSuccess: onDone,
  });
  return (
    <StepDialog
      title="Resubmit Case"
      confirmLabel="Resubmit"
      busy={save.isPending}
      error={save.error}
      onClose={onClose}
      onConfirm={() => {
        setTouched(true);
        if (response.trim() !== '') {
          save.mutate();
        }
      }}
    >
      <p className="muted">
        Returned from {detail.returnedFrom ?? 'approval'} by {detail.returnedBy ?? '—'}. The case
        goes back to that stage.
      </p>
      <TextField
        label="Response"
        value={response}
        onChange={setResponse}
        error={required(response, 'Write your response to the return', touched)}
      />
    </StepDialog>
  );
}

interface DecisionSpec {
  title: string;
  confirmLabel: string;
  stage: string;
  decisionLabel: string;
  decisionMessage: string;
  /** The dispositions that return the case and need a reason and remarks. */
  returning: string[];
  remarksLabel: string;
  remarksMessage: string;
  call: (
    id: number,
    request: { disposition: string; reasonCode?: string; remarks?: string },
  ) => Promise<CaseOutcome>;
}

const UNIT_HEAD: DecisionSpec = {
  title: 'Approve or Disapprove',
  confirmLabel: 'Record Decision',
  stage: 'UNIT_HEAD_APPROVAL',
  decisionLabel: 'Decision',
  decisionMessage: 'Select the decision',
  returning: ['NOT_CONCUR'],
  remarksLabel: 'Rationale / Comment',
  remarksMessage: 'Write the rationale for disapproving',
  call: casesApi.decide,
};

const COMPLIANCE: DecisionSpec = {
  title: 'Record Outcome',
  confirmLabel: 'Record Outcome',
  stage: 'COMPLIANCE_REVIEW',
  decisionLabel: 'Outcome',
  decisionMessage: 'Select the outcome',
  returning: ['RETURN'],
  remarksLabel: 'Remarks',
  remarksMessage: 'Enter the items to correct',
  call: casesApi.outcome,
};

function DecisionDialog({
  spec,
  detail,
  onDone,
  onClose,
}: Readonly<CaseDialogProps & { spec: DecisionSpec }>) {
  const [disposition, setDisposition] = useState('');
  const [reasonCode, setReasonCode] = useState('');
  const [remarks, setRemarks] = useState('');
  const [touched, setTouched] = useState(false);
  const returning = spec.returning.includes(disposition);
  const save = useMutation({
    mutationFn: () =>
      spec.call(detail.row.id, {
        disposition,
        reasonCode: returning ? reasonCode : undefined,
        remarks,
      }),
    onSuccess: onDone,
  });
  const valid = disposition !== '' && (!returning || (reasonCode !== '' && remarks.trim() !== ''));
  return (
    <StepDialog
      title={spec.title}
      confirmLabel={spec.confirmLabel}
      busy={save.isPending}
      error={save.error}
      onClose={onClose}
      onConfirm={() => {
        setTouched(true);
        if (valid) {
          save.mutate();
        }
      }}
    >
      <LovField
        label={spec.decisionLabel}
        type="SCR_DISPOSITION"
        parentCode={spec.stage}
        value={disposition}
        onChange={setDisposition}
        error={required(disposition, spec.decisionMessage, touched)}
      />
      {returning && (
        <LovField
          label="Return Reason"
          type="RETURN_REASON"
          value={reasonCode}
          onChange={setReasonCode}
          error={required(reasonCode, 'Select the return reason', touched)}
        />
      )}
      <TextField
        label={spec.remarksLabel}
        value={remarks}
        onChange={setRemarks}
        required={returning}
        error={returning ? required(remarks, spec.remarksMessage, touched) : undefined}
      />
    </StepDialog>
  );
}

/** The unit head's decision (SNSRP-702; FR-SS-061). */
export function UnitHeadDialog(props: Readonly<CaseDialogProps>) {
  return <DecisionDialog spec={UNIT_HEAD} {...props} />;
}

/** Compliance's outcome of the BU escalation (SNSRP-703; FR-SS-063). */
export function OutcomeDialog(props: Readonly<CaseDialogProps>) {
  return <DecisionDialog spec={COMPLIANCE} {...props} />;
}

/** An AML Committee member's decision (SNSRP-704; FR-SS-064). */
export function VoteDialog({ detail, onDone, onClose }: Readonly<CaseDialogProps>) {
  const [decision, setDecision] = useState('');
  const [remarks, setRemarks] = useState('');
  const [touched, setTouched] = useState(false);
  const save = useMutation({
    mutationFn: () => casesApi.vote(detail.row.id, { decision, remarks }),
    onSuccess: () => onDone(),
  });
  return (
    <StepDialog
      title="Record Decision"
      confirmLabel="Record Decision"
      busy={save.isPending}
      error={save.error}
      onClose={onClose}
      onConfirm={() => {
        setTouched(true);
        if (decision !== '' && remarks.trim() !== '') {
          save.mutate();
        }
      }}
    >
      <p className="muted">
        Your decision is final and cannot be changed. The committee rule decides the case once
        enough members agree.
      </p>
      <LovField
        label="Decision"
        type="SCR_DISPOSITION"
        parentCode="AML_COMMITTEE"
        value={decision}
        onChange={setDecision}
        error={required(decision, 'Select your decision', touched)}
      />
      <TextField
        label="Remarks"
        value={remarks}
        onChange={setRemarks}
        error={required(remarks, 'Enter your remarks', touched)}
      />
    </StepDialog>
  );
}

/** Re-open a closed case with a reason (FR-SS-040 R2). */
export function ReopenDialog({ detail, onDone, onClose }: Readonly<CaseDialogProps>) {
  const [reasonCode, setReasonCode] = useState('');
  const [remarks, setRemarks] = useState('');
  const [touched, setTouched] = useState(false);
  const save = useMutation({
    mutationFn: () => casesApi.reopen(detail.row.id, { reasonCode, remarks }),
    onSuccess: onDone,
  });
  return (
    <StepDialog
      title="Re-open Case"
      confirmLabel="Re-open"
      busy={save.isPending}
      error={save.error}
      onClose={onClose}
      onConfirm={() => {
        setTouched(true);
        if (reasonCode !== '' && remarks.trim() !== '') {
          save.mutate();
        }
      }}
    >
      <LovField
        label="Reason"
        type="RETURN_REASON"
        value={reasonCode}
        onChange={setReasonCode}
        error={required(reasonCode, 'Select the reason', touched)}
      />
      <TextField
        label="Remarks"
        value={remarks}
        onChange={setRemarks}
        error={required(remarks, 'Enter the reason for re-opening', touched)}
      />
    </StepDialog>
  );
}
