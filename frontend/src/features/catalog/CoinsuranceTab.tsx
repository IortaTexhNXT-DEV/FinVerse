import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Pencil, Plus } from 'lucide-react';
import { useState } from 'react';
import { catalogApi } from '@/api/catalog';
import { coinsuranceApi } from '@/api/insurerPage';
import type { InsurerParticipation, ParticipationInput } from '@/api/insurerPage';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { LoadingPanel } from '@/components/ui/LoadingPanel';
import { Modal } from '@/components/ui/Modal';
import { Notice } from '@/components/ui/Notice';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { NumberInput, SelectInput, TextInput } from '@/features/assets/FormControls';
import { formatDate, formatRate } from '@/utils/format';
import { RecordActions } from './RecordActions';
import { participationReady } from './coinsuranceForm';

const REFRESH = [['catalog']] as const;
const YES_NO = [
  { value: 'false', label: 'No' },
  { value: 'true', label: 'Yes' },
];

function ParticipationModal({
  insurerId,
  companyId,
  participation,
  onClose,
}: Readonly<{
  insurerId: number;
  companyId: number;
  participation?: InsurerParticipation;
  onClose: () => void;
}>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<Partial<ParticipationInput>>(participation ?? { lead: false });
  const panel = useQuery({
    queryKey: ['catalog', 'insurers', companyId],
    queryFn: () => catalogApi.insurers(companyId),
  });
  const save = useMutation({
    mutationFn: () => {
      const input = form as ParticipationInput;
      return participation
        ? coinsuranceApi.update(participation.id, input)
        : coinsuranceApi.create(insurerId, input);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['catalog'] });
      toast.success('Participation saved – pending authorization');
      onClose();
    },
  });
  const set = (patch: Partial<ParticipationInput>) => setForm((f) => ({ ...f, ...patch }));
  return (
    <Modal
      title={participation ? 'Edit Participation' : 'New Participation'}
      open
      onClose={onClose}
      footer={
        <Button
          variant="accent"
          busy={save.isPending}
          disabled={!participationReady(form)}
          onClick={() => save.mutate()}
        >
          Save for Authorization
        </Button>
      }
    >
      <ErrorAlert error={save.error} />
      <div className="form-grid">
        <SelectInput
          label="Participating insurer"
          required
          blank="Select an insurer"
          value={form.participantCode}
          options={(panel.data ?? []).map((i) => ({
            value: i.partyCode,
            label: `${i.partyCode} – ${i.name}`,
          }))}
          onChange={(participantCode) => set({ participantCode })}
        />
        <NumberInput
          label="Share (%)"
          required
          step="0.0001"
          value={form.sharePct}
          onChange={(sharePct) => set({ sharePct })}
        />
        <SelectInput
          label="Lead insurer"
          value={String(form.lead ?? false)}
          options={YES_NO}
          onChange={(v) => set({ lead: v === 'true' })}
        />
        <TextInput
          label="Effective from"
          type="date"
          required
          value={form.effectiveFrom}
          onChange={(effectiveFrom) => set({ effectiveFrom })}
        />
        <TextInput
          label="Effective to"
          type="date"
          value={form.effectiveTo ?? undefined}
          onChange={(effectiveTo) => set({ effectiveTo: effectiveTo || null })}
        />
      </div>
    </Modal>
  );
}

/**
 * Co-insurance / Risk Participation (BRD-00 CRQ08): the default co-insurance arrangement of the
 * insurer, used as the co-insurance split of an invoice when none is entered at booking and the
 * authorized shares in force total 100 %. Every change is saved for authorization by another user.
 */
export function CoinsuranceTab({
  insurerId,
  companyId,
  maintain,
}: Readonly<{ insurerId: number; companyId: number; maintain: boolean }>) {
  const [editing, setEditing] = useState<InsurerParticipation | 'new' | null>(null);
  const arrangement = useQuery({
    queryKey: ['catalog', 'coinsurance', insurerId],
    queryFn: () => coinsuranceApi.arrangement(insurerId),
  });
  if (arrangement.data === undefined) {
    return arrangement.error ? <ErrorAlert error={arrangement.error} /> : <LoadingPanel />;
  }
  const a = arrangement.data;
  return (
    <>
      <Notice tone={a.complete ? 'success' : 'info'}>
        {a.complete
          ? 'The authorized shares in force today total 100%: booking uses them as the default co-insurance split.'
          : `The authorized shares in force today total ${formatRate(a.totalToday, '0')}%. Booking uses an arrangement only when its shares total 100% and include this insurer.`}
      </Notice>
      <Card
        title="Co-insurance / Risk Participation"
        flush
        actions={
          maintain && (
            <Button
              size="sm"
              variant="secondary"
              icon={<Plus size={14} />}
              onClick={() => setEditing('new')}
            >
              Add participant
            </Button>
          )
        }
      >
        <DataTable<InsurerParticipation>
          rows={a.participations}
          rowKey={(p) => p.id}
          emptyMessage="No co-insurance participation recorded."
          columns={[
            {
              key: 'c',
              header: 'Participating insurer',
              render: (p) => <strong>{p.participantCode}</strong>,
            },
            { key: 's', header: 'Share (%)', render: (p) => formatRate(p.sharePct) },
            { key: 'l', header: 'Lead', render: (p) => (p.lead ? 'Yes' : 'No') },
            { key: 'f', header: 'Effective from', render: (p) => formatDate(p.effectiveFrom) },
            { key: 't', header: 'Effective to', render: (p) => formatDate(p.effectiveTo) },
            { key: 'r', header: 'Status', render: (p) => <StatusBadge status={p.recordStatus} /> },
            {
              key: 'x',
              header: 'Actions',
              render: (p) => (
                <>
                  {maintain && (
                    <Button
                      size="sm"
                      variant="ghost"
                      icon={<Pencil size={14} />}
                      onClick={() => setEditing(p)}
                    >
                      Edit
                    </Button>
                  )}
                  <RecordActions
                    kind="INSURER_PARTICIPATION"
                    record={p}
                    label={`${p.participantCode} ${String(p.sharePct)}%`}
                    refresh={REFRESH}
                  />
                </>
              ),
            },
          ]}
        />
      </Card>
      {editing && (
        <ParticipationModal
          insurerId={insurerId}
          companyId={companyId}
          participation={editing === 'new' ? undefined : editing}
          onClose={() => setEditing(null)}
        />
      )}
    </>
  );
}
