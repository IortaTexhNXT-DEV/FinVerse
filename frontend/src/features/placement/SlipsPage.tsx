import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { placementApi } from '@/api/placement';
import type { Slip } from '@/api/placement';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';
import { placementLink } from './placementLogic';
import { SlipActions } from './SlipActions';

type Filter = 'ALL' | 'GENERATED' | 'SENT' | 'SUPERSEDED';

const FILTERS: readonly { id: Filter; label: string }[] = [
  { id: 'ALL', label: 'All Slips' },
  { id: 'GENERATED', label: 'To Send' },
  { id: 'SENT', label: 'Sent' },
  { id: 'SUPERSEDED', label: 'Superseded' },
];

/**
 * Placement slips (BRNB.069/071): every slip version by insurer branch with its accounts,
 * download of the PDF and Excel files, send or resend to the insurer and regeneration after a
 * return. The send log of each slip is the messaging outbox.
 */
export default function SlipsPage() {
  const companyId = useCompanyId();
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState<Filter>('ALL');
  const [page, setPage] = useState(0);
  const slips = useQuery({
    queryKey: ['placement', 'slips', companyId, filter, page],
    queryFn: () => placementApi.slips(companyId, filter === 'ALL' ? undefined : filter, page),
    enabled: companyId > 0,
  });
  const refresh = () => void queryClient.invalidateQueries({ queryKey: ['placement'] });
  return (
    <div className="stack">
      <PageHeader
        section="Placement & Booking"
        title="Placement Slips"
        description="Slips PL-yyyy per insurer branch, as PDF and Excel. Sending a slip places its accounts with the insurer; a returned placement gets a new slip version."
      />
      <ErrorAlert error={slips.error} />
      <Card flush>
        <div className="work-tabs">
          <Tabs
            tabs={FILTERS}
            active={filter}
            onChange={(f) => {
              setFilter(f);
              setPage(0);
            }}
          />
        </div>
        <DataTable<Slip>
          caption="Placement slips"
          loading={slips.isLoading}
          rows={slips.data?.content ?? []}
          rowKey={(s) => s.id}
          emptyMessage="No items to display"
          columns={[
            { key: 'no', header: 'Slip', render: (s) => <code>{s.displayNo}</code> },
            { key: 'status', header: 'Status', render: (s) => <StatusBadge status={s.status} /> },
            {
              key: 'insurer',
              header: 'Insurer / Branch',
              render: (s) => `${s.insurerCode} / ${s.branchCode}`,
            },
            {
              key: 'accounts',
              header: 'Accounts',
              render: (s) => (
                <span className="stack">
                  {s.accounts.map((a) => (
                    <Link key={a.arn} to={placementLink(a.arn)}>
                      {a.arn}
                    </Link>
                  ))}
                </span>
              ),
            },
            {
              key: 'sent',
              header: 'Sent',
              render: (s) =>
                s.sentAt ? (
                  <span>
                    {formatDateTime(s.sentAt)}
                    <span className="cell-sub">
                      {s.recipients} · {s.sendCount} send(s)
                    </span>
                  </span>
                ) : (
                  '—'
                ),
            },
            {
              key: 'actions',
              header: '',
              render: (s) => <SlipActions slip={s} onChanged={refresh} />,
            },
          ]}
        />
        <PageFooter data={slips.data} noun="slips" onPage={setPage} />
      </Card>
    </div>
  );
}
