import { useQuery } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { quotationsApi } from '@/api/quotations';
import type { QuotationListItem } from '@/api/quotations';
import { useAuth } from '@/auth/authContext';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageFooter } from '@/components/ui/Pager';
import { useCompanyId } from '@/context/workspaceContext';
import { QUOTATION_COLUMNS } from './quotationColumns';

/** Columns of the client page list: the client is known, so it is not repeated. */
const COLUMNS = QUOTATION_COLUMNS.filter((c) => c.key !== 'client');

/** The quotations of a client (BDOI Client Record Details, tab Quotation). */
export function ClientQuotationsTab({ clientId }: Readonly<{ clientId: number }>) {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const { can } = useAuth();
  const [page, setPage] = useState(0);
  const list = useQuery({
    queryKey: ['quotations', companyId, { clientId }, page],
    queryFn: () => quotationsApi.search(companyId, { clientId }, page, 10),
    enabled: companyId > 0,
  });
  return (
    <Card
      title="Quotations"
      flush
      actions={
        can('QUOTE_MAINTAIN') && (
          <Link className="btn btn-secondary btn-sm" to={`/quotations/new?client=${clientId}`}>
            <Plus size={14} aria-hidden="true" /> Generate Quotation
          </Link>
        )
      }
    >
      <ErrorAlert error={list.error} />
      <DataTable<QuotationListItem>
        loading={list.isLoading}
        rows={list.data?.content ?? []}
        rowKey={(q) => q.id}
        onRowClick={(q) => void navigate(`/quotations/${q.id}`)}
        emptyMessage="No quotation for this client yet."
        columns={COLUMNS}
      />
      <PageFooter data={list.data} noun="quotations" onPage={setPage} />
    </Card>
  );
}
