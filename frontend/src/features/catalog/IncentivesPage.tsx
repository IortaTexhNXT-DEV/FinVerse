import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { productCatalogApi } from '@/api/productCatalog';
import type { IncentiveCriteria } from '@/api/productCatalog';
import { useAuth } from '@/auth/authContext';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { DataTable } from '@/components/ui/DataTable';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, today } from '@/utils/format';
import { IncentiveEditorModal } from './IncentiveEditorModal';
import { isCurrent } from './incentiveForm';
import { RecordActions } from './RecordActions';
import { LovLabel } from '@/components/broking/LovLabel';
import { ConfigUploadButton } from '@/features/configpromo/ConfigUploadButton';

const TABS = [
  { id: 'current', label: 'Current' },
  { id: 'pending', label: 'Pending Authorization' },
  { id: 'history', label: 'History' },
] as const;

type TabId = (typeof TABS)[number]['id'];

const AUTHORIZERS = ['PRODUCT_AUTHORIZE'] as const;
const NO_MAINTAINERS: readonly string[] = [];

function inTab(c: IncentiveCriteria, tab: TabId, day: string): boolean {
  if (tab === 'pending') {
    return c.recordStatus === 'PENDING_AUTHORIZATION';
  }
  const current = c.recordStatus === 'ACTIVE' && isCurrent(c, day);
  return tab === 'current' ? current : !current && c.recordStatus !== 'PENDING_AUTHORIZATION';
}

function valueOf(c: IncentiveCriteria): string {
  if (c.valueBasis === 'RULE') {
    return 'Rule';
  }
  return c.valueBasis === 'RATE' ? `${c.value ?? 0}%` : String(c.value ?? '');
}

/**
 * Incentive Criteria (PMADD07/08): criteria such as CPC2 on the maintained products matrix, with
 * their value, products, effective dates and status. An active criterion is changed by an
 * amendment (successor row) and deactivated, never deleted; booking stamps the codes it matches.
 */
export default function IncentivesPage() {
  const { can } = useAuth();
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<TabId>('current');
  const [text, setText] = useState('');
  const [editing, setEditing] = useState<IncentiveCriteria | 'new' | null>(null);
  const maintain = can('INCENTIVE_CRITERIA_MAINTAIN');
  const criteria = useQuery({
    queryKey: ['catalog', 'incentives', companyId],
    queryFn: () => productCatalogApi.incentives(companyId),
  });
  const [deactivating, setDeactivating] = useState<IncentiveCriteria | null>(null);
  const deactivate = useMutation({
    mutationFn: (reason: string) =>
      productCatalogApi.deactivateIncentive(deactivating?.id ?? 0, { reason }),
    onSuccess: async (c) => {
      setDeactivating(null);
      await queryClient.invalidateQueries({ queryKey: ['catalog', 'incentives'] });
      toast.success(`Criterion ${c.code} deactivated`);
    },
  });
  const day = today();
  const needle = text.trim().toUpperCase();
  const rows = (criteria.data ?? []).filter(
    (c) =>
      inTab(c, tab, day) &&
      (needle === '' ||
        c.code.includes(needle) ||
        c.products.some((p) => p.productCode.includes(needle))),
  );
  return (
    <div className="stack">
      <PageHeader
        section="Product Maintenance"
        title="Incentive Criteria"
        description="Incentive criteria on the products matrix, with their value, products and effective dates."
        actions={
          <>
            <ConfigUploadButton types={['CFG_INCENTIVE_CRITERIA']} />
            {maintain && (
              <Button variant="accent" icon={<Plus size={16} />} onClick={() => setEditing('new')}>
                New Criterion
              </Button>
            )}
          </>
        }
      />
      <Card flush>
        <Tabs tabs={TABS} active={tab} onChange={setTab} />
        <WorklistToolbar onSearch={setText} placeholder="Search Criterion or Product" />
        <ErrorAlert error={criteria.error} />
        {!criteria.isLoading && rows.length === 0 ? (
          <EmptyState message="No incentive criterion in this list." />
        ) : (
          <DataTable<IncentiveCriteria>
            loading={criteria.isLoading}
            rows={rows}
            rowKey={(c) => c.id}
            onRowClick={(c) => {
              if (maintain && c.recordStatus !== 'INACTIVE') {
                setEditing(c);
              }
            }}
            columns={[
              { key: 'c', header: 'Code', render: (c) => <strong>{c.code}</strong> },
              { key: 'n', header: 'Name', render: (c) => c.name },
              {
                key: 't',
                header: 'Type',
                render: (c) => <LovLabel type="INCENTIVE_TYPE" code={c.incentiveType} />,
              },
              { key: 'v', header: 'Value', render: valueOf },
              {
                key: 'p',
                header: 'Products',
                render: (c) => c.products.map((p) => p.productCode).join(', '),
              },
              { key: 'f', header: 'From', render: (c) => formatDate(c.effectiveFrom) },
              { key: 'u', header: 'Until', render: (c) => formatDate(c.effectiveTo) },
              {
                key: 's',
                header: 'Status',
                render: (c) => <StatusBadge status={c.recordStatus} />,
              },
              {
                key: 'a',
                header: <span className="visually-hidden">Actions</span>,
                width: '64px',
                render: (c) => (
                  <RecordActions
                    kind="INCENTIVE_CRITERIA"
                    record={c}
                    label={`${c.code} ${c.name}`}
                    refresh={[['catalog', 'incentives']]}
                    authorizers={AUTHORIZERS}
                    maintainers={NO_MAINTAINERS}
                    extra={
                      maintain && c.recordStatus === 'ACTIVE'
                        ? [
                            {
                              label: 'Deactivate',
                              danger: true,
                              onSelect: () => setDeactivating(c),
                            },
                          ]
                        : []
                    }
                  />
                ),
              },
            ]}
          />
        )}
      </Card>
      {deactivating !== null && (
        <ConfirmDialog
          title="Deactivate Incentive Criterion"
          record={`${deactivating.code} ${deactivating.name}`}
          effect="The criterion ends today and is no longer matched by booking; it stays in the history. The reason is kept in the audit trail."
          confirmLabel="Deactivate"
          destructive
          reason="required"
          busy={deactivate.isPending}
          error={deactivate.error}
          onConfirm={deactivate.mutate}
          onClose={() => setDeactivating(null)}
        />
      )}
      {editing && (
        <IncentiveEditorModal
          initial={editing === 'new' ? undefined : editing}
          onClose={() => setEditing(null)}
        />
      )}
    </div>
  );
}
