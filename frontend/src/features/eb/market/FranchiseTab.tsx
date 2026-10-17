import { useQuery } from '@tanstack/react-query';
import { Download, Gavel, Mail, Send } from 'lucide-react';
import { useState } from 'react';
import { attachmentsApi } from '@/api/attachments';
import type { ProgrammeView } from '@/api/eb';
import { ebMarketApi } from '@/api/ebMarket';
import type { Franchise, FranchiseDecisionInput } from '@/api/ebMarket';
import { useAuth } from '@/auth/authContext';
import { LovSelect } from '@/components/broking/LovSelect';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { FileDropZone } from '@/components/ui/FileDropZone';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { countOf, formatDate, formatDateTime } from '@/utils/format';
import { DialogFooter } from '../common/DialogFooter';
import { EbLov } from '../common/EbLabels';
import { EB_LOV } from '../common/ebCodes';
import { InsurerChecks } from '../common/InsurerChecks';
import { useEbMutation } from '../common/useEbMutation';
import { Notice } from '@/components/ui/Notice';

/** Request Franchise: the franchise form goes to each selected insurer. */
function RequestDialog({ cycleId, onClose }: Readonly<{ cycleId: number; onClose: () => void }>) {
  const [codes, setCodes] = useState<string[]>([]);
  const [submitted, setSubmitted] = useState(false);
  const send = useEbMutation(
    (companyId, v: string[]) => ebMarketApi.requestFranchise(companyId, cycleId, v),
    (r: Franchise[]) => `Franchise requested from ${countOf(r.length, 'insurer')}`,
    onClose,
  );
  const save = () => {
    setSubmitted(true);
    if (codes.length > 0) {
      send.mutate(codes);
    }
  };
  return (
    <Modal
      open
      title="Request Franchise"
      onClose={onClose}
      footer={<DialogFooter busy={send.isPending} label="Send" onClose={onClose} onSave={save} />}
    >
      <div className="stack">
        <ErrorAlert error={send.error} />
        <p className="muted">
          The franchise form and the validated Broker on Record go by e-mail to each insurer.
        </p>
        {submitted && codes.length === 0 && (
          <Notice tone="error">Select at least one insurer</Notice>
        )}
        <InsurerChecks value={codes} onChange={setCodes} />
      </div>
    </Modal>
  );
}

const EMPTY: FranchiseDecisionInput = { approve: true, decidedOn: '', reasonCode: '', remarks: '' };

/** Record Decision: the insurer's approval or decline of the franchise with its evidence. */
function DecisionDialog({ f, onClose }: Readonly<{ f: Franchise; onClose: () => void }>) {
  const [input, setInput] = useState<FranchiseDecisionInput>(EMPTY);
  const [file, setFile] = useState<File>();
  const [submitted, setSubmitted] = useState(false);
  const decide = useEbMutation(
    (companyId, v: FranchiseDecisionInput) => ebMarketApi.decideFranchise(companyId, f.id, v, file),
    `Decision of ${f.insurerName} recorded`,
    onClose,
  );
  const reasonMissing = !input.approve && input.reasonCode === '';
  const save = () => {
    setSubmitted(true);
    if (file && !reasonMissing) {
      decide.mutate(input);
    }
  };
  return (
    <Modal
      open
      title={`Record Decision – ${f.insurerName}`}
      onClose={onClose}
      footer={
        <DialogFooter busy={decide.isPending} label="Record" onClose={onClose} onSave={save} />
      }
    >
      <div className="stack">
        <ErrorAlert error={decide.error} />
        <div className="form-grid">
          <Field label="Decision" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={input.approve ? 'APPROVED' : 'DECLINED'}
                onChange={(e) => setInput({ ...input, approve: e.target.value === 'APPROVED' })}
              >
                <option value="APPROVED">Approved</option>
                <option value="DECLINED">Declined</option>
              </select>
            )}
          </Field>
          <Field label="Decided On" hint="Today when blank">
            {(id) => (
              <DateInput
                id={id}
                value={input.decidedOn}
                onChange={(e) => setInput({ ...input, decidedOn: e.target.value })}
              />
            )}
          </Field>
          {!input.approve && (
            <Field
              label="Reason"
              required
              error={submitted && reasonMissing ? 'Select the reason' : undefined}
            >
              {(id) => (
                <LovSelect
                  id={id}
                  type={EB_LOV.franchiseReject}
                  value={input.reasonCode}
                  onChange={(v) => setInput({ ...input, reasonCode: v })}
                />
              )}
            </Field>
          )}
        </div>
        <Field label="Remarks">
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={2}
              value={input.remarks}
              onChange={(e) => setInput({ ...input, remarks: e.target.value })}
            />
          )}
        </Field>
        <Field
          label="Insurer's Reply"
          required
          error={submitted && !file ? "Add the insurer's reply" : undefined}
        >
          {(id) => (
            <FileDropZone
              id={id}
              accept=".pdf,.doc,.docx,.msg,.eml"
              maxSizeMb={10}
              onChange={(files) => setFile(files[0])}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

function columns(
  market: boolean,
  onDecide: (f: Franchise) => void,
  onAdvise: (f: Franchise) => void,
  onDownload: (f: Franchise) => void,
): Column<Franchise>[] {
  return [
    {
      key: 'no',
      header: 'Franchise',
      kind: 'code',
      render: (f) => <CellStack main={f.franchiseNo} sub={f.insurerName} />,
    },
    {
      key: 'status',
      header: 'Status',
      kind: 'status',
      render: (f) => <StatusBadge status={f.status} />,
    },
    {
      key: 'sent',
      header: 'Requested',
      kind: 'datetime',
      render: (f) => formatDateTime(f.submittedAt),
    },
    { key: 'due', header: 'Due', kind: 'date', render: (f) => formatDate(f.dueDate) },
    {
      key: 'decided',
      header: 'Decided',
      render: (f) =>
        f.decidedOn ? (
          <CellStack main={formatDate(f.decidedOn)} sub={<UserName login={f.decidedBy} />} />
        ) : (
          ''
        ),
    },
    {
      key: 'reason',
      header: 'Reason',
      render: (f) => <EbLov type={EB_LOV.franchiseReject} code={f.reasonCode} />,
    },
    {
      key: 'advised',
      header: 'Client Advised',
      kind: 'datetime',
      render: (f) => formatDateTime(f.advisedAt),
    },
    {
      key: 'actions',
      header: '',
      width: '132px',
      render: (f) => (
        <span className="eb-actions">
          {typeof f.evidenceAttachmentId === 'number' && (
            <Button
              variant="ghost"
              size="sm"
              aria-label={`Download reply ${f.franchiseNo}`}
              icon={<Download size={14} />}
              onClick={() => onDownload(f)}
            />
          )}
          {market && f.status === 'SUBMITTED' && (
            <Button
              variant="ghost"
              size="sm"
              aria-label={`Record decision ${f.franchiseNo}`}
              icon={<Gavel size={14} />}
              onClick={() => onDecide(f)}
            />
          )}
          {market && (f.status === 'APPROVED' || f.status === 'REJECTED') && (
            <Button
              variant="ghost"
              size="sm"
              aria-label={`Advise client ${f.franchiseNo}`}
              icon={<Mail size={14} />}
              onClick={() => onAdvise(f)}
            />
          )}
        </span>
      ),
    },
  ];
}

/**
 * Franchise tab: the franchise requests of the programme per insurer with their due date,
 * decision and the advice to the client; Request Franchise, Record Decision and Advise Client.
 */
export function FranchiseTab({ programme }: Readonly<{ programme: ProgrammeView }>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const download = useFileDownload();
  const [requesting, setRequesting] = useState(false);
  const [deciding, setDeciding] = useState<Franchise>();
  const list = useQuery({
    queryKey: ['eb', 'franchise', programme.id],
    queryFn: () => ebMarketApi.franchises(companyId, programme.id),
  });
  const advise = useEbMutation(
    (c, f: Franchise) => ebMarketApi.adviseFranchise(c, f.id),
    'Client advised of the franchise outcome',
    () => undefined,
  );
  const market = can('EB_MARKET');
  const cycleId = programme.currentCycleId ?? undefined;
  return (
    <Card
      title="Franchise Requests"
      actions={
        market &&
        cycleId !== undefined && (
          <Button
            variant="secondary"
            size="sm"
            icon={<Send size={14} />}
            onClick={() => setRequesting(true)}
          >
            Request Franchise
          </Button>
        )
      }
    >
      <ErrorAlert
        error={list.error ?? advise.error ?? download.error}
        onRetry={() => void list.refetch()}
      />
      <DataTable<Franchise>
        loading={list.isLoading}
        rows={list.data ?? []}
        rowKey={(f) => f.id}
        columns={columns(
          market,
          setDeciding,
          (f) => advise.mutate(f),
          (f) => download.mutate(() => attachmentsApi.download(f.evidenceAttachmentId ?? 0)),
        )}
        emptyMessage="No franchise requested"
      />
      {requesting && cycleId !== undefined && (
        <RequestDialog cycleId={cycleId} onClose={() => setRequesting(false)} />
      )}
      {deciding && <DecisionDialog f={deciding} onClose={() => setDeciding(undefined)} />}
    </Card>
  );
}
