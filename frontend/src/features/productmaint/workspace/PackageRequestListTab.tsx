import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { productMaintApi } from '@/api/productmaint';
import type { RequestListItem } from '@/api/productmaint';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageFooter } from '@/components/ui/Pager';
import { useCompanyId } from '@/context/workspaceContext';
import { PACKAGE_REQUEST_LIST_COLUMNS } from '../requestColumns';

/**
 * The Package Request List tab (BDOI FRS FRPM.002.02): every package creation and update request
 * with its Reference Number, Request Type, Product Line, Account Officer, Assigned TSU User,
 * Submission Date and Current Status; search and pagination; a row opens the request.
 */
export function PackageRequestListTab() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const [text, setText] = useState('');
  const [page, setPage] = useState(0);
  const list = useQuery({
    queryKey: ['package-requests', companyId, 'landing', text, page],
    queryFn: () => productMaintApi.search(companyId, { text: text || undefined }, page),
    enabled: companyId > 0,
  });
  return (
    <>
      <WorklistToolbar
        placeholder="Search Request Number or Package"
        onSearch={(value) => {
          setText(value);
          setPage(0);
        }}
      />
      <ErrorAlert error={list.error} />
      <DataTable<RequestListItem>
        loading={list.isLoading}
        rows={list.data?.content ?? []}
        rowKey={(r) => r.id}
        onRowClick={(r) => void navigate(`/product-maintenance/requests/${r.id}`)}
        columns={PACKAGE_REQUEST_LIST_COLUMNS}
        emptyMessage="No package requests."
      />
      <PageFooter data={list.data} noun="package requests" onPage={setPage} />
    </>
  );
}
