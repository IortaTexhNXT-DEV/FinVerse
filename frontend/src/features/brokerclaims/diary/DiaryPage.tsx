import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2 } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { CLAIMS_SECTION } from '../ClaimsPlaceholder';
import { diaryApi } from './api';
import type { DiaryItem } from './api';

type DiaryTabId = 'open' | 'all';

const TABS: readonly { id: DiaryTabId; label: string }[] = [
  { id: 'open', label: 'Open' },
  { id: 'all', label: 'All' },
];

function dueState(d: DiaryItem): string {
  if (d.doneAt !== undefined) {
    return 'DONE';
  }
  return d.overdue ? 'OVERDUE' : 'OPEN';
}

/**
 * My Diary (BRCLM.022/034, FR-CL-052): the diary entries assigned to the signed-in user across the
 * claims, overdue and due first; each opens its claim, and the user marks them done.
 */
export default function DiaryPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<DiaryTabId>('open');
  const [page, setPage] = useState(0);
  const rows = useQuery({
    queryKey: ['broker-claims', 'my-diary', companyId, tab, page],
    queryFn: () => diaryApi.mine(companyId, tab === 'all', page),
    enabled: companyId > 0,
  });
  const done = useMutation({
    mutationFn: (id: number) => diaryApi.done(id, companyId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['broker-claims'] });
      toast.success('Diary entry done');
    },
  });
  return (
    <div className="stack">
      <PageHeader
        section={CLAIMS_SECTION}
        title="My Diary"
        description="Your calls, e-mails, meetings, notes and follow-ups across claims; overdue and due entries first."
      />
      <ErrorAlert error={rows.error ?? done.error} />
      <Card flush>
        <Tabs
          tabs={TABS}
          active={tab}
          onChange={(t) => {
            setTab(t);
            setPage(0);
          }}
        />
        <DataTable<DiaryItem>
          caption="My diary entries"
          loading={rows.isLoading}
          rows={rows.data?.content ?? []}
          rowKey={(d) => d.id}
          emptyMessage="No diary entries for you"
          onRowClick={(d) => void navigate(`/claims-handling/${d.claimId}`)}
          columns={[
            { key: 'due', header: 'Due', render: (d) => formatDate(d.dueDate) },
            {
              key: 'claim',
              header: 'Claim',
              render: (d) => (
                <>
                  <strong>{d.claimNo}</strong>
                  <div className="muted">{d.assuredName}</div>
                </>
              ),
            },
            { key: 'type', header: 'Type', render: (d) => d.typeLabel },
            { key: 'text', header: 'Text', render: (d) => d.text },
            { key: 'from', header: 'From', render: (d) => d.createdBy },
            { key: 'state', header: 'Status', render: (d) => <StatusBadge status={dueState(d)} /> },
            {
              key: 'act',
              header: '',
              render: (d) =>
                d.doneAt === undefined && (
                  <Button
                    size="sm"
                    variant="ghost"
                    icon={<CheckCircle2 size={14} />}
                    onClick={(e) => {
                      e.stopPropagation();
                      done.mutate(d.id);
                    }}
                  >
                    Mark Done
                  </Button>
                ),
            },
          ]}
        />
        <PageFooter data={rows.data} noun="entries" onPage={setPage} />
      </Card>
    </div>
  );
}
