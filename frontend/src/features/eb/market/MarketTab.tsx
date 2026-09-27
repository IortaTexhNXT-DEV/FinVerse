import { Plus, RotateCcw, Send, XCircle } from 'lucide-react';
import { useState } from 'react';
import type { ProgrammeView } from '@/api/eb';
import { ebMarketApi } from '@/api/ebMarket';
import type { InsurerRequest, Revision, RevisionChange, Tor, TorItem } from '@/api/ebMarket';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { formatDate, formatDateTime } from '@/utils/format';
import { DialogFooter } from '../common/DialogFooter';
import { ActionButton } from '../common/ActionButton';
import { InsurerChecks } from '../common/InsurerChecks';
import { useCycleList } from '../common/useCycleList';
import { useEbMutation } from '../common/useEbMutation';
import { TorCard } from './TorCard';

/** Send Requests: the released TOR goes to each selected insurer for its proposal. */
function SendDialog({ cycleId, onClose }: Readonly<{ cycleId: number; onClose: () => void }>) {
  const [codes, setCodes] = useState<string[]>([]);
  const [submitted, setSubmitted] = useState(false);
  const send = useEbMutation(
    (companyId, v: string[]) => ebMarketApi.sendRequests(companyId, cycleId, v),
    (r: InsurerRequest[]) => `Request sent to ${String(r.length)} insurer(s)`,
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
      title="Send Proposal Requests"
      onClose={onClose}
      footer={<DialogFooter busy={send.isPending} label="Send" onClose={onClose} onSave={save} />}
    >
      <div className="stack">
        <ErrorAlert error={send.error} />
        <p className="muted">
          The released terms of reference and the census go by e-mail to each insurer. Insurers
          under franchise need an approved franchise first.
        </p>
        {submitted && codes.length === 0 && (
          <div className="alert danger">Select at least one insurer</div>
        )}
        <InsurerChecks value={codes} onChange={setCodes} />
      </div>
    </Modal>
  );
}

interface RevisionForm {
  description: string;
  changes: RevisionChange[];
  insurerCodes: string[];
}

/** Request Revision: the client's changes relayed to the insurers for a revised proposal. */
function RevisionDialog({
  cycleId,
  items,
  onClose,
}: Readonly<{ cycleId: number; items: TorItem[]; onClose: () => void }>) {
  const [form, setForm] = useState<RevisionForm>({
    description: '',
    changes: [{ torItemId: null, change: '' }],
    insurerCodes: [],
  });
  const [submitted, setSubmitted] = useState(false);
  const request = useEbMutation(
    (companyId, v: RevisionForm) => ebMarketApi.requestRevision(companyId, cycleId, v),
    (r: Revision) => `Revision ${String(r.revisionNo)} sent`,
    onClose,
  );
  const changes = form.changes.filter((c) => c.change.trim() !== '');
  const valid = changes.length > 0 && form.insurerCodes.length > 0;
  const setChange = (index: number, patch: Partial<RevisionChange>) =>
    setForm({
      ...form,
      changes: form.changes.map((c, i) => (i === index ? { ...c, ...patch } : c)),
    });
  const save = () => {
    setSubmitted(true);
    if (valid) {
      request.mutate({ ...form, changes });
    }
  };
  return (
    <Modal
      open
      title="Request Revision"
      onClose={onClose}
      footer={
        <DialogFooter busy={request.isPending} label="Send" onClose={onClose} onSave={save} />
      }
    >
      <div className="stack">
        <ErrorAlert error={request.error} />
        {submitted && !valid && (
          <div className="alert danger">Enter at least one change and select the insurers</div>
        )}
        <Field label="Description">
          {(id) => (
            <input
              id={id}
              className="input"
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            />
          )}
        </Field>
        {form.changes.map((c, i) => (
          <div key={i} className="form-grid">
            <Field label={`TOR Item ${String(i + 1)}`}>
              {(id) => (
                <select
                  id={id}
                  className="select"
                  value={c.torItemId ?? ''}
                  onChange={(e) =>
                    setChange(i, { torItemId: e.target.value ? Number(e.target.value) : null })
                  }
                >
                  <option value="">General</option>
                  {items.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.benefitLine} – {t.description}
                    </option>
                  ))}
                </select>
              )}
            </Field>
            <Field label={`Change ${String(i + 1)}`}>
              {(id) => (
                <input
                  id={id}
                  className="input"
                  value={c.change}
                  onChange={(e) => setChange(i, { change: e.target.value })}
                />
              )}
            </Field>
          </div>
        ))}
        <div>
          <Button
            variant="secondary"
            size="sm"
            icon={<Plus size={14} />}
            onClick={() =>
              setForm({ ...form, changes: [...form.changes, { torItemId: null, change: '' }] })
            }
          >
            Add Change
          </Button>
        </div>
        <InsurerChecks
          value={form.insurerCodes}
          onChange={(v) => setForm({ ...form, insurerCodes: v })}
        />
      </div>
    </Modal>
  );
}

function requestColumns(
  market: boolean,
  onClose: (r: InsurerRequest) => void,
): Column<InsurerRequest>[] {
  return [
    {
      key: 'no',
      header: 'Request',
      kind: 'code',
      render: (r) => <CellStack main={r.requestNo} sub={r.insurerName} />,
    },
    { key: 'tor', header: 'TOR', kind: 'center', render: (r) => `v${String(r.torVersion)}` },
    {
      key: 'sent',
      header: 'Sent',
      render: (r) => (
        <CellStack main={formatDateTime(r.sentAt)} sub={<UserName login={r.sentBy} />} />
      ),
    },
    { key: 'due', header: 'Due', kind: 'date', render: (r) => formatDate(r.dueDate) },
    {
      key: 'status',
      header: 'Status',
      kind: 'status',
      render: (r) => <StatusBadge status={r.status} />,
    },
    { key: 'reason', header: 'Reason', render: (r) => r.closedReason ?? '' },
    {
      key: 'actions',
      header: '',
      width: '64px',
      render: (r) =>
        market &&
        r.status === 'OPEN' && (
          <Button
            variant="ghost"
            size="sm"
            aria-label={`Mark declined ${r.requestNo}`}
            icon={<XCircle size={14} />}
            onClick={() => onClose(r)}
          />
        ),
    },
  ];
}

const REVISION_COLUMNS: Column<Revision>[] = [
  { key: 'no', header: 'Revision', kind: 'center', render: (r) => r.revisionNo },
  { key: 'desc', header: 'Description', render: (r) => r.description ?? '' },
  { key: 'changes', header: 'Changes', render: (r) => r.items.map((i) => i.change).join('; ') },
  {
    key: 'insurers',
    header: 'Insurers',
    render: (r) => r.targets.map((t) => `${t.insurerCode} (${t.status.toLowerCase()})`).join(', '),
  },
  { key: 'due', header: 'Due', kind: 'date', render: (r) => formatDate(r.dueDate) },
  {
    key: 'status',
    header: 'Status',
    kind: 'status',
    render: (r) => <StatusBadge status={r.status} />,
  },
];

type Mode = 'send' | 'revise' | InsurerRequest;

/** The open dialog of the tab. */
function MarketDialogs({
  mode,
  cycleId,
  items,
  onClose,
}: Readonly<{ mode: Mode; cycleId: number; items: TorItem[]; onClose: () => void }>) {
  const decline = useEbMutation(
    (c, v: { id: number; reason: string }) => ebMarketApi.closeRequest(c, v.id, true, v.reason),
    'Request closed as declined',
    onClose,
  );
  if (mode === 'send') {
    return <SendDialog cycleId={cycleId} onClose={onClose} />;
  }
  if (mode === 'revise') {
    return <RevisionDialog cycleId={cycleId} items={items} onClose={onClose} />;
  }
  return (
    <ConfirmDialog
      title="Mark Declined"
      record={`${mode.requestNo} – ${mode.insurerName}`}
      effect="The insurer is recorded as having declined to quote."
      confirmLabel="Mark Declined"
      reason="required"
      destructive
      busy={decline.isPending}
      error={decline.error}
      onConfirm={(reason) => decline.mutate({ id: mode.id, reason })}
      onClose={onClose}
    />
  );
}

/**
 * TOR & Requests tab: the terms of reference of the cycle, the proposal requests sent to the
 * insurers with their due dates, and the revision requests relayed from the client.
 */
export function MarketTab({ programme }: Readonly<{ programme: ProgrammeView }>) {
  const { can } = useAuth();
  const cycleId = programme.currentCycleId ?? undefined;
  const [mode, setMode] = useState<Mode>();
  const close = () => setMode(undefined);
  const requests = useCycleList('requests', cycleId, ebMarketApi.requests);
  const revisions = useCycleList('revisions', cycleId, ebMarketApi.revisions);
  const tors = useCycleList<Tor>('tor', cycleId, ebMarketApi.tors);
  const market = can('EB_MARKET') && cycleId !== undefined;
  const released = tors.rows.find((t) => t.status === 'RELEASED');
  return (
    <div className="stack">
      <TorCard programme={programme} />
      <Card
        title="Proposal Requests"
        actions={
          <ActionButton
            shown={market}
            icon={<Send size={14} />}
            label="Send Requests"
            onClick={() => setMode('send')}
          />
        }
      >
        <ErrorAlert error={requests.error} onRetry={() => void requests.refetch()} />
        <DataTable<InsurerRequest>
          loading={requests.isLoading}
          rows={requests.rows}
          rowKey={(r) => r.id}
          columns={requestColumns(market, setMode)}
          emptyMessage="No request sent to insurers"
        />
      </Card>
      <Card
        title="Revision Requests"
        actions={
          <ActionButton
            shown={market}
            icon={<RotateCcw size={14} />}
            label="Request Revision"
            onClick={() => setMode('revise')}
          />
        }
      >
        <ErrorAlert error={revisions.error} onRetry={() => void revisions.refetch()} />
        <DataTable<Revision>
          loading={revisions.isLoading}
          rows={revisions.rows}
          rowKey={(r) => r.id}
          columns={REVISION_COLUMNS}
          emptyMessage="No revision requested"
        />
      </Card>
      {mode !== undefined && cycleId !== undefined && (
        <MarketDialogs
          mode={mode}
          cycleId={cycleId}
          items={released?.items ?? []}
          onClose={close}
        />
      )}
    </div>
  );
}
