import { useQuery } from '@tanstack/react-query';
import { FileSearch } from 'lucide-react';
import { Link } from 'react-router-dom';
import { opsApi } from '@/api/operations';
import type { OpsSection } from '@/api/operations';
import { Card } from '@/components/ui/Card';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId } from '@/context/workspaceContext';
import { WorkCountTiles } from './OpsParts';
import { SECTION_LABELS } from './opsLabels';

interface SectionHomeProps {
  section: OpsSection;
  description: string;
}

/**
 * Landing screen of an Operations team section (cashiering, remittance, prodrecon, adjustment,
 * commission): the section's work counts from the Operations home and the invoice search. Each
 * Operations module replaces it with its own workbench.
 */
export function OperationsSectionHome({ section, description }: Readonly<SectionHomeProps>) {
  const companyId = useCompanyId();
  const home = useQuery({
    queryKey: ['ops', 'home', companyId],
    queryFn: () => opsApi.home(companyId),
    enabled: companyId > 0,
  });
  const counts = home.data?.sections.find((s) => s.section === section)?.counts ?? [];
  const title = SECTION_LABELS[section];
  return (
    <div className="stack">
      <PageHeader
        section="Operations"
        title={title}
        description={description}
        actions={
          <Link className="btn btn-secondary" to="/operations/invoices">
            <FileSearch size={16} aria-hidden="true" /> Search Invoices
          </Link>
        }
      />
      <ErrorAlert error={home.error} />
      <Card title="Work Queues">
        {home.isLoading && <span className="spinner" aria-label="Loading" />}
        {!home.isLoading && counts.length === 0 && <EmptyState message="No queues to show yet" />}
        {counts.length > 0 && <WorkCountTiles counts={counts} label={`${title} work queues`} />}
      </Card>
    </div>
  );
}
