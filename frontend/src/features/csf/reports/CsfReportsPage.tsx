import { useQuery } from '@tanstack/react-query';
import { FileBarChart2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import { reportApi } from '@/api/reports';
import { Card } from '@/components/ui/Card';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { FORMAT_LABELS, menuFormats } from '@/features/reports/exportFormats';
import { CSF_SECTION } from '../csfCodes';
import { LoadingPanel } from '@/components/ui/LoadingPanel';

/** Report category of the CSF reports (ReportCategory.CUSTOMER_SERVICE). */
export const CSF_CATEGORY = 'CUSTOMER_SERVICE';

/**
 * Customer Service reports (FR-CSF-041, 042; BRCSF-011 / 11.002): the Report Centre filtered to
 * the Customer Service category - Contact Changes and Agent Activity - in PDF, Excel or CSV. The
 * audit trail of client records is in Administration, Audit Trail.
 */
export default function CsfReportsPage() {
  const catalogue = useQuery({ queryKey: ['report-catalogue'], queryFn: reportApi.catalogue });
  const reports = (catalogue.data ?? []).filter((e) => e.category === CSF_CATEGORY);
  return (
    <div className="stack">
      <PageHeader
        section={CSF_SECTION}
        title="Customer Service Reports"
        description="Contact changes and agent activity for the leads and heads."
      />
      <ErrorAlert error={catalogue.error} />
      {catalogue.isLoading && <LoadingPanel />}
      <Card title="Customer Service">
        {reports.length === 0 && !catalogue.isLoading ? (
          <EmptyState message="No Customer Service reports available to you" />
        ) : (
          <div className="report-grid">
            {reports.map((e) => (
              <Link key={e.code} to={`/reports/${e.code}`} className="report-card">
                <FileBarChart2 size={20} aria-hidden="true" />
                <span>
                  <strong>{e.title}</strong>
                  <span className="muted">
                    {e.code} · {e.description}
                  </span>
                  <span className="muted">
                    {menuFormats(e, ['XLSX', 'PDF', 'CSV'])
                      .map((f) => FORMAT_LABELS[f])
                      .join(' · ')}
                  </span>
                </span>
              </Link>
            ))}
          </div>
        )}
      </Card>
    </div>
  );
}
