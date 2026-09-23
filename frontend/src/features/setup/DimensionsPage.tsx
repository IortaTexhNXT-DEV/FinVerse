import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { mastersApi } from '@/api/masters';
import type { DimensionType, DimensionValue } from '@/api/masters';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';

const TABS = [
  { id: 'COST_CENTER', label: 'Cost centres' },
  { id: 'BUSINESS_LINE', label: 'Lines of business' },
  { id: 'DEPARTMENT', label: 'Departments' },
  { id: 'PROFIT_CENTER', label: 'Profit centres' },
] as const;

/** Financial dimensions used to analyse postings. */
export default function DimensionsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const [type, setType] = useState<DimensionType>('COST_CENTER');
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const values = useQuery({
    queryKey: ['dimensions', companyId, type],
    queryFn: () => mastersApi.dimensions(companyId, type),
    enabled: companyId > 0,
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['dimensions'] });
  const create = useMutation({
    mutationFn: () => mastersApi.createDimension({ companyId, type, code, name }),
    onSuccess: async () => {
      setCode('');
      setName('');
      await refresh();
    },
  });
  const toggle = useMutation({
    mutationFn: (v: DimensionValue) => mastersApi.setDimensionActive(v.id, !v.active),
    onSuccess: refresh,
  });

  return (
    <div className="stack">
      <PageHeader
        section="Setup"
        title="Financial Dimensions"
        description="Cost centres, lines of business, departments and profit centres."
      />
      <Tabs tabs={TABS} active={type} onChange={setType} />
      {can('MASTER_MAINTAIN') && (
        <Card>
          <ErrorAlert error={create.error} />
          <div className="form-grid">
            <Field label="Code" required>
              {(id) => (
                <input
                  id={id}
                  className="input"
                  value={code}
                  onChange={(e) => setCode(e.target.value.toUpperCase())}
                />
              )}
            </Field>
            <Field label="Name" required>
              {(id) => (
                <input
                  id={id}
                  className="input"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                />
              )}
            </Field>
            <Button
              variant="accent"
              style={{ alignSelf: 'end' }}
              disabled={code === '' || name === ''}
              busy={create.isPending}
              onClick={() => create.mutate()}
            >
              Add
            </Button>
          </div>
        </Card>
      )}
      <Card flush>
        <DataTable<DimensionValue>
          loading={values.isLoading}
          rows={values.data ?? []}
          rowKey={(v) => v.id}
          columns={[
            { key: 'c', header: 'Code', render: (v) => <strong>{v.code}</strong> },
            { key: 'n', header: 'Name', render: (v) => v.name },
            {
              key: 's',
              header: 'Status',
              render: (v) => <StatusBadge status={v.active ? 'ACTIVE' : 'INACTIVE'} />,
            },
            {
              key: 'a',
              header: 'Actions',
              render: (v) =>
                can('MASTER_MAINTAIN') && (
                  <Button size="sm" variant="ghost" onClick={() => toggle.mutate(v)}>
                    {v.active ? 'Deactivate' : 'Activate'}
                  </Button>
                ),
            },
          ]}
        />
      </Card>
    </div>
  );
}
