import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { submittedApi } from '@/api/submitted';
import type { Controlled } from '@/api/submitted';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { RowForm } from './RowForm';
import type { Kind } from './setupForms';
import { FORMS } from './setupForms';

function controlColumns<T>(kind: Kind, onDone: () => void): Column<Controlled<T>>[] {
  return [
    {
      key: 'status',
      header: 'Status',
      kind: 'status',
      render: (r) => <StatusBadge status={r.control.recordStatus} />,
    },
    { key: 'maker', header: 'Maker', render: (r) => <UserName login={r.control.maker} /> },
    {
      key: 'actions',
      header: 'Actions',
      render: (r) => <ControlActions kind={kind} row={r} onDone={onDone} />,
    },
  ];
}

function ControlActions<T>({
  kind,
  row,
  onDone,
}: Readonly<{ kind: Kind; row: Controlled<T>; onDone: () => void }>) {
  const { can, user } = useAuth();
  const toast = useToast();
  const decide = useMutation({
    mutationFn: (d: 'authorize' | 'deactivate') => submittedApi.setupDecision(kind, row.id, d),
    onSuccess: () => {
      toast.success('Saved');
      onDone();
    },
  });
  const pending = row.control.recordStatus.startsWith('PENDING');
  return (
    <span className="form-actions">
      {can('SBM_RULE_APPROVE') && pending && user?.username !== row.control.maker && (
        <Button
          variant="ghost"
          disabled={decide.isPending}
          onClick={() => decide.mutate('authorize')}
        >
          Approve
        </Button>
      )}
      {can('SBM_RULE_MAINTAIN') && row.control.recordStatus === 'ACTIVE' && (
        <Button
          variant="ghost"
          disabled={decide.isPending}
          onClick={() => decide.mutate('deactivate')}
        >
          Deactivate
        </Button>
      )}
    </span>
  );
}

/** A setup list kept with maker and checker, with its new-record form. */
export function ControlledTable<T>({
  kind,
  title,
  load,
  columns,
}: Readonly<{
  kind: Kind;
  title: string;
  load: (companyId: number) => Promise<Controlled<T>[]>;
  columns: Column<Controlled<T>>[];
}>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const rows = useQuery({
    queryKey: ['submitted', 'setup', kind, companyId],
    queryFn: () => load(companyId),
    enabled: companyId > 0,
  });
  const refresh = () =>
    void queryClient.invalidateQueries({ queryKey: ['submitted', 'setup', kind] });
  return (
    <div className="stack">
      <ErrorAlert error={rows.error} />
      <Card flush>
        <DataTable<Controlled<T>>
          loading={rows.isLoading}
          rows={rows.data ?? []}
          rowKey={(r) => r.id}
          emptyMessage="Nothing defined"
          columns={[...columns, ...controlColumns<T>(kind, refresh)]}
        />
      </Card>
      {can('SBM_RULE_MAINTAIN') && (
        <RowForm
          title={title}
          fields={FORMS[kind]}
          save={(body) => submittedApi.saveSetup(kind, companyId, body)}
          onSaved={refresh}
        />
      )}
    </div>
  );
}
