import type { ReactNode } from 'react';
import { Card } from '@/components/ui/Card';
import { EmptyState } from '@/components/ui/EmptyState';
import { PageHeader } from '@/components/ui/PageHeader';

interface SectionLandingProps {
  /** Sidebar group shown as the breadcrumb (e.g. "Finance"). */
  section: string;
  title: string;
  description: string;
  /** Title of the work card. */
  cardTitle?: string;
  /** Message while the section has no work to show. */
  emptyMessage: string;
  /** Page actions on the right of the header. */
  actions?: ReactNode;
  /** Work tiles or other content; the empty state is shown when absent. */
  children?: ReactNode;
}

/**
 * Landing screen of a module section registered by a foundation wave (Collections, Disbursement,
 * Refund & Cash Advance Requests, ACSL, Report Pack): page header with the section breadcrumb and
 * a work card that shows the empty state until the module adds its queues. Each module replaces
 * the content with its own home.
 */
export function SectionLanding({
  section,
  title,
  description,
  cardTitle = 'Work Queues',
  emptyMessage,
  actions,
  children,
}: Readonly<SectionLandingProps>) {
  return (
    <div className="stack">
      <PageHeader section={section} title={title} description={description} actions={actions} />
      <Card title={cardTitle}>{children ?? <EmptyState message={emptyMessage} />}</Card>
    </div>
  );
}
