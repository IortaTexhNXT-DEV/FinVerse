import { EmptyState } from '@/components/ui/EmptyState';
import { PageHeader } from '@/components/ui/PageHeader';

/**
 * Placeholder of the pre-registered Product Maintenance catalog screens (version editor,
 * validation queue, coverages and clauses, incentive criteria). The routes are hidden from the
 * menu until the catalog screens replace this component (PRODUCT_MAINTENANCE_DESIGN section 11,
 * wave P1-A).
 */
export default function CatalogPlaceholder() {
  return (
    <div className="stack">
      <PageHeader
        section="Product Maintenance"
        title="Product Maintenance"
        description="Package versions, validation, coverages, clauses and incentive criteria."
        backTo="/catalog/products"
      />
      <EmptyState message="This screen is not available yet." />
    </div>
  );
}
