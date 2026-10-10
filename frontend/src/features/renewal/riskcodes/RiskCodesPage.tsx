import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { renewalApi } from '@/api/renewal';
import { renewalProposalApi } from '@/api/renewalProposal';
import type { RiskCodeEntry, RiskCodeView } from '@/api/renewalProposal';
import { useAuth } from '@/auth/authContext';
import { LineLabel } from '@/components/broking/LovLabel';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime, today } from '@/utils/format';
import { RENEWAL_SECTION } from '../common/renewalCodes';
import '../renewal.css';

const EMPTY: RiskCodeEntry = {
  riskCode: '',
  description: '',
  lineCode: null,
  renewable: false,
  effectiveDate: today(),
  remarks: null,
};

function EntryDialog({
  initial,
  id,
  onClose,
}: Readonly<{ initial: RiskCodeEntry; id: number | null; onClose: () => void }>) {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [entry, setEntry] = useState<RiskCodeEntry>(initial);
  const save = useMutation({
    mutationFn: () =>
      id === null
        ? renewalProposalApi.createRiskCode(companyId, entry)
        : renewalProposalApi.updateRiskCode(companyId, id, entry),
    onSuccess: async () => {
      toast.success(id === null ? 'Record successfully saved.' : 'Record successfully updated.');
      onClose();
      await queryClient.invalidateQueries({ queryKey: ['renewal', 'risk-codes'] });
    },
  });
  const set = (patch: Partial<RiskCodeEntry>) => setEntry((old) => ({ ...old, ...patch }));
  return (
    <Modal
      open
      title={id === null ? 'Add Risk Code' : 'Update Risk Code'}
      onClose={onClose}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={save.isPending} onClick={() => save.mutate()}>
            Save
          </Button>
        </>
      }
    >
      <ErrorAlert error={save.error} />
      <Field label="Risk Code" required>
        {(fid) => (
          <input
            id={fid}
            className="input"
            value={entry.riskCode}
            onChange={(e) => set({ riskCode: e.target.value })}
          />
        )}
      </Field>
      <Field label="Risk Code Description" required>
        {(fid) => (
          <input
            id={fid}
            className="input"
            value={entry.description}
            onChange={(e) => set({ description: e.target.value })}
          />
        )}
      </Field>
      <Field label="Product Line">
        {(fid) => (
          <input
            id={fid}
            className="input"
            value={entry.lineCode ?? ''}
            onChange={(e) => set({ lineCode: e.target.value || null })}
          />
        )}
      </Field>
      <Field label="Renewable Indicator" required>
        {(fid) => (
          <select
            id={fid}
            className="select"
            value={entry.renewable ? 'R' : 'N'}
            onChange={(e) => set({ renewable: e.target.value === 'R' })}
          >
            <option value="R">Renewable</option>
            <option value="N">Non-Renewable</option>
          </select>
        )}
      </Field>
      <Field label="Effective Date" required hint="Today or a later date">
        {(fid) => (
          <DateInput
            id={fid}
            value={entry.effectiveDate}
            min={today()}
            onChange={(e) => set({ effectiveDate: e.target.value })}
          />
        )}
      </Field>
      <Field label="Remarks">
        {(fid) => (
          <input
            id={fid}
            className="input"
            value={entry.remarks ?? ''}
            onChange={(e) => set({ remarks: e.target.value || null })}
          />
        )}
      </Field>
    </Modal>
  );
}

/**
 * Risk Code Maintenance (FRRN.038): every risk code with its description, product line and
 * Renewable or Non-Renewable indicator, searched by code and filtered by indicator; maintained by
 * the users of the Renewal setup and read only for the others.
 */
export default function RiskCodesPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [search, setSearch] = useState('');
  const [indicator, setIndicator] = useState('');
  const [editing, setEditing] = useState<{ entry: RiskCodeEntry; id: number | null } | null>(null);
  const renewable = indicator === '' ? undefined : indicator === 'R';
  const codes = useQuery({
    queryKey: ['renewal', 'risk-codes', companyId, search, indicator],
    queryFn: () => renewalProposalApi.riskCodes(companyId, search, renewable),
  });
  const maintain = can('RNW_SETUP');
  const qc = useQueryClient();
  const toast = useToast();
  const act = useMutation({
    mutationFn: (v: { id: number; action: 'AUTHORIZE' | 'DEACTIVATE' }) =>
      renewalApi.riskCodeAction(companyId, v.id, v.action),
    onSuccess: () => {
      toast.success('Record successfully updated.');
      void qc.invalidateQueries({ queryKey: ['renewal', 'risk-codes'] });
    },
  });
  const rowActions = (c: RiskCodeView) => [
    {
      label: 'Update',
      onSelect: () =>
        setEditing({
          id: c.id,
          entry: {
            riskCode: c.riskCode,
            description: c.description ?? '',
            lineCode: c.lineCode,
            renewable: c.renewable,
            effectiveDate: c.effectiveDate,
            remarks: c.remarks,
          },
        }),
    },
    ...(c.status === 'PENDING_AUTHORIZATION'
      ? [{ label: 'Authorize', onSelect: () => act.mutate({ id: c.id, action: 'AUTHORIZE' }) }]
      : []),
    ...(c.status === 'ACTIVE'
      ? [{ label: 'Deactivate', onSelect: () => act.mutate({ id: c.id, action: 'DEACTIVATE' }) }]
      : []),
  ];
  return (
    <div className="stack">
      <PageHeader
        section={RENEWAL_SECTION}
        title="Risk Code Maintenance"
        description="Risk codes and whether their accounts are renewed."
        actions={
          maintain && (
            <Button
              icon={<Plus size={16} />}
              onClick={() => setEditing({ entry: EMPTY, id: null })}
            >
              Add Risk Code
            </Button>
          )
        }
      />
      <div className="rnw-drill-tools">
        <input
          className="input"
          aria-label="Search risk code"
          placeholder="Risk code"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <select
          className="select"
          aria-label="Renewable Indicator"
          value={indicator}
          onChange={(e) => setIndicator(e.target.value)}
        >
          <option value="">All</option>
          <option value="R">Renewable</option>
          <option value="N">Non-Renewable</option>
        </select>
      </div>
      <Card flush>
        <ErrorAlert error={codes.error ?? act.error} />
        <DataTable<RiskCodeView>
          loading={codes.isLoading}
          rows={codes.data ?? []}
          rowKey={(c) => String(c.id)}
          emptyMessage="No risk code"
          columns={[
            { key: 'code', header: 'Risk Code', kind: 'code', render: (c) => c.riskCode },
            { key: 'desc', header: 'Risk Description', render: (c) => c.description ?? '' },
            {
              key: 'line',
              header: 'Product Line',
              render: (c) => (c.lineCode ? <LineLabel code={c.lineCode} /> : 'All'),
            },
            {
              key: 'ind',
              header: 'Renewable Indicator',
              render: (c) => (c.renewable ? 'Renewable' : 'Non-Renewable'),
            },
            {
              key: 'eff',
              header: 'Effective Date',
              kind: 'date',
              render: (c) => formatDate(c.effectiveDate),
            },
            {
              key: 'status',
              header: 'Status',
              kind: 'status',
              render: (c) => <StatusBadge status={c.status} />,
            },
            { key: 'cb', header: 'Created By', render: (c) => c.createdBy },
            { key: 'cd', header: 'Created Date', render: (c) => formatDateTime(c.createdAt) },
            { key: 'ub', header: 'Last Updated By', render: (c) => c.updatedBy ?? '' },
            { key: 'ud', header: 'Last Updated Date', render: (c) => formatDateTime(c.updatedAt) },
            {
              key: 'actions',
              header: 'Actions',
              render: (c) =>
                maintain && <RowActionMenu label={c.riskCode} actions={rowActions(c)} />,
            },
          ]}
        />
      </Card>
      {editing !== null && (
        <EntryDialog initial={editing.entry} id={editing.id} onClose={() => setEditing(null)} />
      )}
    </div>
  );
}
