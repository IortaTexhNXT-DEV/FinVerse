import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { DEACTIVATION_ENTITY, deactivationApi } from '@/api/pmWorkspace';
import type { Deactivation } from '@/api/pmWorkspace';
import { useAuth } from '@/auth/authContext';
import { Attachments } from '@/components/attachments/Attachments';
import { LovLabel } from '@/components/broking/LovLabel';
import { Button } from '@/components/ui/Button';
import { Combobox } from '@/components/ui/Combobox';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { useDisplayName } from '@/components/ui/useDisplayName';
import { formatDate, formatDateTime } from '@/utils/format';
import { RecordAuditLog } from './RecordAuditLog';
import { deactivationChoices } from './deactivationForm';
import type { Decision } from './deactivationForm';

function facts(r: Deactivation) {
  return [
    { label: 'Request Number', value: r.requestNo },
    { label: 'Package', value: `${r.packageName} (${r.productCode})` },
    { label: 'Deactivation Effective Date', value: formatDate(r.effectiveDate) },
    {
      label: 'Approval Status',
      value: <StatusBadge status={r.status} label={r.statusLabel} full />,
    },
    { label: 'Reason', value: <LovLabel type="PKG_DEACTIVATION_REASON" code={r.reason} /> },
    { label: 'Remarks', value: r.remarks ?? '' },
    { label: 'Requested By', value: <UserName login={r.requestedBy} /> },
    { label: 'Request Date', value: formatDateTime(r.requestedAt) },
    { label: 'Approver', value: <UserName login={r.approver} /> },
    { label: 'Package Expiry Date', value: formatDate(r.packageExpiryDate) },
    { label: 'Approval Remarks', value: r.decisionRemarks ?? '' },
    { label: 'Approval Date and Time', value: formatDateTime(r.decidedAt) },
    { label: 'New Expiry Date', value: formatDate(r.expiryDate) },
  ];
}

function DecisionFields({
  choices,
  current,
  approvers,
  remarks,
  approver,
  error,
  onRemarks,
  onApprover,
}: Readonly<{
  choices: Decision[];
  current: string;
  approvers: string[];
  remarks: string;
  approver: string;
  error: string | null;
  onRemarks: (text: string) => void;
  onApprover: (user: string) => void;
}>) {
  const name = useDisplayName();
  const deciding = choices.includes('approve');
  return (
    <>
      {deciding && (
        <Field label="Approval Remarks" error={error ?? undefined}>
          {(fid) => (
            <textarea
              id={fid}
              className="input"
              rows={3}
              maxLength={1000}
              value={remarks}
              onChange={(e) => onRemarks(e.target.value)}
            />
          )}
        </Field>
      )}
      {choices.includes('reassign') && (
        <Field label="Reassign To" error={deciding ? undefined : (error ?? undefined)}>
          {(fid) => (
            <Combobox
              id={fid}
              placeholder="Select the new approver"
              value={approver}
              options={approvers
                .filter((u) => u !== current)
                .map((u) => ({ value: u, label: name(u) }))}
              onChange={onApprover}
            />
          )}
        </Field>
      )}
    </>
  );
}

function DecisionButtons({
  choices,
  busy,
  onClose,
  onRun,
}: Readonly<{
  choices: Decision[];
  busy: boolean;
  onClose: () => void;
  onRun: (d: Decision) => void;
}>) {
  return (
    <>
      <Button variant="secondary" onClick={onClose}>
        Close
      </Button>
      {choices.includes('cancel') && (
        <Button variant="ghost" busy={busy} onClick={() => onRun('cancel')}>
          Withdraw
        </Button>
      )}
      {choices.includes('reassign') && (
        <Button variant="secondary" busy={busy} onClick={() => onRun('reassign')}>
          Reassign
        </Button>
      )}
      {choices.includes('reject') && (
        <Button variant="danger" busy={busy} onClick={() => onRun('reject')}>
          Reject
        </Button>
      )}
      {choices.includes('approve') && (
        <Button busy={busy} onClick={() => onRun('approve')}>
          Approve
        </Button>
      )}
    </>
  );
}

/**
 * A deactivation request (BDOI FRS FRPM.003.06 and FRPM.003.07): its details and supporting
 * documents; the selected approver approves or rejects with remarks (mandatory for a rejection),
 * the requestor may withdraw it and a team head may give it to another approver.
 */
export function DeactivationDetail({ id, onClose }: Readonly<{ id: number; onClose: () => void }>) {
  const { user, can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [remarks, setRemarks] = useState('');
  const [approver, setApprover] = useState('');
  const [error, setError] = useState<string | null>(null);
  const request = useQuery({
    queryKey: ['pm-deactivations', id],
    queryFn: () => deactivationApi.get(id),
  });
  const mayReassign = can('WORK_ASSIGN') || can('PKG_TSU_APPROVE');
  const settings = useQuery({
    queryKey: ['pm-deactivation', 'settings'],
    queryFn: () => deactivationApi.settings(),
    enabled: mayReassign,
  });
  const act = useMutation({
    mutationFn: (decision: Decision) => {
      switch (decision) {
        case 'approve':
          return deactivationApi.approve(id, remarks || undefined);
        case 'reject':
          return deactivationApi.reject(id, remarks);
        case 'reassign':
          return deactivationApi.reassign(id, approver);
        default:
          return deactivationApi.cancel(id);
      }
    },
    onSuccess: async (r) => {
      toast.success(`${r.requestNo}: ${r.statusLabel}`);
      await queryClient.invalidateQueries({ queryKey: ['pm-deactivations'] });
      await queryClient.invalidateQueries({ queryKey: ['pm-matrix'] });
    },
  });
  const run = (decision: Decision) => {
    if (decision === 'reject' && remarks.trim() === '') {
      setError('Enter the approval remarks of the rejection');
      return;
    }
    if (decision === 'reassign' && approver === '') {
      setError('Select the new approver');
      return;
    }
    setError(null);
    act.mutate(decision);
  };
  const r = request.data;
  const choices = r === undefined ? [] : deactivationChoices(r, user?.username, mayReassign);
  const footer = (
    <DecisionButtons choices={choices} busy={act.isPending} onClose={onClose} onRun={run} />
  );
  return (
    <Modal title="Deactivation Request" open onClose={onClose} footer={footer} size="lg">
      <ErrorAlert error={request.error ?? act.error} />
      {r !== undefined && (
        <div className="stack">
          <DefinitionGrid items={facts(r)} />
          <DecisionFields
            choices={choices}
            current={r.approver}
            approvers={settings.data?.approvers ?? []}
            remarks={remarks}
            approver={approver}
            error={error}
            onRemarks={setRemarks}
            onApprover={setApprover}
          />
          <Attachments
            entityType={DEACTIVATION_ENTITY}
            entityId={r.id}
            title="Supporting Documents"
            reference={r.requestNo}
          />
          <RecordAuditLog reference={r.requestNo} />
        </div>
      )}
    </Modal>
  );
}
