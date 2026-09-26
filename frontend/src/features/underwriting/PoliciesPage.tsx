import { useQuery } from '@tanstack/react-query';
import { Pager as SharedPager } from '@/components/ui/Pager';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { underwritingApi } from '@/api/underwriting';
import type { Policy, PolicyFilters } from '@/api/underwriting';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useWorkspace } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { DateField, SelectField, TextField } from './FormFields';
import { useUwLookups } from './useUwLookups';

type Filters = Omit<PolicyFilters, 'companyId'>;

const STATUSES = ['DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'CANCELLED'].map((s) => ({
  value: s,
  label: s.replace('_', ' '),
}));

function Pager({
  page,
  totalPages,
  total,
  onPage,
}: Readonly<{ page: number; totalPages: number; total: number; onPage: (p: number) => void }>) {
  return <SharedPager page={page} totalPages={totalPages} total={total} onPage={onPage} />;
}

/** Policy register: search by status, product, client, number or insured; open to act on one. */
export default function PoliciesPage() {
  const { branchId } = useWorkspace();
  const lookups = useUwLookups();
  const { can } = useAuth();
  const navigate = useNavigate();
  const [filters, setFilters] = useState<Filters>({ page: 0 });
  const companyId = lookups.companyId;

  const query = useQuery({
    queryKey: ['policies', companyId, branchId, filters],
    queryFn: () => underwritingApi.policies({ ...filters, companyId, branchId, size: 25 }),
    enabled: companyId > 0,
  });
  const set = (patch: Partial<Filters>) => setFilters((f) => ({ ...f, ...patch, page: 0 }));
  const data = query.data;

  return (
    <div className="stack">
      <PageHeader
        section="Underwriting"
        title="Policies"
        description="Policies and marine certificates with their approval status, premium and debit note."
        actions={
          can('POLICY_MAINTAIN') && (
            <Button
              variant="accent"
              icon={<Plus size={16} />}
              onClick={() => void navigate('/underwriting/policies/new')}
            >
              New Policy
            </Button>
          )
        }
      />
      <Card>
        <div className="form-grid">
          <SelectField
            label="Status"
            value={filters.status}
            emptyLabel="All"
            options={STATUSES}
            onChange={(v) => set({ status: v === '' ? undefined : v })}
          />
          <SelectField
            label="Product"
            value={filters.productId === undefined ? '' : String(filters.productId)}
            emptyLabel="All"
            options={lookups.products.map((p) => ({ value: String(p.id), label: p.code }))}
            onChange={(v) => set({ productId: v === '' ? undefined : Number(v) })}
          />
          <SelectField
            label="Client"
            value={filters.customerCode}
            emptyLabel="All"
            options={lookups.clients.map((c) => ({ value: c.code, label: c.name }))}
            onChange={(v) => set({ customerCode: v === '' ? undefined : v })}
          />
          <TextField
            label="Policy no. / insured"
            value={filters.q}
            onChange={(v) => set({ q: v })}
          />
          <DateField
            label="Issued from"
            value={filters.fromDate}
            onChange={(v) => set({ fromDate: v })}
          />
          <DateField
            label="Issued to"
            value={filters.toDate}
            onChange={(v) => set({ toDate: v })}
          />
        </div>
      </Card>
      <ErrorAlert error={query.error} />
      <Card flush>
        <DataTable<Policy>
          loading={query.isLoading}
          rows={data?.content ?? []}
          rowKey={(p) => p.id}
          onRowClick={(p) => void navigate(`/underwriting/policies/${String(p.id)}`)}
          caption="Policies"
          columns={[
            { key: 'no', header: 'Policy No.', render: (p) => <strong>{p.policyNo}</strong> },
            { key: 'prod', header: 'Product', render: (p) => p.productCode },
            { key: 'ins', header: 'Insured', render: (p) => p.insuredName },
            { key: 'src', header: 'Source', render: (p) => p.intermediaryName ?? 'Direct' },
            {
              key: 'per',
              header: 'Period',
              render: (p) => `${formatDate(p.periodFrom)} – ${formatDate(p.periodTo)}`,
            },
            { key: 'ccy', header: 'Ccy', render: (p) => p.currency },
            {
              key: 'net',
              header: 'Our Net Premium',
              numeric: true,
              render: (p) => <Amount value={p.premium.ourNetPremium} />,
            },
            {
              key: 'due',
              header: 'Total Due',
              numeric: true,
              render: (p) => <Amount value={p.premium.totalDue} />,
            },
            {
              key: 'st',
              header: 'Status',
              render: (p) => <StatusBadge status={p.document.status} />,
            },
          ]}
        />
        {data !== undefined && (
          <Pager
            page={data.page}
            totalPages={data.totalPages}
            total={data.totalElements}
            onPage={(page) => setFilters((f) => ({ ...f, page }))}
          />
        )}
      </Card>
    </div>
  );
}
