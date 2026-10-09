import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FileDown } from 'lucide-react';
import { useState } from 'react';
import { productMaintApi } from '@/api/productmaint';
import type { InsurerResponse, NegotiationRound, PackageRequest } from '@/api/productmaint';
import { useAuth } from '@/auth/authContext';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { InsurerChoices } from '@/features/proposals/ProposalFormParts';
import { countOf, formatDate, formatDateTime } from '@/utils/format';
import { ResponseDialog } from './ResponseDialog';
import { responseColumns } from './responseColumns';
import { displayNameOf } from '@/api/users';
import { ConfirmButton } from '@/components/ui/ConfirmButton';

interface RoundProps {
  request: PackageRequest;
  round: NegotiationRound;
  onChanged: () => Promise<void>;
}

function SlipPreparation({ request, round, onChanged }: Readonly<RoundProps>) {
  const toast = useToast();
  const [insurers, setInsurers] = useState(round.insurers);
  const [notes, setNotes] = useState(round.qsNotes ?? '');
  const save = useMutation({
    mutationFn: () => productMaintApi.prepare(request.id, round.roundNo, insurers, notes),
    onSuccess: async () => {
      await onChanged();
      toast.success('Quotation slip saved');
    },
  });
  const submit = useMutation({
    mutationFn: async () => {
      await productMaintApi.prepare(request.id, round.roundNo, insurers, notes);
      return productMaintApi.submitSlip(request.id, round.roundNo);
    },
    onSuccess: async (r) => {
      await onChanged();
      toast.success(`${r.qsNo ?? 'Slip'} submitted for approval`);
    },
  });
  return (
    <div className="stack">
      <ErrorAlert error={save.error ?? submit.error} />
      <InsurerChoices selected={insurers} onChange={setInsurers} />
      <Field label="Notes to the insurers">
        {(id) => (
          <textarea
            id={id}
            className="textarea"
            rows={3}
            maxLength={4000}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
          />
        )}
      </Field>
      <div className="row">
        <Button variant="secondary" busy={save.isPending} onClick={() => save.mutate()}>
          Save Slip
        </Button>
        <Button variant="accent" busy={submit.isPending} onClick={() => submit.mutate()}>
          Submit for Approval
        </Button>
      </div>
    </div>
  );
}

function roundTitle(round: NegotiationRound): string {
  return round.qsNo === undefined
    ? `Round ${round.roundNo}`
    : `Round ${round.roundNo} · ${round.qsNo}`;
}

function roundNote(round: NegotiationRound): string {
  const note = round.qsNotes?.trim() ?? 'Terms as requested.';
  const parts = [/[.!?]$/.test(note) ? note : `${note}.`];
  if (round.replyDue !== undefined) {
    parts.push(`Reply by ${formatDate(round.replyDue)}.`);
  }
  if (round.sentAt !== undefined) {
    parts.push(
      `Sent ${formatDateTime(round.sentAt)} (approved by ${displayNameOf(round.approvedBy) || '—'}).`,
    );
  }
  return parts.join(' ');
}

function RoundActions({ request, round, onChanged }: Readonly<RoundProps>) {
  const { can } = useAuth();
  const toast = useToast();
  const download = useFileDownload();
  const approve = useMutation({
    mutationFn: () => productMaintApi.approveSlip(request.id, round.roundNo),
    onSuccess: async (r) => {
      await onChanged();
      toast.success(`${r.qsNo ?? 'Slip'} sent to ${countOf(r.insurers.length, 'insurer')}`);
    },
  });
  const approvable =
    request.status === 'NEGOTIATION' &&
    !round.locked &&
    round.status === 'FOR_APPROVAL' &&
    can('PKG_QS_APPROVE');
  return (
    <span className="row">
      <StatusBadge status={round.status === 'CLOSED' ? 'SUPERSEDED' : round.status} />
      {round.locked && <span className="tag">Locked</span>}
      {round.qsNo !== undefined && (
        <Button
          size="sm"
          variant="secondary"
          icon={<FileDown size={14} />}
          onClick={() => download.mutate(() => productMaintApi.slipPdf(request.id, round.roundNo))}
        >
          Quotation Slip
        </Button>
      )}
      {approvable && (
        <ConfirmButton
          size="sm"
          variant="accent"
          busy={approve.isPending}
          confirm={{
            title: 'Approve and Send',
            effect: 'The quotation slip is approved and sent to the insurers.',
          }}
          onConfirm={() => approve.mutateAsync()}
        >
          Approve and Send
        </ConfirmButton>
      )}
      <ErrorAlert error={approve.error ?? download.error} />
    </span>
  );
}

function RoundCard({ request, round, onChanged }: Readonly<RoundProps>) {
  const { can } = useAuth();
  const toast = useToast();
  const [editing, setEditing] = useState<InsurerResponse | null>(null);
  const negotiating = request.status === 'NEGOTIATION' && !round.locked;
  const open = negotiating && round.status === 'SENT' && can('PKG_NEGOTIATE');
  const preparing = negotiating && round.status === 'PREPARATION' && can('PKG_NEGOTIATE');
  const resend = useMutation({
    mutationFn: (r: InsurerResponse) =>
      productMaintApi.resend(request.id, round.roundNo, r.insurerCode),
    onSuccess: async (r) => {
      await onChanged();
      toast.success(`Slip resent to ${r.insurerName}`);
    },
  });
  return (
    <Card
      title={roundTitle(round)}
      actions={<RoundActions request={request} round={round} onChanged={onChanged} />}
    >
      <div className="stack">
        <ErrorAlert error={resend.error} />
        <p className="muted">{roundNote(round)}</p>
        {preparing ? (
          <SlipPreparation request={request} round={round} onChanged={onChanged} />
        ) : (
          <DataTable<InsurerResponse>
            rows={round.responses}
            rowKey={(r) => r.id}
            emptyMessage="The slip has not been sent yet"
            columns={responseColumns(open, setEditing, (r) => resend.mutate(r))}
          />
        )}
      </div>
      {editing && (
        <ResponseDialog
          requestId={request.id}
          response={editing}
          requested={request.requestedTerms.coverages}
          lineCode={request.lineCode}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null);
            void onChanged();
            toast.success(`Terms of ${editing.insurerName} saved`);
          }}
        />
      )}
    </Card>
  );
}

/**
 * Negotiation of a package request (BRPM.010/012/013, PMADD04): one card per round, newest first,
 * with its quotation slip (prepare, submit, approve and send, download, resend) and the insurer
 * responses with their outcome pill; keying in terms opens the response dialog.
 */
export function NegotiationTab({ request }: Readonly<{ request: PackageRequest }>) {
  const queryClient = useQueryClient();
  const key = ['package-request-tab', request.id, 'rounds'];
  const rounds = useQuery({ queryKey: key, queryFn: () => productMaintApi.rounds(request.id) });
  const onChanged = async () => {
    await queryClient.invalidateQueries({ queryKey: ['package-request-tab', request.id] });
  };
  if (rounds.isLoading) {
    return <span className="spinner" aria-label="Loading" />;
  }
  const list = [...(rounds.data ?? [])].reverse();
  return (
    <div className="stack">
      <ErrorAlert error={rounds.error} />
      {list.length === 0 ? (
        <Card>
          <EmptyState message="The negotiation starts when the TSU Head approves the request" />
        </Card>
      ) : (
        list.map((r) => <RoundCard key={r.id} request={request} round={r} onChanged={onChanged} />)
      )}
    </div>
  );
}
