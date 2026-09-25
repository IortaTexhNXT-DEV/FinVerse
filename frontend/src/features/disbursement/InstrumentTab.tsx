import { useMutation, useQueryClient } from '@tanstack/react-query';
import { FileDown, PenLine } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime, humanize } from '@/utils/format';
import { disbursementApi } from './api';
import type { Instrument, InstrumentStatus, StatusEdit, Voucher } from './api';
import { INSTRUMENT_ACTION_LABELS, MODE_LABELS, instrumentActions } from './labels';
import type { InstrumentAction } from './labels';
import { DialogFooter, TextDialog } from './VoucherDialogs';
import './disbursement.css';

const PROMPTS: Partial<
  Record<InstrumentAction, { field: string; required: boolean; hint?: string }>
> = {
  release: { field: 'Released To', required: true },
  email: {
    field: 'Branch E-mail',
    required: true,
    hint: 'Mailbox of the processing branch (list BRANCH_EMAIL); several addresses separated by commas.',
  },
  debited: { field: 'Branch or BOB Reference', required: false },
  received: { field: "Manager's Check / Demand Draft No.", required: false },
};

function bodyOf(action: InstrumentAction, value: string): unknown {
  if (action === 'release') {
    return { releasedTo: value };
  }
  if (action === 'email') {
    return {
      to: value
        .split(',')
        .map((s) => s.trim())
        .filter((s) => s !== ''),
    };
  }
  return { reference: value || undefined };
}

function EditDialog({
  instrument,
  voucherId,
  onClose,
  onDone,
}: Readonly<{
  instrument: Instrument;
  voucherId: number;
  onClose: () => void;
  onDone: () => void;
}>) {
  const [to, setTo] = useState<InstrumentStatus | ''>('');
  const [reason, setReason] = useState('');
  const request = useMutation({
    mutationFn: () => disbursementApi.requestEdit(voucherId, to as InstrumentStatus, reason),
    onSuccess: onDone,
  });
  return (
    <Modal
      open
      title="Request Status Edit"
      onClose={onClose}
      footer={
        <DialogFooter
          label="Submit for Approval"
          busy={request.isPending}
          disabled={to === '' || reason.trim() === ''}
          onClose={onClose}
          onConfirm={() => request.mutate()}
        />
      }
    >
      <div className="stack">
        <ErrorAlert error={request.error} />
        <p>
          The team leader approves the change of {instrument.instrumentNo ?? 'the instrument'} from{' '}
          <StatusBadge status={instrument.status} /> (DIS 2.8.5).
        </p>
        <Field label="New Status" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={to}
              onChange={(e) => setTo(e.target.value as InstrumentStatus)}
            >
              <option value="">Select…</option>
              {instrument.statuses
                .filter((s) => s !== instrument.status)
                .map((s) => (
                  <option key={s} value={s}>
                    {humanize(s)}
                  </option>
                ))}
            </select>
          )}
        </Field>
        <Field label="Reason" required>
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={3}
              maxLength={500}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

function PendingEdit({ edit, onChanged }: Readonly<{ edit: StatusEdit; onChanged: () => void }>) {
  const toast = useToast();
  const approve = useMutation({
    mutationFn: () => disbursementApi.approveEdit(edit.id),
    onSuccess: () => {
      toast.success('Status edit approved');
      onChanged();
    },
  });
  return (
    <div className="stack">
      <p>
        {humanize(edit.fromStatus)} to {humanize(edit.toStatus)} requested by {edit.requestedBy}:{' '}
        {edit.reason}
      </p>
      <ErrorAlert error={approve.error} />
      <WorkflowPanel
        entityType="DisbursementStatusEdit"
        entityId={edit.id}
        showHistory={false}
        onChanged={onChanged}
        renderBusinessActions={(actions) =>
          actions.some((a) => a.action === 'approve') ? (
            <Button size="sm" busy={approve.isPending} onClick={() => approve.mutate()}>
              Approve Status Edit
            </Button>
          ) : null
        }
      />
    </div>
  );
}

/** A form is printed for every mode except credits to account and online banking. */
function hasForm(i: Instrument): boolean {
  return i.mode !== 'CTA' && i.mode !== 'ONLINE_BANKING' && i.status !== 'PENDING';
}

function historyNote(e: Instrument['history'][number]): string {
  return [humanize(e.source), e.by, e.note, e.fileRef]
    .filter((x) => x !== undefined && x !== '')
    .join(' · ');
}

function InstrumentFacts({ instrument: i }: Readonly<{ instrument: Instrument }>) {
  return (
    <>
      <dl className="detail-list">
        <dt>Status</dt>
        <dd>
          <StatusBadge status={i.status} />
        </dd>
        <dt>Amount</dt>
        <dd>
          {i.currency} <Amount value={i.amount} />
        </dd>
        <dt>Reference</dt>
        <dd>{i.reference ?? '—'}</dd>
        <dt>Released to</dt>
        <dd>{i.releasedTo ?? '—'}</dd>
      </dl>
      <h3>Status History</h3>
      <ol className="dsb-timeline" aria-label="Status history">
        {i.history.map((e) => (
          <li key={`${e.at}-${e.toStatus}`}>
            <StatusBadge status={e.toStatus} />
            <span>{formatDateTime(e.at)}</span>
            <span className="dsb-muted">{historyNote(e)}</span>
          </li>
        ))}
      </ol>
    </>
  );
}

/**
 * The instrument of an approved voucher (DIS 2.8.0-2.8.5, 3.26.x): number, status and the actions
 * of its mode (print, release, e-mail the ATD, confirm a debit, receive an MC / DD, re-issue a
 * stale check), the status history with its source, and the status edits for the team leader.
 */
export function InstrumentTab({
  voucher,
  onChanged,
}: Readonly<{ voucher: Voucher; onChanged: (v?: Voucher) => void }>) {
  const { can } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const download = useFileDownload();
  const [prompt, setPrompt] = useState<InstrumentAction>();
  const [editing, setEditing] = useState(false);
  const id = voucher.summary.id;
  const act = useMutation({
    mutationFn: ({ action, value }: { action: InstrumentAction; value: string }) =>
      disbursementApi.instrument(id, action, bodyOf(action, value)),
    onSuccess: (v, { action }) => {
      setPrompt(undefined);
      toast.success(`${INSTRUMENT_ACTION_LABELS[action]} done`);
      onChanged(v);
    },
  });
  const reissue = useMutation({
    mutationFn: () => disbursementApi.reissue(id),
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: ['disbursement'] });
      toast.success(`Re-issue request ${r.requestNo} created`);
      if (r.voucherId !== undefined) {
        void navigate(`/disbursement/vouchers/${r.voucherId}`);
      }
    },
  });
  const i = voucher.instrument;
  if (i === undefined) {
    return (
      <Card title="Instrument">
        <EmptyState message="The instrument is issued when the voucher is approved" />
      </Card>
    );
  }
  const actions = can('DISB_PROCESS') ? instrumentActions(i.mode, i.status) : [];
  const run = (action: InstrumentAction) => {
    if (action === 'reissue') {
      reissue.mutate();
    } else if (PROMPTS[action] === undefined) {
      act.mutate({ action, value: '' });
    } else {
      setPrompt(action);
    }
  };
  const pending = i.edits.filter((e) => e.stage === 'REQUESTED');
  return (
    <Card
      title={`${MODE_LABELS[i.mode]} ${i.instrumentNo ?? ''}`}
      actions={
        <div className="dsb-actions">
          {hasForm(i) && (
            <Button
              variant="secondary"
              icon={<FileDown size={16} />}
              busy={download.isPending}
              onClick={() => download.mutate(() => disbursementApi.instrumentDocument(id))}
            >
              Download Form
            </Button>
          )}
          {can('DISB_PROCESS') && pending.length === 0 && (
            <Button
              variant="secondary"
              icon={<PenLine size={16} />}
              onClick={() => setEditing(true)}
            >
              Request Status Edit
            </Button>
          )}
          {actions.map((a) => (
            <Button key={a} busy={act.isPending || reissue.isPending} onClick={() => run(a)}>
              {INSTRUMENT_ACTION_LABELS[a]}
            </Button>
          ))}
        </div>
      }
    >
      <div className="stack">
        <ErrorAlert error={act.error ?? reissue.error ?? download.error} />
        <InstrumentFacts instrument={i} />
        {pending.map((e) => (
          <PendingEdit key={e.id} edit={e} onChanged={() => onChanged()} />
        ))}
      </div>
      {prompt !== undefined && PROMPTS[prompt] !== undefined && (
        <TextDialog
          title={INSTRUMENT_ACTION_LABELS[prompt]}
          label={INSTRUMENT_ACTION_LABELS[prompt]}
          fieldLabel={PROMPTS[prompt].field}
          hint={PROMPTS[prompt].hint}
          required={PROMPTS[prompt].required}
          busy={act.isPending}
          error={act.error}
          onClose={() => setPrompt(undefined)}
          onConfirm={(value) => act.mutate({ action: prompt, value })}
        />
      )}
      {editing && (
        <EditDialog
          instrument={i}
          voucherId={id}
          onClose={() => setEditing(false)}
          onDone={() => {
            setEditing(false);
            toast.success('Status edit sent to the team leader');
            onChanged();
          }}
        />
      )}
    </Card>
  );
}
