import { useQuery } from '@tanstack/react-query';
import { Upload } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { renewalApi } from '@/api/renewal';
import type { LamdLineView, LamdReportView } from '@/api/renewal';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { MonthInput } from '@/components/ui/MonthInput';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatMonth } from '@/utils/dateText';
import { formatDate, formatDateTime } from '@/utils/format';
import { RENEWAL_SECTION } from '../common/renewalCodes';
import { UploadPanel } from '../common/UploadPanel';
import '../renewal.css';

const TYPES: Record<string, string> = { PAID_OFF: 'Paid-off loans', RMU: 'RMU (past due)' };

function Lines({ reportNo }: Readonly<{ reportNo: string }>) {
  const companyId = useCompanyId();
  const lines = useQuery({
    queryKey: ['renewal', 'lamd-lines', companyId, reportNo],
    queryFn: () => renewalApi.lamdLines(companyId, reportNo),
  });
  return (
    <Card title={`Lines of ${reportNo}`} flush>
      <ErrorAlert error={lines.error} />
      <DataTable<LamdLineView>
        loading={lines.isLoading}
        rows={lines.data ?? []}
        rowKey={(l) => l.rowNo}
        emptyMessage="No lines"
        columns={[
          { key: 'row', header: 'Row', kind: 'amount', render: (l) => l.rowNo },
          { key: 'pn', header: 'PN', kind: 'code', render: (l) => l.pnNo },
          { key: 'borrower', header: 'Borrower', render: (l) => l.borrower ?? '' },
          {
            key: 'status',
            header: 'Loan Status',
            kind: 'status',
            render: (l) => <StatusBadge status={l.status} />,
          },
          {
            key: 'date',
            header: 'Status date',
            kind: 'date',
            render: (l) => formatDate(l.statusDate),
          },
          {
            key: 'match',
            header: 'Match',
            kind: 'status',
            render: (l) => <StatusBadge status={l.match} />,
          },
          {
            key: 'ref',
            header: 'Renewal',
            render: (l) =>
              l.renewalRef === null ? (
                ''
              ) : (
                <Link to={`/renewal/candidates/${encodeURIComponent(l.renewalRef)}`}>
                  {l.renewalRef}
                </Link>
              ),
          },
          { key: 'msg', header: 'Outcome', render: (l) => l.message ?? '' },
        ]}
      />
    </Card>
  );
}

/**
 * LAMD Reports (FR-RN-025): the monthly paid-off and RMU reports of the Loan and Mortgage
 * department. Each line is matched to the renewals by PN; a paid-off loan proposes Not for
 * Renewal, a past-due loan routes the renewal to the Team Leader.
 */
export default function LamdPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [upload, setUpload] = useState(false);
  const [type, setType] = useState('PAID_OFF');
  const [period, setPeriod] = useState('');
  const [open, setOpen] = useState<string>();
  const reports = useQuery({
    queryKey: ['renewal', 'lamd', companyId],
    queryFn: () => renewalApi.lamdReports(companyId),
    enabled: companyId > 0,
  });
  return (
    <div className="stack">
      <PageHeader
        section={RENEWAL_SECTION}
        title="LAMD Reports"
        description="Paid-off and past-due loan reports matched to the renewals."
        actions={
          can('RNW_LAMD_UPLOAD') && (
            <Button variant="secondary" icon={<Upload size={16} />} onClick={() => setUpload(true)}>
              Upload LAMD Report
            </Button>
          )
        }
      />
      {upload && (
        <UploadPanel
          label="Upload LAMD Report"
          handler="RNW_LAMD_REPORT"
          parameters={{ reportType: type, period }}
          parametersReady={/^\d{4}-(0[1-9]|1[0-2])$/.test(period)}
          parameterFields={
            <div className="form-grid">
              <Field label="Report" required>
                {(id) => (
                  <select
                    id={id}
                    className="select"
                    value={type}
                    onChange={(e) => setType(e.target.value)}
                  >
                    {Object.entries(TYPES).map(([code, label]) => (
                      <option key={code} value={code}>
                        {label}
                      </option>
                    ))}
                  </select>
                )}
              </Field>
              <Field label="Month" required>
                {(id) => (
                  <MonthInput
                    id={id}
                    className="input"
                    value={period}
                    onChange={(e) => setPeriod(e.target.value)}
                  />
                )}
              </Field>
            </div>
          }
          onClose={() => setUpload(false)}
        />
      )}
      <ErrorAlert error={reports.error} onRetry={() => void reports.refetch()} />
      <Card flush>
        <DataTable<LamdReportView>
          loading={reports.isLoading}
          rows={reports.data ?? []}
          rowKey={(r) => r.reportNo}
          selectedKey={open}
          onRowClick={(r) => setOpen(r.reportNo)}
          emptyMessage="No LAMD reports uploaded"
          columns={[
            { key: 'no', header: 'Report', kind: 'code', render: (r) => r.reportNo },
            { key: 'type', header: 'Type', render: (r) => TYPES[r.type] ?? r.type },
            { key: 'period', header: 'Month', render: (r) => formatMonth(r.period) },
            { key: 'lines', header: 'Lines', kind: 'amount', render: (r) => r.lines },
            { key: 'matched', header: 'Matched', kind: 'amount', render: (r) => r.matched },
            { key: 'by', header: 'Uploaded by', render: (r) => <UserName login={r.by} /> },
            {
              key: 'at',
              header: 'Uploaded',
              kind: 'datetime',
              render: (r) => formatDateTime(r.at),
            },
          ]}
        />
      </Card>
      {open !== undefined && <Lines reportNo={open} />}
    </div>
  );
}
