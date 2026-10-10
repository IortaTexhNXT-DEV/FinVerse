import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { attachmentsApi } from '@/api/attachments';
import { addresses } from '@/api/renewalPlacement';
import { renewalProposalApi } from '@/api/renewalProposal';
import type { ProposalView } from '@/api/renewalProposal';
import type { CandidateDetail } from '@/api/renewal';
import { useAuth } from '@/auth/authContext';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';
import { SaveAsDialog } from '../placement/SaveAsDialog';
import { TsuCard } from './TsuCard';

function SignatoryPicker({
  count,
  value,
  onChange,
}: Readonly<{ count: number; value: string[]; onChange: (v: string[]) => void }>) {
  const users = useQuery({
    queryKey: ['renewal', 'proposal-users', 'RNW_REVIEW'],
    queryFn: () => renewalProposalApi.users('RNW_REVIEW'),
  });
  return (
    <span className="rnw-actions">
      {Array.from({ length: count }, (_, i) => (
        <Field key={i} label={`Signatory ${String(i + 1)}`} required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={value[i] ?? ''}
              onChange={(e) => {
                const next = [...value];
                next[i] = e.target.value;
                onChange(next);
              }}
            >
              <option value="">Select…</option>
              {(users.data ?? []).map((u) => (
                <option key={u.username} value={u.username}>
                  {u.displayName}
                </option>
              ))}
            </select>
          )}
        </Field>
      ))}
    </span>
  );
}

function SendForm({
  proposal,
  onSend,
  busy,
}: Readonly<{
  proposal: ProposalView;
  onSend: (to: string[], cc: string[]) => void;
  busy: boolean;
}>) {
  const [to, setTo] = useState('');
  const [cc, setCc] = useState('');
  return (
    <div className="rnw-actions">
      <Field label={`Recipients of ${proposal.fileName}`} required>
        {(id) => (
          <input id={id} className="input" value={to} onChange={(e) => setTo(e.target.value)} />
        )}
      </Field>
      <Field label="CC recipients">
        {(id) => (
          <input id={id} className="input" value={cc} onChange={(e) => setCc(e.target.value)} />
        )}
      </Field>
      <Button
        busy={busy}
        disabled={addresses(to).length === 0}
        onClick={() => onSend(addresses(to), addresses(cc))}
      >
        Send Proposal
      </Button>
    </div>
  );
}

/**
 * The Proposal tab of a renewal account (FRRN.017, FRRN.018): Quick and Full Proposals of an account
 * For Proposal with the signatories, their sending through CCM, and the TSU request of an account
 * For Quotation with its comparative table.
 */
export function ProposalTab({ detail }: Readonly<{ detail: CandidateDetail }>) {
  const ref = detail.row.renewalRef;
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const download = useFileDownload();
  const queryClient = useQueryClient();
  const [kind, setKind] = useState('QUICK');
  const [signatories, setSignatories] = useState<string[]>([]);
  const [sending, setSending] = useState<ProposalView | null>(null);
  const [saveAs, setSaveAs] = useState<ProposalView | null>(null);
  const list = useQuery({
    queryKey: ['renewal', 'proposals', companyId, ref],
    queryFn: () => renewalProposalApi.proposals(companyId, ref),
  });
  const generate = useMutation({
    mutationFn: () => renewalProposalApi.generate(companyId, ref, kind, signatories),
    onSuccess: async (p) => {
      toast.success(`${p.fileName} generated`);
      await queryClient.invalidateQueries({ queryKey: ['renewal', 'proposals'] });
    },
  });
  const send = useMutation({
    mutationFn: (input: { p: ProposalView; to: string[]; cc: string[] }) =>
      renewalProposalApi.send(companyId, ref, input.p.proposalNo, input.to, input.cc),
    onSuccess: async (r) => {
      toast.success(r.message);
      setSending(null);
      await queryClient.invalidateQueries({ queryKey: ['renewal'] });
    },
  });
  const forProposal = detail.row.disposition === 'FOR_PROPOSAL';
  return (
    <div className="stack">
      <Card title="Proposals" flush>
        <ErrorAlert error={list.error ?? generate.error ?? send.error} />
        {can('RNW_DISPOSE') && (
          <div className="rnw-actions">
            <Field label="Proposal">
              {(id) => (
                <select
                  id={id}
                  className="select"
                  value={kind}
                  onChange={(e) => setKind(e.target.value)}
                >
                  <option value="QUICK">Quick Proposal</option>
                  <option value="FULL">Full Proposal</option>
                </select>
              )}
            </Field>
            {kind === 'FULL' && (
              <SignatoryPicker
                count={list.data?.requiredSignatories ?? 2}
                value={signatories}
                onChange={setSignatories}
              />
            )}
            <Button
              disabled={!forProposal}
              busy={generate.isPending}
              onClick={() => generate.mutate()}
            >
              Generate Proposal Slip
            </Button>
          </div>
        )}
        <DataTable<ProposalView>
          loading={list.isLoading}
          rows={list.data?.proposals ?? []}
          rowKey={(p) => p.proposalNo}
          emptyMessage="No proposal generated"
          columns={[
            { key: 'file', header: 'File Name', render: (p) => p.fileName },
            {
              key: 'kind',
              header: 'Proposal',
              render: (p) => ({ QUICK: 'Quick', FULL: 'Full', TSU: 'TSU' })[p.kind] ?? p.kind,
            },
            {
              key: 'status',
              header: 'Status',
              render: (p) => (
                <StatusBadge
                  status={p.status}
                  label={p.status === 'SENT' ? 'Sent' : 'Generated'}
                  full
                />
              ),
            },
            { key: 'ccm', header: 'CCM Message', render: (p) => p.messageNo ?? '' },
            { key: 'at', header: 'Generated On', render: (p) => formatDateTime(p.generatedAt) },
            {
              key: 'actions',
              header: 'Actions',
              render: (p) => (
                <RowActionMenu
                  label={p.fileName}
                  actions={[
                    ...(p.attachmentId === null
                      ? []
                      : [
                          {
                            label: 'Download',
                            onSelect: () =>
                              download.mutate(() => attachmentsApi.download(p.attachmentId ?? 0)),
                          },
                          { label: 'Download As', onSelect: () => setSaveAs(p) },
                        ]),
                    ...(can('RNW_DISPOSE')
                      ? [{ label: 'Send via CCM', onSelect: () => setSending(p) }]
                      : []),
                  ]}
                />
              ),
            },
          ]}
        />
        {sending !== null && (
          <SendForm
            proposal={sending}
            busy={send.isPending}
            onSend={(to, cc) => send.mutate({ p: sending, to, cc })}
          />
        )}
      </Card>
      <TsuCard renewalRef={ref} forQuotation={detail.row.disposition === 'FOR_QUOTATION'} />
      {saveAs !== null && typeof saveAs.attachmentId === 'number' && (
        <SaveAsDialog
          attachmentId={saveAs.attachmentId}
          fileName={saveAs.fileName}
          onClose={() => setSaveAs(null)}
        />
      )}
    </div>
  );
}
