import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { deactivationApi } from '@/api/pmWorkspace';
import type { Deactivation, DeactivationStatus } from '@/api/pmWorkspace';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Card } from '@/components/ui/Card';
import { Combobox } from '@/components/ui/Combobox';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { DeactivationDetail } from './DeactivationDetail';

const STATUSES = [
  { value: 'PENDING', label: 'Pending Approval' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'CANCELLED', label: 'Cancelled' },
];

function columns(open: (id: number) => void): Column<Deactivation>[] {
  return [
    { key: 'no', header: 'Request Number', render: (r) => <ReferenceChip value={r.requestNo} /> },
    {
      key: 'pkg',
      header: 'Package Name',
      render: (r) => (
        <>
          <strong>{r.packageName}</strong>
          <div className="muted">{r.productCode}</div>
        </>
      ),
    },
    {
      key: 'eff',
      header: 'Deactivation Effective Date',
      render: (r) => formatDate(r.effectiveDate),
    },
    {
      key: 'status',
      header: 'Approval Status',
      render: (r) => <StatusBadge status={r.status} label={r.statusLabel} full />,
    },
    { key: 'by', header: 'Requested By', render: (r) => <UserName login={r.requestedBy} /> },
    { key: 'on', header: 'Request Date', render: (r) => formatDate(r.requestedAt.slice(0, 10)) },
    {
      key: 'exp',
      header: 'Package Expiry Date',
      render: (r) => formatDate(r.expiryDate ?? r.packageExpiryDate),
    },
    {
      key: 'actions',
      header: 'Actions',
      kind: 'actions',
      render: (r) => (
        <RowActionMenu
          label={r.requestNo}
          actions={[{ label: 'View Details', onSelect: () => open(r.id) }]}
        />
      ),
    },
  ];
}

/**
 * Deactivation Requests (BDOI FRS FRPM.003.06): the package deactivation requests with search and
 * filters by approval status and request date; a request opens its details, the supporting
 * documents to download and, for the approver, the approval.
 */
export default function DeactivationsPage() {
  const companyId = useCompanyId();
  const [params, setParams] = useSearchParams();
  const [text, setText] = useState('');
  const [status, setStatus] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [page, setPage] = useState(0);
  const openId = Number(params.get('open') ?? '0');
  const filters = {
    text: text || undefined,
    status: (status || undefined) as DeactivationStatus | undefined,
    from: from || undefined,
    to: to || undefined,
  };
  const list = useQuery({
    queryKey: ['pm-deactivations', companyId, filters, page],
    queryFn: () => deactivationApi.list(companyId, filters, page),
    enabled: companyId > 0,
  });
  const open = (id: number) => setParams({ open: String(id) });
  return (
    <div className="stack">
      <PageHeader
        section="Product Maintenance"
        title="Deactivation Requests"
        description="Requests to deactivate packages, their approval and the new expiry date."
      />
      <Card flush>
        <WorklistToolbar
          placeholder="Search Request Number or Package"
          onSearch={(value) => {
            setText(value);
            setPage(0);
          }}
        />
        <div className="worklist-filters form-grid">
          <Field label="Approval Status">
            {(id) => (
              <Combobox
                id={id}
                emptyLabel="All"
                value={status}
                options={STATUSES}
                onChange={(v) => {
                  setStatus(v);
                  setPage(0);
                }}
              />
            )}
          </Field>
          <Field label="Requested From">
            {(id) => <DateInput id={id} value={from} onChange={(e) => setFrom(e.target.value)} />}
          </Field>
          <Field label="Requested To">
            {(id) => <DateInput id={id} value={to} onChange={(e) => setTo(e.target.value)} />}
          </Field>
        </div>
        <ErrorAlert error={list.error} />
        <DataTable<Deactivation>
          loading={list.isLoading}
          rows={list.data?.content ?? []}
          rowKey={(r) => r.id}
          onRowClick={(r) => open(r.id)}
          columns={columns(open)}
          emptyMessage="No deactivation requests."
        />
        <PageFooter data={list.data} noun="deactivation requests" onPage={setPage} />
      </Card>
      {openId > 0 && <DeactivationDetail id={openId} onClose={() => setParams({})} />}
    </div>
  );
}
