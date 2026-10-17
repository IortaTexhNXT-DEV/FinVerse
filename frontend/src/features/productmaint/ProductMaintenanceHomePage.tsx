import { BarChart3, Plus } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { Card } from '@/components/ui/Card';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { useTabParam } from '@/components/ui/useTabParam';
import { PackageRequestListTab } from './workspace/PackageRequestListTab';
import { ProductMatrixTab } from './workspace/ProductMatrixTab';
import { QuotationRequestsTab } from './workspace/QuotationRequestsTab';

const TABS = [
  { id: 'matrix', label: 'Product Matrix' },
  { id: 'quotations', label: 'Quotation Request List' },
  { id: 'packages', label: 'Package Request List' },
] as const;

type LandingTab = (typeof TABS)[number]['id'];

const TAB_IDS: readonly LandingTab[] = TABS.map((t) => t.id);

/**
 * The Product Maintenance landing page (BDOI FRS FRPM.002.01 and FRPM.002.02): the Product Matrix
 * with its Active, Expiring and Expired Products, the Quotation Request List and the Package Request
 * List; the dashboard opens from the header.
 */
export default function ProductMaintenanceHomePage() {
  const { can } = useAuth();
  const [tab, setTab] = useTabParam<LandingTab>(TAB_IDS, 'matrix');
  return (
    <div className="stack">
      <PageHeader
        section="Product Maintenance"
        title="Product Maintenance Home"
        description="Packages and products, quotation requests and package requests in one place."
        actions={
          <>
            <Link className="btn btn-secondary" to="/product-maintenance/dashboard">
              <BarChart3 size={16} aria-hidden="true" /> Dashboard
            </Link>
            {can('PKG_REQUEST') && (
              <Link className="btn btn-accent" to="/product-maintenance/requests/new">
                <Plus size={16} aria-hidden="true" /> Create Package Request
              </Link>
            )}
          </>
        }
      />
      <Card flush>
        <Tabs tabs={TABS} active={tab} onChange={setTab} />
        {tab === 'matrix' && <ProductMatrixTab />}
        {tab === 'quotations' && <QuotationRequestsTab />}
        {tab === 'packages' && <PackageRequestListTab />}
      </Card>
    </div>
  );
}
