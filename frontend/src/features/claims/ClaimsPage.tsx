import { useQuery } from '@tanstack/react-query';
import { Pager as SharedPager } from '@/components/ui/Pager';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { claimsApi } from '@/api/claims';
import type { Claim, ClaimFilters } from '@/api/claims';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useWorkspace } from '@/context/workspaceContext';
import { DateField, SelectField, TextField } from '@/features/underwriting/FormFields';
import { formatDate } from '@/utils/format';
import { useClaimLookups } from './useClaimLookups';

type Filters = Omit<ClaimFilters, 'companyId'>;

const STATUSES = [
  'REGISTERED',
  'OPEN',
  'PARTIALLY_SETTLED',
  'CLOSED',
  'REOPENED',
  'REJECTED',
  'WITHDRAWN',
].map((s) => ({ value: s, label: s.replace('_', ' ') }));

function Pager({
  page,
  totalPages,
  total,
  onPage,
}: Readonly<{ page: number; totalPages: number; total: number; onPage: (p: number) => void }>) {
  return <SharedPager page={page} totalPages={totalPages} total={total} onPage={onPage} />;
}

/** Claim register: search by status, class, number / insured and loss date; open to handle one. */
export default function ClaimsPage() {
  const { branchId } = useWorkspace();
  const lookups = useClaimLookups();
  const { can } = useAuth();
  const navigate = useNavigate();
  const [filters, setFilters] = useState<Filters>({ page: 0 });
  const companyId = lookups.companyId;
  const query = useQuery({
    queryKey: ['claims', companyId, branchId, filters],
    queryFn: () => claimsApi.claims({ ...filters, companyId, branchId, size: 25 }),
    enabled: companyId > 0,
  });
  const set = (patch: Partial<Filters>) => setFilters((f) => ({ ...f, ...patch, page: 0 }));
  const data = query.data;

  return (
    <div className="stack">
      <PageHeader
        section="Claims"
        title="Claims"
        description="Notified claims with their status, reserve, paid and outstanding (company share)."
        actions={
          can('CLAIM_MAINTAIN') && (
            <Button
              variant="accent"
              icon={<Plus size={16} />}
              onClick={() => void navigate('/claims/new')}
            >
              Notify Claim
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
            label="Class"
            value={filters.businessLine}
            emptyLabel="All"
            options={lookups.businessLines.map((d) => ({ value: d.code, label: d.name }))}
            onChange={(v) => set({ businessLine: v === '' ? undefined : v })}
          />
          <TextField
            label="Claim / policy no. / insured"
            value={filters.q}
            onChange={(v) => set({ q: v })}
          />
          <DateField
            label="Loss from"
            value={filters.lossFrom}
            onChange={(v) => set({ lossFrom: v })}
          />
          <DateField label="Loss to" value={filters.lossTo} onChange={(v) => set({ lossTo: v })} />
        </div>
      </Card>
      <ErrorAlert error={query.error} />
      <Card flush>
        <DataTable<Claim>
          loading={query.isLoading}
          rows={data?.content ?? []}
          rowKey={(c) => c.id}
          onRowClick={(c) => void navigate(`/claims/${String(c.id)}`)}
          caption="Claims"
          columns={[
            { key: 'no', header: 'Claim No.', render: (c) => <strong>{c.claimNo}</strong> },
            { key: 'pol', header: 'Policy', render: (c) => c.policyNo },
            { key: 'ins', header: 'Insured', render: (c) => c.insuredName },
            { key: 'lob', header: 'Class', render: (c) => c.businessLine },
            { key: 'loss', header: 'Loss Date', render: (c) => formatDate(c.lossDate) },
            { key: 'nat', header: 'Nature', render: (c) => c.natureOfLoss },
            { key: 'ccy', header: 'Ccy', render: (c) => c.currency },
            {
              key: 'est',
              header: 'Estimate',
              numeric: true,
              render: (c) => <Amount value={c.totals.ourEstimate} />,
            },
            {
              key: 'paid',
              header: 'Paid',
              numeric: true,
              render: (c) => <Amount value={c.totals.ourPaid} />,
            },
            {
              key: 'os',
              header: 'Outstanding',
              numeric: true,
              render: (c) => <Amount value={c.totals.ourOutstanding} />,
            },
            { key: 'st', header: 'Status', render: (c) => <StatusBadge status={c.status} /> },
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
