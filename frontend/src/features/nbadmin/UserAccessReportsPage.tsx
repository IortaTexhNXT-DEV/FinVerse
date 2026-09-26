import { FileBarChart } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Card } from '@/components/ui/Card';
import { PageHeader } from '@/components/ui/PageHeader';
import { USER_ACCESS_REPORTS } from './userAccessReports';
import './userAccessReports.css';

/**
 * User Access Reports (BRD 1.008, 3.003.1-3, 4.003.1; FRS BRD-11 section 6): the five reports of
 * user access, each opened in the report runner with its parameters and PDF, Excel and CSV exports.
 */
export default function UserAccessReportsPage() {
  return (
    <div className="stack">
      <PageHeader
        section="User Access"
        title="User Access Reports"
        description="Who has which access, who granted it and every access activity; each report opens in the report runner with PDF, Excel and CSV exports."
      />
      <Card title="Reports">
        <ul className="uam-reports">
          {USER_ACCESS_REPORTS.map((r) => (
            <li key={r.code}>
              <FileBarChart size={20} aria-hidden="true" />
              <div>
                <h3>
                  <Link to={`/reports/${r.code}`}>{r.name}</Link>
                </h3>
                <p className="uam-report-text">{r.text}</p>
                <div className="uam-report-meta">
                  {r.code} · {r.brd} · Parameters: {r.parameters}
                </div>
              </div>
            </li>
          ))}
        </ul>
      </Card>
    </div>
  );
}
