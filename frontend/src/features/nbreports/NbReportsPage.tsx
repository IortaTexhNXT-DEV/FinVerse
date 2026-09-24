import { useQuery } from '@tanstack/react-query';
import { FileBarChart2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import { reportApi } from '@/api/reports';
import { Card } from '@/components/ui/Card';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { groupReports } from './reportGroups';
import './nbreports.css';

/**
 * New Business reports (BRNB.011/057/075/078): the NB reports of the Report Centre the user may
 * run, grouped by process step. Each report runs on screen, exports to PDF, Excel, ODS, CSV or XML
 * and keeps saved variants of its parameters.
 */
export default function NbReportsPage() {
  const catalogue = useQuery({ queryKey: ['report-catalogue'], queryFn: reportApi.catalogue });
  const groups = groupReports(catalogue.data ?? []);
  return (
    <div className="stack">
      <PageHeader
        section="Reports"
        title="New Business Reports"
        description="Operational reports from quotation to booking. Run a report on screen, save your filters as a variant, print it or download it in PDF, Excel, ODS, CSV or XML."
        actions={
          <Link className="btn btn-secondary" to="/reports">
            <FileBarChart2 size={16} aria-hidden="true" /> Report Centre
          </Link>
        }
      />
      <ErrorAlert error={catalogue.error} />
      {catalogue.isLoading && <span className="spinner" aria-label="Loading" />}
      {catalogue.data !== undefined && groups.length === 0 && (
        <Card>
          <EmptyState message="No New Business report is available to your role" />
        </Card>
      )}
      {groups.map((g) => (
        <Card key={g.title} title={g.title}>
          <div className="report-grid">
            {g.reports.map((r) => (
              <Link key={r.code} to={`/reports/${r.code}`} className="report-card">
                <FileBarChart2 size={20} aria-hidden="true" />
                <span>
                  <strong>{r.title}</strong>
                  <span className="muted">
                    {r.code} · {r.description}
                  </span>
                </span>
              </Link>
            ))}
          </div>
        </Card>
      ))}
    </div>
  );
}
