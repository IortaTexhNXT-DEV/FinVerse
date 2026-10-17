import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { proposalsApi } from '@/api/submittedProposals';
import type { NominatedRate } from '@/api/submittedProposals';
import { useAuth } from '@/auth/authContext';
import { InsurerSelect } from '@/components/broking/InsurerSelect';
import { InsurerName, LovLabel } from '@/components/broking/LovLabel';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { RowActions } from '@/components/ui/RowActions';
import type { RowAction } from '@/components/ui/RowActions';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { SBM_LOV } from '../common/submittedCodes';

const KEY = ['submitted', 'proposals', 'rates'] as const;

interface RateForm {
  segment: string;
  vehicleType: string;
  insurer: string;
  rate: string;
  from: string;
  until: string;
}

function initialForm(row: NominatedRate | null): RateForm {
  if (row === null) {
    return { segment: '', vehicleType: '', insurer: '', rate: '', from: '', until: '' };
  }
  return {
    segment: row.segment,
    vehicleType: row.vehicleType ?? '',
    insurer: row.insurerCode,
    rate: String(row.rate),
    from: row.effectiveFrom,
    until: row.effectiveTo ?? '',
  };
}

function RateDialog({
  row,
  onClose,
}: Readonly<{ row: NominatedRate | null; onClose: () => void }>) {
  const companyId = useCompanyId();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<RateForm>(() => initialForm(row));
  const set = (patch: Partial<RateForm>) => setForm((f) => ({ ...f, ...patch }));
  const save = useMutation({
    mutationFn: () =>
      proposalsApi.saveRate(companyId, row?.id ?? null, {
        segment: form.segment,
        vehicleType: form.vehicleType.trim() || undefined,
        insurerCode: form.insurer,
        rate: Number(form.rate),
        effectiveFrom: form.from,
        effectiveTo: form.until || undefined,
      }),
    onSuccess: async () => {
      onClose();
      await queryClient.invalidateQueries({ queryKey: KEY });
    },
  });
  const locked = row !== null;
  const ready =
    form.segment !== '' && form.insurer !== '' && Number(form.rate) > 0 && form.from !== '';
  return (
    <Modal
      open
      title={locked ? 'Edit nominated rate' : 'New nominated rate'}
      onClose={onClose}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={save.isPending} disabled={!ready} onClick={() => save.mutate()}>
            Save for Authorization
          </Button>
        </>
      }
    >
      <ErrorAlert error={save.error} />
      <div className="form-grid">
        <Field label="Segment" required>
          {(id) => (
            <LovSelect
              id={id}
              type={SBM_LOV.segment}
              value={form.segment}
              disabled={locked}
              onChange={(v) => set({ segment: v })}
            />
          )}
        </Field>
        <Field label="Vehicle Classification" hint="Leave empty for every classification">
          {(id) => (
            <input
              id={id}
              className="input"
              value={form.vehicleType}
              disabled={locked}
              onChange={(e) => set({ vehicleType: e.target.value })}
            />
          )}
        </Field>
        <Field label="Insurer" required>
          {(id) =>
            locked ? (
              <InsurerName code={form.insurer} />
            ) : (
              <InsurerSelect id={id} value={form.insurer} onChange={(v) => set({ insurer: v })} />
            )
          }
        </Field>
        <Field label="Rate (%)" required>
          {(id) => (
            <input
              id={id}
              className="input"
              inputMode="decimal"
              value={form.rate}
              onChange={(e) => set({ rate: e.target.value })}
            />
          )}
        </Field>
        <Field label="Effective From" required>
          {(id) => (
            <DateInput id={id} value={form.from} onChange={(e) => set({ from: e.target.value })} />
          )}
        </Field>
        <Field label="Effective To">
          {(id) => (
            <DateInput
              id={id}
              value={form.until}
              onChange={(e) => set({ until: e.target.value })}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

function rateActions(
  r: NominatedRate,
  may: boolean,
  on: { authorize: () => unknown; edit: () => void; deactivate: () => unknown },
): RowAction[] {
  if (!may) {
    return [];
  }
  const actions: RowAction[] = [];
  if (r.recordStatus === 'PENDING_AUTHORIZATION') {
    actions.push({
      label: 'Authorize',
      onSelect: on.authorize,
      confirm: {
        title: 'Authorize nominated rate',
        effect: 'The rate is proposed for new renewal proposals.',
      },
    });
  }
  actions.push({ label: 'Edit', onSelect: on.edit });
  if (r.recordStatus === 'ACTIVE') {
    actions.push({
      label: 'Deactivate',
      onSelect: on.deactivate,
      danger: true,
      confirm: {
        title: 'Deactivate nominated rate',
        effect: 'The rate is no longer proposed.',
        destructive: true,
      },
    });
  }
  return actions;
}

/** Nominated package rates per segment, vehicle classification and insurer, maker-checker. */
export function NominatedRates() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<NominatedRate | null>();
  const rows = useQuery({
    queryKey: [...KEY, companyId],
    queryFn: () => proposalsApi.rates(companyId),
    enabled: companyId > 0,
  });
  const act = useMutation({
    mutationFn: ({ id, action }: { id: number; action: 'authorize' | 'deactivate' }) =>
      proposalsApi.rateAction(companyId, id, action),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: KEY }),
  });
  const may = can('SBM_PROPOSAL');
  return (
    <Card
      title="Nominated package rates"
      flush
      actions={
        may && (
          <Button icon={<Plus size={16} />} onClick={() => setEditing(null)}>
            New Nominated Rate
          </Button>
        )
      }
    >
      <ErrorAlert error={rows.error ?? act.error} />
      <DataTable<NominatedRate>
        loading={rows.isLoading}
        rows={rows.data ?? []}
        rowKey={(r) => r.id}
        emptyMessage="No nominated rate is set up"
        columns={[
          {
            key: 'seg',
            header: 'Segment',
            render: (r) => <LovLabel type={SBM_LOV.segment} code={r.segment} />,
          },
          {
            key: 'class',
            header: 'Vehicle Classification',
            render: (r) => r.vehicleType ?? 'Every classification',
          },
          { key: 'ins', header: 'Insurer', render: (r) => <InsurerName code={r.insurerCode} /> },
          { key: 'rate', header: 'Rate', kind: 'amount', render: (r) => `${String(r.rate)}%` },
          { key: 'from', header: 'From', kind: 'date', render: (r) => formatDate(r.effectiveFrom) },
          { key: 'to', header: 'To', kind: 'date', render: (r) => formatDate(r.effectiveTo) },
          { key: 'st', header: 'Status', render: (r) => <StatusBadge status={r.recordStatus} /> },
          {
            key: 'act',
            header: '',
            render: (r) => (
              <RowActions
                record={`${r.segment} ${r.insurerCode}`}
                actions={rateActions(r, may, {
                  authorize: () => act.mutateAsync({ id: r.id, action: 'authorize' }),
                  edit: () => setEditing(r),
                  deactivate: () => act.mutateAsync({ id: r.id, action: 'deactivate' }),
                })}
              />
            ),
          },
        ]}
      />
      {editing !== undefined && <RateDialog row={editing} onClose={() => setEditing(undefined)} />}
    </Card>
  );
}
