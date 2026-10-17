import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { CodeSelect, InsurerField, TextField } from './CashFields';
import type { UnappliedFilters, UnappliedRow, UnappliedType } from './unappliedInquiryApi';
import {
  UNAPPLIED_TYPE_LABELS,
  filtersFromSearch,
  pnAndLoan,
  unappliedInquiryApi,
} from './unappliedInquiryApi';
import './cashiering.css';

const COLUMNS: Column<UnappliedRow>[] = [
  { key: 'source', header: 'Source', width: '190px', render: (u) => u.source },
  { key: 'type', header: 'Unapplied Payment Type', render: (u) => UNAPPLIED_TYPE_LABELS[u.type] },
  { key: 'no', header: 'Unapplied Payment No.', render: (u) => <strong>{u.reference}</strong> },
  { key: 'date', header: 'Payment/Creation Date', render: (u) => formatDate(u.paidOn) },
  { key: 'account', header: 'Account No.', render: (u) => u.account.accountNo ?? '' },
  { key: 'pn', header: 'PN / Loan Application No.', render: (u) => pnAndLoan(u) },
  {
    key: 'client',
    header: 'Client Name',
    render: (u) => <CellStack main={u.parties.clientName ?? ''} sub={u.parties.assured} />,
  },
  { key: 'insurer', header: 'Insurer Name', render: (u) => u.parties.insurerName ?? '' },
  {
    key: 'amount',
    header: 'Unapplied Payment Amount',
    numeric: true,
    render: (u) => <Amount value={u.amount} />,
  },
  {
    key: 'balance',
    header: 'Unapplied Payment Outstanding',
    numeric: true,
    render: (u) => <Amount value={u.outstanding} />,
  },
  {
    key: 'status',
    header: 'Unapplied Payment Status',
    render: (u) => <StatusBadge status={u.status} />,
  },
];

function Filters({
  f,
  onChange,
}: Readonly<{ f: UnappliedFilters; onChange: (f: UnappliedFilters) => void }>) {
  const text = (key: keyof UnappliedFilters, label: string, type: 'text' | 'date' = 'text') => (
    <TextField
      label={label}
      type={type}
      value={f[key]}
      onChange={(v) => onChange({ ...f, [key]: v })}
    />
  );
  return (
    <div className="worklist-filters csh-filters">
      <CodeSelect
        label="Unapplied Payment Type"
        value={f.type}
        options={Object.keys(UNAPPLIED_TYPE_LABELS)}
        empty="All types"
        labelOf={(t) => UNAPPLIED_TYPE_LABELS[t as UnappliedType]}
        onChange={(type) => onChange({ ...f, type: type as UnappliedType | '' })}
      />
      {text('from', 'Payment Date From', 'date')}
      {text('to', 'Payment Date To', 'date')}
      <InsurerField value={f.insurer} onChange={(insurer) => onChange({ ...f, insurer })} />
      {text('client', 'Client Code')}
      {text('assured', "Assured's Name")}
      {text('account', 'Account Reference Number')}
      {text('q', 'Unapplied Payment No., Reference or Payor')}
    </div>
  );
}

/**
 * Unapplied Payment List (FRS.CSH.06.01.01 to 06.01.07): every unapplied payment with an
 * outstanding amount, by type, payment date, insurer, client, assured, account and a partial text,
 * with its source, account, PN and loan application numbers, client, insurer, amount, outstanding
 * and status. Marketing users see the payments of their marketing unit.
 */
export default function UnappliedInquiryPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const location = useLocation();
  const [filters, setFilters] = useState<UnappliedFilters>(() =>
    filtersFromSearch(location.search),
  );
  const [applied, setApplied] = useState<UnappliedFilters>(filters);
  const [page, setPage] = useState(0);
  const list = useQuery({
    queryKey: ['cashiering', 'unapplied-inquiry', companyId, applied, page],
    queryFn: () => unappliedInquiryApi.search(companyId, applied, page),
    enabled: companyId > 0,
  });
  return (
    <div className="stack">
      <PageHeader
        section="Cashiering"
        title="Unapplied Payment List"
        description="Unapplied payments with an outstanding amount, by type, date, insurer, client and account."
      />
      <ErrorAlert error={list.error} />
      <Card flush>
        <Filters f={filters} onChange={setFilters} />
        <div className="worklist-toolbar">
          <Button
            variant="secondary"
            onClick={() => {
              setApplied(filters);
              setPage(0);
            }}
          >
            Search
          </Button>
        </div>
        <DataTable
          caption="Unapplied payments"
          list="cashiering-unapplied-inquiry"
          columns={COLUMNS}
          rows={list.data?.content ?? []}
          rowKey={(u) => u.id}
          loading={list.isLoading}
          emptyMessage="No unapplied payment matches the criteria"
          onRowClick={(u) => void navigate(`/cashiering/unapplied/${u.id}`)}
        />
        <PageFooter data={list.data} noun="payments" onPage={setPage} />
      </Card>
    </div>
  );
}
