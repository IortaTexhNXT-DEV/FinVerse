import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { adviceRecipientsApi } from '@/api/adviceRecipients';
import type { AdviceRecipient } from '@/api/adviceRecipients';
import { useAuth } from '@/auth/authContext';
import { LovLabel } from '@/components/broking/LovLabel';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { RowActions } from '@/components/ui/RowActions';
import type { RowAction } from '@/components/ui/RowActions';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { addresses, recipientProblem } from './issuanceLogic';

const KEY = ['issuance', 'advice-recipients'] as const;

interface RecipientForm {
  bank: string;
  segment: string;
  to: string;
  cc: string;
  auto: boolean;
  from: string;
  until: string;
}

function initialForm(row: AdviceRecipient | null): RecipientForm {
  if (row === null) {
    return { bank: '', segment: '', to: '', cc: '', auto: false, from: '', until: '' };
  }
  return {
    bank: row.mortgageeBank,
    segment: row.marketSegment ?? '',
    to: row.to.join(', '),
    cc: row.cc.join(', '),
    auto: row.autoSend,
    from: row.effectiveFrom,
    until: row.effectiveTo ?? '',
  };
}

function RecipientDialog({
  row,
  onClose,
}: Readonly<{ row: AdviceRecipient | null; onClose: () => void }>) {
  const companyId = useCompanyId();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<RecipientForm>(() => initialForm(row));
  const set = (patch: Partial<RecipientForm>) => setForm((f) => ({ ...f, ...patch }));
  const data = {
    mortgageeBank: form.bank,
    marketSegment: form.segment || undefined,
    to: addresses(form.to),
    cc: addresses(form.cc),
    autoSend: form.auto,
    effectiveFrom: form.from,
    effectiveTo: form.until || undefined,
  };
  const problem = recipientProblem(data);
  const save = useMutation({
    mutationFn: () => adviceRecipientsApi.save(companyId, row?.id ?? null, data),
    onSuccess: async () => {
      onClose();
      await queryClient.invalidateQueries({ queryKey: KEY });
    },
  });
  return (
    <Modal
      open
      title={row === null ? 'New Insurance Advice recipient' : 'Edit Insurance Advice recipient'}
      onClose={onClose}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={save.isPending}
            disabled={problem !== undefined}
            onClick={() => save.mutate()}
          >
            Save for Authorization
          </Button>
        </>
      }
    >
      <ErrorAlert error={save.error} />
      {problem !== undefined && <p className="muted">{problem}</p>}
      <div className="form-grid">
        <Field label="Mortgagee Bank" required>
          {(id) => (
            <LovSelect
              id={id}
              type="MORTGAGEE_BANK"
              value={form.bank}
              disabled={row !== null}
              onChange={(v) => set({ bank: v })}
            />
          )}
        </Field>
        <Field label="Market Segment" hint="Leave empty for every segment of the bank">
          {(id) => (
            <LovSelect
              id={id}
              type="MARKET_SEGMENT"
              value={form.segment}
              placeholder="Every segment"
              disabled={row !== null}
              onChange={(v) => set({ segment: v })}
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
      <Field label="Recipients" hint="E-mail addresses separated by commas">
        {(id) => (
          <input
            id={id}
            className="input"
            value={form.to}
            onChange={(e) => set({ to: e.target.value })}
          />
        )}
      </Field>
      <Field label="Copy To">
        {(id) => (
          <input
            id={id}
            className="input"
            value={form.cc}
            onChange={(e) => set({ cc: e.target.value })}
          />
        )}
      </Field>
      <label className="checkbox">
        <input
          type="checkbox"
          checked={form.auto}
          onChange={(e) => set({ auto: e.target.checked })}
        />{' '}
        Send each new Insurance Advice automatically, with the password in a separate e-mail
      </label>
    </Modal>
  );
}

function recipientActions(
  r: AdviceRecipient,
  may: { maintain: boolean; authorize: boolean },
  on: { authorize: () => unknown; edit: () => void; deactivate: () => unknown },
): RowAction[] {
  const actions: RowAction[] = [];
  if (may.authorize && r.recordStatus === 'PENDING_AUTHORIZATION') {
    actions.push({
      label: 'Authorize',
      onSelect: on.authorize,
      confirm: { title: 'Authorize recipient', effect: 'The recipient set-up takes effect.' },
    });
  }
  if (may.maintain) {
    actions.push({ label: 'Edit', onSelect: on.edit });
  }
  if (may.maintain && r.recordStatus === 'ACTIVE') {
    actions.push({
      label: 'Deactivate',
      onSelect: on.deactivate,
      danger: true,
      confirm: {
        title: 'Deactivate recipient',
        effect: 'New Insurance Advices of the bank wait in the register for manual sending.',
        destructive: true,
      },
    });
  }
  return actions;
}

/**
 * Insurance Advice recipients: per mortgagee bank, and where needed per market segment, the
 * recipients and whether each new advice is sent to them automatically. A change takes effect
 * once a second user authorizes it.
 */
export default function AdviceRecipientsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<AdviceRecipient | null>();
  const rows = useQuery({
    queryKey: [...KEY, companyId],
    queryFn: () => adviceRecipientsApi.list(companyId),
    enabled: companyId > 0,
  });
  const act = useMutation({
    mutationFn: ({ id, action }: { id: number; action: 'authorize' | 'deactivate' }) =>
      adviceRecipientsApi.action(companyId, id, action),
    onSuccess: async (r) => {
      toast.success(r.recordStatus === 'ACTIVE' ? 'Recipient authorized' : 'Recipient deactivated');
      await queryClient.invalidateQueries({ queryKey: KEY });
    },
  });
  const may = {
    maintain: can('LOV_MANAGE'),
    authorize: can('LOV_MANAGE') || can('MASTER_AUTHORIZE'),
  };
  return (
    <div className="stack">
      <PageHeader
        title="Insurance Advice Recipients"
        description="Who receives the Insurance Advices of each mortgagee bank, and whether they are sent automatically when generated."
        actions={
          may.maintain && (
            <Button icon={<Plus size={16} />} onClick={() => setEditing(null)}>
              New Recipient
            </Button>
          )
        }
      />
      <ErrorAlert error={rows.error ?? act.error} />
      <Card flush>
        <DataTable<AdviceRecipient>
          caption="Insurance Advice recipients"
          loading={rows.isLoading}
          rows={rows.data ?? []}
          rowKey={(r) => r.id}
          emptyMessage="No mortgagee bank is set up; every advice waits in the register"
          columns={[
            {
              key: 'bank',
              header: 'Mortgagee Bank',
              render: (r) => <LovLabel type="MORTGAGEE_BANK" code={r.mortgageeBank} />,
            },
            {
              key: 'seg',
              header: 'Segment',
              render: (r) =>
                r.marketSegment ? (
                  <LovLabel type="MARKET_SEGMENT" code={r.marketSegment} />
                ) : (
                  'Every segment'
                ),
            },
            {
              key: 'to',
              header: 'Recipients',
              render: (r) => (
                <span>
                  {r.to.join(', ') || '—'}
                  {r.cc.length > 0 && <span className="cell-sub">Copy: {r.cc.join(', ')}</span>}
                </span>
              ),
            },
            { key: 'auto', header: 'Automatic', render: (r) => (r.autoSend ? 'Yes' : 'No') },
            {
              key: 'from',
              header: 'From',
              kind: 'date',
              render: (r) => formatDate(r.effectiveFrom),
            },
            { key: 'until', header: 'To', kind: 'date', render: (r) => formatDate(r.effectiveTo) },
            { key: 'st', header: 'Status', render: (r) => <StatusBadge status={r.recordStatus} /> },
            {
              key: 'act',
              header: '',
              render: (r) => (
                <RowActions
                  record={r.mortgageeBank}
                  actions={recipientActions(r, may, {
                    authorize: () => act.mutateAsync({ id: r.id, action: 'authorize' }),
                    edit: () => setEditing(r),
                    deactivate: () => act.mutateAsync({ id: r.id, action: 'deactivate' }),
                  })}
                />
              ),
            },
          ]}
        />
      </Card>
      {editing !== undefined && (
        <RecipientDialog row={editing} onClose={() => setEditing(undefined)} />
      )}
    </div>
  );
}
