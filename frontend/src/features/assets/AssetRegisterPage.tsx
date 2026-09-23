import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { assetsApi } from '@/api/assets';
import type { AssetStatus, FixedAsset } from '@/api/assets';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Kpi } from '@/components/ui/Kpi';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useDefaultBranchId } from '@/context/workspaceContext';
import { formatAmount, formatDate, today } from '@/utils/format';
import { AssetActionModal } from './AssetActionModal';
import type { AssetAction } from './AssetActionModal';
import { AssetFormModal } from './AssetFormModal';
import type { AssetForm } from './AssetFormModal';
import { SelectInput, TextInput } from './FormControls';
import { enumOptions } from './options';
import { useAssetLookups } from './useAssetLookups';

const STATUSES = enumOptions([
  'PENDING_CAPITALIZATION',
  'ACTIVE',
  'TRANSFERRED',
  'FULLY_DEPRECIATED',
  'DISPOSED',
]);
const IN_SERVICE: readonly AssetStatus[] = ['ACTIVE', 'TRANSFERRED', 'FULLY_DEPRECIATED'];

function sum(rows: FixedAsset[], pick: (a: FixedAsset) => number): number {
  return rows.reduce((acc, a) => acc + pick(a), 0);
}

/** Fixed asset register: registration, capitalization (checker), disposal and transfer. */
export default function AssetRegisterPage() {
  const { companyId, categories, branchName } = useAssetLookups();
  const defaultBranch = useDefaultBranchId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<AssetStatus | ''>('');
  const [categoryId, setCategoryId] = useState('');
  const [q, setQ] = useState('');
  const [form, setForm] = useState<AssetForm | null>(null);
  const [action, setAction] = useState<{ asset: FixedAsset; action: AssetAction } | null>(null);

  const assets = useQuery({
    queryKey: ['fixed-assets', companyId, status, categoryId, q],
    queryFn: () =>
      assetsApi.search(companyId, {
        status: status === '' ? undefined : status,
        categoryId: categoryId === '' ? undefined : Number(categoryId),
        q,
      }),
    enabled: companyId > 0,
  });
  const capitalize = useMutation({
    mutationFn: (id: number) => assetsApi.capitalize(id),
    onSuccess: async (a) => {
      await queryClient.invalidateQueries({ queryKey: ['fixed-assets'] });
      toast.success(`${a.tagNo} capitalized – journal ${a.capitalizationBatchNo ?? ''}`);
    },
  });
  const rows = assets.data ?? [];
  const inService = rows.filter((a) => IN_SERVICE.includes(a.status));

  const actions = (a: FixedAsset) => (
    <div className="row">
      {a.status === 'PENDING_CAPITALIZATION' && can('MASTER_AUTHORIZE') && (
        <Button size="sm" variant="secondary" onClick={() => capitalize.mutate(a.id)}>
          Capitalize
        </Button>
      )}
      {IN_SERVICE.includes(a.status) && can('ASSET_MANAGE') && (
        <>
          <Button
            size="sm"
            variant="ghost"
            onClick={() => setAction({ asset: a, action: 'TRANSFER' })}
          >
            Transfer
          </Button>
          <Button
            size="sm"
            variant="ghost"
            onClick={() => setAction({ asset: a, action: 'DISPOSE' })}
          >
            Dispose
          </Button>
        </>
      )}
    </div>
  );

  return (
    <div className="stack">
      <PageHeader
        section="Assets & Investments"
        title="Fixed Asset Register"
        description="Property and equipment with cost, accumulated depreciation and net book value. New assets are capitalized by a checker."
        actions={
          can('MASTER_MAINTAIN') && (
            <Button
              variant="accent"
              icon={<Plus size={16} />}
              onClick={() =>
                setForm({
                  branchId: defaultBranch || undefined,
                  acquisitionDate: today(),
                  takeOn: false,
                })
              }
            >
              Register asset
            </Button>
          )
        }
      />
      <div className="form-grid">
        <Kpi label="Assets in service" value={inService.length} />
        <Kpi label="Cost" value={formatAmount(sum(inService, (a) => a.acquisitionCost))} />
        <Kpi
          label="Accumulated depreciation"
          value={formatAmount(sum(inService, (a) => a.accumulatedDepreciation))}
        />
        <Kpi
          label="Net book value"
          accent
          value={formatAmount(sum(inService, (a) => a.netBookValue))}
        />
      </div>
      <Card>
        <div className="form-grid">
          <SelectInput
            label="Status"
            blank="All"
            value={status}
            options={STATUSES}
            onChange={(v) => setStatus(v as AssetStatus | '')}
          />
          <SelectInput
            label="Category"
            blank="All"
            value={categoryId}
            options={categories.map((c) => ({ value: String(c.id), label: c.name }))}
            onChange={setCategoryId}
          />
          <TextInput label="Tag / description" value={q} onChange={setQ} />
        </div>
      </Card>
      <ErrorAlert error={assets.error ?? capitalize.error} />
      <Card flush>
        <DataTable<FixedAsset>
          loading={assets.isLoading}
          rows={rows}
          rowKey={(a) => a.id}
          onRowClick={
            can('MASTER_MAINTAIN')
              ? (a) => a.status === 'PENDING_CAPITALIZATION' && setForm(a)
              : undefined
          }
          columns={[
            { key: 't', header: 'Tag', render: (a) => <strong>{a.tagNo}</strong> },
            { key: 'd', header: 'Description', render: (a) => a.description },
            { key: 'c', header: 'Category', render: (a) => a.categoryCode },
            { key: 'b', header: 'Branch', render: (a) => branchName(a.branchId) },
            { key: 'ad', header: 'Acquired', render: (a) => formatDate(a.acquisitionDate) },
            {
              key: 'co',
              header: 'Cost',
              numeric: true,
              render: (a) => <Amount value={a.acquisitionCost} />,
            },
            {
              key: 'ac',
              header: 'Accum. depr.',
              numeric: true,
              render: (a) => <Amount value={a.accumulatedDepreciation} />,
            },
            {
              key: 'n',
              header: 'NBV',
              numeric: true,
              render: (a) => <Amount value={a.netBookValue} />,
            },
            { key: 'p', header: 'Depreciated to', render: (a) => a.lastDepreciationPeriod ?? '' },
            { key: 's', header: 'Status', render: (a) => <StatusBadge status={a.status} /> },
            { key: 'x', header: 'Actions', render: actions },
          ]}
        />
      </Card>
      {form !== null && <AssetFormModal initial={form} onClose={() => setForm(null)} />}
      {action !== null && (
        <AssetActionModal
          asset={action.asset}
          action={action.action}
          onClose={() => setAction(null)}
        />
      )}
    </div>
  );
}
