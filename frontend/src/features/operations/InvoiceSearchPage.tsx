import { useQuery } from '@tanstack/react-query';
import { Filter, Search } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { opsApi } from '@/api/operations';
import type { InvoiceSearch } from '@/api/operations';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { OriginFilter } from '@/components/ui/OriginFilter';
import { DateInput } from '@/components/ui/DateInput';
import { InsurerSelect } from '@/components/broking/InsurerSelect';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { INVOICE_COLUMNS } from './invoiceColumns';
import {
  FILTER_FIELDS,
  SEARCH_TABS,
  activeFilterCount,
  keptFilters,
  orUndefined,
  searchFromParams,
  tabFilter,
  tabOf,
} from './invoiceSearch';
import type { SearchTab } from './invoiceSearch';

type FilterKey = (typeof FILTER_FIELDS)[number]['key'];

function Filters({
  search,
  onApply,
}: Readonly<{ search: InvoiceSearch; onApply: (patch: InvoiceSearch) => void }>) {
  const [values, setValues] = useState<Record<FilterKey, string>>(() => {
    const initial = {} as Record<FilterKey, string>;
    FILTER_FIELDS.forEach((f) => {
      initial[f.key] = search[f.key] ?? '';
    });
    return initial;
  });
  return (
    <form
      className="ops-toolbar"
      onSubmit={(e) => {
        e.preventDefault();
        const patch: InvoiceSearch = {};
        FILTER_FIELDS.forEach((f) => {
          patch[f.key] = orUndefined(values[f.key]);
        });
        onApply(patch);
      }}
    >
      {FILTER_FIELDS.map((f) => (
        <Field key={f.key} label={f.label}>
          {(id) => {
            if (f.key === 'insurer') {
              return (
                <InsurerSelect
                  id={id}
                  value={values.insurer}
                  placeholder="All insurers"
                  onChange={(code) => setValues({ ...values, insurer: code })}
                />
              );
            }
            return f.date ? (
              <DateInput
                id={id}
                value={values[f.key]}
                onChange={(e) => setValues({ ...values, [f.key]: e.target.value })}
              />
            ) : (
              <input
                id={id}
                className="input"
                value={values[f.key]}
                onChange={(e) => setValues({ ...values, [f.key]: e.target.value })}
              />
            );
          }}
        </Field>
      ))}
      <Button type="submit" variant="secondary">
        Apply Filters
      </Button>
    </form>
  );
}

/**
 * Invoice search of the Operations ledger (RMTID.026, ADJID.024, DIS 3.27.2): every booked invoice
 * and endorsement with its outstanding premium, payment and remittance status and flags, filtered by
 * insurer, assured, account officer, booking and inception dates and origin (open legacy invoices
 * migrated at cut-over carry a LEGACY badge); open one for its invoice 360.
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
  const keep = keptFilters(search);
  const filters = activeFilterCount(search);
  return (
    <div className="stack">
      <PageHeader
        section="Operations"
        title="Invoice Search"
        description="Booked invoices and endorsements in the Operations ledger with their outstanding premium."
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
              placeholder="Search Invoice No., ARN, policy, client or assured"
              value={text}
              onChange={(e) => setText(e.target.value)}
            />
            <Button type="submit" icon={<Search size={14} />}>
              Search
            </Button>
            <OriginFilter
              value={search.origin}
              onChange={(origin) => apply({ ...search, origin })}
            />
            <Button
              variant="ghost"
              icon={<Filter size={14} />}
              onClick={() => setShowFilters(!showFilters)}
            >
              {filters > 0 ? `Filters (${filters})` : 'Filters'}
            </Button>
          </form>
          {showFilters && (
            <Filters search={search} onApply={(patch) => apply({ ...search, ...patch })} />
          )}
          <DataTable
            caption="Operations invoices"
            columns={INVOICE_COLUMNS}
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
