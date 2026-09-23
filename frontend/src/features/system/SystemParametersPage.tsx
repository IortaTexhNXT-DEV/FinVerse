import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { systemApi } from '@/api/system';
import type { ConfigEntry, SystemParameter } from '@/api/system';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime, humanize } from '@/utils/format';

type Tab = 'parameters' | 'configuration';

const TABS = [
  { id: 'parameters', label: 'Business parameters' },
  { id: 'configuration', label: 'Configuration (read only)' },
] as const;

function hintFor(p: SystemParameter): string {
  const range = p.minValue !== undefined ? ` between ${p.minValue} and ${p.maxValue ?? '…'}` : '';
  switch (p.valueType) {
    case 'INTEGER':
      return `Whole number${range}`;
    case 'INTEGER_LIST':
      return `Comma separated ascending numbers${range}`;
    case 'CODE_LIST':
      return 'Comma separated codes (blank allowed)';
    case 'BOOLEAN':
      return 'true or false';
    default:
      return humanize(p.valueType);
  }
}

function ConfigurationTable() {
  const config = useQuery({
    queryKey: ['system', 'configuration'],
    queryFn: systemApi.configuration,
  });
  return (
    <Card flush>
      <ErrorAlert error={config.error} />
      <DataTable<ConfigEntry>
        loading={config.isLoading}
        rows={config.data ?? []}
        rowKey={(c) => c.key}
        columns={[
          { key: 'k', header: 'Setting', render: (c) => <code>{c.key}</code> },
          { key: 'v', header: 'Value', render: (c) => c.value },
        ]}
      />
      <p className="muted" style={{ padding: 'var(--space-3) var(--space-4)' }}>
        Environment settings are changed through deployment configuration. Secrets are never shown.
      </p>
    </Card>
  );
}

/**
 * System parameters: editable business parameters (validated by type, audited) and a read-only
 * view of the non-secret runtime configuration.
 */
export default function SystemParametersPage() {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<Tab>('parameters');
  const [editing, setEditing] = useState<SystemParameter | null>(null);
  const parameters = useQuery({
    queryKey: ['system', 'parameters'],
    queryFn: systemApi.parameters,
  });
  const canEdit = can('SYSTEM_PARAMETER_MANAGE');

  const save = useMutation({
    mutationFn: (p: SystemParameter) => systemApi.updateParameter(p.key, p.value),
    onSuccess: async (p) => {
      setEditing(null);
      await queryClient.invalidateQueries({ queryKey: ['system'] });
      await queryClient.invalidateQueries({ queryKey: ['session-policy'] });
      toast.success(`${p.key} updated`);
    },
  });

  return (
    <div className="stack">
      <PageHeader
        section="Administration"
        title="System Parameters"
        description="Business parameters used across modules. Changes take effect immediately and are recorded in the audit trail."
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'configuration' && <ConfigurationTable />}
      {tab === 'parameters' && (
        <Card flush>
          <ErrorAlert error={parameters.error} />
          <DataTable<SystemParameter>
            loading={parameters.isLoading}
            rows={parameters.data ?? []}
            rowKey={(p) => p.key}
            onRowClick={canEdit ? (p) => setEditing(p) : undefined}
            columns={[
              { key: 'c', header: 'Category', render: (p) => humanize(p.category) },
              { key: 'k', header: 'Parameter', render: (p) => <strong>{p.key}</strong> },
              { key: 'v', header: 'Value', render: (p) => p.value || '—' },
              { key: 'd', header: 'Description', render: (p) => p.description },
              {
                key: 'u',
                header: 'Last changed',
                render: (p) =>
                  p.updatedBy === undefined ? '' : `${p.updatedBy}, ${formatDateTime(p.updatedAt)}`,
              },
            ]}
          />
        </Card>
      )}
      <Modal
        title={editing?.key ?? ''}
        open={editing !== null}
        onClose={() => setEditing(null)}
        footer={
          <Button
            variant="accent"
            busy={save.isPending}
            onClick={() => editing && save.mutate(editing)}
          >
            Save
          </Button>
        }
      >
        <ErrorAlert error={save.error} />
        {editing !== null && (
          <div className="stack">
            <p className="muted">{editing.description}</p>
            <Field label="Value" hint={hintFor(editing)}>
              {(id) => (
                <input
                  id={id}
                  className="input"
                  maxLength={1000}
                  value={editing.value}
                  onChange={(e) => setEditing({ ...editing, value: e.target.value })}
                />
              )}
            </Field>
          </div>
        )}
      </Modal>
    </div>
  );
}
