import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { taxApi } from '@/api/tax';
import type { IcLine, IcLineRequest, IcMeasure, IcSchedule, NormalBalance } from '@/api/tax';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { AuthorizeButton, SelectField, TextField } from './MasterControls';
import { awaitsOtherChecker } from '@/utils/makerChecker';

const SCHEDULES: readonly IcSchedule[] = [
  'PREMIUMS',
  'LOSSES',
  'COMMISSIONS',
  'NET_WORTH',
  'RBC',
  'RESERVES',
  'INVESTMENTS',
];
const SIDES: readonly NormalBalance[] = ['DEBIT', 'CREDIT'];
const MEASURES: readonly IcMeasure[] = ['BALANCE', 'MOVEMENT'];

type Form = Partial<IcLineRequest> & { id?: number };

/**
 * Mapping of the IC schedule lines to account ranges or report groups, with the sign of each line
 * and the RBC factors (maker-checker).
 */
export default function IcMappingPage() {
  const companyId = useCompanyId();
  const { can, user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [schedule, setSchedule] = useState<IcSchedule | undefined>(undefined);
  const [form, setForm] = useState<Form | null>(null);
  const lines = useQuery({
    queryKey: ['ic-mappings', companyId],
    queryFn: () => taxApi.icMappings(companyId),
    enabled: companyId > 0,
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['ic-mappings'] });
  const save = useMutation({
    mutationFn: (f: Form) => {
      const body = { ...f, companyId } as IcLineRequest;
      return f.id === undefined ? taxApi.createIcLine(body) : taxApi.updateIcLine(f.id, body);
    },
    onSuccess: async (l) => {
      await refresh();
      setForm(null);
      toast.success(`IC line ${l.lineCode} saved – pending authorization`);
    },
  });
  const authorize = useMutation({
    mutationFn: taxApi.authorizeIcLine,
    onSuccess: async (l) => {
      await refresh();
      toast.success(`IC line ${l.lineCode} authorized`);
    },
  });
  const set = (patch: Partial<Form>) => setForm((f) => ({ ...f, ...patch }));
  const rows = (lines.data ?? []).filter((l) => schedule === undefined || l.schedule === schedule);

  return (
    <div className="stack">
      <PageHeader
        section="Tax & Statutory"
        title="IC Schedule Mapping"
        description="Which ledger accounts feed each Insurance Commission schedule line; the sign and RBC factors are parameters."
        actions={
          can('TAX_MANAGE') && (
            <Button
              variant="accent"
              icon={<Plus size={16} />}
              onClick={() =>
                setForm({
                  schedule: schedule ?? 'PREMIUMS',
                  normalBalance: 'CREDIT',
                  signFactor: 1,
                  lineOrder: 100,
                })
              }
            >
              New line
            </Button>
          )
        }
      />
      <Card>
        <SelectField
          label="Schedule"
          value={schedule}
          options={SCHEDULES}
          allowEmpty
          onChange={setSchedule}
        />
      </Card>
      <ErrorAlert error={lines.error ?? authorize.error} />
      <Card flush>
        <DataTable<IcLine>
          rows={rows}
          loading={lines.isLoading}
          rowKey={(l) => l.id}
          caption="IC mapping"
          onRowClick={can('TAX_MANAGE') ? (l) => setForm(l) : undefined}
          columns={[
            { key: 'sc', header: 'Schedule', render: (l) => l.schedule },
            { key: 'c', header: 'Line', render: (l) => <strong>{l.lineCode}</strong> },
            { key: 'd', header: 'Description', render: (l) => l.description },
            {
              key: 'm',
              header: 'Accounts',
              render: (l) => l.reportGroup ?? `${l.accountFrom ?? ''}–${l.accountTo ?? ''}`,
            },
            { key: 'b', header: 'Side', render: (l) => l.normalBalance },
            {
              key: 'g',
              header: 'Sign',
              numeric: true,
              render: (l) => (l.signFactor > 0 ? '+' : '−'),
            },
            { key: 'me', header: 'Measure', render: (l) => l.measure },
            { key: 'f', header: 'RBC %', numeric: true, render: (l) => l.rbcFactor ?? '' },
            { key: 's', header: 'Status', render: (l) => <StatusBadge status={l.recordStatus} /> },
            {
              key: 'x',
              header: 'Actions',
              render: (l) =>
                awaitsOtherChecker(l, user?.username) && can('MASTER_AUTHORIZE') ? (
                  <AuthorizeButton
                    busy={authorize.isPending && authorize.variables === l.id}
                    onClick={() => authorize.mutate(l.id)}
                  />
                ) : null,
            },
          ]}
        />
      </Card>
      <Modal
        title={form?.id === undefined ? 'New IC line' : `Edit ${form.lineCode ?? ''}`}
        open={form !== null}
        onClose={() => setForm(null)}
        footer={
          <Button variant="accent" busy={save.isPending} onClick={() => form && save.mutate(form)}>
            Save for authorization
          </Button>
        }
      >
        <ErrorAlert error={save.error} />
        {form !== null && (
          <div className="form-grid">
            <SelectField
              label="Schedule"
              required
              value={form.schedule}
              options={SCHEDULES}
              onChange={(v) => set({ schedule: v })}
            />
            <TextField
              label="Line code"
              required
              disabled={form.id !== undefined}
              value={form.lineCode}
              onChange={(v) => set({ lineCode: v.toUpperCase() })}
            />
            <TextField
              label="Description"
              required
              value={form.description}
              onChange={(v) => set({ description: v })}
            />
            <TextField
              label="Print order"
              type="number"
              value={form.lineOrder}
              onChange={(v) => set({ lineOrder: Number(v) })}
            />
            <TextField
              label="Account from"
              value={form.accountFrom}
              onChange={(v) => set({ accountFrom: v })}
            />
            <TextField
              label="Account to"
              value={form.accountTo}
              onChange={(v) => set({ accountTo: v })}
            />
            <TextField
              label="Report group"
              value={form.reportGroup}
              onChange={(v) => set({ reportGroup: v })}
            />
            <SelectField
              label="Natural side"
              required
              value={form.normalBalance}
              options={SIDES}
              onChange={(v) => set({ normalBalance: v })}
            />
            <SelectField
              label="Sign"
              required
              value={form.signFactor === -1 ? 'DEDUCT' : 'ADD'}
              options={['ADD', 'DEDUCT'] as const}
              onChange={(v) => set({ signFactor: v === 'DEDUCT' ? -1 : 1 })}
            />
            <SelectField
              label="Measure"
              value={form.measure}
              options={MEASURES}
              allowEmpty
              onChange={(v) => set({ measure: v })}
            />
            {form.schedule === 'RBC' && (
              <TextField
                label="RBC factor %"
                type="number"
                value={form.rbcFactor}
                onChange={(v) => set({ rbcFactor: v === '' ? undefined : Number(v) })}
              />
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}
