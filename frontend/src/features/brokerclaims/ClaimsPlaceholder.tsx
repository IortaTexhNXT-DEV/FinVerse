import type { ReactNode } from 'react';
import { Card } from '@/components/ui/Card';
import { EmptyState } from '@/components/ui/EmptyState';
import { PageHeader } from '@/components/ui/PageHeader';

/** Breadcrumb of every Claims Handling screen (group Claims & Insurance). */
export const CLAIMS_SECTION = 'Claims & Insurance · Claims Handling';

interface ClaimsPlaceholderProps {
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
 * Screen of the Claims Handling section declared by the foundation (CL0) before its wave builds it
 * (CLAIMS_BROKING_DESIGN section 14): page header with the section breadcrumb and a work card with
 * the empty state. Waves CL1-A and CL1-B replace their own pages; the route stays the same.
 */
export function ClaimsPlaceholder({
  title,
  description,
  emptyMessage,
  cardTitle = 'Claims',
  backTo,
  actions,
}: Readonly<ClaimsPlaceholderProps>) {
  return (
    <div className="stack">
      <PageHeader
        section={CLAIMS_SECTION}
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
