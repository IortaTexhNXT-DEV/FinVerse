import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { PlayCircle, UserPlus } from 'lucide-react';
import { useState } from 'react';
import { submittedApi } from '@/api/submitted';
import type { MasterlistFilters, MasterlistTab, PolicyRow } from '@/api/submitted';
import { useAuth } from '@/auth/authContext';
import { LovSelect } from '@/components/broking/LovSelect';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useTabParam } from '@/components/ui/useTabParam';
import { useCompanyId } from '@/context/workspaceContext';
import { SBM_LOV, MASTERLIST_TABS, SUBMITTED_SECTION } from '../common/submittedCodes';
import { UserSelect } from '../common/UserSelect';
import { policyColumns } from './policyColumns';

function AssignDialog({ ids, onClose }: Readonly<{ ids: number[]; onClose: () => void }>) {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [handler, setHandler] = useState('');
  const assign = useMutation({
    mutationFn: () => submittedApi.assign(companyId, ids, handler),
    onSuccess: () => {
      toast.success(`${String(ids.length)} records assigned`);
      void queryClient.invalidateQueries({ queryKey: ['submitted'] });
      onClose();
    },
  });
  return (
    <Modal
      title="Assign Handler"
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button disabled={handler === '' || assign.isPending} onClick={() => assign.mutate()}>
            Assign
          </Button>
        </>
      }
    >
      <ErrorAlert error={assign.error} />
      <p>{ids.length === 1 ? '1 record selected.' : `${String(ids.length)} records selected.`}</p>
      <Field label="Handler" required>
        {(id) => (
          <UserSelect id={id} permission="SBM_MAINTAIN" value={handler} onChange={setHandler} />
        )}
      </Field>
    </Modal>
  );
}

/**
 * Masterlist (FR-SP-010 to 033): the submitted policies by tab with search and filters; a Team
 * Leader assigns the handlers and a handler runs the processing of the records selected.
 */
export default function MasterlistPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [tab, setTab] = useTabParam<MasterlistTab>(
    MASTERLIST_TABS.map((t) => t.id),
    'ALL',
  );
  const [filters, setFilters] = useState<MasterlistFilters>({});
  const [page, setPage] = useState(0);
  const [assigning, setAssigning] = useState(false);
  const selection = useRowSelection();
  const query = { ...filters, tab };
  const list = useQuery({
    queryKey: ['submitted', 'masterlist', companyId, query, page],
    queryFn: () => submittedApi.list(companyId, query, page),
    enabled: companyId > 0,
  });
  const counts = useQuery({
    queryKey: ['submitted', 'counts', companyId, filters],
    queryFn: () => submittedApi.counts(companyId, filters),
    enabled: companyId > 0,
  });
  const run = useMutation({
    mutationFn: () => submittedApi.run(companyId, selection.keys.map(Number)),
    onSuccess: (r) => {
      toast.success(`Run ${r.runNo}: ${String(r.bucketed)} bucketed, ${String(r.fallout)} fallout`);
      selection.clear();
      void queryClient.invalidateQueries({ queryKey: ['submitted'] });
    },
  });
  const rows = list.data?.content ?? [];
  const ids = selection.keys.map(Number);
  return (
    <div className="stack">
      <PageHeader
        section={SUBMITTED_SECTION}
        title="Masterlist"
        description="Every submitted policy with its classification, bucket and renewal status."
      />
      <Tabs
        tabs={MASTERLIST_TABS.map((t) => ({ ...t, count: counts.data?.[t.id] }))}
        active={tab}
        onChange={(t) => {
          setTab(t);
          setPage(0);
          selection.clear();
        }}
      />
      <WorklistToolbar
        placeholder="Search Masterlist No., PN, Policy No. or Assured"
        onSearch={(q) => {
          setFilters((f) => ({ ...f, q }));
          setPage(0);
        }}
        extra={
          <LovSelect
            id="sbm-segment"
            type={SBM_LOV.segment}
            value={filters.segment ?? ''}
            placeholder="All segments"
            onChange={(segment) => {
              setFilters((f) => ({ ...f, segment }));
              setPage(0);
            }}
          />
        }
      >
        {can('WORK_ASSIGN') && (
          <Button
            variant="secondary"
            icon={<UserPlus size={16} />}
            disabled={ids.length === 0}
            onClick={() => setAssigning(true)}
          >
            Assign Handler
          </Button>
        )}
        {can('SBM_PROCESS') && (
          <Button
            icon={<PlayCircle size={16} />}
            disabled={ids.length === 0 || run.isPending}
            onClick={() => run.mutate()}
          >
            Run Processing
          </Button>
        )}
      </WorklistToolbar>
      <ErrorAlert error={list.error ?? run.error} onRetry={() => void list.refetch()} />
      <Card flush>
        <DataTable<PolicyRow>
          loading={list.isLoading}
          rows={rows}
          rowKey={(p) => p.id}
          emptyMessage="No submitted policies in this tab"
          columns={[
            selectionColumn(
              rows,
              (p) => String(p.id),
              selection,
              (p) => p.sbmNo,
            ),
            ...policyColumns,
          ]}
          footer={<PageFooter data={list.data} noun="policies" onPage={setPage} />}
        />
      </Card>
      {assigning && <AssignDialog ids={ids} onClose={() => setAssigning(false)} />}
    </div>
  );
}
