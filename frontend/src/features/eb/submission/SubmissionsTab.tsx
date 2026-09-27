import { useQuery } from '@tanstack/react-query';
import { CalendarCheck, Send } from 'lucide-react';
import { useState } from 'react';
import type { ProgrammeView } from '@/api/eb';
import { ebMarketApi } from '@/api/ebMarket';
import type { Submission, SubmissionInput } from '@/api/ebMarket';
import { useAuth } from '@/auth/authContext';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { DialogFooter } from '../common/DialogFooter';
import { EbLov } from '../common/EbLabels';
import { EB_LOV } from '../common/ebCodes';
import { InsurerSelect } from '../common/EbSelects';
import { useEbMutation } from '../common/useEbMutation';
import { SubmissionChecklist } from './SubmissionChecklist';

/** Process types submitted from a member change rather than a cycle. */
const CHANGE_PROCESSES = new Set(['ENDORSEMENT', 'ADJUSTMENT']);

/**
 * Submit to Insurer: the documents of a process sent to the insurer, checked against the required
 * documents of the process first.
 */
export function SubmitDialog({
  programmeId,
  cycleId,
  memberChangeId,
  processType: fixedProcess,
  onClose,
}: Readonly<{
  programmeId: number;
  cycleId?: number;
  memberChangeId?: number;
  processType?: string;
  onClose: () => void;
}>) {
  const [processType, setProcessType] = useState(fixedProcess ?? '');
  const [insurerCode, setInsurerCode] = useState('');
  const [selected, setSelected] = useState<number[]>([]);
  const [remarks, setRemarks] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const submit = useEbMutation(
    (companyId, v: SubmissionInput) => ebMarketApi.submit(companyId, v),
    (s: Submission) => `Documents submitted to ${s.insurerName}`,
    onClose,
  );
  const scope = CHANGE_PROCESSES.has(processType)
    ? { programmeId, memberChangeId, processType }
    : { programmeId, cycleId, processType };
  const valid = processType !== '' && insurerCode !== '' && selected.length > 0;
  const save = () => {
    setSubmitted(true);
    if (valid) {
      submit.mutate({
        ...scope,
        insurerCode,
        attachmentIds: selected,
        remarks: remarks || undefined,
      });
    }
  };
  return (
    <Modal
      open
      title="Submit to Insurer"
      onClose={onClose}
      footer={
        <DialogFooter busy={submit.isPending} label="Submit" onClose={onClose} onSave={save} />
      }
    >
      <div className="stack">
        <ErrorAlert error={submit.error} />
        {submitted && !valid && (
          <div className="alert danger">
            Select the process, the insurer and the documents to submit
          </div>
        )}
        <div className="form-grid">
          <Field label="Process" required>
            {(id) => (
              <LovSelect
                id={id}
                type={EB_LOV.processType}
                value={processType}
                disabled={fixedProcess !== undefined}
                onChange={(v) => {
                  setProcessType(v);
                  setSelected([]);
                }}
              />
            )}
          </Field>
          <Field label="Insurer" required>
            {(id) => <InsurerSelect id={id} value={insurerCode} onChange={setInsurerCode} />}
          </Field>
        </div>
        {processType !== '' && (
          <SubmissionChecklist scope={scope} selected={selected} onSelect={setSelected} />
        )}
        <Field label="Remarks">
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={2}
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

/** Record Acknowledgement: the date the insurer acknowledged the submission. */
function AcknowledgeDialog({ s, onClose }: Readonly<{ s: Submission; onClose: () => void }>) {
  const [date, setDate] = useState('');
  const ack = useEbMutation(
    (companyId, v: string) => ebMarketApi.acknowledge(companyId, s.id, v),
    'Acknowledgement recorded',
    onClose,
  );
  return (
    <Modal
      open
      title={`Acknowledgement – ${s.insurerName}`}
      onClose={onClose}
      footer={
        <DialogFooter
          busy={ack.isPending}
          label="Record"
          onClose={onClose}
          onSave={() => date !== '' && ack.mutate(date)}
        />
      }
    >
      <div className="stack">
        <ErrorAlert error={ack.error} />
        <Field label="Acknowledged On" required>
          {(id) => <DateInput id={id} value={date} onChange={(e) => setDate(e.target.value)} />}
        </Field>
      </div>
    </Modal>
  );
}

function columns(mayAct: boolean, onAck: (s: Submission) => void): Column<Submission>[] {
  return [
    {
      key: 'process',
      header: 'Process',
      render: (s) => <EbLov type={EB_LOV.processType} code={s.processType} />,
    },
    { key: 'insurer', header: 'Insurer', render: (s) => s.insurerName },
    {
      key: 'sent',
      header: 'Sent',
      render: (s) => (
        <CellStack main={formatDateTime(s.sentAt)} sub={<UserName login={s.sentBy} />} />
      ),
    },
    { key: 'to', header: 'Recipients', render: (s) => s.recipients },
    { key: 'docs', header: 'Documents', kind: 'center', render: (s) => s.documents.length },
    {
      key: 'ack',
      header: 'Acknowledged',
      kind: 'date',
      render: (s) => formatDate(s.acknowledgedOn),
    },
    {
      key: 'actions',
      header: '',
      width: '64px',
      render: (s) =>
        mayAct &&
        !s.acknowledgedOn && (
          <Button
            variant="ghost"
            size="sm"
            aria-label={`Record acknowledgement ${s.insurerName}`}
            icon={<CalendarCheck size={14} />}
            onClick={() => onAck(s)}
          />
        ),
    },
  ];
}

/**
 * Submissions tab: the document sets sent to insurers for placement, endorsements and
 * adjustments, with their recipients and acknowledgement; Submit to Insurer checks the required
 * documents of the process.
 */
export function SubmissionsTab({ programme }: Readonly<{ programme: ProgrammeView }>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [mode, setMode] = useState<'submit' | Submission>();
  const close = () => setMode(undefined);
  const list = useQuery({
    queryKey: ['eb', 'submissions', programme.id],
    queryFn: () => ebMarketApi.submissions(companyId, programme.id),
  });
  const mayAct = can('EB_MARKET') || can('EB_PROCESS');
  return (
    <Card
      title="Submissions to Insurers"
      actions={
        mayAct && (
          <Button
            variant="secondary"
            size="sm"
            icon={<Send size={14} />}
            onClick={() => setMode('submit')}
          >
            Submit to Insurer
          </Button>
        )
      }
    >
      <ErrorAlert error={list.error} onRetry={() => void list.refetch()} />
      <DataTable<Submission>
        loading={list.isLoading}
        rows={list.data ?? []}
        rowKey={(s) => s.id}
        columns={columns(mayAct, setMode)}
        emptyMessage="No submission sent"
      />
      {mode === 'submit' && (
        <SubmitDialog
          programmeId={programme.id}
          cycleId={programme.currentCycleId ?? undefined}
          onClose={close}
        />
      )}
      {typeof mode === 'object' && <AcknowledgeDialog s={mode} onClose={close} />}
    </Card>
  );
}
