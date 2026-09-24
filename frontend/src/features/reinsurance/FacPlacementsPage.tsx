import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { reinsuranceApi } from '@/api/reinsurance';
import type { FacPlacement, FacStatus } from '@/api/reinsurance';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { today } from '@/utils/format';
import { ageInDays } from './fac';
import { FacSlipDialog } from './FacSlipDialog';
import { useRiLookups } from './useRiLookups';

type Filter = FacStatus | 'ALL';

const TABS: readonly { id: Filter; label: string }[] = [
  { id: 'PROVISIONAL', label: 'Provisional' },
  { id: 'PENDING_APPROVAL', label: 'Pending approval' },
  { id: 'PLACED', label: 'Placed' },
  { id: 'CLOSED', label: 'Closed' },
  { id: 'ALL', label: 'All' },
];

/** Facultative placements of the parts of risks that exceed treaty capacity. */
export default function FacPlacementsPage() {
  const lookups = useRiLookups();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState<Filter>('PROVISIONAL');
  const [open, setOpen] = useState<FacPlacement | null>(null);
  const list = useQuery({
    queryKey: ['ri-fac', lookups.companyId, filter],
    queryFn: () =>
      reinsuranceApi.placements(lookups.companyId, filter === 'ALL' ? undefined : filter),
    enabled: lookups.companyId > 0,
  });
  const now = today();

  return (
    <div className="stack">
      <PageHeader
        section="Reinsurance"
        title="Facultative Placements"
        description="Sum insured beyond treaty capacity. Record the reinsurers on the slip and submit it; a checker approves the placement, which cedes the premium to them."
      />
      <Tabs tabs={TABS} active={filter} onChange={setFilter} />
      <ErrorAlert error={list.error} />
      <Card flush>
        <DataTable<FacPlacement>
          rows={list.data ?? []}
          loading={list.isLoading}
          rowKey={(p) => p.id}
          onRowClick={(p) => setOpen(p)}
          caption="Facultative placements"
          columns={[
            { key: 'n', header: 'Placement', render: (p) => <strong>{p.placementNo}</strong> },
            { key: 'p', header: 'Policy', render: (p) => p.policyNo },
            { key: 'l', header: 'Class', render: (p) => p.businessLine },
            { key: 'r', header: 'Risk', render: (p) => `${p.riskLineNo} ${p.riskDescription}` },
            {
              key: 'si',
              header: 'FAC SI',
              numeric: true,
              render: (p) => <Amount value={p.facSi} />,
            },
            {
              key: 'pr',
              header: 'FAC Premium',
              numeric: true,
              render: (p) => <Amount value={p.facPremium} />,
            },
            { key: 'c', header: 'Ccy', render: (p) => p.currency },
            { key: 'pc', header: 'Placed %', numeric: true, render: (p) => p.placementPct },
            {
              key: 'a',
              header: 'Age (days)',
              numeric: true,
              render: (p) => ageInDays(p.riDate, now),
            },
            { key: 's', header: 'Status', render: (p) => <StatusBadge status={p.status} /> },
          ]}
        />
      </Card>
      {open !== null && (
        <FacSlipDialog
          key={open.id}
          placement={open}
          reinsurers={lookups.reinsurers}
          onClose={() => setOpen(null)}
          onChanged={(p, message) => {
            setOpen(p);
            toast.success(message);
            void queryClient.invalidateQueries({ queryKey: ['ri-fac'] });
          }}
        />
      )}
    </div>
  );
}
