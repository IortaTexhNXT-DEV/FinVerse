import { EmptyState } from '@/components/ui/EmptyState';
import { PageHeader } from '@/components/ui/PageHeader';

/**
 * Placeholder of the pre-registered Product Maintenance (BRD-3) package request screens. The
 * routes are hidden from the menu until the productmaint screens replace this component
 * (PRODUCT_MAINTENANCE_DESIGN section 11, wave P1-B).
 */
export default function ProductMaintenancePlaceholder() {
  return (
    <div className="stack">
      <PageHeader
        section="Product Maintenance"
        title="Package Requests"
        description="Package requests, negotiation with insurers, ManCom sign-off, set-up and expiry monitoring."
      />
      <EmptyState message="This screen is not available yet." />
    </div>
  );
}
