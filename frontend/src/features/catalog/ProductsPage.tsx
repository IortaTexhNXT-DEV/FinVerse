import { useQuery } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { catalogApi } from '@/api/catalog';
import type { Product, ProductFilter } from '@/api/catalog';
import { useAuth } from '@/auth/authContext';
import { LovSelect } from '@/components/broking/LovSelect';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { CatalogRulesPanel } from './CatalogRulesPanel';
import { ProductEditorModal } from './ProductEditorModal';
import { newProductForm } from './productForm';
import type { ProductForm } from './productForm';
import { RecordActions } from './RecordActions';

const TABS = [
  { id: 'products', label: 'Products' },
  { id: 'fields', label: 'Field rules' },
  { id: 'documents', label: 'Document rules' },
  { id: 'tsu', label: 'TSU routing' },
] as const;

type TabId = (typeof TABS)[number]['id'];

function ProductFilters({
  filter,
  onChange,
}: Readonly<{ filter: ProductFilter; onChange: (f: ProductFilter) => void }>) {
  const lines = useQuery({ queryKey: ['catalog', 'lines'], queryFn: catalogApi.lines });
  let packagedValue = '';
  if (filter.packaged !== undefined) {
    packagedValue = filter.packaged ? 'yes' : 'no';
  }
  return (
    <div className="form-grid" style={{ padding: 'var(--space-4)' }}>
      <Field label="Search">
        {(id) => (
          <input
            id={id}
            className="input"
            placeholder="Code or name"
            value={filter.q ?? ''}
            onChange={(e) => onChange({ ...filter, q: e.target.value })}
          />
        )}
      </Field>
      <Field label="Product line">
        {(id) => (
          <select
            id={id}
            className="select"
            value={filter.line ?? ''}
            onChange={(e) => onChange({ ...filter, line: e.target.value || undefined })}
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
      <Field label="Package">
        {(id) => (
          <select
            id={id}
            className="select"
            value={packagedValue}
            onChange={(e) =>
              onChange({
                ...filter,
                packaged: e.target.value === '' ? undefined : e.target.value === 'yes',
              })
            }
          >
            <option value="">Package and non-package</option>
            <option value="yes">Package only</option>
            <option value="no">Non-package only</option>
          </select>
        )}
      </Field>
      <Field label="Market segment">
        {(id) => (
          <LovSelect
            id={id}
            type="MARKET_SEGMENT"
            placeholder="All segments"
            value={filter.segment ?? ''}
            onChange={(segment) => onChange({ ...filter, segment: segment || undefined })}
          />
        )}
      </Field>
      <label className="checkbox" style={{ alignSelf: 'end' }}>
        <input
          type="checkbox"
          checked={filter.activeOnly === true}
          onChange={(e) => onChange({ ...filter, activeOnly: e.target.checked })}
        />
        Active only
      </label>
    </div>
  );
}

function ProductList() {
  const navigate = useNavigate();
  const [filter, setFilter] = useState<ProductFilter>({});
  const products = useQuery({
    queryKey: ['catalog', 'products', filter],
    queryFn: () => catalogApi.products(filter),
  });
  return (
    <Card flush>
      <ProductFilters filter={filter} onChange={setFilter} />
      <ErrorAlert error={products.error} />
      <DataTable<Product>
        loading={products.isLoading}
        rows={products.data ?? []}
        rowKey={(p) => p.id}
        onRowClick={(p) => void navigate(`/catalog/products/${p.code}`)}
        emptyMessage="No product matches the filters."
        columns={[
          { key: 'c', header: 'Code', render: (p) => <strong>{p.code}</strong> },
          { key: 'n', header: 'Name', render: (p) => p.name },
          { key: 'l', header: 'Line', render: (p) => p.lineCode },
          { key: 'p', header: 'Package', render: (p) => (p.packaged ? 'Yes' : '') },
          {
            key: 's',
            header: 'Segments',
            render: (p) => (p.marketSegments.length ? p.marketSegments.join(', ') : 'All'),
          },
          { key: 'r', header: 'Rate %', numeric: true, render: (p) => p.defaultRate ?? '' },
          {
            key: 'm',
            header: 'Min. Premium',
            numeric: true,
            render: (p) => <Amount value={p.minimumPremium} />,
          },
          { key: 'st', header: 'Status', render: (p) => <StatusBadge status={p.recordStatus} /> },
          {
            key: 'a',
            header: 'Actions',
            render: (p) => (
              <RecordActions kind="PRODUCT" record={p} refresh={[['catalog', 'products']]} />
            ),
          },
        ]}
      />
    </Card>
  );
}

/**
 * Product catalog (BRNB.001-004): lines, cover types and products with their features, the
 * field and document rules that make an account complete, and the TSU routing rules.
 */
export default function ProductsPage() {
  const { can } = useAuth();
  const [tab, setTab] = useState<TabId>('products');
  const [editing, setEditing] = useState<ProductForm | null>(null);
  return (
    <div className="stack">
      <PageHeader
        section="Product Maintenance"
        title="Products"
        description="Products offered to clients, the data and documents each requires, and when TSU reviews an account."
        actions={
          can('MASTER_MAINTAIN') && (
            <Button
              variant="accent"
              icon={<Plus size={16} />}
              onClick={() => setEditing(newProductForm())}
            >
              New Product
            </Button>
          )
        }
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'products' && <ProductList />}
      {tab !== 'products' && <CatalogRulesPanel kind={tab} />}
      {editing && <ProductEditorModal initial={editing} onClose={() => setEditing(null)} />}
    </div>
  );
}
