import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Hourglass, Plus, UserPlus } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { productMaintApi } from '@/api/productmaint';
import type { RequestListItem, RequestType } from '@/api/productmaint';
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
import { useCompanyId } from '@/context/workspaceContext';
import { SelectInput } from '@/features/assets/FormControls';
import { BulkAssignDialog } from './BulkAssignDialog';
import { REQUEST_TABS, REQUEST_TYPES, isRequestTab, requestCriteria } from './packageRequest';
import type { RequestTab } from './packageRequest';
import { REQUEST_COLUMNS } from './requestColumns';

/**
 * Package Requests (BRPM.008-019): the package request work list by stage, from the draft to the
 * released version, with the request type, scope flag, client or programme, product, stage, time
 * in stage (SLA) and assignee; bulk Assign for team leaders.
 */
export default function PackageRequestsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [params] = useSearchParams();
  const initialTab = params.get('tab');
  const [tab, setTab] = useState<RequestTab>(isRequestTab(initialTab) ? initialTab : 'drafts');
  const [applied, setApplied] = useState('');
  const [mine, setMine] = useState(false);
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [type, setType] = useState<RequestType | ''>('');
  const [page, setPage] = useState(0);
  const [assigning, setAssigning] = useState(false);
  const selection = useRowSelection();
  const criteria = requestCriteria(tab, applied, mine, type);
  const list = useQuery({
    queryKey: ['package-requests', companyId, criteria, page],
    queryFn: () => productMaintApi.search(companyId, criteria, page),
    enabled: companyId > 0,
  });
  const rows = list.data?.content ?? [];
  const selected = rows.filter((r) => selection.has(String(r.id)));
  const sameStage = selected.length > 0 && selected.every((r) => r.status === selected[0]?.status);
  const columns = [
    selectionColumn<RequestListItem>(
      rows,
      (r) => String(r.id),
      selection,
      (r) => r.requestNo,
    ),
    ...REQUEST_COLUMNS,
  ];
  return (
    <div className="stack">
      <PageHeader
        section="Product Maintenance"
        title="Package Requests"
        description="Requests to create, amend, renew or retire packages: approvals, insurer negotiation, ManCom sign-off, MBS set-up and release."
        actions={
          <>
            <Link className="btn btn-secondary" to="/product-maintenance/expiry">
              <Hourglass size={16} aria-hidden="true" /> Package Expiry
            </Link>
            {can('PKG_REQUEST') && (
              <Link className="btn btn-accent" to="/product-maintenance/requests/new">
                <Plus size={16} aria-hidden="true" /> New Package Request
              </Link>
            )}
          </>
        }
      />
      <Card flush>
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
          placeholder="Search Request No."
          onSearch={(text) => {
            setApplied(text);
            setPage(0);
          }}
          filters={{ open: filtersOpen, onToggle: () => setFiltersOpen(!filtersOpen) }}
          extra={
            <label className="checkbox">
              <input type="checkbox" checked={mine} onChange={(e) => setMine(e.target.checked)} />
              Only My Requests
            </label>
          }
        >
          {can('WORK_ASSIGN') && (
            <Button
              variant="secondary"
              icon={<UserPlus size={16} />}
              disabled={!sameStage}
              title={sameStage ? undefined : 'Select requests of one stage'}
              onClick={() => setAssigning(true)}
            >
              Assign
            </Button>
          )}
        </WorklistToolbar>
        {filtersOpen && (
          <div className="worklist-filters">
            <SelectInput
              label="Request type"
              blank="All types"
              value={type}
              options={REQUEST_TYPES}
              onChange={(v) => {
                setType(v as RequestType | '');
                setPage(0);
              }}
            />
          </div>
        )}
        <ErrorAlert error={list.error} />
        <DataTable<RequestListItem>
          loading={list.isLoading}
          rows={rows}
          rowKey={(r) => r.id}
          onRowClick={(r) => void navigate(`/product-maintenance/requests/${r.id}`)}
          columns={columns}
        />
        <PageFooter data={list.data} noun="package requests" onPage={setPage} />
      </Card>
      {assigning && (
        <BulkAssignDialog
          requests={selected}
          onClose={() => setAssigning(false)}
          onDone={async () => {
            selection.clear();
            await queryClient.invalidateQueries({ queryKey: ['package-requests'] });
          }}
        />
      )}
    </div>
  );
}
