import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FilePlus2 } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { CertificateDialog } from './CertificateDialog';
import { commissionApi } from './commissionApi';
import type { Certificate, CertificateInput } from './commissionApi';

type StageTab = 'SUBMITTED' | 'REJECTED' | 'ACKNOWLEDGED';

const TABS: readonly { id: StageTab; label: string }[] = [
  { id: 'SUBMITTED', label: 'Submitted' },
  { id: 'REJECTED', label: 'Rejected' },
  { id: 'ACKNOWLEDGED', label: 'Acknowledged' },
];

const COLUMNS: Column<Certificate>[] = [
  {
    key: 'no',
    header: 'Submission No.',
    render: (c) => (
      <>
        <strong>{c.submissionNo}</strong>
        <div className="muted">
          {c.certificate.form} {c.certificate.number}
        </div>
      </>
    ),
  },
  { key: 'insurer', header: 'Insurer', render: (c) => c.insurerCode },
  {
    key: 'period',
    header: 'Period',
    render: (c) =>
      `${formatDate(c.certificate.periodFrom)} – ${formatDate(c.certificate.periodTo)}`,
  },
  {
    key: 'tax',
    header: 'Tax Withheld',
    numeric: true,
    render: (c) => <Amount value={c.certificate.taxWithheld} />,
  },
  { key: 'ors', header: 'ORs', numeric: true, render: (c) => c.certificate.receipts.length },
  {
    key: 'at',
    header: 'Submitted',
    render: (c) => `${formatDateTime(c.createdAt)} · ${c.createdBy}`,
  },
  { key: 'stage', header: 'Status', render: (c) => <StatusBadge status={c.stage} /> },
];

/**
 * BIR certificates (CMRID.015): certificates of the tax the insurers withheld on commission,
 * tagged to the official receipts, submitted to Comptrollership and acknowledged or rejected.
 */
export default function CertificatesPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<StageTab>('SUBMITTED');
  const [page, setPage] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const list = useQuery({
    queryKey: ['commission', 'certificates', companyId, tab, page],
    queryFn: () => commissionApi.certificates(companyId, tab, page),
    enabled: companyId > 0,
  });
  const submit = useMutation({
    mutationFn: (input: CertificateInput) => commissionApi.submitCertificate(input),
    onSuccess: async (c) => {
      setSubmitting(false);
      await queryClient.invalidateQueries({ queryKey: ['commission', 'certificates'] });
      toast.success(`${c.submissionNo} submitted`);
      void navigate(`/commission/certificates/${String(c.id)}`);
    },
  });
  return (
    <div className="stack">
      <PageHeader
        section="Commission Receivables"
        title="BIR Certificates"
        description="Certificates of tax withheld on commission, submitted to Comptrollership."
        actions={
          can('BIR_CERT_SUBMIT') ? (
            <Button icon={<FilePlus2 size={16} />} onClick={() => setSubmitting(true)}>
              Submit Certificate
            </Button>
          ) : undefined
        }
      />
      <ErrorAlert error={list.error} />
      <Card>
        <div className="stack">
          <Tabs
            tabs={TABS}
            active={tab}
            onChange={(t) => {
              setTab(t);
              setPage(0);
            }}
          />
          <DataTable
            caption="BIR certificate submissions"
            columns={COLUMNS}
            rows={list.data?.content ?? []}
            rowKey={(c) => c.id}
            loading={list.isLoading}
            onRowClick={(c) => void navigate(`/commission/certificates/${String(c.id)}`)}
            emptyMessage="No items to display"
          />
          <PageFooter data={list.data} noun="submissions" onPage={setPage} />
        </div>
      </Card>
      {submitting && (
        <CertificateDialog
          companyId={companyId}
          certificate={undefined}
          busy={submit.isPending}
          error={submit.error}
          onClose={() => setSubmitting(false)}
          onSave={(input) => submit.mutate(input)}
        />
      )}
    </div>
  );
}
