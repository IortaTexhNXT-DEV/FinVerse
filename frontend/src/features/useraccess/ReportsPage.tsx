import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FileBarChart, FileDown } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { saveFile } from '@/api/client';
import { userAccessApi } from '@/api/userAccess';
import type { CertificationLogEntry } from '@/api/userAccess';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { USER_ACCESS_REPORTS } from '@/features/nbadmin/userAccessReports';
import { formatDateTime } from '@/utils/format';
import '@/features/nbadmin/userAccessReports.css';

type Format = 'CSV' | 'XLSX';

function CertificationCard() {
  const toast = useToast();
  const queries = useQueryClient();
  const [format, setFormat] = useState<Format | null>(null);
  const log = useQuery({ queryKey: ['uam', 'extracts'], queryFn: userAccessApi.extracts });
  const take = useMutation({
    mutationFn: (f: Format) => userAccessApi.takeExtract(f),
    onSuccess: async (file) => {
      saveFile(file.blob, file.fileName);
      toast.success(`${file.fileName} saved; Information Security is told`);
      setFormat(null);
      await queries.invalidateQueries({ queryKey: ['uam', 'extracts'] });
    },
  });
  return (
    <Card
      title="Certification Extract"
      actions={
        <>
          <Button
            variant="secondary"
            icon={<FileDown size={16} />}
            onClick={() => setFormat('CSV')}
          >
            Extract CSV
          </Button>
          <Button variant="accent" icon={<FileDown size={16} />} onClick={() => setFormat('XLSX')}>
            Extract XLSX
          </Button>
        </>
      }
    >
      <div className="stack">
        <p className="muted" style={{ margin: 0 }}>
          Every account with its status, each group profile it holds and each permission the profile
          grants, for the periodic access certification. Each extract is logged with its rows and
          SHA-256, and Information Security is told.
        </p>
        <ErrorAlert error={log.error} />
        <DataTable<CertificationLogEntry>
          caption="Extracts taken"
          loading={log.isLoading}
          rows={log.data ?? []}
          rowKey={(e) => `${e.takenAt}-${e.sha256}`}
          emptyMessage="No extract has been taken"
          columns={[
            {
              key: 't',
              header: 'Taken',
              kind: 'datetime',
              render: (e) => formatDateTime(e.takenAt),
            },
            { key: 'b', header: 'Taken By', render: (e) => <UserName login={e.takenBy} /> },
            { key: 'f', header: 'File', render: (e) => e.fileName },
            { key: 'r', header: 'Rows', numeric: true, render: (e) => e.rows },
            { key: 's', header: 'SHA-256', render: (e) => <code>{e.sha256}</code> },
          ]}
        />
      </div>
      {format !== null && (
        <ConfirmDialog
          title={`Take a certification extract (${format})`}
          effect="The file lists every account, group profile and permission. The extract is logged and Information Security is told."
          confirmLabel="Take Extract"
          busy={take.isPending}
          error={take.error}
          onConfirm={() => take.mutate(format)}
          onClose={() => setFormat(null)}
        />
      )}
    </Card>
  );
}

/**
 * Reports & Certification: the user access reports, each opened in the report runner with its
 * parameters and exports, and the certification extract with the log of the extracts taken.
 */
export default function ReportsPage() {
  return (
    <div className="stack">
      <PageHeader
        section="User Access"
        title="Reports & Certification"
        description="Who has which access, who granted it, every access activity, and the certification extract."
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
                <div className="uam-report-meta">Parameters: {r.parameters}</div>
              </div>
            </li>
          ))}
        </ul>
      </Card>
      <CertificationCard />
    </div>
  );
}
