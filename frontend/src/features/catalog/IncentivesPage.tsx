import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { productCatalogApi } from '@/api/productCatalog';
import type { IncentiveCriteria } from '@/api/productCatalog';
import { useAuth } from '@/auth/authContext';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, humanize, today } from '@/utils/format';
import { IncentiveEditorModal } from './IncentiveEditorModal';
import { isCurrent } from './incentiveForm';
import { RecordActions } from './RecordActions';

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
  const deactivate = useMutation({
    mutationFn: (id: number) => productCatalogApi.deactivateIncentive(id),
    onSuccess: async (c) => {
      await queryClient.invalidateQueries({ queryKey: ['catalog', 'incentives'] });
      toast.success(`Criterion ${c.code} deactivated`);
    },
    onError: (e) => toast.error(e.message),
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
          maintain && (
            <Button variant="accent" icon={<Plus size={16} />} onClick={() => setEditing('new')}>
              New Criterion
            </Button>
          )
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
              { key: 't', header: 'Type', render: (c) => humanize(c.incentiveType) },
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
                header: 'Actions',
                render: (c) => (
                  <div className="row">
                    <RecordActions
                      kind="INCENTIVE_CRITERIA"
                      record={c}
                      refresh={[['catalog', 'incentives']]}
                      authorizers={AUTHORIZERS}
                      maintainers={NO_MAINTAINERS}
                    />
                    {maintain && c.recordStatus === 'ACTIVE' && (
                      <Button
                        size="sm"
                        variant="ghost"
                        busy={deactivate.isPending}
                        onClick={(e) => {
                          e.stopPropagation();
                          deactivate.mutate(c.id);
                        }}
                      >
                        Deactivate
                      </Button>
                    )}
                  </div>
                ),
              },
            ]}
          />
        )}
      </Card>
      {editing && (
        <IncentiveEditorModal
          initial={editing === 'new' ? undefined : editing}
          onClose={() => setEditing(null)}
        />
      )}
    </div>
  );
}
