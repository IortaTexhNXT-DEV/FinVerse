import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { LovLabel } from '@/components/broking/LovLabel';
import { Button } from '@/components/ui/Button';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { formatAmount, formatDate } from '@/utils/format';
import { DialogFooter, InputField, LovField, TextAreaField } from '../record/FormParts';
import { claimStatusApi } from '../status/api';
import type { FollowUp } from './api';
import { followUpReady, handlingApi, toFollowUpInput } from './api';

const COLUMNS: Column<FollowUp>[] = [
  { key: 'on', header: 'Date', render: (f) => formatDate(f.followedUpOn) },
  {
    key: 'type',
    header: 'Type',
    render: (f) => <LovLabel type="BCL_FOLLOW_UP_TYPE" code={f.type} />,
  },
  { key: 'action', header: 'Action Taken', render: (f) => f.actionTaken },
  { key: 'plan', header: 'Next Action Plan', render: (f) => f.nextPlanSummary ?? '' },
  {
    key: 'status',
    header: 'Status as of the Date',
    render: (f) => <LovLabel type="BCL_CLAIM_STATUS" code={f.statusCode} />,
  },
  {
    key: 'eval',
    header: "Adjuster's Evaluation",
    numeric: true,
    render: (f) => formatAmount(f.adjusterEvaluation),
  },
  {
    key: 'ded',
    header: "Insurer's Deductible",
    numeric: true,
    render: (f) => formatAmount(f.insurerDeductible),
  },
  {
    key: 'settle',
    header: 'Settlement Amount',
    numeric: true,
    render: (f) => formatAmount(f.settlementAmount),
  },
  { key: 'by', header: 'By', render: (f) => <UserName login={f.createdBy} /> },
];

function FollowUpDialog({
  claimId,
  companyId,
  onClose,
}: Readonly<{ claimId: number; companyId: number; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<Record<string, string>>({});
  const set = (key: string, value: string) => setForm((f) => ({ ...f, [key]: value }));
  const v = (key: string) => form[key] ?? '';
  const statuses = useQuery({
    queryKey: ['broker-claims', 'allowed-statuses', claimId],
    queryFn: () => claimStatusApi.allowedStatuses(claimId, companyId),
  });
  const input = toFollowUpInput(form);
  const save = useMutation({
    mutationFn: () => handlingApi.recordFollowUp(companyId, claimId, input),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['broker-claims'] });
      toast.success('Follow-up recorded');
      onClose();
    },
  });
  return (
    <Modal
      title="Record Follow-up"
      open
      onClose={onClose}
      footer={
        <DialogFooter
          label="Save Follow-up"
          busy={save.isPending}
          disabled={!followUpReady(input)}
          onClose={onClose}
          onConfirm={() => save.mutate()}
        />
      }
    >
      <ErrorAlert error={save.error ?? statuses.error} />
      <div className="form-grid">
        <LovField
          label="Follow-up type"
          type="BCL_FOLLOW_UP_TYPE"
          required
          value={v('type')}
          onChange={(v) => set('type', v)}
        />
        <Field label="Status as of today">
          {(id) => (
            <select
              id={id}
              className="select"
              value={v('statusCode')}
              onChange={(e) => set('statusCode', e.target.value)}
            >
              <option value="">Keep the current status</option>
              {(statuses.data ?? []).map((o) => (
                <option key={o.code} value={o.code}>
                  {o.label}
                </option>
              ))}
            </select>
          )}
        </Field>
        <TextAreaField
          label="Action taken today"
          required
          value={v('actionTaken')}
          onChange={(v) => set('actionTaken', v)}
        />
        <InputField
          label="Next action plan summary"
          maxLength={500}
          value={v('nextPlanSummary')}
          onChange={(v) => set('nextPlanSummary', v)}
        />
        <TextAreaField
          label="Next action plan detail"
          value={v('nextPlanDetail')}
          onChange={(v) => set('nextPlanDetail', v)}
        />
        <InputField
          label="Adjuster's evaluation"
          type="number"
          value={v('adjusterEvaluation')}
          onChange={(v) => set('adjusterEvaluation', v)}
        />
        <InputField
          label="Insurer's deductible"
          type="number"
          value={v('insurerDeductible')}
          onChange={(v) => set('insurerDeductible', v)}
        />
        <InputField
          label="Settlement amount"
          type="number"
          value={v('settlementAmount')}
          onChange={(v) => set('settlementAmount', v)}
        />
      </div>
    </Modal>
  );
}

/**
 * The follow-ups of a claim with the fields of the BRD scope (FRS BRD-07 FRCLM.009.04): type,
 * action taken today, next action plan, status as of the date, adjuster's evaluation, insurer's
 * deductible and settlement amount; a new follow-up may change the status of the claim.
 */
export function FollowUpsTab({
  claimId,
  companyId,
  open,
}: Readonly<{ claimId: number; companyId: number; open: boolean }>) {
  const { can } = useAuth();
  const [adding, setAdding] = useState(false);
  const list = useQuery({
    queryKey: ['broker-claims', 'follow-ups', claimId],
    queryFn: () => handlingApi.followUps(companyId, claimId),
  });
  return (
    <div className="stack">
      {open && can('BCL_STATUS_UPDATE') && (
        <div>
          <Button variant="secondary" icon={<Plus size={14} />} onClick={() => setAdding(true)}>
            Record Follow-up
          </Button>
        </div>
      )}
      <ErrorAlert error={list.error} />
      <DataTable
        caption="Follow-ups"
        columns={COLUMNS}
        rows={list.data ?? []}
        rowKey={(f) => f.id}
        emptyMessage="No follow-up recorded yet."
      />
      {adding && (
        <FollowUpDialog claimId={claimId} companyId={companyId} onClose={() => setAdding(false)} />
      )}
    </div>
  );
}
