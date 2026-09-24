import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Landmark, Search } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { placementApi } from '@/api/placement';
import type { WorkbenchTab } from '@/api/placement';
import { useAuth } from '@/auth/authContext';
import { useRowSelection } from '@/components/broking/rowSelection';
import { WorkTiles } from '@/components/broking/WorkTiles';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { tilesOf, WORKBENCH_TABS } from './placementLogic';
import { WorkbenchBulkBar } from './WorkbenchBulkBar';
import { WorkbenchTable } from './WorkbenchTable';

/**
 * Placement Workbench (BRNB.069/071/033/034/062/103, BDOI Placement & Booking design): tiles for
 * awaiting payment, ready for placement, placed, returned by insurer and hold cover expiring;
 * status tabs; search by proposal number; bulk For Placement, Send Slips, For Booking, Cancel
 * Placement and Reactivate enabled by the row selection.
 */
export default function PlacementWorkbenchPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const selection = useRowSelection();
  const [tab, setTab] = useState<WorkbenchTab>('FOR_PLACEMENT');
  const [text, setText] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const counts = useQuery({
    queryKey: ['placement', 'counts', companyId],
    queryFn: () => placementApi.counts(companyId),
    enabled: companyId > 0,
  });
  const list = useQuery({
    queryKey: ['placement', 'workbench', companyId, tab, search, page],
    queryFn: () => placementApi.workbench(companyId, tab, search, page),
    enabled: companyId > 0,
  });
  const choose = (next: WorkbenchTab) => {
    setTab(next);
    setPage(0);
    selection.clear();
  };
  const refresh = () => void queryClient.invalidateQueries({ queryKey: ['placement'] });
  const rows = list.data?.content ?? [];
  return (
    <div className="stack">
      <PageHeader
        section="Placement & Booking"
        title="Placement Workbench"
        description="Accounts from payment to placement with the insurer: generate and send placement slips, follow hold covers and insurer returns, and hand issued policies to booking."
        actions={
          can('BILLING_MANAGE') && (
            <Link className="btn btn-secondary" to="/placement/billing">
              <Landmark size={16} aria-hidden="true" /> CLPC Billing
            </Link>
          )
        }
      />
      <ErrorAlert error={counts.error ?? list.error} />
      <WorkTiles
        label="Placement status"
        tiles={tilesOf(counts.data).map((t) => ({
          key: t.label,
          label: t.label,
          value: t.value,
          alert: t.alert,
          active: t.tab === tab,
          onClick: () => choose(t.tab),
        }))}
      />
      <Card flush>
        <div className="work-tabs">
          <Tabs tabs={WORKBENCH_TABS} active={tab} onChange={choose} />
        </div>
        <div className="work-toolbar">
          <form
            className="row"
            onSubmit={(e) => {
              e.preventDefault();
              setSearch(text.trim());
              setPage(0);
            }}
          >
            <label className="visually-hidden" htmlFor="placement-search">
              Search Proposal No.
            </label>
            <input
              id="placement-search"
              className="input"
              placeholder="Search Proposal No."
              value={text}
              onChange={(e) => setText(e.target.value)}
            />
            <Button type="submit" variant="secondary" icon={<Search size={16} />}>
              Search
            </Button>
          </form>
          <span className="spacer" />
          <WorkbenchBulkBar
            companyId={companyId}
            tab={tab}
            rows={rows}
            selection={selection}
            onChanged={refresh}
          />
        </div>
        <WorkbenchTable rows={rows} loading={list.isLoading} selection={selection} />
        <PageFooter data={list.data} noun="accounts" onPage={setPage} />
      </Card>
    </div>
  );
}
