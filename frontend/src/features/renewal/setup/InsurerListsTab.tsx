import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { catalogApi } from '@/api/catalog';
import { insurerListsApi } from '@/api/renewalInsurerLists';
import type { RenewableRiskView } from '@/api/renewalInsurerLists';
import { useAuth } from '@/auth/authContext';
import { InsurerName } from '@/components/broking/LovLabel';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { RowActions } from '@/components/ui/RowActions';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { setupRecordActions } from '../common/presentation';
import { ApprovalCell } from './setupBits';

const KEY = ['renewal', 'setup', 'insurer-lists'] as const;

function initialRisk(row: RenewableRiskView | null) {
  return {
    insurer: row?.insurerCode ?? '',
    risk: row?.riskCode ?? '',
    remarks: row?.remarks ?? '',
    from: row?.effectiveFrom ?? '',
    to: row?.effectiveTo ?? '',
  };
}

function RenewableRiskDialog({
  row,
  onClose,
}: Readonly<{ row: RenewableRiskView | null; onClose: () => void }>) {
  const companyId = useCompanyId();
  const queryClient = useQueryClient();
  const insurers = useQuery({
    queryKey: ['catalog', 'insurers', companyId],
    queryFn: () => catalogApi.insurers(companyId),
    enabled: companyId > 0,
  });
  const [init] = useState(() => initialRisk(row));
  const [insurer, setInsurer] = useState(init.insurer);
  const [risk, setRisk] = useState(init.risk);
  const [remarks, setRemarks] = useState(init.remarks);
  const [from, setFrom] = useState(init.from);
  const [to, setTo] = useState(init.to);
  const save = useMutation({
    mutationFn: () =>
      insurerListsApi.save(companyId, row?.id ?? null, {
        insurerCode: insurer,
        riskCode: risk.trim().toUpperCase(),
        remarks: remarks.trim() || undefined,
        effectiveFrom: from,
        effectiveTo: to || undefined,
      }),
    onSuccess: async () => {
      onClose();
      await queryClient.invalidateQueries({ queryKey: KEY });
    },
  });
  return (
    <Modal
      open
      title={row === null ? 'New renewable risk code of an insurer' : `Edit ${row.riskCode}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={save.isPending}
            disabled={insurer === '' || risk.trim() === '' || from === ''}
            onClick={() => save.mutate()}
          >
            Save for Authorization
          </Button>
        </>
      }
    >
      <ErrorAlert error={save.error} />
      <div className="form-grid">
        <Field label="Insurer" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={insurer}
              disabled={row !== null}
              onChange={(e) => setInsurer(e.target.value)}
            >
              <option value="">—</option>
              {(insurers.data ?? []).map((i) => (
                <option key={i.partyCode} value={i.partyCode}>
                  {i.name}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Risk Code" required>
          {(id) => (
            <input
              id={id}
              className="input"
              value={risk}
              disabled={row !== null}
              onChange={(e) => setRisk(e.target.value)}
            />
          )}
        </Field>
        <Field label="Effective From" required>
          {(id) => <DateInput id={id} value={from} onChange={(e) => setFrom(e.target.value)} />}
        </Field>
        <Field label="Effective To">
          {(id) => <DateInput id={id} value={to} onChange={(e) => setTo(e.target.value)} />}
        </Field>
      </div>
      <Field label="Remarks">
        {(id) => (
          <input
            id={id}
            className="input"
            maxLength={200}
            value={remarks}
            onChange={(e) => setRemarks(e.target.value)}
          />
        )}
      </Field>
    </Modal>
  );
}

/**
 * Insurer renewable lists (Annex BRRN.020 SC-10; FR-RN-020): the risk codes each insurer renews.
 * A renewal on a risk outside the list of its insurer is held for review; an insurer without a
 * list is not checked.
 */
export function InsurerListsTab() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<RenewableRiskView | null>();
  const rows = useQuery({
    queryKey: [...KEY, companyId],
    queryFn: () => insurerListsApi.list(companyId),
    enabled: companyId > 0,
  });
  const act = useMutation({
    mutationFn: ({ id, action }: { id: number; action: 'AUTHORIZE' | 'DEACTIVATE' }) =>
      insurerListsApi.action(companyId, id, action),
    onSuccess: async (r) => {
      toast.success(
        `${r.riskCode} ${r.approval.recordStatus === 'ACTIVE' ? 'authorized' : 'deactivated'}`,
      );
      await queryClient.invalidateQueries({ queryKey: KEY });
    },
  });
  const maintain = can('RNW_SETUP');
  return (
    <Card
      title="Insurer renewable lists"
      flush
      actions={
        maintain && (
          <Button icon={<Plus size={16} />} onClick={() => setEditing(null)}>
            New Renewable Risk Code
          </Button>
        )
      }
    >
      <ErrorAlert error={rows.error ?? act.error} />
      <DataTable<RenewableRiskView>
        loading={rows.isLoading}
        rows={rows.data ?? []}
        rowKey={(r) => r.id}
        emptyMessage="No insurer has given a renewable list"
        columns={[
          { key: 'ins', header: 'Insurer', render: (r) => <InsurerName code={r.insurerCode} /> },
          { key: 'code', header: 'Risk Code', kind: 'code', render: (r) => r.riskCode },
          { key: 'rem', header: 'Remarks', render: (r) => r.remarks ?? '' },
          { key: 'from', header: 'From', kind: 'date', render: (r) => formatDate(r.effectiveFrom) },
          { key: 'to', header: 'To', kind: 'date', render: (r) => formatDate(r.effectiveTo) },
          {
            key: 'status',
            header: 'Status',
            render: (r) => <ApprovalCell approval={r.approval} />,
          },
          {
            key: 'act',
            header: '',
            render: (r) => (
              <RowActions
                record={r.riskCode}
                actions={setupRecordActions(r.approval.recordStatus, maintain, 'Risk Code', {
                  authorize: () => act.mutateAsync({ id: r.id, action: 'AUTHORIZE' }),
                  edit: () => setEditing(r),
                  deactivate: () => act.mutateAsync({ id: r.id, action: 'DEACTIVATE' }),
                })}
              />
            ),
          },
        ]}
      />
      {editing !== undefined && (
        <RenewableRiskDialog row={editing} onClose={() => setEditing(undefined)} />
      )}
    </Card>
  );
}
