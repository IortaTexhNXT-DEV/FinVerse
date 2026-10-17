import { useQuery } from '@tanstack/react-query';
import { FileBarChart2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import { reportApi } from '@/api/reports';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { receivablesReportGroups } from './receivablesMath';

/** Shortcut to the receivables, bank reconciliation and PDC reports of the report centre. */
export default function ReceivablesReportsPage() {
  const catalogue = useQuery({ queryKey: ['report-catalogue'], queryFn: reportApi.catalogue });
  const groups = receivablesReportGroups(catalogue.data ?? []);

  return (
    <div className="stack">
      <PageHeader
        section="Receivables & Banking"
        title="Receivables Reports"
        description="Debtors ageing, statements, cheque registers, bank reconciliation and PDC reports."
      />
      <ErrorAlert error={catalogue.error} />
      <div className="grid-2">
        {groups.map(([group, entries]) => (
          <Card key={group} title={group}>
            <ul className="stack" aria-label={group}>
              {entries.map((e) => (
                <li key={e.code} className="row">
                  <FileBarChart2 size={16} aria-hidden="true" />
                  <Link to={`/reports/${e.code}`}>{e.title}</Link>
                  <span className="muted">{e.code}</span>
                </li>
              ))}
            </ul>
          </Card>
        ))}
      </div>
    </div>
  );
}
