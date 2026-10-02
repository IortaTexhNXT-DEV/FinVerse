import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { formatAmount, formatDate, humanize, today } from '@/utils/format';
import type { Claim } from './api';
import { claimApi } from './api';
import { ClaimantDialog, ReportedDateDialog } from './ClaimDialogs';
import { DialogFooter } from './FormParts';
import type { LossFieldKey } from './LossFields';
import { LossFields } from './LossFields';
import { noErrors } from '../location/locationLogic';
import { toLoss } from './recordInput';
import type { ClaimForm, ClaimFormErrors } from './recordLogic';
import { amountError, dateErrors, SOURCE_LABELS } from './recordLogic';

type Dialog = 'loss' | 'reported' | 'claimant';

function formOf(claim: Claim): ClaimForm {
  const l = claim.loss;
  const text = (v?: number) => (v === undefined ? '' : String(v));
  return {
    arn: claim.cover.arn,
    source: claim.source,
    lossDate: l.lossDate,
    reportedDate: l.reportedDate,
    lossNature: l.lossNature ?? '',
    claimType: l.claimType ?? '',
    lossDescription: l.lossDescription ?? '',
    lossPlace: l.lossPlace ?? '',
    catastropheCode: l.catastropheCode ?? '',
    catastropheEvent: l.catastropheEvent ?? '',
    claimAmount: text(l.claimAmount),
    deductible: text(l.deductible),
    initialReserve: text(l.initialReserve),
    insurerClaimNos: [],
  };
}

function lossErrors(form: ClaimForm): ClaimFormErrors {
  const errors: ClaimFormErrors = {
    lossDate: dateErrors(form.lossDate, form.reportedDate, today()).lossDate,
  };
  errors.lossNature = form.lossNature === '' ? 'Select the nature of loss' : undefined;
  errors.claimType = form.claimType === '' ? 'Select the claim type' : undefined;
  errors.lossDescription =
    form.lossDescription.trim() === '' ? 'Enter the loss description' : undefined;
  errors.catastropheEvent =
    form.catastropheCode === '' && form.catastropheEvent.trim() !== ''
      ? 'Select the catastrophe code of the event'
      : undefined;
  errors.claimAmount = amountError(form.claimAmount);
  errors.deductible = amountError(form.deductible);
  errors.initialReserve = amountError(form.initialReserve);
  return errors;
}

function LossDialog({
  claim,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<{
  claim: Claim;
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onSave: (form: ClaimForm) => void;
}>) {
  const [form, setForm] = useState(() => formOf(claim));
  const [errors, setErrors] = useState<ClaimFormErrors>({});
  const save = () => {
    const found = lossErrors(form);
    setErrors(found);
    if (noErrors(found)) {
      onSave(form);
    }
  };
  return (
    <Modal
      title="Change Loss Details"
      open
      onClose={onClose}
      footer={<DialogFooter label="Save Details" busy={busy} onClose={onClose} onConfirm={save} />}
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <LossFields
          form={form}
          errors={errors}
          currency={claim.cover.currency}
          reportedDateEditable={false}
          onChange={(key: LossFieldKey, value: string) => setForm({ ...form, [key]: value })}
        />
      </div>
    </Modal>
  );
}

function Facts({ claim }: Readonly<{ claim: Claim }>) {
  const l = claim.loss;
  const c = claim.cover;
  const money = (v?: number) => (v === undefined ? '—' : `${c.currency} ${formatAmount(v)}`);
  return (
    <div className="grid-2">
      <dl className="detail-list">
        <dt>Source</dt>
        <dd>{SOURCE_LABELS[claim.source]}</dd>
        <dt>Loss date</dt>
        <dd>{formatDate(l.lossDate)}</dd>
        <dt>Reported date</dt>
        <dd>{formatDate(l.reportedDate)}</dd>
        <dt>Nature of loss</dt>
        <dd>{l.lossNatureLabel ?? '—'}</dd>
        <dt>Claim type</dt>
        <dd>{l.claimTypeLabel ?? '—'}</dd>
        <dt>Description</dt>
        <dd>{l.lossDescription ?? '—'}</dd>
        <dt>Place of loss</dt>
        <dd>{l.lossPlace ?? '—'}</dd>
        <dt>Catastrophe</dt>
        <dd>{l.catastropheLabel ? `${l.catastropheLabel} ${l.catastropheEvent ?? ''}` : '—'}</dd>
      </dl>
      <dl className="detail-list">
        <dt>Claimant</dt>
        <dd>
          {l.claimantName ?? '—'}
          {l.claimantOverridden && (
            <span className="muted"> (overridden: {humanize(l.claimantReason ?? '')})</span>
          )}
        </dd>
        <dt>Claim amount</dt>
        <dd>{money(l.claimAmount)}</dd>
        <dt>Deductible</dt>
        <dd>{money(l.deductible)}</dd>
        <dt>Initial loss reserve</dt>
        <dd>{money(l.initialReserve)}</dd>
        <dt>Cover</dt>
        <dd>
          {c.arn} year {c.policyYear} · {c.versionLabel}
        </dd>
        <dt>Period</dt>
        <dd>
          {formatDate(c.periodFrom)} – {formatDate(c.periodTo)}
        </dd>
        <dt>Product / line</dt>
        <dd>
          {c.productCode} · {c.lineCode}
        </dd>
        <dt>Lead insurer</dt>
        <dd>{c.leadInsurerCode ?? '—'}</dd>
      </dl>
    </div>
  );
}

/**
 * Details tab of a claim (BRCLM.004/006/036; FR-CM-011/012/030/033): the loss, the claimant and the
 * cover snapshot, with Change Loss Details (BCL_RECORD), Correct Reported Date
 * (BCL_STATUS_UPDATE) and Override Claimant (BCL_CLAIMANT_OVERRIDE) on an open claim.
 */
export function DetailsTab({ claim, companyId }: Readonly<{ claim: Claim; companyId: number }>) {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [dialog, setDialog] = useState<Dialog>();
  const open = claim.progress.phase !== 'CLOSED';
  const done = (message: string) => {
    toast.success(message);
    setDialog(undefined);
    void queryClient.invalidateQueries({ queryKey: ['broker-claims', 'claim', claim.id] });
  };
  const loss = useMutation({
    mutationFn: (form: ClaimForm) => claimApi.amendLoss(companyId, claim.id, toLoss(form)),
    onSuccess: () => done('Loss details saved'),
  });
  const reported = useMutation({
    mutationFn: (v: { reportedDate: string; reason: string; remark?: string }) =>
      claimApi.correctReportedDate(companyId, claim.id, v),
    onSuccess: () => done('Reported date corrected'),
  });
  const claimant = useMutation({
    mutationFn: (v: { name: string; reason: string }) =>
      claimApi.overrideClaimant(companyId, claim.id, v.name, v.reason),
    onSuccess: () => done('Claimant overridden'),
  });
  return (
    <div className="stack">
      {open && (
        <div className="row">
          {can('BCL_RECORD') && (
            <Button variant="secondary" onClick={() => setDialog('loss')}>
              Change Loss Details
            </Button>
          )}
          {can('BCL_STATUS_UPDATE') && (
            <Button variant="secondary" onClick={() => setDialog('reported')}>
              Correct Reported Date
            </Button>
          )}
          {can('BCL_CLAIMANT_OVERRIDE') && (
            <Button variant="secondary" onClick={() => setDialog('claimant')}>
              Override Claimant
            </Button>
          )}
        </div>
      )}
      <Facts claim={claim} />
      {dialog === 'loss' && (
        <LossDialog
          claim={claim}
          busy={loss.isPending}
          error={loss.error}
          onClose={() => setDialog(undefined)}
          onSave={(form) => loss.mutate(form)}
        />
      )}
      {dialog === 'reported' && (
        <ReportedDateDialog
          claim={claim}
          busy={reported.isPending}
          error={reported.error}
          onClose={() => setDialog(undefined)}
          onSave={(v) => reported.mutate(v)}
        />
      )}
      {dialog === 'claimant' && (
        <ClaimantDialog
          claim={claim}
          busy={claimant.isPending}
          error={claimant.error}
          onClose={() => setDialog(undefined)}
          onSave={(name, reason) => claimant.mutate({ name, reason })}
        />
      )}
    </div>
  );
}
