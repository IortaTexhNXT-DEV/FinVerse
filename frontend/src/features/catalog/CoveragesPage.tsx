import { useQuery } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { catalogApi } from '@/api/catalog';
import { productCatalogApi } from '@/api/productCatalog';
import type { Clause, Coverage } from '@/api/productCatalog';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { formatDate, humanize } from '@/utils/format';
import { ClauseEditorModal, CoverageEditorModal } from './CoverageModals';
import { RecordActions } from './RecordActions';

const TABS = [
  { id: 'coverages', label: 'Coverages & Perils' },
  { id: 'clauses', label: 'Clause Library' },
] as const;

type TabId = (typeof TABS)[number]['id'];

const MAINTAINERS = ['PRODUCT_MAINTAIN'] as const;
const AUTHORIZERS = ['PRODUCT_AUTHORIZE'] as const;

function Coverages({ onEdit }: Readonly<{ onEdit: (c: Coverage) => void }>) {
  const [line, setLine] = useState('');
  const lines = useQuery({ queryKey: ['catalog', 'lines'], queryFn: catalogApi.lines });
  const coverages = useQuery({
    queryKey: ['catalog', 'coverages', line],
    queryFn: () => productCatalogApi.coverages(line || undefined),
  });
  return (
    <Card flush>
      <div className="form-grid" style={{ padding: 'var(--space-4)' }}>
        <Field label="Product line">
          {(id) => (
            <select
              id={id}
              className="select"
              value={line}
              onChange={(e) => setLine(e.target.value)}
            >
              <option value="">All lines</option>
              {(lines.data ?? []).map((l) => (
                <option key={l.code} value={l.code}>
                  {l.name}
                </option>
              ))}
            </select>
          )}
        </Field>
      </div>
      <ErrorAlert error={coverages.error} />
      <DataTable<Coverage>
        loading={coverages.isLoading}
        rows={coverages.data ?? []}
        rowKey={(c) => c.id}
        onRowClick={onEdit}
        emptyMessage="No coverage is set up for this line."
        columns={[
          { key: 'l', header: 'Line', render: (c) => c.lineCode },
          { key: 'c', header: 'Code', render: (c) => <strong>{c.code}</strong> },
          { key: 'n', header: 'Name', render: (c) => c.name },
          { key: 'k', header: 'Kind', render: (c) => humanize(c.kind) },
          { key: 'b', header: 'Basic', render: (c) => (c.basic ? 'Yes' : '') },
          { key: 's', header: 'Status', render: (c) => <StatusBadge status={c.recordStatus} /> },
          {
            key: 'a',
            header: 'Actions',
            render: (c) => (
              <RecordActions
                kind="COVERAGE"
                record={c}
                refresh={[['catalog', 'coverages']]}
                authorizers={AUTHORIZERS}
                maintainers={MAINTAINERS}
              />
            ),
          },
        ]}
      />
    </Card>
  );
}

function Clauses({ onEdit }: Readonly<{ onEdit: (c: Clause) => void }>) {
  const clauses = useQuery({
    queryKey: ['catalog', 'clauses'],
    queryFn: productCatalogApi.clauses,
  });
  return (
    <Card flush>
      <ErrorAlert error={clauses.error} />
      <DataTable<Clause>
        loading={clauses.isLoading}
        rows={clauses.data ?? []}
        rowKey={(c) => c.id}
        onRowClick={onEdit}
        emptyMessage="The clause library is empty."
        columns={[
          { key: 'c', header: 'Code', render: (c) => <strong>{c.code}</strong> },
          { key: 'k', header: 'Kind', render: (c) => humanize(c.kind) },
          { key: 't', header: 'Title', render: (c) => c.title },
          { key: 'l', header: 'Line', render: (c) => c.lineCode ?? 'All lines' },
          { key: 'f', header: 'Effective', render: (c) => formatDate(c.effectiveFrom) },
          { key: 'e', header: 'Until', render: (c) => formatDate(c.effectiveTo) },
          { key: 's', header: 'Status', render: (c) => <StatusBadge status={c.recordStatus} /> },
          {
            key: 'a',
            header: 'Actions',
            render: (c) => (
              <RecordActions
                kind="CLAUSE"
                record={c}
                refresh={[['catalog', 'clauses']]}
                authorizers={AUTHORIZERS}
                maintainers={MAINTAINERS}
              />
            ),
          },
        ]}
      />
    </Card>
  );
}

/**
 * Coverages & Clauses (PMADD01/02): the coverage / peril level of the product hierarchy per line
 * and the clause library (warranties, clauses, exclusions, deductible wordings) that package
 * versions use in their insurer terms. Changes wait for authorization.
 */
export default function CoveragesPage() {
  const { can } = useAuth();
  const [tab, setTab] = useState<TabId>('coverages');
  const [coverage, setCoverage] = useState<Coverage | 'new' | null>(null);
  const [clause, setClause] = useState<Clause | 'new' | null>(null);
  const maintain = can('PRODUCT_MAINTAIN');
  const edit =
    <T,>(set: (v: T) => void) =>
    (row: T) => {
      if (maintain) {
        set(row);
      }
    };
  return (
    <div className="stack">
      <PageHeader
        section="Product Maintenance"
        title="Coverages & Clauses"
        description="Coverages and perils per product line, and the clause library used in insurer terms."
        actions={
          maintain && (
            <Button
              variant="accent"
              icon={<Plus size={16} />}
              onClick={() => (tab === 'coverages' ? setCoverage('new') : setClause('new'))}
            >
              {tab === 'coverages' ? 'New Coverage' : 'New Clause'}
            </Button>
          )
        }
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'coverages' && <Coverages onEdit={edit(setCoverage)} />}
      {tab === 'clauses' && <Clauses onEdit={edit(setClause)} />}
      {coverage && (
        <CoverageEditorModal
          initial={coverage === 'new' ? undefined : coverage}
          onClose={() => setCoverage(null)}
        />
      )}
      {clause && (
        <ClauseEditorModal
          initial={clause === 'new' ? undefined : clause}
          onClose={() => setClause(null)}
        />
      )}
    </div>
  );
}
