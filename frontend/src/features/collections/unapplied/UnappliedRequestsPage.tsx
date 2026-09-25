import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { RefreshCw } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { unappliedApi } from './api';
import type { RequestTab } from './labels';
import { REQUEST_TABS, requestStatuses } from './labels';
import { REQUEST_COLUMNS } from './columns';

/**
 * Requests to Cashiering (BRCLXN.030/032, 040): the status of every request sent on a collector
 * disposition - queued, handed over, accepted, executed or rejected - with Cashiering's reference
 * and message, and the application file that listed it (BRCLXN.041). Check Status asks Cashiering
 * again for the selected open requests.
 */
export default function UnappliedRequestsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<RequestTab>('OPEN');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const selection = useRowSelection();
  const rows = useQuery({
    queryKey: ['collections', 'unapplied', 'requests', companyId, tab, query, page],
    queryFn: () => unappliedApi.requests(companyId, requestStatuses(tab), query, page),
    enabled: companyId > 0,
  });
  const refresh = useMutation({
    mutationFn: async () => {
      for (const id of selection.keys) {
        await unappliedApi.refresh(Number(id));
      }
      return selection.keys.length;
    },
    onSuccess: async (n) => {
      selection.clear();
      await queryClient.invalidateQueries({ queryKey: ['collections', 'unapplied'] });
      toast.success(`${n} request(s) checked with Cashiering`);
    },
  });
  const list = rows.data?.content ?? [];
  const selectable = tab === 'OPEN' && can('CLX_UNAPPLIED_WORK');
  const columns = selectable
    ? [
        selectionColumn(
          list,
          (r) => String(r.id),
          selection,
          (r) => r.unappliedRef,
        ),
        ...REQUEST_COLUMNS,
      ]
    : REQUEST_COLUMNS;
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Collections · Unapplied Payments"
        backTo="/collections/unapplied"
        title="Requests to Cashiering"
        description="Applications, refunds, reclasses and transfers asked of Cashiering on unapplied payments, with where each one stands."
      />
      <ErrorAlert error={rows.error ?? refresh.error} />
      <Card flush>
        <div>
          <Tabs
            tabs={REQUEST_TABS}
            active={tab}
            onChange={(t) => {
              setTab(t);
              setPage(0);
              selection.clear();
            }}
          />
          <WorklistToolbar
            placeholder="Search Unapplied Reference"
            initial={query}
            onSearch={(text) => {
              setQuery(text);
              setPage(0);
            }}
          >
            {selectable && (
              <Button
                variant="secondary"
                icon={<RefreshCw size={16} />}
                busy={refresh.isPending}
                disabled={selection.keys.length === 0}
                onClick={() => refresh.mutate()}
              >
                Check Status
              </Button>
            )}
          </WorklistToolbar>
          <DataTable
            caption="Requests to Cashiering"
            columns={columns}
            rows={list}
            rowKey={(r) => r.id}
            loading={rows.isLoading}
            onRowClick={(r) =>
              void navigate(`/collections/unapplied/${encodeURIComponent(r.unappliedRef)}`)
            }
            emptyMessage="No requests to display"
          />
          <PageFooter data={rows.data} noun="requests" onPage={setPage} />
        </div>
      </Card>
    </div>
  );
}
