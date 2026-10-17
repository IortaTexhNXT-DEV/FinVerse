import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { csfCaseApi } from '@/api/csfCases';
import type { CaseAction, CaseRecord } from '@/api/csfCases';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { ACTION_LABELS, CASE_LOV } from './caseCodes';

type Input =
  | 'resolution'
  | 'closure'
  | 'returnReason'
  | 'instruction'
  | 'comment'
  | 'reassign'
  | 'escalation'
  | 'category'
  | 'reason';

/** The inputs of each action and the path it posts to. */
const SPEC: Record<string, { path: string; inputs: Input[] }> = {
  RESOLVE: { path: 'resolve', inputs: ['resolution', 'closure'] },
  CLOSE: { path: 'close', inputs: ['resolution', 'closure'] },
  RETURN: { path: 'return', inputs: ['returnReason', 'comment'] },
  CSR_ACTION: { path: 'csr-action', inputs: ['instruction'] },
  REENDORSE: { path: 'reendorse', inputs: ['comment'] },
  REASSIGN: { path: 'reassign', inputs: ['reassign', 'comment'] },
  ESCALATE: { path: 'escalate', inputs: ['escalation', 'comment'] },
  CHANGE_CATEGORY: { path: 'category', inputs: ['category', 'reason'] },
  FOLLOW_UP: { path: 'follow-up', inputs: ['reason'] },
};

function bodyOf(action: CaseAction, v: Record<string, string>): Record<string, unknown> {
  switch (action) {
    case 'RESOLVE':
    case 'CLOSE':
      return { resolution: v.resolution, closureReason: v.closure };
    case 'RETURN':
      return { reason: v.returnReason, comment: v.comment };
    case 'ESCALATE':
      return { reason: v.escalation, comment: v.comment };
    case 'REASSIGN':
      return { toUser: v.toUser, toUnit: v.toUnit, comment: v.comment };
    case 'CHANGE_CATEGORY':
      return { caseType: v.caseType, nature: v.nature, reason: v.reason };
    case 'CSR_ACTION':
      return { text: v.instruction };
    case 'FOLLOW_UP':
      return { text: v.reason };
    default:
      return { text: v.comment };
  }
}

/**
 * The dialog of a case action (FR-CSF-051, 052, 056, 058, 060): resolve and close, return, CSR
 * action, re-endorse, close, reassign, escalate, change category and create a follow-up case.
 */
export function CaseActionDialog({
  companyId,
  record,
  action,
  onClose,
}: Readonly<{
  companyId: number;
  record: CaseRecord;
  action: CaseAction;
  onClose: () => void;
}>) {
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const spec = SPEC[action] ?? { path: '', inputs: [] };
  const [v, setV] = useState<Record<string, string>>({
    caseType: record.row.caseType.code,
    nature: record.row.nature.code,
  });
  const set = (key: string) => (value: string) => setV((old) => ({ ...old, [key]: value }));
  const run = useMutation({
    mutationFn: () => csfCaseApi.act(companyId, record.row.id, spec.path, bodyOf(action, v)),
    onSuccess: (r) => {
      toast.success(`${r.row.caseNo}: ${r.row.statusLabel}`);
      void queryClient.invalidateQueries({ queryKey: ['csf-cases'] });
      onClose();
      if (r.row.id !== record.row.id) {
        void navigate(`/csf/cases/${String(r.row.id)}`);
      }
    },
  });
  const has = (i: Input) => spec.inputs.includes(i);
  const text = (key: string, label: string, required: boolean, rows = 3) => (
    <Field label={label} required={required}>
      {(id) => (
        <textarea
          id={id}
          className="textarea"
          rows={rows}
          maxLength={4000}
          value={v[key] ?? ''}
          onChange={(e) => set(key)(e.target.value)}
        />
      )}
    </Field>
  );
  const lov = (key: string, label: string, type: string, required: boolean) => (
    <Field label={label} required={required}>
      {(id) => (
        <LovSelect
          id={id}
          type={type}
          value={v[key] ?? ''}
          onChange={set(key)}
          required={required}
        />
      )}
    </Field>
  );
  return (
    <Modal
      title={`${ACTION_LABELS[action]} ${record.row.caseNo}`}
      open
      onClose={onClose}
      facts={[
        { label: 'Status', value: record.row.statusLabel },
        { label: 'Due Date', value: record.row.dueDate },
      ]}
      footer={
        <Button variant="accent" busy={run.isPending} onClick={() => run.mutate()}>
          {ACTION_LABELS[action]}
        </Button>
      }
    >
      <div className="stack">
        <ErrorAlert error={run.error} title="The action was not saved" />
        {has('resolution') && text('resolution', 'Resolution', true, 4)}
        {has('closure') && lov('closure', 'Closure Reason', CASE_LOV.closure, true)}
        {has('returnReason') && lov('returnReason', 'Return Reason', CASE_LOV.returnReason, true)}
        {has('instruction') && text('instruction', 'Instruction to the Agent', true, 4)}
        {has('escalation') && lov('escalation', 'Escalation Reason', CASE_LOV.escalation, true)}
        {has('reassign') && (
          <>
            <Field label="Reassign to Agent">
              {(id) => (
                <input
                  id={id}
                  className="input"
                  placeholder="User ID"
                  maxLength={50}
                  value={v.toUser ?? ''}
                  onChange={(e) => set('toUser')(e.target.value)}
                />
              )}
            </Field>
            {lov('toUnit', 'Or to Fulfilment Unit', CASE_LOV.unit, false)}
          </>
        )}
        {has('category') && (
          <>
            {lov('caseType', 'Case Type', CASE_LOV.type, true)}
            {lov('nature', 'Nature of Transaction', CASE_LOV.nature, true)}
          </>
        )}
        {has('reason') && text('reason', 'Reason', true, 2)}
        {has('comment') && text('comment', 'Comment', false, 2)}
      </div>
    </Modal>
  );
}
