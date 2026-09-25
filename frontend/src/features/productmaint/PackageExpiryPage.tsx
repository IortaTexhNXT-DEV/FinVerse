import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CalendarRange, RefreshCcw } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { productMaintApi } from '@/api/productmaint';
import type { ExpiryRow } from '@/api/productmaint';
import { useAuth } from '@/auth/authContext';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { NumberInput } from '@/features/assets/FormControls';
import { formatDate } from '@/utils/format';
import { expiryRows, expiryTone } from './packageRequest';
import type { ExpiryTab } from './packageRequest';

const TABS: { id: ExpiryTab; label: string }[] = [
  { id: 'expiring', label: 'Expiring' },
  { id: 'renewal', label: 'Renewal in Progress' },
  { id: 'expired', label: 'Expired' },
];

const DEFAULT_WITHIN = 90;

const COLUMNS: Column<ExpiryRow>[] = [
  {
    key: 'product',
    header: 'Package',
    render: (r) => (
      <>
        <strong className="mono">{r.productCode}</strong> v{r.versionNo}
        <div className="muted">{r.productName}</div>
      </>
    ),
  },
  { key: 'end', header: 'Package End', render: (r) => formatDate(r.packageEndDate) },
  {
    key: 'days',
    header: 'Days Left',
    numeric: true,
    render: (r) => <span className={`badge ${expiryTone(r.daysLeft)}`}>{r.daysLeft}</span>,
  },
  { key: 'anniversary', header: 'Anniversary', render: (r) => formatDate(r.anniversaryDate) },
  {
    key: 'renewal',
    header: 'Renewal Request',
    render: (r) =>
      r.renewalRequestId === undefined ? (
        '—'
      ) : (
        <span className="row">
          <Link to={`/product-maintenance/requests/${r.renewalRequestId}`}>
            {r.renewalRequestNo}
          </Link>
          {r.renewalStage && <StatusBadge status={r.renewalStage} />}
        </span>
      ),
  },
];

function ExpiryListDialog({
  within,
  onClose,
  onApply,
}: Readonly<{ within: number; onClose: () => void; onApply: (days: number) => void }>) {
  const [days, setDays] = useState<number | undefined>(within);
  return (
    <Modal
      open
      title="Generate Expiry List"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="accent"
            disabled={days === undefined || days < 0}
            onClick={() => onApply(days ?? DEFAULT_WITHIN)}
          >
            Generate
          </Button>
        </>
      }
    >
      <NumberInput
        label="Packages ending within (days)"
        step="1"
        hint="From today; the monitor alerts at 60, 30 and 7 days (PACKAGE_EXPIRY_NOTICE_DAYS)."
        value={days}
        onChange={setDays}
      />
    </Modal>
  );
}

/**
 * Package Expiry (BRPM.017, BRPM.006): released packages by end date with their renewal status;
 * the bulk "Generate Renewal Request" drafts RENEW requests pre-filled from the version in force;
 * "Generate Expiry List" changes the look-ahead. Expired packages stay readable on the Products
 * screen.
 */
export default function PackageExpiryPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [params] = useSearchParams();
  const [within, setWithin] = useState(Number(params.get('within') ?? DEFAULT_WITHIN));
  const [tab, setTab] = useState<ExpiryTab>('expiring');
  const [dialog, setDialog] = useState(false);
  const selection = useRowSelection();
  const list = useQuery({
    queryKey: ['package-expiry', companyId, within],
    queryFn: () => productMaintApi.expiry(companyId, within),
    enabled: companyId > 0,
  });
  const rows = expiryRows(list.data ?? [], tab);
  const renew = useMutation({
    mutationFn: () => productMaintApi.renew(companyId, selection.keys),
    onSuccess: async (results) => {
      selection.clear();
      await queryClient.invalidateQueries({ queryKey: ['package-expiry'] });
      await queryClient.invalidateQueries({ queryKey: ['package-requests'] });
      const created = results.filter((r) => r.created).length;
      toast.success(`${created} renewal request(s) drafted`);
      if (results.length === 1 && results[0] !== undefined) {
        void navigate(`/product-maintenance/requests/${results[0].requestId}`);
      }
    },
  });
  const columns =
    tab === 'expiring' && can('PKG_NEGOTIATE')
      ? [
          selectionColumn<ExpiryRow>(
            rows,
            (r) => r.productCode,
            selection,
            (r) => r.productCode,
          ),
          ...COLUMNS,
        ]
      : COLUMNS;
  return (
    <div className="stack">
      <PageHeader
        section="Product Maintenance"
        title="Package Expiry"
        description={`Released packages ending within ${within} days, with their renewal status.`}
        actions={
          <Button
            variant="secondary"
            icon={<CalendarRange size={16} />}
            onClick={() => setDialog(true)}
          >
            Generate Expiry List
          </Button>
        }
      />
      <Card flush>
        <Tabs
          tabs={TABS}
          active={tab}
          onChange={(t) => {
            setTab(t);
            selection.clear();
          }}
        />
        <div className="worklist-toolbar">
          <span className="muted">{rows.length} package(s)</span>
          {tab === 'expiring' && can('PKG_NEGOTIATE') && (
            <div className="worklist-actions">
              <Button
                variant="accent"
                icon={<RefreshCcw size={16} />}
                disabled={selection.keys.length === 0}
                busy={renew.isPending}
                onClick={() => renew.mutate()}
              >
                Generate Renewal Request
              </Button>
            </div>
          )}
        </div>
        <ErrorAlert error={list.error ?? renew.error} />
        {tab === 'expired' ? (
          <div className="stack">
            <EmptyState message="Expired packages are archived in the catalog" />
            <p className="muted">
              Open <Link to="/catalog/products">Products</Link> with the Expired filter to read an
              expired package and its version history; reactivation is a REACTIVATE request.
            </p>
          </div>
        ) : (
          <DataTable<ExpiryRow>
            loading={list.isLoading}
            rows={rows}
            rowKey={(r) => r.productCode}
            columns={columns}
          />
        )}
      </Card>
      {dialog && (
        <ExpiryListDialog
          within={within}
          onClose={() => setDialog(false)}
          onApply={(days) => {
            setWithin(days);
            setDialog(false);
          }}
        />
      )}
    </div>
  );
}
