import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Paperclip, Pencil, Star } from 'lucide-react';
import { useState } from 'react';
import { proposalsApi } from '@/api/proposals';
import type { InsurerResponse, Proposal, ResponseStatus, TermsInput } from '@/api/proposals';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { NumberInput, SelectInput, TextInput } from '@/features/assets/FormControls';
import { formatDate, formatDateTime } from '@/utils/format';
import { RESPONSE_STAGES } from './proposalList';

const STATUSES = [
  { value: 'RECEIVED', label: 'Terms received' },
  { value: 'DECLINED', label: 'Declined to quote' },
];

function TermsDialog({
  response,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<{
  response: InsurerResponse;
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onSave: (terms: TermsInput, file?: File) => void;
}>) {
  const [terms, setTerms] = useState<TermsInput>({
    status: response.status === 'PENDING' ? 'RECEIVED' : response.status,
    premium: response.premium,
    rate: response.rate,
    deductibles: response.deductibles ?? '',
    conditions: response.conditions ?? '',
    validUntil: response.validUntil ?? '',
    remarks: response.remarks ?? '',
  });
  const [file, setFile] = useState<File>();
  const set = (patch: Partial<TermsInput>) => setTerms((t) => ({ ...t, ...patch }));
  const missingPremium = terms.status === 'RECEIVED' && terms.premium === undefined;
  return (
    <Modal
      open
      title={`Terms of ${response.insurerName}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="accent"
            busy={busy}
            disabled={missingPremium}
            onClick={() =>
              onSave(
                { ...terms, validUntil: terms.validUntil === '' ? undefined : terms.validUntil },
                file,
              )
            }
          >
            Save Terms
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <div className="form-grid">
          <SelectInput
            label="Response"
            value={terms.status}
            options={STATUSES}
            onChange={(v) => set({ status: v as ResponseStatus })}
          />
          <NumberInput
            label="Premium"
            required={terms.status === 'RECEIVED'}
            error={missingPremium ? 'Enter the premium quoted' : undefined}
            value={terms.premium}
            onChange={(premium) => set({ premium })}
          />
          <NumberInput
            label="Rate %"
            step="0.0001"
            value={terms.rate}
            onChange={(rate) => set({ rate })}
          />
          <TextInput
            label="Valid until"
            type="date"
            value={terms.validUntil}
            onChange={(validUntil) => set({ validUntil })}
          />
          <TextInput
            label="Deductibles"
            value={terms.deductibles}
            onChange={(deductibles) => set({ deductibles })}
          />
          <TextInput
            label="Conditions"
            value={terms.conditions}
            onChange={(conditions) => set({ conditions })}
          />
          <TextInput
            label="Remarks"
            value={terms.remarks}
            onChange={(remarks) => set({ remarks })}
          />
          <Field label="Response document" hint="The insurer's quotation or e-mail">
            {(id) => (
              <input
                id={id}
                className="input"
                type="file"
                onChange={(e) => setFile(e.target.files?.[0])}
              />
            )}
          </Field>
        </div>
      </div>
    </Modal>
  );
}

function ResponseHistory({ proposalId }: Readonly<{ proposalId: number }>) {
  const history = useQuery({
    queryKey: ['proposal', proposalId, 'response-history'],
    queryFn: () => proposalsApi.history(proposalId),
  });
  return (
    <Card title="Version history" flush>
      <DataTable
        loading={history.isLoading}
        rows={history.data ?? []}
        rowKey={(h) => `${h.responseId}-${h.revision}`}
        emptyMessage="No change recorded yet."
        columns={[
          {
            key: 'w',
            header: 'Changed',
            render: (h) => `${h.changedBy} ${formatDateTime(h.changedAt)}`,
          },
          {
            key: 'r',
            header: 'Response / revision',
            render: (h) => `#${h.responseId} r${h.revision}`,
          },
          { key: 's', header: 'Status', render: (h) => <StatusBadge status={h.status} /> },
          {
            key: 'p',
            header: 'Premium',
            numeric: true,
            render: (h) => <Amount value={h.premium} />,
          },
          { key: 'v', header: 'Valid until', render: (h) => formatDate(h.validUntil) },
          { key: 'c', header: 'Recommended', render: (h) => (h.recommended ? 'Yes' : '') },
        ]}
      />
    </Card>
  );
}

/**
 * Insurer responses (BRNB.009): the grid of the insurers approached with their terms, editable by
 * TSU while the terms are being collected, the response documents, the recommended insurer and the
 * version history of every change.
 */
export function ResponsesTab({ proposal }: Readonly<{ proposal: Proposal }>) {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<InsurerResponse | null>(null);
  const responses = useQuery({
    queryKey: ['proposal', proposal.id, 'responses'],
    queryFn: () => proposalsApi.responses(proposal.id),
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['proposal', proposal.id] });
  const save = useMutation({
    mutationFn: async ({
      r,
      terms,
      file,
    }: {
      r: InsurerResponse;
      terms: TermsInput;
      file?: File;
    }) => {
      const saved = await proposalsApi.recordTerms(proposal.id, r.id, terms);
      return file === undefined ? saved : proposalsApi.attachResponse(proposal.id, r.id, file);
    },
    onSuccess: async (r) => {
      setEditing(null);
      await refresh();
      toast.success(`Terms of ${r.insurerName} saved (revision ${r.revision})`);
    },
  });
  const recommend = useMutation({
    mutationFn: (r: InsurerResponse) => proposalsApi.recommend(proposal.id, r.id),
    onSuccess: async (r) => {
      await refresh();
      toast.success(`${r.insurerName} is the recommended insurer`);
    },
  });
  const editable = RESPONSE_STAGES.has(proposal.status) && can('TSU_PROCESS');
  return (
    <div className="stack">
      <ErrorAlert error={responses.error ?? recommend.error} />
      <Card title="Insurer responses" flush>
        <DataTable<InsurerResponse>
          loading={responses.isLoading}
          rows={responses.data ?? []}
          rowKey={(r) => r.id}
          emptyMessage="The quotation slip has not been sent to the insurers yet."
          columns={[
            { key: 'i', header: 'Insurer', render: (r) => <strong>{r.insurerName}</strong> },
            { key: 's', header: 'Response', render: (r) => <StatusBadge status={r.status} /> },
            {
              key: 'p',
              header: 'Premium',
              numeric: true,
              render: (r) => <Amount value={r.premium} />,
            },
            { key: 'r', header: 'Rate %', numeric: true, render: (r) => r.rate ?? '' },
            { key: 'd', header: 'Deductibles', render: (r) => r.deductibles ?? '' },
            { key: 'c', header: 'Conditions', render: (r) => r.conditions ?? '' },
            { key: 'v', header: 'Valid until', render: (r) => formatDate(r.validUntil) },
            {
              key: 'x',
              header: 'Document',
              render: (r) =>
                r.documentId === undefined ? (
                  ''
                ) : (
                  <Paperclip size={14} aria-label="Document attached" />
                ),
            },
            {
              key: 'a',
              header: 'Actions',
              render: (r) => (
                <div className="row">
                  {r.recommended && <StatusBadge status="RECOMMENDED" />}
                  {editable && (
                    <Button
                      size="sm"
                      variant="secondary"
                      icon={<Pencil size={14} />}
                      onClick={() => setEditing(r)}
                    >
                      Terms
                    </Button>
                  )}
                  {editable && r.status === 'RECEIVED' && !r.recommended && (
                    <Button
                      size="sm"
                      variant="ghost"
                      icon={<Star size={14} />}
                      onClick={() => recommend.mutate(r)}
                    >
                      Recommend
                    </Button>
                  )}
                </div>
              ),
            },
          ]}
        />
      </Card>
      <ResponseHistory proposalId={proposal.id} />
      {editing && (
        <TermsDialog
          response={editing}
          busy={save.isPending}
          error={save.error}
          onClose={() => setEditing(null)}
          onSave={(terms, file) => save.mutate({ r: editing, terms, file })}
        />
      )}
    </div>
  );
}
