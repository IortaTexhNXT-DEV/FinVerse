import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Upload } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { issuanceApi } from '@/api/issuance';
import type { IssuanceTab } from '@/api/issuance';
import { useAuth } from '@/auth/authContext';
import { useRowSelection } from '@/components/broking/rowSelection';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { WorkTiles } from '@/components/broking/WorkTiles';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { IssuanceBulkBar } from './IssuanceBulkBar';
import { issuanceTiles, ISSUANCE_TABS } from './issuanceLogic';
import { IssuanceTable } from './IssuanceTable';

/**
 * Issuance Workbench (BRNB.073/074/070/077): placed accounts awaiting their e-policy, e-policies
 * whose extraction waits for review, confirmed e-policies ready to send and mortgaged accounts
 * without an Insurance Advice.
 */
export default function IssuanceWorkbenchPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const selection = useRowSelection();
  const [tab, setTab] = useState<IssuanceTab>('AWAITING_POLICY');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const counts = useQuery({
    queryKey: ['issuance', 'counts', companyId],
    queryFn: () => issuanceApi.counts(companyId),
    enabled: companyId > 0,
  });
  const list = useQuery({
    queryKey: ['issuance', 'workbench', companyId, tab, search, page],
    queryFn: () => issuanceApi.workbench(companyId, tab, search, page),
    enabled: companyId > 0,
  });
  const choose = (next: IssuanceTab) => {
    setTab(next);
    setPage(0);
    selection.clear();
  };
  const rows = list.data?.content ?? [];
  return (
    <div className="stack">
      <PageHeader
        section="Policy Issuance"
        title="Issuance Workbench"
        description="From the placed account to the issued policy: receive and review e-policies, update the policy numbers, generate Insurance Advices and send e-policies to clients."
        actions={
          can('EPOLICY_MANAGE') && (
            <Link className="btn btn-primary" to="/issuance/upload">
              <Upload size={16} aria-hidden="true" /> Upload E-policies
            </Link>
          )
        }
      />
      <ErrorAlert error={counts.error ?? list.error} />
      <WorkTiles
        label="Issuance status"
        tiles={issuanceTiles(counts.data).map((t) => ({
          key: t.tab,
          label: t.label,
          value: t.value,
          alert: t.alert,
          active: t.tab === tab,
          onClick: () => choose(t.tab),
        }))}
      />
      <Card flush>
        <div className="work-tabs">
          <Tabs tabs={ISSUANCE_TABS} active={tab} onChange={choose} />
        </div>
        <WorklistToolbar
          onSearch={(text) => {
            setSearch(text);
            setPage(0);
          }}
        >
          <IssuanceBulkBar
            tab={tab}
            rows={rows}
            selection={selection}
            onChanged={() => void queryClient.invalidateQueries({ queryKey: ['issuance'] })}
          />
        </WorklistToolbar>
        <IssuanceTable tab={tab} rows={rows} loading={list.isLoading} selection={selection} />
        <PageFooter data={list.data} noun="items" onPage={setPage} />
      </Card>
    </div>
  );
}
