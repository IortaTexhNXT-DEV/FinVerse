import type { ReactNode } from 'react';
import { Card } from '@/components/ui/Card';
import { EmptyState } from '@/components/ui/EmptyState';
import { PageHeader } from '@/components/ui/PageHeader';

/** Breadcrumb of every Employee Benefits screen (group Client & Policy). */
export const EB_SECTION = 'Client & Policy · Employee Benefits';

interface EbPlaceholderProps {
  title: string;
  description: string;
  /** Message of the work card until the screen is built. */
  emptyMessage: string;
  /** Title of the work card. */
  cardTitle?: string;
  /** Back link of a record screen. */
  backTo?: string;
  /** Page actions on the right of the header. */
  actions?: ReactNode;
}

/**
 * Screen of the Employee Benefits section declared by the foundation (E0) before its wave builds it
 * (EMPLOYEE_BENEFITS_DESIGN section 13): page header with the section breadcrumb and a work card
 * with the empty state. Waves E1-B and E1-C replace their own pages; the route stays the same.
 */
export function EbPlaceholder({
  title,
  description,
  emptyMessage,
  cardTitle = 'Employee Benefits',
  backTo,
  actions,
}: Readonly<EbPlaceholderProps>) {
  return (
    <div className="stack">
      <PageHeader
        section={EB_SECTION}
        title={title}
        description={description}
        backTo={backTo}
        actions={actions}
      />
      <Card title={cardTitle}>
        <EmptyState message={emptyMessage} />
      </Card>
    </div>
  );
}
