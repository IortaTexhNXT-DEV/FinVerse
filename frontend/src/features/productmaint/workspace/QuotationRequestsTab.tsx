import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { pmWorkspaceApi } from '@/api/pmWorkspace';
import type { QuotationRow } from '@/api/pmWorkspace';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { DataTable } from '@/components/ui/DataTable';
import type { Column, SortState } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageFooter } from '@/components/ui/Pager';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { LineLabel } from '@/components/broking/LovLabel';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';

const DEFAULT_SORT: SortState = { key: 'requestDate', direction: 'desc' };

function columns(open: (r: QuotationRow, edit: boolean) => void): Column<QuotationRow>[] {
  return [
    {
      key: 'no',
      header: 'Quotation Request Number',
      sortKey: 'requestNo',
      render: (r) => (
        <>
          <ReferenceChip value={r.requestNo} />
          <div className="muted">{formatDate(r.requestDate)}</div>
        </>
      ),
    },
    { key: 'ref', header: 'Related Reference Number', render: (r) => r.referenceNo ?? '' },
    { key: 'biz', header: 'Business Type', render: (r) => r.businessType },
    { key: 'assured', header: "Assured's Name", sortKey: 'assured', render: (r) => r.assuredName },
    {
      key: 'line',
      header: 'Product Line',
      sortKey: 'line',
      render: (r) => <LineLabel code={r.lineCode} />,
    },
    { key: 'ao', header: 'Account Officer', render: (r) => <UserName login={r.accountOfficer} /> },
    {
      key: 'tsu',
      header: 'Assigned TSU User',
      render: (r) => <UserName login={r.tsuUser} empty="Unassigned" />,
    },
    {
      key: 'submitted',
      header: 'Submission Date',
      sortKey: 'submittedAt',
      render: (r) => formatDate(r.submittedAt?.slice(0, 10)),
    },
    {
      key: 'period',
      header: 'Effective / Expiry Date',
      render: (r) =>
        r.effectiveDate === null
          ? ''
          : `${formatDate(r.effectiveDate)} – ${formatDate(r.expiryDate)}`,
    },
    {
      key: 'status',
      header: 'Current Status',
      sortKey: 'status',
      render: (r) => <StatusBadge status={r.statusCode} label={r.status} full />,
    },
    {
      key: 'aging',
      header: 'Aging (Days)',
      sortKey: 'aging',
      numeric: true,
      render: (r) => (r.agingDays === null ? '' : String(r.agingDays)),
    },
    {
      key: 'actions',
      header: 'Actions',
      kind: 'actions',
      render: (r) => (
        <RowActionMenu
          label={r.requestNo}
          actions={[
            { label: 'View Quotation Request', onSelect: () => open(r, false) },
            {
              label: 'Update Quotation Request Details',
              disabled: r.statusCode !== 'DRAFT',
              onSelect: () => open(r, true),
            },
          ]}
        />
      ),
    },
  ];
}

/**
 * The Quotation Request List tab (BDOI FRS FRPM.002.02 and FRPM.005.01): the quotation requests of
 * non-package products raised by Marketing with their status in BDOI's words and the aging; search,
 * sort and pagination; a row opens the quotation request.
 */
export function QuotationRequestsTab() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const [text, setText] = useState('');
  const [onlyOpen, setOnlyOpen] = useState(true);
  const [sort, setSort] = useState<SortState>(DEFAULT_SORT);
  const [page, setPage] = useState(0);
  const filters = {
    text: text || undefined,
    open: onlyOpen ? true : undefined,
    sort: sort.key,
    direction: sort.direction,
  };
  const list = useQuery({
    queryKey: ['pm-quotations', companyId, filters, page],
    queryFn: () => pmWorkspaceApi.quotationRequests(companyId, filters, page),
    enabled: companyId > 0,
  });
  const open = (r: QuotationRow, edit: boolean) =>
    void navigate(edit ? `/proposals/${r.id}/edit` : `/proposals/${r.id}`);
  return (
    <>
      <WorklistToolbar
        placeholder="Search Request Number or Assured"
        onSearch={(value) => {
          setText(value);
          setPage(0);
        }}
        extra={
          <label className="checkbox">
            <input
              type="checkbox"
              checked={onlyOpen}
              onChange={(e) => {
                setOnlyOpen(e.target.checked);
                setPage(0);
              }}
            />
            Open Requests Only
          </label>
        }
      />
      <ErrorAlert error={list.error} />
      <DataTable<QuotationRow>
        loading={list.isLoading}
        rows={list.data?.content ?? []}
        rowKey={(r) => r.id}
        sort={sort}
        onSort={(next) => {
          setSort(next);
          setPage(0);
        }}
        onRowClick={(r) => open(r, false)}
        columns={columns(open)}
        emptyMessage="No quotation requests."
      />
      <PageFooter data={list.data} noun="quotation requests" onPage={setPage} />
    </>
  );
}
