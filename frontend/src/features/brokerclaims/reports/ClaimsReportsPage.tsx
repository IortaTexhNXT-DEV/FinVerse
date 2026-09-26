import { useQuery } from '@tanstack/react-query';
import { FileBarChart2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import { reportApi } from '@/api/reports';
import { Card } from '@/components/ui/Card';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { FORMAT_LABELS, menuFormats } from '@/features/reports/exportFormats';
import { CLAIMS_SECTION } from '../ClaimsPlaceholder';

/** Report category of the Claims Handling reports (ReportCategory.CLAIMS_HANDLING). */
export const CLAIMS_CATEGORY = 'CLAIMS_HANDLING';

/**
 * Claims Handling reports (BRCLM.026-034/038/040-043; FR-CM-060-066): the Report Centre filtered to
 * the Claims Handling category. Each report runs on screen with BCL_REPORT_VIEW and downloads in
 * Excel, PDF or CSV with BCL_REPORT_EXPORT; the data extract needs BCL_DATA_EXTRACT.
 */
export default function ClaimsReportsPage() {
  const catalogue = useQuery({ queryKey: ['report-catalogue'], queryFn: reportApi.catalogue });
  const reports = (catalogue.data ?? []).filter((e) => e.category === CLAIMS_CATEGORY);
  return (
    <div className="stack">
      <PageHeader
        section={CLAIMS_SECTION}
        title="Claims Reports"
        description="Outstanding and past due claims, settled claims, ageing overall and per status, loss experience and loss ratio, pending actions, claims-prone locations, insurer claim numbers, the activity log and the data extract."
      />
      <ErrorAlert error={catalogue.error} />
      {catalogue.isLoading && <span className="spinner" aria-label="Loading" />}
      <Card title="Claims Handling">
        {reports.length === 0 && !catalogue.isLoading ? (
          <EmptyState message="No Claims Handling reports available to you" />
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
                    {e.exportable === false
                      ? 'View only'
                      : menuFormats(e, ['XLSX', 'PDF', 'CSV'])
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
