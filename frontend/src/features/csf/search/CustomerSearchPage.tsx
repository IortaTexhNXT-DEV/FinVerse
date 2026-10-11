import { useQuery } from '@tanstack/react-query';
import { Search } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import type { SyntheticEvent } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { csfApi } from '@/api/csf';
import type { ClientHit, LegacyAccount, SearchKeyType, SearchResult } from '@/api/csf';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Notice } from '@/components/ui/Notice';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';
import { CSF_SECTION, SEARCH_KEYS, searchKeyLabel, searchRefusal } from '../csfCodes';
import { AccountsTable } from '../view/AccountsTab';

const CLIENT_COLUMNS: Column<ClientHit>[] = [
  {
    key: 'client',
    header: 'Client',
    render: (c) => <CellStack main={c.name} sub={c.code} />,
  },
  {
    key: 'type',
    header: 'Type',
    render: (c) => (c.clientType === 'CORPORATE' ? 'Corporate' : 'Individual'),
  },
  {
    key: 'status',
    header: 'Status',
    kind: 'status',
    render: (c) => <StatusBadge status={c.status} />,
  },
  { key: 'mobile', header: 'Mobile', render: (c) => c.mobile ?? '' },
  { key: 'email', header: 'E-mail', render: (c) => c.email ?? '' },
  { key: 'city', header: 'City', render: (c) => c.city ?? '' },
  {
    key: 'accounts',
    header: 'Matching Accounts',
    kind: 'center',
    render: (c) => (c.accounts.length === 0 ? '' : String(c.accounts.length)),
  },
];

const LEGACY_COLUMNS: Column<LegacyAccount>[] = [
  { key: 'source', header: 'System', render: (l) => l.source },
  { key: 'reference', header: 'Reference', kind: 'code', render: (l) => l.reference },
  { key: 'client', header: 'Client', render: (l) => l.clientName },
  { key: 'description', header: 'Description', render: (l) => l.description },
];

function isKey(value: string | null): value is SearchKeyType {
  return SEARCH_KEYS.some((k) => k.key === value);
}

/** The key type and value of a search; refuses a blank value and a name under 3 characters. */
function SearchForm({
  initialKey,
  initialValue,
  onSearch,
}: Readonly<{
  initialKey: SearchKeyType;
  initialValue: string;
  onSearch: (key: SearchKeyType, value: string) => void;
}>) {
  const [key, setKey] = useState<SearchKeyType>(initialKey);
  const [value, setValue] = useState(initialValue);
  const [error, setError] = useState<string>();
  const submit = (e: SyntheticEvent) => {
    e.preventDefault();
    const text = value.trim();
    const refusal = searchRefusal(key, text);
    setError(refusal);
    if (refusal === undefined) {
      onSearch(key, text);
    }
  };
  const placeholder = SEARCH_KEYS.find((k) => k.key === key)?.placeholder ?? '';
  return (
    <form className="filter-bar" onSubmit={submit} aria-label="Customer search">
      <Field label="Search By">
        {(id) => (
          <select
            id={id}
            className="select"
            value={key}
            onChange={(e) => setKey(e.target.value as SearchKeyType)}
          >
            {SEARCH_KEYS.map((k) => (
              <option key={k.key} value={k.key}>
                {k.label}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Field label="Value" error={error}>
        {(id) => (
          <input
            id={id}
            className="input"
            placeholder={placeholder}
            value={value}
            aria-invalid={error !== undefined}
            onChange={(e) => setValue(e.target.value)}
          />
        )}
      </Field>
      <div className="filter-bar-actions">
        <Button type="submit" variant="accent" icon={<Search size={16} aria-hidden="true" />}>
          Search
        </Button>
      </div>
    </form>
  );
}

/** Clients found with their matching accounts, the legacy accounts, or "No client found". */
function SearchResults({ data }: Readonly<{ data: SearchResult }>) {
  const navigate = useNavigate();
  if (data.clients.length === 0 && data.legacy.length === 0) {
    return (
      <Card>
        <EmptyState
          message={`No client found for ${searchKeyLabel(data.keyType)}: ${data.value}`}
        />
      </Card>
    );
  }
  const expanded = new Set(data.clients.filter((c) => c.accounts.length > 0).map((c) => c.id));
  return (
    <>
      {data.truncated && (
        <Notice tone="warning" title="Refine the search">
          More than {data.max} clients match. Refine the search.
        </Notice>
      )}
      {data.clients.length > 0 && (
        <Card title={`Clients (${String(data.clients.length)})`} flush>
          <DataTable
            columns={CLIENT_COLUMNS}
            rows={data.clients}
            rowKey={(c) => c.id}
            onRowClick={(c) => void navigate(`/csf/clients/${String(c.id)}`)}
            expanded={expanded}
            renderExpanded={(c) => <AccountsTable rows={c.accounts} compact />}
            caption="Clients found"
          />
        </Card>
      )}
      {data.legacy.length > 0 && (
        <Card title="Accounts in the Legacy Systems" flush>
          <DataTable
            columns={LEGACY_COLUMNS}
            rows={data.legacy}
            rowKey={(l) => `${l.source}-${l.reference}`}
          />
        </Card>
      )}
    </>
  );
}

/**
 * Customer Search (FR-CSF-010; BRCSF-003 / 3.001): one search box by key type - name, client ID,
 * account number, PN number or application number - with the results grouped by client and the
 * matching accounts under each client. One match opens its Servicing View at once; no match says
 * so with the criteria. The landing page of the contact centre roles.
 */
export default function CustomerSearchPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const urlKey = params.get('key');
  const key: SearchKeyType = isKey(urlKey) ? urlKey : 'NAME';
  const q = params.get('q') ?? '';
  const submitted = useRef(false);
  const result = useQuery({
    queryKey: ['csf', 'search', companyId, key, q],
    queryFn: () => csfApi.search(companyId, key, q),
    enabled: q.trim() !== '',
  });
  const data = result.data;
  useEffect(() => {
    const only = data?.clients.length === 1 ? data.clients.at(0) : undefined;
    if (submitted.current && only && data?.legacy.length === 0) {
      submitted.current = false;
      void navigate(`/csf/clients/${String(only.id)}`);
    }
  }, [data, navigate]);
  return (
    <div className="stack">
      <PageHeader
        section={CSF_SECTION}
        title="Customer Search"
        description="Find the client of a caller by name, client ID, account, PN or application number."
      />
      <Card>
        <SearchForm
          initialKey={key}
          initialValue={q}
          onSearch={(k, v) => {
            submitted.current = true;
            setParams({ key: k, q: v }, { replace: true });
          }}
        />
      </Card>
      <ErrorAlert error={result.error} onRetry={() => void result.refetch()} />
      {result.isLoading && q !== '' && <span className="spinner" aria-label="Loading" />}
      {q !== '' && data && <SearchResults data={data} />}
    </div>
  );
}
