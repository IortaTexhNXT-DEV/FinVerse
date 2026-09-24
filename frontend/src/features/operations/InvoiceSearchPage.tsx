import { useQuery } from '@tanstack/react-query';
import { Filter, Search } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { opsApi } from '@/api/operations';
import type { InvoiceSearch, OpsInvoiceSummary } from '@/api/operations';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { FlagChips } from './OpsParts';
import { SEARCH_TABS, orUndefined, searchFromParams, tabFilter, tabOf } from './invoiceSearch';
import type { SearchTab } from './invoiceSearch';

const COLUMNS: Column<OpsInvoiceSummary>[] = [
  {
    key: 'no',
    header: 'Invoice No.',
    render: (i) => (
      <>
        <strong>{i.invoiceNo}</strong>
        <div className="ops-muted">{i.arn}</div>
      </>
    ),
  },
  {
    key: 'client',
    header: 'Assured / Client Code',
    render: (i) => (
      <>
        {i.assuredName}
        <div className="ops-muted">{i.clientCode}</div>
      </>
    ),
  },
  { key: 'insurer', header: 'Insurer', render: (i) => i.insurerCode },
  { key: 'date', header: 'Booking Date', render: (i) => formatDate(i.bookingDate) },
  {
    key: 'gross',
    header: 'Gross Premium',
    numeric: true,
    render: (i) => <Amount value={i.grossPremium} />,
  },
  {
    key: 'balance',
    header: 'Outstanding',
    numeric: true,
    render: (i) => <Amount value={i.premiumBalance} />,
  },
  { key: 'status', header: 'Status', render: (i) => <StatusBadge status={i.paymentStatus} /> },
  {
    key: 'remit',
    header: 'Remittance',
    render: (i) => <StatusBadge status={i.remittanceStatus} />,
  },
  { key: 'flags', header: 'Flags', render: (i) => <FlagChips flags={i.flags} /> },
];

function Filters({
  search,
  onApply,
}: Readonly<{ search: InvoiceSearch; onApply: (patch: InvoiceSearch) => void }>) {
  const [insurer, setInsurer] = useState(search.insurer ?? '');
  const [from, setFrom] = useState(search.from ?? '');
  const [to, setTo] = useState(search.to ?? '');
  return (
    <form
      className="ops-toolbar"
      onSubmit={(e) => {
        e.preventDefault();
        onApply({ insurer: orUndefined(insurer), from: orUndefined(from), to: orUndefined(to) });
      }}
    >
      <Field label="Insurer Code">
        {(id) => (
          <input
            id={id}
            className="input"
            value={insurer}
            onChange={(e) => setInsurer(e.target.value)}
          />
        )}
      </Field>
      <Field label="Booked From">
        {(id) => (
          <input
            id={id}
            type="date"
            className="input"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
          />
        )}
      </Field>
      <Field label="Booked To">
        {(id) => (
          <input
            id={id}
            type="date"
            className="input"
            value={to}
            onChange={(e) => setTo(e.target.value)}
          />
        )}
      </Field>
      <Button type="submit" variant="secondary">
        Apply Filters
      </Button>
    </form>
  );
}

/**
 * Invoice search of the Operations ledger (RMTID.026, ADJID.024): every booked invoice and
 * endorsement with its outstanding premium, payment and remittance status and flags; open one for
 * its invoice 360.
 */
export default function InvoiceSearchPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [search, setSearch] = useState<InvoiceSearch>(() => searchFromParams(params));
  const [text, setText] = useState(search.q ?? '');
  const [showFilters, setShowFilters] = useState(false);
  const [page, setPage] = useState(0);
  const tab = tabOf(search);
  const rows = useQuery({
    queryKey: ['ops', 'invoices', companyId, search, page],
    queryFn: () => opsApi.invoices(companyId, search, page),
    enabled: companyId > 0,
  });
  const apply = (next: InvoiceSearch) => {
    setSearch(next);
    setPage(0);
  };
  const keep = { q: search.q, insurer: search.insurer, from: search.from, to: search.to };
  return (
    <div className="stack">
      <PageHeader
        section="Operations"
        title="Invoice Search"
        description="Booked invoices and endorsements in the Operations ledger with their outstanding premium, payment and remittance status."
      />
      <ErrorAlert error={rows.error} />
      <Card>
        <div className="stack">
          <Tabs<SearchTab>
            tabs={SEARCH_TABS}
            active={tab}
            onChange={(t) => apply({ ...keep, ...tabFilter(t) })}
          />
          <form
            className="ops-toolbar"
            onSubmit={(e) => {
              e.preventDefault();
              apply({ ...search, q: orUndefined(text) });
            }}
          >
            <label className="visually-hidden" htmlFor="ops-invoice-search">
              Search Invoice No.
            </label>
            <input
              id="ops-invoice-search"
              className="input"
              placeholder="Search Invoice No., ARN, policy or client"
              value={text}
              onChange={(e) => setText(e.target.value)}
            />
            <Button type="submit" icon={<Search size={14} />}>
              Search
            </Button>
            <Button
              variant="ghost"
              icon={<Filter size={14} />}
              onClick={() => setShowFilters(!showFilters)}
            >
              Filters
            </Button>
          </form>
          {showFilters && (
            <Filters search={search} onApply={(patch) => apply({ ...search, ...patch })} />
          )}
          <DataTable
            caption="Operations invoices"
            columns={COLUMNS}
            rows={rows.data?.content ?? []}
            rowKey={(i) => i.invoiceNo}
            loading={rows.isLoading}
            emptyMessage="No items to display"
            onRowClick={(i) =>
              void navigate(`/operations/invoices/${encodeURIComponent(i.invoiceNo)}`)
            }
          />
          <PageFooter data={rows.data} noun="invoices" onPage={setPage} />
        </div>
      </Card>
    </div>
  );
}
