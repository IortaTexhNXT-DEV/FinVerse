import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { UserPlus } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime, humanize } from '@/utils/format';
import { disbursementApi, toQuery } from './api';
import type { PayeeRequest, PayeeSummary } from './api';
import { MODE_LABELS, PAYEE_TABS } from './labels';
import type { PayeeTab } from './labels';
import './disbursement.css';

const PAYEE_COLUMNS: Column<PayeeSummary>[] = [
  { key: 'code', header: 'Payee Code', render: (p) => p.payeeCode },
  { key: 'name', header: 'Name', render: (p) => p.name },
  { key: 'class', header: 'Class', render: (p) => humanize(p.payeeClass) },
  { key: 'mode', header: 'Default Mode', render: (p) => MODE_LABELS[p.defaultMode] },
  { key: 'account', header: 'Primary Account', render: (p) => p.accountNo ?? '—' },
  { key: 'source', header: 'Source', render: (p) => humanize(p.source) },
  { key: 'stage', header: 'Status', render: (p) => <StatusBadge status={p.stage} /> },
];

function RequestsTable({ companyId }: Readonly<{ companyId: number }>) {
  const { can } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const requests = useQuery({
    queryKey: ['disbursement', 'payee-requests', companyId, page],
    queryFn: () => disbursementApi.payeeRequests(companyId, page),
    enabled: companyId > 0,
  });
  const close = useMutation({
    mutationFn: (id: number) => disbursementApi.closePayeeRequest(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['disbursement'] });
      toast.success('Payee request closed');
    },
  });
  const columns: Column<PayeeRequest>[] = [
    {
      key: 'source',
      header: 'Source',
      render: (r) => (r.source === 'NO_MATCH' ? 'No match' : r.source),
    },
    { key: 'code', header: 'Payee Code', render: (r) => r.payeeCode ?? '—' },
    { key: 'name', header: 'Payee Name', render: (r) => r.payeeName },
    { key: 'ref', header: 'Reference', render: (r) => r.sourceRef ?? '' },
    { key: 'details', header: 'Details', render: (r) => r.details ?? '' },
    { key: 'at', header: 'Received', render: (r) => formatDateTime(r.createdAt) },
    {
      key: 'act',
      header: 'Action',
      render: (r) =>
        can('DISB_PAYEE_MAINTAIN') ? (
          <div className="dsb-actions">
            <Button
              size="sm"
              onClick={() =>
                void navigate(
                  `/disbursement/payees/new${toQuery({ code: r.payeeCode, name: r.payeeName })}`,
                )
              }
            >
              Create Payee
            </Button>
            <Button
              size="sm"
              variant="secondary"
              busy={close.isPending}
              onClick={() => close.mutate(r.id)}
            >
              Close
            </Button>
          </div>
        ) : null,
    },
  ];
  return (
    <>
      <ErrorAlert error={requests.error ?? close.error} />
      <DataTable
        caption="Payee requests"
        columns={columns}
        rows={requests.data?.content ?? []}
        rowKey={(r) => r.id}
        loading={requests.isLoading}
        emptyMessage="No open payee request"
      />
      <PageFooter data={requests.data} noun="requests" onPage={setPage} />
    </>
  );
}

/**
 * Payee master (DIS 2.2.0-2.2.8, 3.25.2): insurers, clients, suppliers, agencies and employees
 * with their bank accounts and allowed modes, maintained by the processor and authorised by the
 * team leader; the payee requests list the payees that other units or unmatched requests need.
 */
export default function PayeesPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const navigate = useNavigate();
  const [tab, setTab] = useState<PayeeTab>('ACTIVE');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const stages = PAYEE_TABS.find((t) => t.id === tab)?.stages ?? [];
  const payees = useQuery({
    queryKey: ['disbursement', 'payees', companyId, tab, query, page],
    queryFn: () => disbursementApi.payees(companyId, stages, query, page),
    enabled: companyId > 0 && tab !== 'REQUESTS',
  });
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Disbursement"
        title="Payees"
        description="Payees with their bank accounts and allowed payment modes; new payees and changes are authorised by the team leader."
        actions={
          can('DISB_PAYEE_MAINTAIN') ? (
            <Button
              variant="accent"
              icon={<UserPlus size={16} />}
              onClick={() => void navigate('/disbursement/payees/new')}
            >
              New Payee
            </Button>
          ) : undefined
        }
      />
      <ErrorAlert error={payees.error} />
      <Card flush>
        <div>
          <Tabs
            tabs={PAYEE_TABS}
            active={tab}
            onChange={(t) => {
              setTab(t);
              setPage(0);
            }}
          />
          {tab === 'REQUESTS' ? (
            <RequestsTable companyId={companyId} />
          ) : (
            <>
              <WorklistToolbar
                placeholder="Search Payee Code or Name"
                onSearch={(text) => {
                  setQuery(text);
                  setPage(0);
                }}
              />
              <DataTable
                caption="Payees"
                columns={PAYEE_COLUMNS}
                rows={payees.data?.content ?? []}
                rowKey={(p) => p.id}
                loading={payees.isLoading}
                onRowClick={(p) => void navigate(`/disbursement/payees/${p.id}`)}
              />
              <PageFooter data={payees.data} noun="payees" onPage={setPage} />
            </>
          )}
        </div>
      </Card>
    </div>
  );
}
