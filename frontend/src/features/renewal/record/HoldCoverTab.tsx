import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import type { CandidateDetail } from '@/api/renewal';
import { renewalHoldCoverApi } from '@/api/renewalHoldCover';
import type { HoldCoverView } from '@/api/renewalHoldCover';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DateInput } from '@/components/ui/DateInput';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import type { Definition } from '@/components/ui/DefinitionGrid';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { Notice } from '@/components/ui/Notice';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { HoldCoverRequestsCard } from '../holdcover/HoldCoverRequestsCard';
import { InsurerAllocationCard } from '../holdcover/InsurerAllocationCard';
import { formatDate, formatDateTime } from '@/utils/format';
import { TextDialog } from '../common/ActionDialogs';
import { holdCoverActions, holdCoverStatusLabel } from '../common/holdCover';

type Props = Readonly<{ detail: CandidateDetail }>;
type Dialog = 'request' | 'confirm' | 'decline' | 'cancel' | null;

/** The details of a hold cover shown on the tab. */
function holdCoverItems(current: HoldCoverView): Definition[] {
  return [
    { label: 'Status', value: holdCoverStatusLabel(current.status) },
    {
      label: 'Coverage',
      value: `${formatDate(current.startDate)} to ${formatDate(current.expiryDate)}`,
    },
    {
      label: 'Duration',
      value: current.durationDays ? `${current.durationDays} days` : '',
    },
    { label: 'Expiring policy', value: current.expiringPolicyNo ?? '' },
    { label: 'Requested by', value: <UserName login={current.requestedBy} /> },
    { label: 'Requested on', value: formatDateTime(current.requestedAt) },
    { label: 'Insurer reference', value: current.insurerRef ?? '' },
    { label: 'Confirmed on', value: formatDate(current.confirmedOn) },
    { label: 'Conditions', value: current.conditions ?? '', wide: true },
    { label: 'Remarks', value: current.remarks ?? '', wide: true },
    { label: 'Cancellation reason', value: current.cancelReason ?? '', wide: true },
  ];
}

/** The hold cover actions the user may take. */
function HoldCoverButtons({
  actions,
  onOpen,
}: Readonly<{ actions: ReturnType<typeof holdCoverActions>; onOpen: (d: Dialog) => void }>) {
  return (
    <span className="rnw-actions">
      {actions.request && <Button onClick={() => onOpen('request')}>Request Hold Cover</Button>}
      {actions.confirm && (
        <>
          <Button variant="secondary" onClick={() => onOpen('confirm')}>
            Record Confirmation
          </Button>
          <Button variant="secondary" onClick={() => onOpen('decline')}>
            Record Decline
          </Button>
        </>
      )}
      {actions.cancel && (
        <Button variant="ghost" onClick={() => onOpen('cancel')}>
          Cancel Hold Cover
        </Button>
      )}
    </span>
  );
}

/**
 * Hold cover of a renewal (FR-RN-086): request it from the insurer with the duration and remarks,
 * record the insurer's confirmation or decline, or cancel it with a reason. A confirmed hold cover
 * moves the effective expiry date of the renewal to its end.
 */
export function HoldCoverTab({ detail }: Props) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const ref = detail.row.renewalRef;
  const [dialog, setDialog] = useState<Dialog>(null);
  const key = ['renewal', 'hold-cover', companyId, ref];
  const panel = useQuery({
    queryKey: key,
    queryFn: () => renewalHoldCoverApi.get(companyId, ref),
  });
  const done = async (text: string) => {
    setDialog(null);
    toast.success(text);
    await queryClient.invalidateQueries({ queryKey: ['renewal'] });
  };
  const decline = useMutation({
    mutationFn: (reason: string) => renewalHoldCoverApi.decline(companyId, ref, reason),
    onSuccess: () => done('Decline of the insurer recorded'),
  });
  const cancel = useMutation({
    mutationFn: (reason: string) => renewalHoldCoverApi.cancel(companyId, ref, reason),
    onSuccess: () => done('Hold cover cancelled'),
  });
  const current = panel.data?.current ?? null;
  const hasAccount = detail.lifecycle.links.renewalArn !== null;
  const actions = holdCoverActions(current, hasAccount, can);
  return (
    <div className="stack">
      <ErrorAlert error={panel.error} />
      {!hasAccount && (
        <Notice tone="info">
          A hold cover is requested once the renewal account is created by Processing.
        </Notice>
      )}
      <Card title="Hold cover" actions={<HoldCoverButtons actions={actions} onOpen={setDialog} />}>
        {current === null ? (
          <p className="muted">No hold cover has been requested for this renewal.</p>
        ) : (
          <DefinitionGrid columns={2} items={holdCoverItems(current)} collapseEmpty />
        )}
      </Card>
      {dialog === 'request' && (
        <RequestDialog
          detail={detail}
          durations={panel.data?.durations ?? [30]}
          onClose={() => setDialog(null)}
          onDone={() => void done('Hold cover requested from the insurer')}
        />
      )}
      {dialog === 'confirm' && (
        <ConfirmDialog
          detail={detail}
          onClose={() => setDialog(null)}
          onDone={() => void done('Confirmation of the insurer recorded')}
        />
      )}
      {dialog === 'decline' && (
        <TextDialog
          title="Record the decline of the insurer"
          label="Insurer reference or remarks"
          confirmLabel="Record Decline"
          busy={decline.isPending}
          error={decline.error}
          onClose={() => setDialog(null)}
          onConfirm={(text) => decline.mutate(text)}
        />
      )}
      {dialog === 'cancel' && (
        <TextDialog
          title="Cancel the hold cover"
          label="Reason"
          confirmLabel="Cancel Hold Cover"
          busy={cancel.isPending}
          error={cancel.error}
          onClose={() => setDialog(null)}
          onConfirm={(text) => cancel.mutate(text)}
        />
      )}
      <HoldCoverRequestsCard renewalRef={ref} />
      <InsurerAllocationCard renewalRef={ref} />
    </div>
  );
}

function RequestDialog({
  detail,
  durations,
  onClose,
  onDone,
}: Readonly<{
  detail: CandidateDetail;
  durations: number[];
  onClose: () => void;
  onDone: () => void;
}>) {
  const companyId = useCompanyId();
  const [days, setDays] = useState(durations[0] ?? 30);
  const [start, setStart] = useState(detail.row.expiry);
  const [remarks, setRemarks] = useState('');
  const request = useMutation({
    mutationFn: () =>
      renewalHoldCoverApi.request(companyId, detail.row.renewalRef, {
        startDate: start,
        durationDays: days,
        remarks: remarks.trim() || undefined,
      }),
    onSuccess: onDone,
  });
  return (
    <Modal
      open
      title="Request hold cover"
      onClose={onClose}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={request.isPending} disabled={!start} onClick={() => request.mutate()}>
            Send Request
          </Button>
        </>
      }
    >
      <ErrorAlert error={request.error} />
      <div className="form-grid">
        <Field label="Coverage start date" required hint="The policy expiry date by default">
          {(id) => <DateInput id={id} value={start} onChange={(e) => setStart(e.target.value)} />}
        </Field>
        <Field label="Duration" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={days}
              onChange={(e) => setDays(Number(e.target.value))}
            >
              {durations.map((d) => (
                <option key={d} value={d}>
                  {d} days
                </option>
              ))}
            </select>
          )}
        </Field>
      </div>
      <Field label="Remarks" hint="Up to 200 characters">
        {(id) => (
          <textarea
            id={id}
            className="input"
            maxLength={200}
            rows={3}
            value={remarks}
            onChange={(e) => setRemarks(e.target.value)}
          />
        )}
      </Field>
    </Modal>
  );
}

function ConfirmDialog({
  detail,
  onClose,
  onDone,
}: Readonly<{ detail: CandidateDetail; onClose: () => void; onDone: () => void }>) {
  const companyId = useCompanyId();
  const [reference, setReference] = useState('');
  const [confirmedOn, setConfirmedOn] = useState('');
  const [validTo, setValidTo] = useState('');
  const [conditions, setConditions] = useState('');
  const confirm = useMutation({
    mutationFn: () =>
      renewalHoldCoverApi.confirm(companyId, detail.row.renewalRef, {
        reference: reference.trim(),
        confirmedOn: confirmedOn || undefined,
        validTo,
        conditions: conditions.trim() || undefined,
      }),
    onSuccess: onDone,
  });
  return (
    <Modal
      open
      title="Record the confirmation of the insurer"
      onClose={onClose}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={confirm.isPending}
            disabled={reference.trim() === '' || validTo === ''}
            onClick={() => confirm.mutate()}
          >
            Record Confirmation
          </Button>
        </>
      }
    >
      <ErrorAlert error={confirm.error} />
      <div className="form-grid">
        <Field label="Insurer reference" required>
          {(id) => (
            <input
              id={id}
              className="input"
              value={reference}
              onChange={(e) => setReference(e.target.value)}
            />
          )}
        </Field>
        <Field label="Confirmed on">
          {(id) => (
            <DateInput
              id={id}
              value={confirmedOn}
              onChange={(e) => setConfirmedOn(e.target.value)}
            />
          )}
        </Field>
        <Field label="Valid until" required hint="Becomes the effective expiry date">
          {(id) => (
            <DateInput id={id} value={validTo} onChange={(e) => setValidTo(e.target.value)} />
          )}
        </Field>
      </div>
      <Field label="Conditions">
        {(id) => (
          <textarea
            id={id}
            className="input"
            maxLength={500}
            rows={3}
            value={conditions}
            onChange={(e) => setConditions(e.target.value)}
          />
        )}
      </Field>
    </Modal>
  );
}
