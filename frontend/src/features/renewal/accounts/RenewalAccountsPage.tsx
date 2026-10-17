import { useInfiniteQuery, useQuery } from '@tanstack/react-query';
import { Upload } from 'lucide-react';
import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { CandidateRow } from '@/api/renewal';
import { renewalDashboardApi } from '@/api/renewalDashboard';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { SortState } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { RENEWAL_SECTION } from '../common/renewalCodes';
import { UploadPanel } from '../common/UploadPanel';
import { bucketColumns, panelTabs } from './bucketColumns';
import '../renewal.css';

const CHUNK = 500;

/**
 * Renewal Accounts (BDOI Renewal FRS FRRN.002.05): the renewal accounts of every year in four
 * panels - Clean, Review, Non-Renewable (or Exception, by the setting) and All - with BDOI's
 * columns, the search across all columns, the sort of every column and the rows loaded as the list
 * scrolls, without a pager; a row opens the renewal account.
 */
export default function RenewalAccountsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [upload, setUpload] = useState(false);
  const navigate = useNavigate();
  const [tab, setTab] = useState('BUCKET_CLEAN');
  const [search, setSearch] = useState('');
  const [sort, setSort] = useState<SortState>({ key: 'expiry', direction: 'asc' });
  const panels = useQuery({
    queryKey: ['renewal', 'panels'],
    queryFn: () => renewalDashboardApi.panels(),
  });
  const tabs = panelTabs(panels.data?.thirdBucket ?? 'NON_RENEWABLE');
  const q = { tab, q: search === '' ? undefined : search, sort: `${sort.key},${sort.direction}` };
  const list = useInfiniteQuery({
    queryKey: ['renewal', 'accounts', companyId, q],
    queryFn: ({ pageParam }) => renewalDashboardApi.accounts(companyId, q, pageParam, CHUNK),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.page + 1 < last.totalPages ? last.page + 1 : undefined),
    enabled: companyId > 0,
  });
  const rows = useMemo(() => (list.data?.pages ?? []).flatMap((p) => p.content), [list.data]);
  const total = list.data?.pages[0]?.totalElements ?? 0;
  const onScroll = (e: React.UIEvent<HTMLDivElement>) => {
    const el = e.currentTarget;
    if (
      el.scrollTop + el.clientHeight > el.scrollHeight - 400 &&
      list.hasNextPage &&
      !list.isFetchingNextPage
    ) {
      void list.fetchNextPage();
    }
  };
  return (
    <div className="stack">
      <PageHeader
        section={RENEWAL_SECTION}
        title="Renewal Accounts"
        description="The renewal accounts of the current and previous years by classification panel."
        actions={
          can('RNW_DISPOSE') && (
            <Button variant="secondary" icon={<Upload size={16} />} onClick={() => setUpload(true)}>
              Upload Account Tags
            </Button>
          )
        }
      />
      {upload && (
        <UploadPanel
          label="Upload Account Tags"
          handler="RNW_ACCOUNT_TAGS"
          onClose={() => setUpload(false)}
        />
      )}
      <Tabs tabs={tabs} active={tab} onChange={setTab} />
      <div className="rnw-drill-tools">
        <input
          className="input"
          aria-label="Search the renewal accounts"
          placeholder="Search reference, invoice, policy, PN or client"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <span className="muted" aria-live="polite">
          {total.toLocaleString()} renewal accounts
        </span>
        {list.hasNextPage && (
          <Button
            variant="ghost"
            busy={list.isFetchingNextPage}
            onClick={() => void list.fetchNextPage()}
          >
            Load more
          </Button>
        )}
      </div>
      <ErrorAlert error={list.error} onRetry={() => void list.refetch()} />
      <Card flush>
        <div className="rnw-scroll" onScroll={onScroll}>
          <DataTable<CandidateRow>
            loading={list.isLoading}
            rows={rows}
            rowKey={(r) => r.renewalRef}
            sort={sort}
            onSort={setSort}
            onRowClick={(r) =>
              void navigate(`/renewal/candidates/${encodeURIComponent(r.renewalRef)}`)
            }
            emptyMessage="No renewal accounts in this panel"
            columns={bucketColumns()}
          />
        </div>
      </Card>
    </div>
  );
}
