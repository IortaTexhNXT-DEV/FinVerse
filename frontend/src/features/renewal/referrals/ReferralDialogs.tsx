import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { renewalReferralsApi } from '@/api/renewalReferrals';
import type { NewBusinessInput, ReferralOutcome, ReferralView } from '@/api/renewalReferrals';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useCompanyId } from '@/context/workspaceContext';
import { UnitSelect } from '../common/ActionDialogs';

interface Common {
  busy: boolean;
  error: unknown;
  onClose: () => void;
}

/**
 * Transfer to Other Unit (FRRN.011.01): the receiving Marketing unit and the justification; the
 * request is submitted (Pending Acceptance) or saved as a Draft. The renewal account keeps its
 * unit and officer.
 */
export function ReferralRequestDialog({
  renewalRef,
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<
  Common & {
    renewalRef: string;
    onConfirm: (toUnit: string, justification: string, submit: boolean) => void;
  }
>) {
  const [unit, setUnit] = useState('');
  const [text, setText] = useState('');
  const ready = unit.trim() !== '' && text.trim() !== '';
  return (
    <Modal
      open
      size="md"
      title={`Transfer to Other Unit ${renewalRef}`}
      onClose={onClose}
      helper="The receiving unit opens a New Business account; this renewal account keeps its unit and officer."
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="secondary"
            disabled={!ready}
            busy={busy}
            onClick={() => onConfirm(unit.trim(), text.trim(), false)}
          >
            Save as Draft
          </Button>
          <Button
            disabled={!ready}
            busy={busy}
            onClick={() => onConfirm(unit.trim(), text.trim(), true)}
          >
            Submit
          </Button>
        </>
      }
    >
      <ErrorAlert error={error} />
      <Field label="Receiving Marketing Unit" required>
        {(id) => <UnitSelect id={id} value={unit} onChange={setUnit} />}
      </Field>
      <Field label="Business Justification" required>
        {(id) => (
          <textarea
            id={id}
            className="input"
            rows={4}
            maxLength={1000}
            value={text}
            onChange={(e) => setText(e.target.value)}
          />
        )}
      </Field>
    </Modal>
  );
}

const OUTCOME_TITLES: Record<ReferralOutcome, string> = {
  ACCEPTED: 'Accept Transfer Request',
  REJECTED: 'Reject Transfer Request',
  RETURNED: 'Return Transfer Request for Clarification',
};

/** Accept, reject (remarks required) or return for clarification (remarks required). */
export function ReferralDecisionDialog({
  referral,
  outcome,
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<
  Common & {
    referral: ReferralView;
    outcome: ReferralOutcome;
    onConfirm: (remarks: string) => void;
  }
>) {
  const [remarks, setRemarks] = useState('');
  const required = outcome !== 'ACCEPTED';
  return (
    <Modal
      open
      size="md"
      title={`${OUTCOME_TITLES[outcome]} ${referral.referralNo}`}
      onClose={onClose}
      facts={[
        { label: 'Renewal account', value: referral.renewalRef },
        { label: 'From unit', value: referral.fromUnit ?? '' },
        { label: 'Justification', value: referral.justification },
      ]}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={busy}
            disabled={required && remarks.trim() === ''}
            onClick={() => onConfirm(remarks.trim())}
          >
            Confirm
          </Button>
        </>
      }
    >
      <ErrorAlert error={error} />
      <Field label="Remarks" required={required}>
        {(id) => (
          <textarea
            id={id}
            className="input"
            rows={3}
            maxLength={1000}
            value={remarks}
            onChange={(e) => setRemarks(e.target.value)}
          />
        )}
      </Field>
    </Modal>
  );
}

/**
 * Create New Business Account (FRRN.011.03): the data copied from the renewal account, which the
 * receiving unit reviews and changes before saving.
 */
export function NewBusinessDialog({
  referral,
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<Common & { referral: ReferralView; onConfirm: (input: NewBusinessInput) => void }>) {
  const companyId = useCompanyId();
  const proposal = useQuery({
    queryKey: ['renewal', 'referral-nb', companyId, referral.id],
    queryFn: () => renewalReferralsApi.proposal(companyId, referral.id),
  });
  return (
    <Modal
      open
      size="lg"
      title={`Create New Business Account ${referral.referralNo}`}
      onClose={onClose}
      helper="The data is copied from the renewal account; change it before saving."
    >
      <ErrorAlert error={error ?? proposal.error} />
      {proposal.data && (
        <NewBusinessForm
          initial={proposal.data}
          busy={busy}
          onClose={onClose}
          onConfirm={onConfirm}
        />
      )}
    </Modal>
  );
}

function NewBusinessForm({
  initial,
  busy,
  onClose,
  onConfirm,
}: Readonly<{
  initial: NewBusinessInput;
  busy: boolean;
  onClose: () => void;
  onConfirm: (input: NewBusinessInput) => void;
}>) {
  const [v, setV] = useState<NewBusinessInput>(initial);
  const set = (patch: Partial<NewBusinessInput>) => setV((old) => ({ ...old, ...patch }));
  return (
    <div className="stack">
      <Field label="Risk Code" required>
        {(id) => (
          <input
            id={id}
            className="input"
            value={v.productCode ?? ''}
            onChange={(e) => set({ productCode: e.target.value })}
          />
        )}
      </Field>
      <Field label="Market Segment" required>
        {(id) => (
          <LovSelect
            id={id}
            type="MARKET_SEGMENT"
            value={v.marketSegment ?? ''}
            onChange={(code) => set({ marketSegment: code })}
          />
        )}
      </Field>
      <Field label="Period From" required>
        {(id) => (
          <DateInput
            id={id}
            value={v.periodFrom ?? ''}
            onChange={(e) => set({ periodFrom: e.target.value })}
          />
        )}
      </Field>
      <Field label="Period To" required>
        {(id) => (
          <DateInput
            id={id}
            value={v.periodTo ?? ''}
            onChange={(e) => set({ periodTo: e.target.value })}
          />
        )}
      </Field>
      <label className="checkbox">
        <input
          type="checkbox"
          checked={v.copyRiskItems}
          onChange={(e) => set({ copyRiskItems: e.target.checked })}
        />{' '}
        Copy the insured items
      </label>
      <div className="rnw-actions">
        <Button variant="secondary" onClick={onClose}>
          Cancel
        </Button>
        <Button busy={busy} onClick={() => onConfirm(v)}>
          Create New Business Account
        </Button>
      </div>
    </div>
  );
}
