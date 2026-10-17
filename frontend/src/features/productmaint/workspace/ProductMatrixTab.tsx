import { useQuery } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { catalogApi } from '@/api/catalog';
import { deactivationApi, matrixExportParams, pmWorkspaceApi } from '@/api/pmWorkspace';
import type { MatrixFilters, MatrixRow, MatrixTab } from '@/api/pmWorkspace';
import { useAuth } from '@/auth/authContext';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Button } from '@/components/ui/Button';
import { Combobox } from '@/components/ui/Combobox';
import { DataTable } from '@/components/ui/DataTable';
import type { SortState } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageFooter } from '@/components/ui/Pager';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { ProductEditorModal } from '@/features/catalog/ProductEditorModal';
import { newProductForm } from '@/features/catalog/productForm';
import type { ProductForm } from '@/features/catalog/productForm';
import { DeactivateDialog } from './DeactivateDialog';
import { ExportButtons } from './ExportButtons';
import { matrixColumns } from './matrixColumns';
import type { MatrixActions } from './matrixColumns';
import { PACKAGE_TYPES, packagedFlag } from './pmDashboard';

const DEFAULT_SORT: SortState = { key: 'line', direction: 'asc' };

function matrixTabs(notice: number, archive: boolean): { id: MatrixTab; label: string }[] {
  const tabs: { id: MatrixTab; label: string }[] = [
    { id: 'ACTIVE', label: 'Active Products' },
    { id: 'EXPIRING', label: `Expiring Products (${notice} days)` },
  ];
  if (archive) {
    tabs.push({ id: 'EXPIRED', label: 'Expired Products' });
  }
  return tabs;
}

function matrixFilters(
  tab: MatrixTab,
  text: string,
  lineCode: string,
  productType: string,
  sort: SortState,
): MatrixFilters {
  return {
    tab,
    text: text || undefined,
    lineCode: lineCode || undefined,
    packaged: packagedFlag(productType),
    sort: sort.key,
    direction: sort.direction,
  };
}

/** The row actions the user may take: deactivation follows the setting PM_DEACTIVATION_ROUTE. */
function useMatrixActions(onDeactivate: (row: MatrixRow) => void): MatrixActions {
  const { can } = useAuth();
  const navigate = useNavigate();
  const mayDeactivate = can('PKG_REQUEST') || can('PRODUCT_MAINTAIN');
  const settings = useQuery({
    queryKey: ['pm-deactivation', 'settings'],
    queryFn: () => deactivationApi.settings(),
    enabled: mayDeactivate,
  });
  const open = (r: MatrixRow) => void navigate(`/catalog/products/${r.productCode}`);
  const deactivate = (row: MatrixRow) => {
    if (settings.data?.route === 'RETIRE_REQUEST') {
      void navigate(`/product-maintenance/requests/new?type=RETIRE&product=${row.productCode}`);
    } else {
      onDeactivate(row);
    }
  };
  return {
    view: open,
    update: can('PRODUCT_MAINTAIN') ? open : undefined,
    deactivate: mayDeactivate ? deactivate : undefined,
    openDeactivation: (r) =>
      void navigate(`/product-maintenance/deactivations?open=${r.deactivationId}`),
  };
}

function MatrixFilterFields({
  lineCode,
  productType,
  onChange,
}: Readonly<{
  lineCode: string;
  productType: string;
  onChange: (lineCode: string, productType: string) => void;
}>) {
  const lines = useQuery({
    queryKey: ['catalog', 'lines'],
    queryFn: () => catalogApi.lines(),
    staleTime: 10 * 60_000,
  });
  return (
    <div className="worklist-filters form-grid">
      <Field label="Line of Insurance">
        {(id) => (
          <Combobox
            id={id}
            emptyLabel="All"
            value={lineCode}
            options={(lines.data ?? []).map((l) => ({ value: l.code, label: l.name }))}
            onChange={(v) => onChange(v, productType)}
          />
        )}
      </Field>
      <Field label="Product Type">
        {(id) => (
          <Combobox
            id={id}
            emptyLabel="All"
            value={productType}
            options={PACKAGE_TYPES}
            onChange={(v) => onChange(lineCode, v)}
          />
        )}
      </Field>
    </div>
  );
}

/**
 * The Product Matrix tab of the Product Maintenance landing page (BDOI FRS FRPM.002.02 and
 * FRPM.003.01): Active, Expiring (within 90 days) and Expired Products with search, filters, sort,
 * pagination and export; Add New Package, Update Package and Deactivate Package for authorized
 * users.
 */
export function ProductMatrixTab() {
  const { can } = useAuth();
  const companyId = useCompanyId();
  const [tab, setTab] = useState<MatrixTab>('ACTIVE');
  const [text, setText] = useState('');
  const [lineCode, setLineCode] = useState('');
  const [productType, setProductType] = useState('');
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [sort, setSort] = useState<SortState>(DEFAULT_SORT);
  const [page, setPage] = useState(0);
  const [adding, setAdding] = useState<ProductForm | null>(null);
  const [deactivating, setDeactivating] = useState<MatrixRow | null>(null);
  const filters = matrixFilters(tab, text, lineCode, productType, sort);
  const matrix = useQuery({
    queryKey: ['pm-matrix', filters, page],
    queryFn: () => pmWorkspaceApi.matrix(filters, page),
  });
  const tabs = matrixTabs(matrix.data?.noticeDays ?? 90, can('PRODUCT_ARCHIVE_VIEW'));
  const columns = matrixColumns(useMatrixActions(setDeactivating));
  return (
    <>
      <Tabs
        tabs={tabs}
        active={tab}
        onChange={(t) => {
          setTab(t);
          setPage(0);
        }}
      />
      <WorklistToolbar
        placeholder="Search Package Name or Risk Code"
        onSearch={(value) => {
          setText(value);
          setPage(0);
        }}
        filters={{ open: filtersOpen, onToggle: () => setFiltersOpen(!filtersOpen) }}
      >
        <ExportButtons report="PM-MATRIX" params={matrixExportParams(companyId, filters)} />
        {can('PRODUCT_MAINTAIN') && (
          <Button
            variant="accent"
            icon={<Plus size={16} />}
            onClick={() => setAdding({ ...newProductForm(), packaged: true })}
          >
            Add New Package
          </Button>
        )}
      </WorklistToolbar>
      {filtersOpen && (
        <MatrixFilterFields
          lineCode={lineCode}
          productType={productType}
          onChange={(line, type) => {
            setLineCode(line);
            setProductType(type);
            setPage(0);
          }}
        />
      )}
      <ErrorAlert error={matrix.error} />
      <DataTable<MatrixRow>
        loading={matrix.isLoading}
        rows={matrix.data?.page.content ?? []}
        rowKey={(r) => r.productCode}
        sort={sort}
        onSort={(next) => {
          setSort(next);
          setPage(0);
        }}
        columns={columns}
        emptyMessage="No packages in this tab."
      />
      <PageFooter data={matrix.data?.page} noun="packages" onPage={setPage} />
      {adding !== null && <ProductEditorModal initial={adding} onClose={() => setAdding(null)} />}
      {deactivating !== null && (
        <DeactivateDialog
          productCode={deactivating.productCode}
          packageName={deactivating.packageName}
          expiryDate={deactivating.expiryDate}
          onClose={() => setDeactivating(null)}
        />
      )}
    </>
  );
}
