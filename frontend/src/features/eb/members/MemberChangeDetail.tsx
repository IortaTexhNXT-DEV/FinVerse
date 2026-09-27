import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import type { BillingInput, MemberChange } from '@/api/ebMarket';
import { ebServiceApi } from '@/api/ebService';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { DateInput } from '@/components/ui/DateInput';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { FileDropZone } from '@/components/ui/FileDropZone';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { DialogFooter } from '../common/DialogFooter';
import { ebLabel } from '../common/ebCodes';
import { useEbMutation } from '../common/useEbMutation';
import { SubmitDialog } from '../submission/SubmissionsTab';

type Line = MemberChange['lines'][number];

const LINE_COLUMNS: Column<Line>[] = [
  { key: 'n', header: '#', kind: 'center', render: (l) => l.sortOrder },
  { key: 'action', header: 'Change', render: (l) => ebLabel(l.action) },
  { key: 'no', header: 'Employee No.', kind: 'code', render: (l) => l.employeeNo },
  {
    key: 'name',
    header: 'Name',
    render: (l) => [l.lastName, l.firstName].filter(Boolean).join(', '),
  },
  { key: 'plan', header: 'Plan', kind: 'code', render: (l) => l.planCode ?? '' },
  { key: 'eff', header: 'Effective', kind: 'date', render: (l) => formatDate(l.effectiveDate) },
];

/** Record Billing: the insurer's billing of the change; a direct billing comes with its file. */
function BillDialog({ change, onClose }: Readonly<{ change: MemberChange; onClose: () => void }>) {
  const [input, setInput] = useState<BillingInput>({
    billedOn: '',
    reference: '',
    amount: '',
    direct: false,
  });
  const [files, setFiles] = useState<File[]>([]);
  const [submitted, setSubmitted] = useState(false);
  const bill = useEbMutation(
    (companyId, v: BillingInput) => ebServiceApi.billChange(companyId, change.id, v, files),
    `Billing of ${change.changeNo} recorded`,
    onClose,
  );
  const fileMissing = input.direct && files.length === 0;
  const amountMissing = change.financial && input.amount.trim() === '';
  const save = () => {
    setSubmitted(true);
    if (!fileMissing && !amountMissing) {
      bill.mutate(input);
    }
  };
  return (
    <Modal
      open
      title={`Record Billing – ${change.changeNo}`}
      onClose={onClose}
      footer={<DialogFooter busy={bill.isPending} label="Record" onClose={onClose} onSave={save} />}
    >
      <div className="stack">
        <ErrorAlert error={bill.error} />
        <div className="form-grid">
          <Field label="Billed On" hint="Today when blank">
            {(id) => (
              <DateInput
                id={id}
                value={input.billedOn}
                onChange={(e) => setInput({ ...input, billedOn: e.target.value })}
              />
            )}
          </Field>
          <Field label="Billing Reference">
            {(id) => (
              <input
                id={id}
                className="input"
                value={input.reference}
                onChange={(e) => setInput({ ...input, reference: e.target.value })}
              />
            )}
          </Field>
          <Field
            label="Amount"
            required={change.financial}
            error={submitted && amountMissing ? 'Enter the amount billed' : undefined}
          >
            {(id) => (
              <input
                id={id}
                className="input"
                inputMode="decimal"
                value={input.amount}
                onChange={(e) => setInput({ ...input, amount: e.target.value })}
              />
            )}
          </Field>
          <label className="checkbox">
            <input
              type="checkbox"
              checked={input.direct}
              onChange={(e) => setInput({ ...input, direct: e.target.checked })}
            />
            Billed directly by the insurer
          </label>
        </div>
        {input.direct && (
          <Field
            label="Direct Billing"
            required
            error={submitted && fileMissing ? "Attach the insurer's direct billing" : undefined}
          >
            {(id) => (
              <FileDropZone
                id={id}
                multiple
                accept=".pdf,.xls,.xlsx"
                maxSizeMb={10}
                onChange={setFiles}
              />
            )}
          </Field>
        )}
      </div>
    </Modal>
  );
}

/** The actions offered on a change in its status. */
function Actions({ c, onAct }: Readonly<{ c: MemberChange; onAct: (a: string) => void }>) {
  const { can } = useAuth();
  const market = can('EB_MARKET');
  const either = market || can('EB_PROCESS');
  return (
    <>
      {either && ['RELAYED', 'BILLED'].includes(c.status) && (
        <Button variant="secondary" onClick={() => onAct('submit')}>
          Submit to Insurer
        </Button>
      )}
      {market && c.status === 'CAPTURED' && (
        <Button onClick={() => onAct('relay')}>Relay to Insurer</Button>
      )}
      {either && c.status === 'RELAYED' && (
        <Button onClick={() => onAct('bill')}>Record Billing</Button>
      )}
      {can('EB_PROCESS') && c.status === 'BILLED' && (
        <Button onClick={() => onAct('validate')}>Validate</Button>
      )}
      {either && c.status === 'VALIDATED' && (
        <Button onClick={() => onAct('close')}>Close and Apply</Button>
      )}
    </>
  );
}

const STATUS_DONE: Record<string, string> = {
  RELAYED: 'relayed to the insurer',
  VALIDATED: 'validated',
  CLOSED: 'closed and applied to the roster',
};

/** The facts and member lines of a change. */
function ChangeFacts({ c }: Readonly<{ c: MemberChange }>) {
  return (
    <>
      <DefinitionGrid
        columns={2}
        items={[
          { label: 'Status', value: <StatusBadge status={c.status} /> },
          { label: 'Programme', value: c.programmeNo ?? '' },
          { label: 'Client', value: c.clientName ?? '' },
          { label: 'Benefit Line', value: `Line ${String(c.lineNo)} – ${c.benefitLine}` },
          { label: 'Policy Year', value: String(c.policyYear) },
          { label: 'Requested By', value: ebLabel(c.source) },
          { label: 'Affects Premium', value: c.financial ? 'Yes' : 'No' },
          { label: 'Relayed', value: formatDateTime(c.relayedAt) },
          { label: 'Billed On', value: formatDate(c.billedOn) },
          {
            label: 'Billing',
            value: [c.billingRef, c.directBilled ? 'direct' : ''].filter(Boolean).join(' – '),
          },
          { label: 'Amount', value: <Amount value={c.billedAmount} /> },
          { label: 'Endorsement Request', value: c.endorsementRequestNo ?? '' },
          {
            label: 'Validated By',
            value: c.validatedBy ? <UserName login={c.validatedBy} /> : '',
          },
          { label: 'Captured', value: formatDateTime(c.createdAt) },
          { label: 'Description', value: c.description ?? '' },
        ]}
      />
      <DataTable<Line> rows={c.lines} rowKey={(l) => l.sortOrder} columns={LINE_COLUMNS} />
    </>
  );
}

/** Record Billing or Submit to Insurer on a change. */
function ChangeDialog({
  c,
  dialog,
  onClose,
}: Readonly<{ c: MemberChange; dialog: string; onClose: () => void }>) {
  if (dialog === 'bill') {
    return <BillDialog change={c} onClose={onClose} />;
  }
  return (
    <SubmitDialog
      programmeId={c.programmeId}
      memberChangeId={c.id}
      processType={c.financial ? 'ENDORSEMENT' : 'ADJUSTMENT'}
      onClose={onClose}
    />
  );
}

/**
 * A member change with its lines, billing and endorsement request; relay, billing, validation,
 * closing and the submission to the insurer, each offered in its status.
 */
export function MemberChangeDetail({ id, onClose }: Readonly<{ id: number; onClose: () => void }>) {
  const companyId = useCompanyId();
  const [dialog, setDialog] = useState<string>();
  const change = useQuery({
    queryKey: ['eb', 'member-change', id],
    queryFn: () => ebServiceApi.memberChange(companyId, id),
  });
  const act = useEbMutation(
    (c, action: 'relay' | 'validate' | 'close') => ebServiceApi.changeAction(c, id, action),
    (r: MemberChange) => `Member change ${r.changeNo} ${STATUS_DONE[r.status] ?? 'updated'}`,
    () => undefined,
  );
  const c = change.data;
  const onAct = (a: string) => {
    if (a === 'relay' || a === 'validate' || a === 'close') {
      act.mutate(a);
    } else {
      setDialog(a);
    }
  };
  return (
    <Modal
      open
      title={c ? `Member Change ${c.changeNo}` : 'Member Change'}
      onClose={onClose}
      footer={c && <Actions c={c} onAct={onAct} />}
    >
      <div className="stack">
        <ErrorAlert error={change.error ?? act.error} onRetry={() => void change.refetch()} />
        {c && <ChangeFacts c={c} />}
      </div>
      {c && dialog !== undefined && (
        <ChangeDialog c={c} dialog={dialog} onClose={() => setDialog(undefined)} />
      )}
    </Modal>
  );
}
